package comapp.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import comapp.ConfigServlet;
import comapp.cloud.GenesysUser;
import comapp.service.TperService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/tperApp")
public class MainController extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger log = Logger.getLogger("comapp");

    private final TperService tperService = new TperService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    private void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        String sessionId = (session != null) ? session.getId() : "NO_SESSION";

        String action = request.getParameter("action");
        action = (action != null) ? action.trim() : "";

        log.info("[" + sessionId + "] MainController.processRequest() - ENTRY - action=" + action);

        try {
            switch (action) {
                case "searchCall":
                    handleSearchCall(request, response, session, sessionId);
                    break;
                case "playAudio":
                    handlePlayAudio(request, response, session, sessionId);
                    break;
                case "extendRetention":
                    handleExtendRetention(request, response, session, sessionId);
                    break;
                case "login":
                    handleLogin(request, response, sessionId);
                    break;
                case "logout":
                    handleLogout(request, response, sessionId);
                    break;
                default:
                    log.warning("[" + sessionId + "] MainController.processRequest() - Unknown or empty action: '"
                            + action + "'. Forwarding to /SearchCall.jsp as default.");
                    request.getRequestDispatcher("/SearchCall.jsp").forward(request, response);
                    break;
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "[" + sessionId + "] MainController.processRequest() - Unhandled exception", e);
            request.setAttribute("error", "An unexpected error occurred. Please try again later.");
            request.getRequestDispatcher("/SearchCall.jsp").forward(request, response);
        }

        log.info("[" + sessionId + "] MainController.processRequest() - EXIT - action=" + action);
    }

    private void handleSearchCall(HttpServletRequest request, HttpServletResponse response,
                                  HttpSession session, String sessionId)
            throws ServletException, IOException {

        log.info("[" + sessionId + "] MainController.handleSearchCall() - ENTRY");

        if (session == null) {
            log.warning("[" + sessionId + "] MainController.handleSearchCall() - No active session. Redirecting to index.jsp.");
            response.sendRedirect("index.jsp");
            return;
        }

        GenesysUser guser = (GenesysUser) session.getAttribute("guser");
        if (guser == null) {
            log.warning("[" + sessionId + "] MainController.handleSearchCall() - guser is null. Invalidating session and redirecting to index.jsp.");
            session.invalidate();
            response.sendRedirect("index.jsp");
            return;
        }

        log.info("[" + sessionId + "] MainController.handleSearchCall() - guser=" + guser);

        String ani      = StringUtils.defaultString(request.getParameter("ani"), "");
        String dnis     = StringUtils.defaultString(request.getParameter("dnis"), "");
        String dateFrom = StringUtils.defaultString(request.getParameter("from"), "");
        String dateTo   = StringUtils.defaultString(request.getParameter("to"), "");
        String order    = StringUtils.defaultString(request.getParameter("order"), "desc");
        int currentPage = 1;
        try {
            String cp = request.getParameter("currentpage");
            if (StringUtils.isNotBlank(cp)) {
                currentPage = Integer.parseInt(cp);
            }
        } catch (NumberFormatException e) {
            log.warning("[" + sessionId + "] MainController.handleSearchCall() - Invalid currentpage value, defaulting to 1.");
        }

        Properties cs = ConfigServlet.getProperties();
        int pageSize = 10;
        try {
            String ps = cs.getProperty("pageSize");
            if (ps != null) {
                pageSize = Integer.parseInt(ps);
            }
        } catch (NumberFormatException e) {
            log.warning("[" + sessionId + "] MainController.handleSearchCall() - Invalid pageSize in config, defaulting to 10.");
        }

        log.info("[" + sessionId + "] MainController.handleSearchCall() - Parameters: "
                + "ani=" + ani
                + ", dnis=" + dnis
                + ", from=" + dateFrom
                + ", to=" + dateTo
                + ", order=" + order
                + ", currentPage=" + currentPage
                + ", pageSize=" + pageSize);

        String dateFromFormatted = convertToUtc(dateFrom, sessionId);
        String dateToFormatted   = convertToUtc(dateTo, sessionId);

        log.info("[" + sessionId + "] MainController.handleSearchCall() - Converted dates: "
                + "dateFromFormatted=" + dateFromFormatted
                + ", dateToFormatted=" + dateToFormatted);

        JSONObject result = tperService.searchCalls(sessionId, guser,
                dateFromFormatted, dateToFormatted,
                ani, dnis,
                currentPage, pageSize, order);

        log.info("[" + sessionId + "] MainController.handleSearchCall() - Service returned result="
                + (result != null ? "non-null" : "null"));

        List<Map<String, String>> conversations = new ArrayList<>();
        int totalHits  = 0;
        int totalPages = 0;

        if (result != null) {
            totalHits = result.optInt("totalHits", 0);
            totalPages = (int) Math.ceil((double) totalHits / pageSize);

            log.info("[" + sessionId + "] MainController.handleSearchCall() - "
                    + "totalHits=" + totalHits + ", totalPages=" + totalPages);

            JSONArray convArray = result.optJSONArray("conversations");
            if (convArray != null) {
                log.info("[" + sessionId + "] MainController.handleSearchCall() - "
                        + "Converting " + convArray.length() + " conversations to List<Map>...");

                for (int i = 0; i < convArray.length(); i++) {
                  try {
                    JSONObject conv = convArray.getJSONObject(i);
                    Map<String, String> row = new LinkedHashMap<>();

                    row.put("conversationId", conv.optString("conversationId", ""));
                    row.put("conversationStart", conv.optString("conversationStart", ""));
                    row.put("conversationEnd", conv.optString("conversationEnd", ""));
                    row.put("originatingDirection", conv.optString("originatingDirection", ""));

                    try {
                        ZonedDateTime utcStart = ZonedDateTime.parse(conv.getString("conversationStart"));
                        ZonedDateTime rome = utcStart.withZoneSameInstant(ZoneId.of("Europe/Rome"));
                        String formatted = rome.format(
                                DateTimeFormatter.ofPattern("E, dd MMM, yyyy HH:mm", Locale.ENGLISH));
                        row.put("formattedStart", formatted);
                    } catch (Exception e) {
                        log.warning("[" + sessionId + "] MainController.handleSearchCall() - "
                                + "Failed to format conversationStart for index=" + i);
                        row.put("formattedStart", conv.optString("conversationStart", ""));
                    }
                    try {
                        ZonedDateTime t1 = ZonedDateTime.parse(conv.getString("conversationStart"));
                        ZonedDateTime t2 = ZonedDateTime.parse(conv.getString("conversationEnd"));
                        Duration dur = Duration.between(t1, t2);
                        row.put("duration", dur.toMinutes() + "min, " + (dur.getSeconds() % 60) + "sec");
                    } catch (Exception e) {
                        log.warning("[" + sessionId + "] MainController.handleSearchCall() - "
                                + "Failed to compute duration for index=" + i);
                        row.put("duration", "N/A");
                    }
                    try {
                        JSONObject firstParticipant = conv.getJSONArray("participants").getJSONObject(0);
                        JSONObject firstSession = firstParticipant.getJSONArray("sessions").getJSONObject(0);
                        String direction = firstSession.optString("direction", "");

                        if ("outbound".equalsIgnoreCase(direction)) {
                            row.put("ani", firstParticipant.optString("userId", ""));
                        } else {
                            row.put("ani", firstSession.optString("ani", ""));
                        }
                        row.put("dnis", firstSession.optString("dnis", ""));
                    } catch (Exception e) {
                        log.warning("[" + sessionId + "] MainController.handleSearchCall() - "
                                + "Failed to extract participant data for index=" + i);
                        row.put("ani", "N/A");
                        row.put("dnis", "N/A");
                    }
                    try {
                        JSONArray divs = conv.optJSONArray("divisionIds");
                        if (divs != null) {
                            StringBuilder sb = new StringBuilder();
                            for (int d = 0; d < divs.length(); d++) {
                                if (d > 0) sb.append(", ");
                                sb.append(divs.getString(d));
                            }
                            row.put("divisionIds", sb.toString());
                        } else {
                            row.put("divisionIds", "");
                        }
                    } catch (Exception e) {
                        row.put("divisionIds", "");
                    }

                    conversations.add(row);
                  } catch (Exception e) {
                    log.log(Level.WARNING, "[" + sessionId + "] MainController.handleSearchCall() - "
                            + "Failed to parse conversation at index=" + i, e);
                  }
                }

                log.info("[" + sessionId + "] MainController.handleSearchCall() - "
                        + "Conversion complete. conversationsSize=" + conversations.size());
            } else {
                log.info("[" + sessionId + "] MainController.handleSearchCall() - "
                        + "No conversations array in result.");
            }
        } else {
            log.warning("[" + sessionId + "] MainController.handleSearchCall() - "
                    + "Result is null, setting empty list.");
        }
        request.setAttribute("conversations", conversations);
        request.setAttribute("totalHits", totalHits);
        request.setAttribute("totalPages", totalPages);
        request.setAttribute("currentpage", currentPage);
        request.setAttribute("pageSize", pageSize);
        request.setAttribute("ani", ani);
        request.setAttribute("dnis", dnis);
        request.setAttribute("from", dateFrom);
        request.setAttribute("to", dateTo);
        request.setAttribute("order", order);

        log.info("[" + sessionId + "] MainController.handleSearchCall() - Forwarding to /SearchCall.jsp");
        request.getRequestDispatcher("/SearchCall.jsp").forward(request, response);

        log.info("[" + sessionId + "] MainController.handleSearchCall() - EXIT");
    }
    private void handlePlayAudio(HttpServletRequest request, HttpServletResponse response,
                                 HttpSession session, String sessionId)
            throws ServletException, IOException {

        log.info("[" + sessionId + "] MainController.handlePlayAudio() - ENTRY");

        if (session == null) {
            log.warning("[" + sessionId + "] MainController.handlePlayAudio() - No active session. Returning 401.");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "No active session.");
            return;
        }

        GenesysUser guser = (GenesysUser) session.getAttribute("guser");
        if (guser == null) {
            log.warning("[" + sessionId + "] MainController.handlePlayAudio() - guser is null. Returning 401.");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "User session expired.");
            return;
        }
        String conversationId = request.getParameter("convId");
        log.info("[" + sessionId + "] MainController.handlePlayAudio() - convId=" + conversationId);

        if (StringUtils.isBlank(conversationId)) {
            log.warning("[" + sessionId + "] MainController.handlePlayAudio() - convId is blank. Returning 400.");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing required parameter: convId");
            return;
        }

        log.info("[" + sessionId + "] MainController.handlePlayAudio() - "
                + "Requesting morphed audio stream from TperService for convId=" + conversationId);

        InputStream audioStream = tperService.getMorphedAudioStream(sessionId, guser, conversationId);

        if (audioStream == null) {
            log.warning("[" + sessionId + "] MainController.handlePlayAudio() - "
                    + "TperService returned null stream for convId=" + conversationId + ". Returning 404.");
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Audio not available for the requested conversation.");
            return;
        }
        response.setContentType("audio/wav");
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Content-Disposition", "inline");

        log.info("[" + sessionId + "] MainController.handlePlayAudio() - "
                + "Streaming morphed audio to response for convId=" + conversationId);

        try (InputStream in = audioStream;
             OutputStream out = response.getOutputStream()) {

            byte[] buffer = new byte[4096];
            int bytesRead;
            long totalBytes = 0;

            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                totalBytes += bytesRead;
            }
            out.flush();

            log.info("[" + sessionId + "] MainController.handlePlayAudio() - "
                    + "Streaming completed. totalBytesWritten=" + totalBytes
                    + ", convId=" + conversationId);

        } catch (IOException e) {
            log.log(Level.SEVERE,
                    "[" + sessionId + "] MainController.handlePlayAudio() - "
                    + "IO error while streaming audio for convId=" + conversationId, e);
            if (!response.isCommitted()) {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Error streaming audio data.");
            }
        }

        log.info("[" + sessionId + "] MainController.handlePlayAudio() - EXIT - convId=" + conversationId);
    }
    private void handleExtendRetention(HttpServletRequest request, HttpServletResponse response,
                                       HttpSession session, String sessionId)
            throws ServletException, IOException {
        log.info("[" + sessionId + "] MainController.handleExtendRetention() - ENTRY");

        if (session == null) {
            log.warning("[" + sessionId + "] MainController.handleExtendRetention() - "
                    + "No active session. Redirecting to index.jsp.");
            response.sendRedirect("index.jsp");
            return;
        }

        GenesysUser guser = (GenesysUser) session.getAttribute("guser");
        if (guser == null) {
            log.warning("[" + sessionId + "] MainController.handleExtendRetention() - "
                    + "guser is null. Invalidating session and redirecting to index.jsp.");
            session.invalidate();
            response.sendRedirect("index.jsp");
            return;
        }
        log.info("[" + sessionId + "] MainController.handleExtendRetention() - guser=" + guser);

        String conversationId = request.getParameter("convId");
        log.info("[" + sessionId + "] MainController.handleExtendRetention() - convId=" + conversationId);

        if (StringUtils.isBlank(conversationId)) {
            log.warning("[" + sessionId + "] MainController.handleExtendRetention() - "
                    + "convId is blank. Setting error message and forwarding to /SearchCall.jsp.");
            request.setAttribute("retentionMsg", "Conversation ID is missing. Cannot extend retention.");
            request.getRequestDispatcher("/SearchCall.jsp").forward(request, response);
            return;
        }

        int yearsToKeep = 5;
        String yearsParam = request.getParameter("years");
        log.info("[" + sessionId + "] MainController.handleExtendRetention() - "
                + "Raw years parameter=" + yearsParam);

        if (StringUtils.isNotBlank(yearsParam)) {
            try {
                yearsToKeep = Integer.parseInt(yearsParam);
                log.info("[" + sessionId + "] MainController.handleExtendRetention() - "
                        + "Parsed yearsToKeep=" + yearsToKeep + " from request parameter.");
            } catch (NumberFormatException e) {
                log.warning("[" + sessionId + "] MainController.handleExtendRetention() - "
                        + "Invalid years parameter '" + yearsParam + "'. Defaulting to 5.");
                yearsToKeep = 5;
            }
        } else {
            log.info("[" + sessionId + "] MainController.handleExtendRetention() - "
                    + "Years parameter not provided. Using default yearsToKeep=5.");
        }

        log.info("[" + sessionId + "] MainController.handleExtendRetention() - "
                + "Final parameters: convId=" + conversationId + ", yearsToKeep=" + yearsToKeep);

        log.info("[" + sessionId + "] MainController.handleExtendRetention() - "
                + "Calling TperService.extendPersonalRetention()...");

        boolean success = tperService.extendPersonalRetention(sessionId, guser, conversationId, yearsToKeep);

        log.info("[" + sessionId + "] MainController.handleExtendRetention() - "
                + "Service returned success=" + success + " for convId=" + conversationId);

        if (success) {
            String msg = "Retention extended successfully for conversation " + conversationId
                    + " (" + yearsToKeep + " years).";
            log.info("[" + sessionId + "] MainController.handleExtendRetention() - " + msg);
            request.setAttribute("retentionMsg", msg);
        } else {
            String msg = "Failed to extend retention for conversation " + conversationId
                    + ". Please try again later.";
            log.warning("[" + sessionId + "] MainController.handleExtendRetention() - " + msg);
            request.setAttribute("retentionMsg", msg);
        }

        log.info("[" + sessionId + "] MainController.handleExtendRetention() - Forwarding to /SearchCall.jsp");
        request.getRequestDispatcher("/SearchCall.jsp").forward(request, response);

        log.info("[" + sessionId + "] MainController.handleExtendRetention() - EXIT");
    }

    private void handleLogin(HttpServletRequest request, HttpServletResponse response,
                             String sessionId)
            throws ServletException, IOException {

        log.info("[" + sessionId + "] MainController.handleLogin() - ENTRY");

        String username = StringUtils.defaultString(request.getParameter("username"), "");
        String password = request.getParameter("password") != null ? "***" : "null";
        String code     = StringUtils.defaultString(request.getParameter("code"), "");

        log.info("[" + sessionId + "] MainController.handleLogin() - Parameters: "
                + "username=" + username
                + ", password=" + password
                + ", code=" + (StringUtils.isNotBlank(code) ? "present" : "absent"));

        HttpSession session = request.getSession(true);
        sessionId = session.getId();
        log.info("[" + sessionId + "] MainController.handleLogin() - Session created/retrieved.");

        try {
            Properties cs = ConfigServlet.getProperties();
            String guiClientId     = cs.getProperty("gui_clientId", "");
            String guiClientSecret = cs.getProperty("gui_clientSecret", "");
            String urlRegion       = cs.getProperty("urlRegion", "");
            String redirectUri     = cs.getProperty("redirect_uri", "");
            String urlAuthorize    = cs.getProperty("urlAuthorizeString",
                                         "https://login.mypurecloud.ie/oauth/authorize");
            boolean enabledSecurity = Boolean.parseBoolean(
                                         cs.getProperty("enabled_security", "true"));

            log.info("[" + sessionId + "] MainController.handleLogin() - Config: "
                    + "guiClientId=" + guiClientId
                    + ", urlRegion=" + urlRegion
                    + ", enabledSecurity=" + enabledSecurity);

            if (!enabledSecurity) {
                log.info("[" + sessionId + "] MainController.handleLogin() - "
                        + "Security is DISABLED. Creating dummy GenesysUser for development.");

                GenesysUser guser = new GenesysUser(sessionId, guiClientId,
                        guiClientSecret, urlRegion, redirectUri, urlAuthorize);
                session.setAttribute("guser", guser);

                JSONObject dummyUser = new JSONObject();
                dummyUser.put("name", StringUtils.isNotBlank(username) ? username : "admin");
                dummyUser.put("id", "admin");
                session.setAttribute("gui_user", dummyUser);

                log.info("[" + sessionId + "] MainController.handleLogin() - "
                        + "Dummy user stored in session. Redirecting to tperApp (searchCall default).");
                request.getRequestDispatcher("/SearchCall.jsp").forward(request, response);

                log.info("[" + sessionId + "] MainController.handleLogin() - EXIT (security disabled)");
                return;
            }

            if (StringUtils.isBlank(code)) {
                String authorizeUrl = urlAuthorize
                        + "?client_id=" + guiClientId
                        + "&response_type=code"
                        + "&redirect_uri=" + redirectUri;

                log.info("[" + sessionId + "] MainController.handleLogin() - "
                        + "No OAuth code present. Redirecting to Genesys: " + authorizeUrl);
                response.sendRedirect(authorizeUrl);

                log.info("[" + sessionId + "] MainController.handleLogin() - EXIT (redirect to Genesys OAuth)");
                return;
            }
            log.info("[" + sessionId + "] MainController.handleLogin() - "
                    + "OAuth code received. Initialising GenesysUser with code...");

            GenesysUser guser = new GenesysUser(sessionId, guiClientId,
                    guiClientSecret, urlRegion, redirectUri, urlAuthorize);
            guser.setCode(code);

            log.info("[" + sessionId + "] MainController.handleLogin() - Requesting access token from Genesys...");
            guser.getToken(false);
            log.info("[" + sessionId + "] MainController.handleLogin() - Access token obtained successfully.");

            session.setAttribute("guser", guser);

            log.info("[" + sessionId + "] MainController.handleLogin() - "
                    + "GenesysUser stored in session. Redirecting to SearchCall.jsp");
            request.getRequestDispatcher("/SearchCall.jsp").forward(request, response);

        } catch (Exception e) {
            log.log(Level.SEVERE,
                    "[" + sessionId + "] MainController.handleLogin() - Authentication failed", e);
            request.setAttribute("error", "Authentication failed. Please try again.");
            request.getRequestDispatcher("/index.jsp").forward(request, response);
        }

        log.info("[" + sessionId + "] MainController.handleLogin() - EXIT");
    }
    private void handleLogout(HttpServletRequest request, HttpServletResponse response,
                              String sessionId)
            throws ServletException, IOException {

        log.info("[" + sessionId + "] MainController.handleLogout() - ENTRY");

        HttpSession session = request.getSession(false);
        if (session != null) {
            log.info("[" + sessionId + "] MainController.handleLogout() - "
                    + "Invalidating session for user logout.");
            session.invalidate();
            log.info("[" + sessionId + "] MainController.handleLogout() - Session invalidated successfully.");
        } else {
            log.warning("[" + sessionId + "] MainController.handleLogout() - "
                    + "No active session found to invalidate.");
        }

        log.info("[" + sessionId + "] MainController.handleLogout() - "
                + "Redirecting to index.jsp. EXIT");
        response.sendRedirect("index.jsp");
    }
    private String convertToUtc(String localDateStr, String sessionId) {
        log.fine("[" + sessionId + "] MainController.convertToUtc() - ENTRY - localDateStr=" + localDateStr);

        if (StringUtils.isBlank(localDateStr)) {
            log.fine("[" + sessionId + "] MainController.convertToUtc() - Input is blank, returning null.");
            return null;
        }

        try {
            DateTimeFormatter localFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
            DateTimeFormatter utcFormatter   = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

            LocalDateTime localDateTime      = LocalDateTime.parse(localDateStr, localFormatter);
            ZonedDateTime localZonedDateTime  = localDateTime.atZone(ZoneId.of("Europe/Rome"));
            ZonedDateTime utcDateTime         = localZonedDateTime.withZoneSameInstant(ZoneId.of("UTC"));
            String result = utcDateTime.format(utcFormatter);

            log.fine("[" + sessionId + "] MainController.convertToUtc() - EXIT - result=" + result);
            return result;

        } catch (Exception e) {
            log.log(Level.WARNING,
                    "[" + sessionId + "] MainController.convertToUtc() - Failed to parse date: " + localDateStr, e);
            return null;
        }
    }
}
