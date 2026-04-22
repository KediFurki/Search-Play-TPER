package comapp.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AnalyzerRepository {

    private static final Logger log = Logger.getLogger("comapp");

    private Connection getConnection() throws Exception {
        log.fine("AnalyzerRepository.getConnection() - Requesting pooled connection from Db...");
        return Db.open();
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
        List<String> countParams = new ArrayList<>();
        StringBuilder countSql = new StringBuilder();

        countSql.append("SELECT COUNT(DISTINCT c.conversationid) AS total ");
        countSql.append("FROM conversations c ");
        countSql.append("WHERE 1=1 ");

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
            countSql.append("AND EXISTS (SELECT 1 FROM participants p2 JOIN sessions s2 ON p2.participantid = s2.participantid WHERE p2.conversationid = c.conversationid AND s2.ani ILIKE ?) ");
            countParams.add("%" + ani + "%");
        }
        if (hasDnis) {
            countSql.append("AND EXISTS (SELECT 1 FROM participants p2 JOIN sessions s2 ON p2.participantid = s2.participantid WHERE p2.conversationid = c.conversationid AND s2.dnis ILIKE ?) ");
            countParams.add("%" + dnis + "%");
        }
        if (hasQueue) {
            countSql.append("AND EXISTS (SELECT 1 FROM participants p2 JOIN sessions s2 ON p2.participantid = s2.participantid JOIN segments seg2 ON s2.sessionid = seg2.sessionid JOIN conf_queue cq2 ON seg2.queueid = cq2.id WHERE p2.conversationid = c.conversationid AND cq2.name ILIKE ?) ");
            countParams.add("%" + queue + "%");
        }
        if (hasOperator) {
            countSql.append("AND EXISTS (SELECT 1 FROM participants p2 JOIN conf_user cu2 ON p2.userid = cu2.id WHERE p2.conversationid = c.conversationid AND cu2.name ILIKE ?) ");
            countParams.add("%" + operator + "%");
        }
        if (hasGroupFilter) {
            StringBuilder placeholders = new StringBuilder();
            for (int i = 0; i < userGroups.size(); i++) {
                if (i > 0) placeholders.append(", ");
                placeholders.append("?");
            }
            countSql.append("AND EXISTS (SELECT 1 FROM participants p2 JOIN sessions s2 ON p2.participantid = s2.participantid JOIN segments seg2 ON s2.sessionid = seg2.sessionid JOIN conf_queue cq2 ON seg2.queueid = cq2.id WHERE p2.conversationid = c.conversationid AND cq2.name IN (").append(placeholders).append(")) ");
            countParams.addAll(userGroups);
        }
      log.info("COUNT SQL: " + countSql.toString());

        String orderDir = "desc".equalsIgnoreCase(order) ? "DESC" : "ASC";
        List<String> dataParams = new ArrayList<>();
        StringBuilder dataSql = new StringBuilder();
        dataSql.append("WITH PagedCalls AS ( ");
        dataSql.append("SELECT DISTINCT c.conversationid, c.conversationstart ");
        dataSql.append("FROM conversations c ");
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
            dataSql.append("AND EXISTS (SELECT 1 FROM participants p2 JOIN sessions s2 ON p2.participantid = s2.participantid WHERE p2.conversationid = c.conversationid AND s2.ani ILIKE ?) ");
            dataParams.add("%" + ani + "%");
        }
        if (hasDnis) {
            dataSql.append("AND EXISTS (SELECT 1 FROM participants p2 JOIN sessions s2 ON p2.participantid = s2.participantid WHERE p2.conversationid = c.conversationid AND s2.dnis ILIKE ?) ");
            dataParams.add("%" + dnis + "%");
        }
        if (hasQueue) {
            dataSql.append("AND EXISTS (SELECT 1 FROM participants p2 JOIN sessions s2 ON p2.participantid = s2.participantid JOIN segments seg2 ON s2.sessionid = seg2.sessionid JOIN conf_queue cq2 ON seg2.queueid = cq2.id WHERE p2.conversationid = c.conversationid AND cq2.name ILIKE ?) ");
            dataParams.add("%" + queue + "%");
        }
        if (hasOperator) {
            dataSql.append("AND EXISTS (SELECT 1 FROM participants p2 JOIN conf_user cu2 ON p2.userid = cu2.id WHERE p2.conversationid = c.conversationid AND cu2.name ILIKE ?) ");
            dataParams.add("%" + operator + "%");
        }
        if (hasGroupFilter) {
            StringBuilder placeholders = new StringBuilder();
            for (int i = 0; i < userGroups.size(); i++) {
                if (i > 0) placeholders.append(", ");
                placeholders.append("?");
            }
            dataSql.append("AND EXISTS (SELECT 1 FROM participants p2 JOIN sessions s2 ON p2.participantid = s2.participantid JOIN segments seg2 ON s2.sessionid = seg2.sessionid JOIN conf_queue cq2 ON seg2.queueid = cq2.id WHERE p2.conversationid = c.conversationid AND cq2.name IN (").append(placeholders).append(")) ");
            dataParams.addAll(userGroups);
        }

        dataSql.append("ORDER BY c.conversationstart ").append(orderDir).append(" ");
        dataSql.append("LIMIT ? OFFSET ? ");
        dataSql.append(") ");
        dataSql.append("SELECT paged.conversationid, paged.conversationstart, ");
        dataSql.append("MAX(c.conversationend) AS conversationend, ");
        dataSql.append("MAX(s.ani) AS ani, MAX(s.dnis) AS dnis, ");
        dataSql.append("STRING_AGG(DISTINCT cq.name, ', ') AS queue_name, ");
        dataSql.append("STRING_AGG(DISTINCT cu.name, ', ') AS operator_name, ");
        dataSql.append("BOOL_OR(c.retention_locked) AS retention_locked ");
        dataSql.append("FROM PagedCalls paged ");
        dataSql.append("JOIN conversations c ON paged.conversationid = c.conversationid ");
        dataSql.append("LEFT JOIN participants p ON c.conversationid = p.conversationid ");
        dataSql.append("LEFT JOIN sessions s ON p.participantid = s.participantid ");
        dataSql.append("LEFT JOIN segments seg ON s.sessionid = seg.sessionid ");
        dataSql.append("LEFT JOIN conf_queue cq ON seg.queueid = cq.id ");
        dataSql.append("LEFT JOIN conf_user cu ON p.userid = cu.id ");
        dataSql.append("GROUP BY paged.conversationid, paged.conversationstart ");
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
                        row.put("retention_locked", String.valueOf(rs.getBoolean("retention_locked")));
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

    public void saveRetentionLocked(String conversationId, boolean locked) {
        String sql = "UPDATE conversations SET retention_locked = ? WHERE conversationid = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, locked);
            ps.setString(2, conversationId);
            int updated = ps.executeUpdate();
            log.info("AnalyzerRepository.saveRetentionLocked() - conversationId=" + conversationId
                    + " locked=" + locked + " rows=" + updated);
        } catch (Exception e) {
            log.log(Level.SEVERE,
                    "AnalyzerRepository.saveRetentionLocked() - Error for conversationId=" + conversationId, e);
        }
    }
}
