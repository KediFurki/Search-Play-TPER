package comapp.controller;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
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
                case "toggleRetention":
                    handleToggleRetention(request, response, session, sessionId);
                    break;
                case "cancelAudio":
                    handleCancelAudio(request, response, session, sessionId);
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
        File audioFile = getOrCreateAudioTempFile(session, sessionId, guser, conversationId);

        if (audioFile == null || !audioFile.exists()) {
            log.warning("[" + sessionId + "] MainController.handlePlayAudio() - "
                    + "Audio temp file not available for convId=" + conversationId + ". Returning 404.");
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Audio not available for the requested conversation.");
            return;
        }
        serveAudioFromFile(audioFile, request, response, sessionId, conversationId);

        log.info("[" + sessionId + "] MainController.handlePlayAudio() - EXIT - convId=" + conversationId);
    }

    @SuppressWarnings("unchecked")
    private File getOrCreateAudioTempFile(HttpSession session, String sessionId,
                                          GenesysUser guser, String conversationId) {
        ConcurrentHashMap<String, File> audioCache =
                (ConcurrentHashMap<String, File>) session.getAttribute("audioCache");
        if (audioCache == null) {
            synchronized (session) {
                audioCache = (ConcurrentHashMap<String, File>) session.getAttribute("audioCache");
                if (audioCache == null) {
                    audioCache = new ConcurrentHashMap<>();
                    session.setAttribute("audioCache", audioCache);
                    log.info("[" + sessionId + "] getOrCreateAudioTempFile() - Created new audioCache for session.");
                }
            }
        }
        File cached = audioCache.get(conversationId);
        if (cached != null && cached.exists()) {
            log.info("[" + sessionId + "] getOrCreateAudioTempFile() - Cache HIT for convId=" + conversationId
                    + ", tempFile=" + cached.getAbsolutePath() + ", size=" + cached.length() + " bytes");
            return cached;
        }
        log.info("[" + sessionId + "] getOrCreateAudioTempFile() - Cache MISS for convId=" + conversationId
                + ". Requesting morphed audio from TperService...");

        InputStream audioStream = tperService.getMorphedAudioStream(sessionId, guser, conversationId);
        if (audioStream == null) {
            log.warning("[" + sessionId + "] getOrCreateAudioTempFile() - TperService returned null stream for convId=" + conversationId);
            return null;
        }

        File tempFile = null;
        try {
            tempFile = File.createTempFile("sp_audio_" + conversationId + "_", ".wav");
            tempFile.deleteOnExit();

            long bytesCopied;
            try (InputStream in = audioStream;
                 OutputStream out = Files.newOutputStream(tempFile.toPath())) {
                bytesCopied = in.transferTo(out);
            }

            log.info("[" + sessionId + "] getOrCreateAudioTempFile() - Audio written to temp file: "
                    + tempFile.getAbsolutePath() + ", size=" + bytesCopied + " bytes, convId=" + conversationId);

            audioCache.put(conversationId, tempFile);
            return tempFile;

        } catch (IOException e) {
            log.log(Level.SEVERE, "[" + sessionId + "] getOrCreateAudioTempFile() - "
                    + "Failed to write audio to temp file for convId=" + conversationId, e);
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
            return null;
        }
    }
    private void serveAudioFromFile(File audioFile, HttpServletRequest request,
                                     HttpServletResponse response, String sessionId,
                                     String conversationId) throws IOException {
        long totalLength = audioFile.length();
        String rangeHeader = request.getHeader("Range");

        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            String rangeValue = rangeHeader.substring("bytes=".length());
            String[] parts = rangeValue.split("-");
            long start = Long.parseLong(parts[0]);
            long end = (parts.length > 1 && !parts[1].isEmpty())
                    ? Long.parseLong(parts[1])
                    : totalLength - 1;

            if (start >= totalLength) {
                log.warning("[" + sessionId + "] serveAudioFromFile() - Range start (" + start
                        + ") >= totalLength (" + totalLength + "). Returning 416. convId=" + conversationId);
                response.setStatus(416);
                response.setHeader("Content-Range", "bytes */" + totalLength);
                return;
            }
            if (end >= totalLength) {
                end = totalLength - 1;
            }

            long contentLength = end - start + 1;
            response.setStatus(206);
            response.setContentType("audio/wav");
            response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + totalLength);
            response.setHeader("Content-Length", String.valueOf(contentLength));
            response.setHeader("Accept-Ranges", "bytes");
            response.setHeader("Content-Disposition", "inline");

            log.info("[" + sessionId + "] serveAudioFromFile() - Range request: bytes " + start + "-" + end
                    + "/" + totalLength + ", contentLength=" + contentLength + ", convId=" + conversationId);

            try (RandomAccessFile raf = new RandomAccessFile(audioFile, "r");
                 OutputStream out = response.getOutputStream()) {
                raf.seek(start);
                byte[] buffer = new byte[8192];
                long remaining = contentLength;
                while (remaining > 0) {
                    int toRead = (int) Math.min(buffer.length, remaining);
                    int bytesRead = raf.read(buffer, 0, toRead);
                    if (bytesRead == -1) break;
                    out.write(buffer, 0, bytesRead);
                    remaining -= bytesRead;
                }
                out.flush();
            }
        } else {
            response.setContentType("audio/wav");
            response.setHeader("Content-Length", String.valueOf(totalLength));
            response.setHeader("Accept-Ranges", "bytes");
            response.setHeader("Content-Disposition", "inline");

            log.info("[" + sessionId + "] serveAudioFromFile() - Full response: "
                    + totalLength + " bytes, convId=" + conversationId);

            try (InputStream in = Files.newInputStream(audioFile.toPath());
                 OutputStream out = response.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
                out.flush();
            }
        }
    }
    private void handleToggleRetention(HttpServletRequest request, HttpServletResponse response,
                                       HttpSession session, String sessionId)
            throws ServletException, IOException {
        log.info("[" + sessionId + "] MainController.handleToggleRetention() - ENTRY");
        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");

        if (session == null) {
            log.warning("[" + sessionId + "] MainController.handleToggleRetention() - No active session. Returning 401.");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("error:no_session");
            return;
        }

        GenesysUser guser = (GenesysUser) session.getAttribute("guser");
        if (guser == null) {
            log.warning("[" + sessionId + "] MainController.handleToggleRetention() - guser is null. Returning 401.");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("error:no_session");
            return;
        }

        String conversationId = request.getParameter("conversationId");
        String convStart      = request.getParameter("convStart");
        String lockState      = request.getParameter("lockState");
        log.info("[" + sessionId + "] MainController.handleToggleRetention() - "
                + "Parameters: conversationId=" + conversationId
                + ", convStart=" + convStart
                + ", lockState=" + lockState);

        if (StringUtils.isBlank(conversationId)) {
            log.warning("[" + sessionId + "] MainController.handleToggleRetention() - conversationId is blank. Returning 400.");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("error:missing_conversation_id");
            return;
        }

        boolean success;

        if ("true".equalsIgnoreCase(lockState)) {
            log.info("[" + sessionId + "] MainController.handleToggleRetention() - "
                    + "lockState=true -> Extending retention (17 years) for conversationId=" + conversationId);
            success = tperService.extendPersonalRetention(sessionId, guser, conversationId, convStart);
        } else {
            log.info("[" + sessionId + "] MainController.handleToggleRetention() - "
                    + "lockState=false -> Reverting retention (start+90 days) for conversationId=" + conversationId);
            success = tperService.revertPersonalRetention(sessionId, guser, conversationId, convStart);
        }

        log.info("[" + sessionId + "] MainController.handleToggleRetention() - "
                + "Service returned success=" + success + " for conversationId=" + conversationId);

        if (success) {
            response.getWriter().write("success");
            log.info("[" + sessionId + "] MainController.handleToggleRetention() - Responded with 'success'.");
        } else {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("error:api_failure");
            log.warning("[" + sessionId + "] MainController.handleToggleRetention() - Responded with 'error:api_failure'.");
        }

        log.info("[" + sessionId + "] MainController.handleToggleRetention() - EXIT");
    }

    private void handleCancelAudio(HttpServletRequest request, HttpServletResponse response,
                                   HttpSession session, String sessionId) throws IOException {
        String convId = request.getParameter("convId");
        if (org.apache.commons.lang3.StringUtils.isNotBlank(convId)) {
            comapp.cloud.Genesys.cancelMap.put(convId, true);
            log.info("[" + sessionId + "] - Cancel signal received for convId: " + convId);
        }
        response.getWriter().write("cancelled");
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
            cleanupAudioCache(session, sessionId);

            log.info("[" + sessionId + "] MainController.handleLogout() - "
                    + "Superficial logout: removing local session attributes (Genesys token preserved).");
            session.removeAttribute("guser");
            session.removeAttribute("userGroups");
            session.removeAttribute("gui_user");
            session.removeAttribute("audioCache");
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
    @SuppressWarnings("unchecked")
    private void cleanupAudioCache(HttpSession session, String sessionId) {
        ConcurrentHashMap<String, File> audioCache =
                (ConcurrentHashMap<String, File>) session.getAttribute("audioCache");
        if (audioCache == null || audioCache.isEmpty()) {
            log.info("[" + sessionId + "] cleanupAudioCache() - No audio cache to clean up.");
            return;
        }

        int deleted = 0;
        int failed = 0;
        for (Map.Entry<String, File> entry : audioCache.entrySet()) {
            File tempFile = entry.getValue();
            if (tempFile != null && tempFile.exists()) {
                if (tempFile.delete()) {
                    deleted++;
                    log.fine("[" + sessionId + "] cleanupAudioCache() - Deleted temp file: "
                            + tempFile.getAbsolutePath() + " (convId=" + entry.getKey() + ")");
                } else {
                    failed++;
                    log.warning("[" + sessionId + "] cleanupAudioCache() - Failed to delete temp file: "
                            + tempFile.getAbsolutePath() + " (convId=" + entry.getKey() + ")");
                }
            }
        }
        audioCache.clear();
        log.info("[" + sessionId + "] cleanupAudioCache() - Cleanup complete. deleted=" + deleted + ", failed=" + failed);
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