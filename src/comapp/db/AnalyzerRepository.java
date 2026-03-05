package comapp.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import comapp.ConfigServlet;

public class AnalyzerRepository {

    private static final Logger log = Logger.getLogger("comapp");

    private Connection getConnection() throws Exception {
        Properties props = ConfigServlet.getProperties();
        String url = props.getProperty("db.url");
        String user = props.getProperty("db.user");
        String pwd = props.getProperty("db.pwd");
        Class.forName("org.postgresql.Driver");
        return java.sql.DriverManager.getConnection(url, user, pwd);
    }

    public Map<String, Object> searchCallsInDatabase(
            String startDate, String endDate,
            String ani, String dnis,
            String conversationId, String queue, String operator,
            List<String> userGroups, boolean enableGroupFilter,
            int pageNumber, int pageSize, String order) {

        log.info("AnalyzerRepository.searchCallsInDatabase() - ENTRY - "
                + "startDate=" + startDate
                + ", endDate=" + endDate
                + ", ani=" + ani
                + ", dnis=" + dnis
                + ", conversationId=" + conversationId
                + ", queue=" + queue
                + ", operator=" + operator
                + ", userGroups=" + userGroups
                + ", enableGroupFilter=" + enableGroupFilter
                + ", pageNumber=" + pageNumber
                + ", pageSize=" + pageSize
                + ", order=" + order);

        List<Map<String, String>> results = new ArrayList<>();
        int totalCount = 0;

        boolean hasStartDate      = (startDate != null && !startDate.isEmpty());
        boolean hasEndDate        = (endDate != null && !endDate.isEmpty());
        boolean hasConversationId = (conversationId != null && !conversationId.isEmpty());
        boolean hasAni            = (ani != null && !ani.isEmpty());
        boolean hasDnis           = (dnis != null && !dnis.isEmpty());
        boolean hasQueue          = (queue != null && !queue.isEmpty());
        boolean hasOperator       = (operator != null && !operator.isEmpty());
        boolean hasGroupFilter    = (enableGroupFilter && userGroups != null && !userGroups.isEmpty());
        boolean needsParticipantJoin = hasAni || hasDnis || hasQueue || hasOperator || hasGroupFilter;

        List<String> countParams = new ArrayList<>();
        StringBuilder countSql = new StringBuilder();

        if (!needsParticipantJoin) {
            countSql.append("SELECT COUNT(*) AS total FROM conversations c WHERE 1=1 ");
        } else {
            countSql.append("SELECT COUNT(DISTINCT c.conversationid) AS total ");
            countSql.append("FROM conversations c ");
            countSql.append("JOIN participants p ON c.conversationid = p.conversationid ");
            if (hasQueue || hasGroupFilter) {
                countSql.append("LEFT JOIN conf_queue cq ON p.queueid = cq.id ");
            }
            if (hasOperator) {
                countSql.append("LEFT JOIN conf_user cu ON p.userid = cu.id ");
            }
            countSql.append("WHERE 1=1 ");
        }
        if (hasStartDate) {
            countSql.append("AND c.conversationstart >= ? ");
            countParams.add(startDate);
        }
        if (hasEndDate) {
            countSql.append("AND c.conversationstart <= ? ");
            countParams.add(endDate);
        }
        if (hasConversationId) {
            countSql.append("AND c.conversationid = ? ");
            countParams.add(conversationId);
        }
        if (hasAni) {
            countSql.append("AND p.ani ILIKE ? ");
            countParams.add("%" + ani + "%");
        }
        if (hasDnis) {
            countSql.append("AND p.dnis ILIKE ? ");
            countParams.add("%" + dnis + "%");
        }
        if (hasQueue) {
            countSql.append("AND cq.name ILIKE ? ");
            countParams.add("%" + queue + "%");
        }
        if (hasOperator) {
            countSql.append("AND cu.name ILIKE ? ");
            countParams.add("%" + operator + "%");
        }
        if (hasGroupFilter) {
            StringBuilder placeholders = new StringBuilder();
            for (int i = 0; i < userGroups.size(); i++) {
                if (i > 0) placeholders.append(", ");
                placeholders.append("?");
            }
            countSql.append("AND cq.name IN (").append(placeholders).append(") ");
            countParams.addAll(userGroups);
        }
      log.info("COUNT SQL: " + countSql.toString());

        String orderDir = "desc".equalsIgnoreCase(order) ? "DESC" : "ASC";
        List<String> dataParams = new ArrayList<>();
        StringBuilder dataSql = new StringBuilder();
        dataSql.append("WITH PagedCalls AS ( ");
        dataSql.append("SELECT DISTINCT c.conversationid, c.conversationstart ");
        dataSql.append("FROM conversations c ");

        if (needsParticipantJoin) {
            dataSql.append("JOIN participants p ON c.conversationid = p.conversationid ");
            if (hasQueue || hasGroupFilter) {
                dataSql.append("LEFT JOIN conf_queue cq ON p.queueid = cq.id ");
            }
            if (hasOperator) {
                dataSql.append("LEFT JOIN conf_user cu ON p.userid = cu.id ");
            }
        }

        dataSql.append("WHERE 1=1 ");

        if (hasStartDate) {
            dataSql.append("AND c.conversationstart >= ? ");
            dataParams.add(startDate);
        }
        if (hasEndDate) {
            dataSql.append("AND c.conversationstart <= ? ");
            dataParams.add(endDate);
        }
        if (hasConversationId) {
            dataSql.append("AND c.conversationid = ? ");
            dataParams.add(conversationId);
        }
        if (hasAni) {
            dataSql.append("AND p.ani ILIKE ? ");
            dataParams.add("%" + ani + "%");
        }
        if (hasDnis) {
            dataSql.append("AND p.dnis ILIKE ? ");
            dataParams.add("%" + dnis + "%");
        }
        if (hasQueue) {
            dataSql.append("AND cq.name ILIKE ? ");
            dataParams.add("%" + queue + "%");
        }
        if (hasOperator) {
            dataSql.append("AND cu.name ILIKE ? ");
            dataParams.add("%" + operator + "%");
        }
        if (hasGroupFilter) {
            StringBuilder placeholders = new StringBuilder();
            for (int i = 0; i < userGroups.size(); i++) {
                if (i > 0) placeholders.append(", ");
                placeholders.append("?");
            }
            dataSql.append("AND cq.name IN (").append(placeholders).append(") ");
            dataParams.addAll(userGroups);
        }

        dataSql.append("ORDER BY c.conversationstart ").append(orderDir).append(" ");
        dataSql.append("LIMIT ? OFFSET ? ");
        dataSql.append(") ");
        dataSql.append("SELECT paged.conversationid, paged.conversationstart, ");
        dataSql.append("c.conversationend, p.ani, p.dnis, ");
        dataSql.append("cq.name AS queue_name, cu.name AS operator_name ");
        dataSql.append("FROM PagedCalls paged ");
        dataSql.append("JOIN conversations c ON paged.conversationid = c.conversationid ");
        dataSql.append("LEFT JOIN participants p ON c.conversationid = p.conversationid ");
        dataSql.append("LEFT JOIN conf_queue cq ON p.queueid = cq.id ");
        dataSql.append("LEFT JOIN conf_user cu ON p.userid = cu.id ");
        dataSql.append("ORDER BY paged.conversationstart ").append(orderDir);
        log.info("DATA SQL: " + dataSql.toString());

        try (Connection conn = getConnection()) {
            try (PreparedStatement countPs = conn.prepareStatement(countSql.toString())) {
                for (int i = 0; i < countParams.size(); i++) {
                    countPs.setString(i + 1, countParams.get(i));
                }
                try (ResultSet countRs = countPs.executeQuery()) {
                    if (countRs.next()) {
                        totalCount = countRs.getInt("total");
                    }
                }
            }
            log.info("AnalyzerRepository - totalCount=" + totalCount);

            try (PreparedStatement dataPs = conn.prepareStatement(dataSql.toString())) {
                int idx = 1;
                for (int i = 0; i < dataParams.size(); i++) {
                    dataPs.setString(idx++, dataParams.get(i));
                }
                dataPs.setInt(idx++, pageSize);
                dataPs.setInt(idx, (pageNumber - 1) * pageSize);

                try (ResultSet rs = dataPs.executeQuery()) {
                    while (rs.next()) {
                        Map<String, String> row = new LinkedHashMap<>();
                        row.put("conversationid", rs.getString("conversationid"));
                        row.put("conversationstart", rs.getString("conversationstart"));
                        row.put("conversationend", rs.getString("conversationend"));
                        row.put("ani", rs.getString("ani"));
                        row.put("dnis", rs.getString("dnis"));
                        row.put("queue_name", rs.getString("queue_name"));
                        row.put("operator_name", rs.getString("operator_name"));
                        results.add(row);
                    }
                }
            }

            log.info("AnalyzerRepository.searchCallsInDatabase() - Query completed. "
                    + "pageResults=" + results.size() + ", totalCount=" + totalCount);

        } catch (Exception e) {
            log.log(Level.SEVERE,
                    "AnalyzerRepository.searchCallsInDatabase() - Error occurred during database query", e);
        }

        log.info("AnalyzerRepository.searchCallsInDatabase() - EXIT - resultSize=" + results.size()
                + ", totalCount=" + totalCount);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("results", results);
        response.put("totalCount", totalCount);
        return response;
    }
}
