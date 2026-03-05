package comapp.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import comapp.ConfigServlet;
import comapp.cloud.Genesys;
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
                            + action + "'. Checking session...");
                    if (session == null || session.getAttribute("guser") == null) {
                        log.info("[" + sessionId + "] MainController.processRequest() - No session/guser. Triggering login.");
                        handleLogin(request, response, sessionId);
                    } else {
                        request.getRequestDispatcher("/SearchCall.jsp").forward(request, response);
                    }
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
            log.warning("[" + sessionId + "] MainController.handleSearchCall() - No active session. Redirecting to relogin.jsp.");
            request.setAttribute("message", "Your session has expired. Please log in again.");
            request.getRequestDispatcher("/relogin.jsp").forward(request, response);
            return;
        }

        GenesysUser guser = (GenesysUser) session.getAttribute("guser");
        if (guser == null) {
            log.warning("[" + sessionId + "] MainController.handleSearchCall() - guser is null. Redirecting to relogin.jsp.");
            request.setAttribute("message", "Your session has expired. Please log in again.");
            request.getRequestDispatcher("/relogin.jsp").forward(request, response);
            return;
        }
        log.info("[" + sessionId + "] MainController.handleSearchCall() - guser=" + guser);

        String ani      = StringUtils.defaultString(request.getParameter("ani"), "");
        String dnis     = StringUtils.defaultString(request.getParameter("dnis"), "");
        String dateFrom = StringUtils.defaultString(request.getParameter("from"), "");
        String dateTo   = StringUtils.defaultString(request.getParameter("to"), "");
        String order    = StringUtils.defaultString(request.getParameter("order"), "desc");
        String conversationId = StringUtils.defaultString(request.getParameter("conversationId"), "");
        String queue    = StringUtils.defaultString(request.getParameter("queue"), "");
        String operator = StringUtils.defaultString(request.getParameter("operator"), "");

        @SuppressWarnings("unchecked")
        List<String> userGroups = (List<String>) session.getAttribute("userGroups");
        boolean enableGroupFilter = Boolean.parseBoolean(
                ConfigServlet.getProperties().getProperty("enable_group_filter", "false"));

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
        int pageSize = 50;
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
                + ", conversationId=" + conversationId
                + ", queue=" + queue
                + ", operator=" + operator
                + ", enableGroupFilter=" + enableGroupFilter
                + ", userGroups=" + userGroups
                + ", order=" + order
                + ", currentPage=" + currentPage
                + ", pageSize=" + pageSize);

        String dateFromFormatted = convertToUtc(dateFrom, sessionId);
        String dateToFormatted   = convertToUtc(dateTo, sessionId);

        log.info("[" + sessionId + "] MainController.handleSearchCall() - Converted dates: "
                + "dateFromFormatted=" + dateFromFormatted
                + ", dateToFormatted=" + dateToFormatted);

        Map<String, Object> searchResult = tperService.searchCalls(sessionId, guser,
                dateFromFormatted, dateToFormatted,
                ani, dnis, conversationId, queue, operator,
                userGroups, enableGroupFilter,
                currentPage, pageSize, order);

        List<Map<String, String>> conversations = new ArrayList<>();
        int totalHits = 0;

        if (searchResult != null) {
            @SuppressWarnings("unchecked")
            List<Map<String, String>> resultList = (List<Map<String, String>>) searchResult.get("results");
            if (resultList != null) {
                conversations = resultList;
            }
            Object tc = searchResult.get("totalCount");
            if (tc instanceof Number) {
                totalHits = ((Number) tc).intValue();
            }
        }

        int totalPages = (totalHits > 0) ? (int) Math.ceil((double) totalHits / pageSize) : 0;

        log.info("[" + sessionId + "] MainController.handleSearchCall() - "
                + "totalHits=" + totalHits + ", totalPages=" + totalPages
                + ", conversationsSize=" + conversations.size());

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
        request.setAttribute("conversationId", conversationId);
        request.setAttribute("queue", queue);
        request.setAttribute("operator", operator);

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
                    + "No active session. Redirecting to relogin.jsp.");
            request.setAttribute("message", "Your session has expired. Please log in again.");
            request.getRequestDispatcher("/relogin.jsp").forward(request, response);
            return;
        }

        GenesysUser guser = (GenesysUser) session.getAttribute("guser");
        if (guser == null) {
            log.warning("[" + sessionId + "] MainController.handleExtendRetention() - "
                    + "guser is null. Redirecting to relogin.jsp.");
            request.setAttribute("message", "Your session has expired. Please log in again.");
            request.getRequestDispatcher("/relogin.jsp").forward(request, response);
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

        log.info("[" + sessionId + "] MainController.handleExtendRetention() - "
                + "Final parameters: convId=" + conversationId);

        log.info("[" + sessionId + "] MainController.handleExtendRetention() - "
                + "Calling TperService.extendPersonalRetention()...");

        boolean success = tperService.extendPersonalRetention(sessionId, guser, conversationId);

        log.info("[" + sessionId + "] MainController.handleExtendRetention() - "
                + "Service returned success=" + success + " for convId=" + conversationId);

        if (success) {
            String msg = "Retention extended successfully for conversation " + conversationId + ".";
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
                        + "Dummy user stored in session. Redirecting to searchCall action");
                String redirectUrl = response.encodeRedirectURL("tperApp?action=searchCall");
                response.sendRedirect(redirectUrl);

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

            Genesys.fetchUserGroups(guser);
            log.info("[" + sessionId + "] MainController.handleLogin() - "
                    + "User groups fetched. count=" + guser.getUserGroups().size()
                    + ", groups=" + guser.getUserGroups());

            session.setAttribute("guser", guser);
            session.setAttribute("userGroups", guser.getUserGroups());

            log.info("[" + sessionId + "] MainController.handleLogin() - "
                    + "GenesysUser stored in session. Redirecting to searchCall action");
            String redirectUrl = response.encodeRedirectURL("tperApp?action=searchCall");
            response.sendRedirect(redirectUrl);

        } catch (Exception e) {
            log.log(Level.SEVERE,
                    "[" + sessionId + "] MainController.handleLogin() - Authentication failed", e);
            request.setAttribute("message", "Authentication failed. Please try again.");
            request.getRequestDispatcher("/relogin.jsp").forward(request, response);
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
                    + "Superficial logout: removing local session attributes (Genesys token preserved).");
            session.removeAttribute("guser");
            session.removeAttribute("userGroups");
            session.removeAttribute("gui_user");
            log.info("[" + sessionId + "] MainController.handleLogout() - Local attributes removed.");
        } else {
            log.warning("[" + sessionId + "] MainController.handleLogout() - "
                    + "No active session found.");
        }

        log.info("[" + sessionId + "] MainController.handleLogout() - "
                + "Redirecting to relogin.jsp. EXIT");
        request.setAttribute("message", "You have been logged out successfully. Would you like to log in again?");
        request.getRequestDispatcher("/relogin.jsp").forward(request, response);
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
