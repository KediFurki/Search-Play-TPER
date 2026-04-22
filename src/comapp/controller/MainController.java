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
        log.info("[" + sessionId + "] processRequest | action=" + action);
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
                    if (session == null || session.getAttribute("guser") == null) {
                        handleLogin(request, response, sessionId);
                    } else {
                        request.getRequestDispatcher("/SearchCall.jsp").forward(request, response);
                    }
                    break;
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "[" + sessionId + "] processRequest | unhandled exception", e);
            request.setAttribute("error", "An unexpected error occurred. Please try again later.");
            request.getRequestDispatcher("/SearchCall.jsp").forward(request, response);
        }
    }

    private void handleSearchCall(HttpServletRequest request, HttpServletResponse response,
                                  HttpSession session, String sessionId)
            throws ServletException, IOException {
        if (session == null || (GenesysUser) session.getAttribute("guser") == null) {
            log.warning("[" + sessionId + "] handleSearchCall | no session, redirecting");
            request.setAttribute("message", "Your session has expired. Please log in again.");
            request.getRequestDispatcher("/relogin.jsp").forward(request, response);
            return;
        }
        GenesysUser guser = (GenesysUser) session.getAttribute("guser");
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
            log.warning("[" + sessionId + "] handleSearchCall | invalid currentpage, defaulting to 1");
        }
        Properties cs = ConfigServlet.getProperties();
        int pageSize = 50;
        try {
            String ps = cs.getProperty("pageSize");
            if (ps != null) {
                pageSize = Integer.parseInt(ps);
            }
        } catch (NumberFormatException e) {
            log.warning("[" + sessionId + "] handleSearchCall | invalid pageSize config, defaulting to 50");
        }
        String dateFromFormatted = convertToUtc(dateFrom, sessionId);
        String dateToFormatted   = convertToUtc(dateTo, sessionId);
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
        log.info("[" + sessionId + "] handleSearchCall | hits=" + totalHits + " pages=" + totalPages + " shown=" + conversations.size());
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
        request.getRequestDispatcher("/SearchCall.jsp").forward(request, response);
    }
    private void handlePlayAudio(HttpServletRequest request, HttpServletResponse response,
                                 HttpSession session, String sessionId)
            throws ServletException, IOException {
        if (session == null) {
            log.warning("[" + sessionId + "] handlePlayAudio | no session");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "No active session.");
            return;
        }
        GenesysUser guser = (GenesysUser) session.getAttribute("guser");
        if (guser == null) {
            log.warning("[" + sessionId + "] handlePlayAudio | no guser");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "User session expired.");
            return;
        }
        String conversationId = request.getParameter("convId");
        if (StringUtils.isBlank(conversationId)) {
            log.warning("[" + sessionId + "] handlePlayAudio | convId blank");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing required parameter: convId");
            return;
        }
        byte[] audioData = getOrCreateAudioCache(session, sessionId, guser, conversationId);
        if (audioData == null || audioData.length == 0) {
            log.warning("[" + sessionId + "] handlePlayAudio | no audio, convId=" + conversationId);
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Audio not available for the requested conversation.");
            return;
        }
        serveAudioFromMemory(audioData, request, response, sessionId, conversationId);
    }
    @SuppressWarnings("unchecked")
    private byte[] getOrCreateAudioCache(HttpSession session, String sessionId,
                                           GenesysUser guser, String conversationId) {
        ConcurrentHashMap<String, byte[]> audioCache =
                (ConcurrentHashMap<String, byte[]>) session.getAttribute("audioCache");
        if (audioCache == null) {
            synchronized (session) {
                audioCache = (ConcurrentHashMap<String, byte[]>) session.getAttribute("audioCache");
                if (audioCache == null) {
                    audioCache = new ConcurrentHashMap<>();
                    session.setAttribute("audioCache", audioCache);
                }
            }
        }
        byte[] cached = audioCache.get(conversationId);
        if (cached != null && cached.length > 0) {
            log.info("[" + sessionId + "] audioCache | HIT convId=" + conversationId + " size=" + cached.length + "B");
            return cached;
        }
        log.info("[" + sessionId + "] audioCache | MISS convId=" + conversationId);
        InputStream audioStream = tperService.getMorphedAudioStream(sessionId, guser, conversationId);
        if (audioStream == null) {
            log.warning("[" + sessionId + "] audioCache | stream null, convId=" + conversationId);
            return null;
        }
        try (InputStream in = audioStream) {
            byte[] audioBytes = in.readAllBytes();
            audioCache.put(conversationId, audioBytes);
            log.info("[" + sessionId + "] audioCache | cached convId=" + conversationId + " size=" + audioBytes.length + "B");
            return audioBytes;
        } catch (IOException e) {
            log.log(Level.SEVERE, "[" + sessionId + "] audioCache | read failed, convId=" + conversationId, e);
            return null;
        }
    }
    private void serveAudioFromMemory(byte[] audioData, HttpServletRequest request,
                                     HttpServletResponse response, String sessionId,
                                     String conversationId) throws IOException {
        long totalLength = audioData.length;
        String rangeHeader = request.getHeader("Range");
        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            String rangeValue = rangeHeader.substring("bytes=".length());
            String[] parts = rangeValue.split("-");
            long start = Long.parseLong(parts[0]);
            long end = (parts.length > 1 && !parts[1].isEmpty())
                    ? Long.parseLong(parts[1])
                    : totalLength - 1;
            if (start >= totalLength) {
                log.warning("[" + sessionId + "] serveAudio | range out of bounds, convId=" + conversationId);
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
            log.fine("[" + sessionId + "] serveAudio | range " + start + "-" + end + "/" + totalLength);
            try (OutputStream out = response.getOutputStream()) {
                out.write(audioData, (int) start, (int) contentLength);
                out.flush();
            }
        } else {
            response.setContentType("audio/wav");
            response.setHeader("Content-Length", String.valueOf(totalLength));
            response.setHeader("Accept-Ranges", "bytes");
            response.setHeader("Content-Disposition", "inline");
            log.fine("[" + sessionId + "] serveAudio | full " + totalLength + "B, convId=" + conversationId);
            try (OutputStream out = response.getOutputStream()) {
                out.write(audioData);
                out.flush();
            }
        }
    }
    private void handleToggleRetention(HttpServletRequest request, HttpServletResponse response,
                                       HttpSession session, String sessionId)
            throws ServletException, IOException {
        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");
        if (session == null) {
            log.warning("[" + sessionId + "] toggleRetention | no session");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("error:no_session");
            return;
        }
        GenesysUser guser = (GenesysUser) session.getAttribute("guser");
        if (guser == null) {
            log.warning("[" + sessionId + "] toggleRetention | no guser");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("error:no_session");
            return;
        }
        String conversationId = request.getParameter("conversationId");
        String convStart      = request.getParameter("convStart");
        String lockState      = request.getParameter("lockState");
        if (StringUtils.isBlank(conversationId)) {
            log.warning("[" + sessionId + "] toggleRetention | convId blank");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("error:missing_conversation_id");
            return;
        }
        log.info("[" + sessionId + "] toggleRetention | convId=" + conversationId + " lock=" + lockState);
        boolean success;
        if ("true".equalsIgnoreCase(lockState)) {
            success = tperService.extendPersonalRetention(sessionId, guser, conversationId, convStart);
        } else {
            success = tperService.revertPersonalRetention(sessionId, guser, conversationId, convStart);
        }
        if (success) {
            response.getWriter().write("success");
        } else {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("error:api_failure");
            log.warning("[" + sessionId + "] toggleRetention | api_failure, convId=" + conversationId);
        }
    }

    private void handleCancelAudio(HttpServletRequest request, HttpServletResponse response,
                                   HttpSession session, String sessionId) throws IOException {
        String convId = request.getParameter("convId");
        if (org.apache.commons.lang3.StringUtils.isNotBlank(convId)) {
            comapp.cloud.Genesys.cancelMap.put(convId, true);
            log.info("[" + sessionId + "] cancelAudio | convId=" + convId);
        }
        response.getWriter().write("cancelled");
    }

    private void handleLogin(HttpServletRequest request, HttpServletResponse response,
                             String sessionId)
            throws ServletException, IOException {
        String code = StringUtils.defaultString(request.getParameter("code"), "");
        HttpSession session = request.getSession(true);
        sessionId = session.getId();
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
            if (!enabledSecurity) {
                log.info("[" + sessionId + "] handleLogin | security disabled, creating dummy user");
                String username = StringUtils.defaultString(request.getParameter("username"), "");
                GenesysUser guser = new GenesysUser(sessionId, guiClientId,
                        guiClientSecret, urlRegion, redirectUri, urlAuthorize);
                session.setAttribute("guser", guser);
                JSONObject dummyUser = new JSONObject();
                dummyUser.put("name", StringUtils.isNotBlank(username) ? username : "admin");
                dummyUser.put("id", "admin");
                session.setAttribute("gui_user", dummyUser);
                response.sendRedirect(response.encodeRedirectURL("tperApp?action=searchCall"));
                return;
            }
            if (StringUtils.isBlank(code)) {
                String authorizeUrl = urlAuthorize
                        + "?client_id=" + guiClientId
                        + "&response_type=code"
                        + "&redirect_uri=" + redirectUri;
                log.info("[" + sessionId + "] handleLogin | redirecting to OAuth");
                response.sendRedirect(authorizeUrl);
                return;
            }
            log.info("[" + sessionId + "] handleLogin | OAuth code received, authenticating...");
            GenesysUser guser = new GenesysUser(sessionId, guiClientId,
                    guiClientSecret, urlRegion, redirectUri, urlAuthorize);
            guser.setCode(code);
            guser.getToken(false);
            Genesys.fetchUserGroups(guser);
            log.info("[" + sessionId + "] handleLogin | authenticated, groups=" + guser.getUserGroups().size());
            session.setAttribute("guser", guser);
            session.setAttribute("userGroups", guser.getUserGroups());
            response.sendRedirect(response.encodeRedirectURL("tperApp?action=searchCall"));
        } catch (Exception e) {
            log.log(Level.SEVERE, "[" + sessionId + "] handleLogin | auth failed", e);
            request.setAttribute("message", "Authentication failed. Please try again.");
            request.getRequestDispatcher("/relogin.jsp").forward(request, response);
        }
    }
    private void handleLogout(HttpServletRequest request, HttpServletResponse response,
                              String sessionId)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            cleanupAudioCache(session, sessionId);
            session.removeAttribute("guser");
            session.removeAttribute("userGroups");
            session.removeAttribute("gui_user");
            session.removeAttribute("audioCache");
            log.info("[" + sessionId + "] handleLogout | session cleared");
        }
        request.setAttribute("message", "You have been logged out successfully. Would you like to log in again?");
        request.getRequestDispatcher("/relogin.jsp").forward(request, response);
    }
    @SuppressWarnings("unchecked")
    private void cleanupAudioCache(HttpSession session, String sessionId) {
        ConcurrentHashMap<String, byte[]> audioCache =
                (ConcurrentHashMap<String, byte[]>) session.getAttribute("audioCache");
        if (audioCache == null || audioCache.isEmpty()) {
            return;
        }
        int count = audioCache.size();
        audioCache.clear();
        log.info("[" + sessionId + "] cleanupAudioCache | cleared " + count + " entries");
    }
    private String convertToUtc(String localDateStr, String sessionId) {
        if (StringUtils.isBlank(localDateStr)) {
            return null;
        }
        try {
            DateTimeFormatter localFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
            DateTimeFormatter utcFormatter   = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
            LocalDateTime localDateTime      = LocalDateTime.parse(localDateStr, localFormatter);
            ZonedDateTime localZonedDateTime  = localDateTime.atZone(ZoneId.of("Europe/Rome"));
            ZonedDateTime utcDateTime         = localZonedDateTime.withZoneSameInstant(ZoneId.of("UTC"));
            return utcDateTime.format(utcFormatter);
        } catch (Exception e) {
            log.log(Level.WARNING, "[" + sessionId + "] convertToUtc | failed for: " + localDateStr, e);
            return null;
        }
    }
}
