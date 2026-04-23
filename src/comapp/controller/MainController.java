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

    private static String shortSid(String sessionId) {
        if (sessionId == null || sessionId.length() <= 8) return sessionId == null ? "-" : sessionId;
        return sessionId.substring(sessionId.length() - 8);
    }
    private static String ctx(String sessionId) {
        return "[sid=" + shortSid(sessionId) + "]";
    }
    private static String ctx(String sessionId, GenesysUser guser) {
        String who = (guser == null) ? "-" : guser.getLogId();
        return "[sid=" + shortSid(sessionId) + " user=" + who + "]";
    }
    private static String ctx(String sessionId, HttpSession session) {
        GenesysUser g = (session == null) ? null : (GenesysUser) session.getAttribute("guser");
        return ctx(sessionId, g);
    }

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
        String remote = request.getRemoteAddr();
        log.fine(ctx(sessionId, session) + " request | action=" + (action.isEmpty() ? "(none)" : action)
                + " ip=" + remote);
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
            log.log(Level.SEVERE, ctx(sessionId, session) + " request | unhandled exception action=" + action, e);
            request.setAttribute("error", "An unexpected error occurred. Please try again later.");
            request.getRequestDispatcher("/SearchCall.jsp").forward(request, response);
        }
    }

    private void handleSearchCall(HttpServletRequest request, HttpServletResponse response,
                                  HttpSession session, String sessionId)
            throws ServletException, IOException {
        if (session == null || (GenesysUser) session.getAttribute("guser") == null) {
            log.warning(ctx(sessionId) + " search | denied=no_session - redirecting to relogin");
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
            log.warning(ctx(sessionId, guser) + " search | invalid currentpage, defaulting to 1");
        }
        Properties cs = ConfigServlet.getProperties();
        int pageSize = 50;
        try {
            String ps = cs.getProperty("pageSize");
            if (ps != null) {
                pageSize = Integer.parseInt(ps);
            }
        } catch (NumberFormatException e) {
            log.warning(ctx(sessionId, guser) + " search | invalid pageSize config, defaulting to 50");
        }
        String dateFromFormatted = convertToUtc(dateFrom, sessionId);
        String dateToFormatted   = convertToUtc(dateTo, sessionId);
        Boolean authorizedFlag = (Boolean) session.getAttribute("authorized");
        boolean isAuthorized = (authorizedFlag == null) || authorizedFlag.booleanValue();
        if (!isAuthorized) {
            String requiredGroupName = (String) session.getAttribute("requiredGroupName");
            if (StringUtils.isBlank(requiredGroupName)) {
                requiredGroupName = "SearchAndPlay_Users";
            }
            log.warning(ctx(sessionId, guser) + " search | ACCESS DENIED | reason=not_in_group requiredGroup='"
                    + requiredGroupName + "' userGroups=" + guser.getUserGroups()
                    + " - returning empty result");
            request.setAttribute("error",
                    "Access denied by Genesys Cloud: your account is not a member of the required group '"
                            + requiredGroupName + "'. No data can be displayed.");
            request.setAttribute("conversations", new ArrayList<Map<String, String>>());
            request.setAttribute("totalHits", 0);
            request.setAttribute("totalPages", 0);
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
            return;
        }
        long t0 = System.currentTimeMillis();
        log.info(ctx(sessionId, guser) + " search | START from=" + dateFrom + " to=" + dateTo
                + " ani=" + (ani.isEmpty() ? "-" : ani)
                + " dnis=" + (dnis.isEmpty() ? "-" : dnis)
                + " convId=" + (conversationId.isEmpty() ? "-" : conversationId)
                + " queue=" + (queue.isEmpty() ? "-" : queue)
                + " operator=" + (operator.isEmpty() ? "-" : operator)
                + " page=" + currentPage + "/" + pageSize + " order=" + order);
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
        long dur = System.currentTimeMillis() - t0;
        log.info(ctx(sessionId, guser) + " search | DONE hits=" + totalHits + " pages=" + totalPages
                + " shown=" + conversations.size() + " took=" + dur + "ms");
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
            log.warning(ctx(sessionId) + " audio | denied=no_session");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "No active session.");
            return;
        }
        GenesysUser guser = (GenesysUser) session.getAttribute("guser");
        if (guser == null) {
            log.warning(ctx(sessionId) + " audio | denied=session_expired");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "User session expired.");
            return;
        }
        Boolean authorizedFlag = (Boolean) session.getAttribute("authorized");
        boolean isAuthorized = (authorizedFlag == null) || authorizedFlag.booleanValue();
        if (!isAuthorized) {
            log.warning(ctx(sessionId, guser) + " audio | ACCESS DENIED | reason=not_in_group - refusing audio stream");
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied by Genesys Cloud group policy.");
            return;
        }
        String conversationId = request.getParameter("convId");
        if (StringUtils.isBlank(conversationId)) {
            log.warning(ctx(sessionId, guser) + " audio | bad_request=convId_blank");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing required parameter: convId");
            return;
        }
        log.info(ctx(sessionId, guser) + " audio | REQUEST convId=" + conversationId);
        byte[] audioData = getOrCreateAudioCache(session, sessionId, guser, conversationId);
        if (audioData == null || audioData.length == 0) {
            log.warning(ctx(sessionId, guser) + " audio | NOT_FOUND convId=" + conversationId);
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Audio not available for the requested conversation.");
            return;
        }
        serveAudioFromMemory(audioData, request, response, sessionId, guser, conversationId);
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
            log.info(ctx(sessionId, guser) + " audio | cache=HIT convId=" + conversationId
                    + " size=" + cached.length + "B");
            return cached;
        }
        log.info(ctx(sessionId, guser) + " audio | cache=MISS convId=" + conversationId
                + " - fetching from Genesys");
        long t0 = System.currentTimeMillis();
        InputStream audioStream = tperService.getMorphedAudioStream(sessionId, guser, conversationId);
        if (audioStream == null) {
            log.warning(ctx(sessionId, guser) + " audio | fetch=FAIL convId=" + conversationId
                    + " reason=no_stream");
            return null;
        }
        try (InputStream in = audioStream) {
            byte[] audioBytes = in.readAllBytes();
            audioCache.put(conversationId, audioBytes);
            long dur = System.currentTimeMillis() - t0;
            log.info(ctx(sessionId, guser) + " audio | fetch=OK convId=" + conversationId
                    + " size=" + audioBytes.length + "B took=" + dur + "ms (cached)");
            return audioBytes;
        } catch (IOException e) {
            log.log(Level.SEVERE, ctx(sessionId, guser) + " audio | read_failed convId=" + conversationId, e);
            return null;
        }
    }
    private void serveAudioFromMemory(byte[] audioData, HttpServletRequest request,
                                     HttpServletResponse response, String sessionId,
                                     GenesysUser guser, String conversationId) throws IOException {
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
                log.warning(ctx(sessionId, guser) + " audio | serve=RANGE_OUT convId=" + conversationId
                        + " start=" + start + " total=" + totalLength);
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
            log.fine(ctx(sessionId, guser) + " audio | serve=PARTIAL convId=" + conversationId
                    + " range=" + start + "-" + end + "/" + totalLength);
            try (OutputStream out = response.getOutputStream()) {
                out.write(audioData, (int) start, (int) contentLength);
                out.flush();
            }
        } else {
            response.setContentType("audio/wav");
            response.setHeader("Content-Length", String.valueOf(totalLength));
            response.setHeader("Accept-Ranges", "bytes");
            response.setHeader("Content-Disposition", "inline");
            log.fine(ctx(sessionId, guser) + " audio | serve=FULL convId=" + conversationId
                    + " size=" + totalLength + "B");
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
            log.warning(ctx(sessionId) + " retention | denied=no_session");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("error:no_session");
            return;
        }
        GenesysUser guser = (GenesysUser) session.getAttribute("guser");
        if (guser == null) {
            log.warning(ctx(sessionId) + " retention | denied=session_expired");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("error:no_session");
            return;
        }
        Boolean authorizedFlag = (Boolean) session.getAttribute("authorized");
        boolean isAuthorized = (authorizedFlag == null) || authorizedFlag.booleanValue();
        if (!isAuthorized) {
            log.warning(ctx(sessionId, guser) + " retention | ACCESS DENIED | reason=not_in_group");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("error:forbidden");
            return;
        }
        String conversationId = request.getParameter("conversationId");
        String convStart      = request.getParameter("convStart");
        String lockState      = request.getParameter("lockState");
        if (StringUtils.isBlank(conversationId)) {
            log.warning(ctx(sessionId, guser) + " retention | bad_request=convId_blank");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("error:missing_conversation_id");
            return;
        }
        String op = "true".equalsIgnoreCase(lockState) ? "LOCK" : "UNLOCK";
        log.info(ctx(sessionId, guser) + " retention | START op=" + op
                + " convId=" + conversationId + " convStart=" + convStart);
        long t0 = System.currentTimeMillis();
        boolean success;
        if ("true".equalsIgnoreCase(lockState)) {
            success = tperService.extendPersonalRetention(sessionId, guser, conversationId, convStart);
        } else {
            success = tperService.revertPersonalRetention(sessionId, guser, conversationId, convStart);
        }
        long dur = System.currentTimeMillis() - t0;
        if (success) {
            log.info(ctx(sessionId, guser) + " retention | DONE op=" + op
                    + " convId=" + conversationId + " took=" + dur + "ms");
            response.getWriter().write("success");
        } else {
            log.warning(ctx(sessionId, guser) + " retention | FAIL op=" + op
                    + " convId=" + conversationId + " took=" + dur + "ms reason=api_failure");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("error:api_failure");
        }
    }

    private void handleCancelAudio(HttpServletRequest request, HttpServletResponse response,
                                   HttpSession session, String sessionId) throws IOException {
        String convId = request.getParameter("convId");
        GenesysUser guser = (session != null) ? (GenesysUser) session.getAttribute("guser") : null;
        if (org.apache.commons.lang3.StringUtils.isNotBlank(convId)) {
            comapp.cloud.Genesys.cancelMap.put(convId, true);
            log.info(ctx(sessionId, guser) + " audio | CANCEL convId=" + convId);
        }
        response.getWriter().write("cancelled");
    }

    private void handleLogin(HttpServletRequest request, HttpServletResponse response,
                             String sessionId)
            throws ServletException, IOException {
        String code = StringUtils.defaultString(request.getParameter("code"), "");
        HttpSession session = request.getSession(true);
        sessionId = session.getId();
        String remoteIp = request.getRemoteAddr();
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
                String username = StringUtils.defaultString(request.getParameter("username"), "");
                String u = StringUtils.isNotBlank(username) ? username : "admin";
                log.info(ctx(sessionId) + " login | mode=insecure (enabled_security=false) user=" + u
                        + " ip=" + remoteIp + " - SUCCESS (bypassed)");
                GenesysUser guser = new GenesysUser(sessionId, guiClientId,
                        guiClientSecret, urlRegion, redirectUri, urlAuthorize);
                guser.setUserName(u);
                guser.setUserId("admin");
                session.setAttribute("guser", guser);
                session.setAttribute("authorized", true);
                JSONObject dummyUser = new JSONObject();
                dummyUser.put("name", u);
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
                log.info(ctx(sessionId) + " login | step=redirect_to_oauth ip=" + remoteIp);
                response.sendRedirect(authorizeUrl);
                return;
            }
            log.info(ctx(sessionId) + " login | step=oauth_code_received ip=" + remoteIp + " - exchanging for token");
            GenesysUser guser = new GenesysUser(sessionId, guiClientId,
                    guiClientSecret, urlRegion, redirectUri, urlAuthorize);
            guser.setCode(code);
            guser.getToken(false);
            Genesys.fetchUserGroups(guser);

            String requiredGroupId = cs.getProperty("required_group_id", "").trim();
            String requiredGroupName = cs.getProperty("required_group_name", "SearchAndPlay_Users").trim();
            boolean enforceGroup = Boolean.parseBoolean(
                    cs.getProperty("enforce_required_group", "true"));
            boolean authorized = true;
            String authReason;
            if (enforceGroup && StringUtils.isNotBlank(requiredGroupId)) {
                List<String> ids = guser.getUserGroupIds();
                authorized = (ids != null && ids.contains(requiredGroupId));
                authReason = authorized ? "member_of_" + requiredGroupName
                                        : "NOT_member_of_" + requiredGroupName;
            } else {
                authReason = "group_check_disabled";
            }
            session.setAttribute("guser", guser);
            session.setAttribute("userGroups", guser.getUserGroups());
            session.setAttribute("userGroupIds", guser.getUserGroupIds());
            session.setAttribute("authorized", authorized);
            session.setAttribute("requiredGroupName", requiredGroupName);

            log.info(ctx(sessionId, guser) + " login | SUCCESS ip=" + remoteIp
                    + " id=" + guser.getUserId()
                    + " groups=" + guser.getUserGroups().size()
                    + " authorized=" + authorized + " reason=" + authReason);
            if (!authorized) {
                log.warning(ctx(sessionId, guser) + " login | user will see empty table"
                        + " - required group '" + requiredGroupName + "' (" + requiredGroupId + ") not found in user's groups "
                        + guser.getUserGroups());
            }
            response.sendRedirect(response.encodeRedirectURL("tperApp?action=searchCall"));
        } catch (Exception e) {
            log.log(Level.WARNING, ctx(sessionId) + " login | FAILED ip=" + remoteIp
                    + " reason=" + e.getClass().getSimpleName() + ": " + e.getMessage(), e);
            request.setAttribute("message", "Authentication failed. Please try again.");
            request.getRequestDispatcher("/relogin.jsp").forward(request, response);
        }
    }
    private void handleLogout(HttpServletRequest request, HttpServletResponse response,
                              String sessionId)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            GenesysUser guser = (GenesysUser) session.getAttribute("guser");
            cleanupAudioCache(session, sessionId, guser);
            session.removeAttribute("guser");
            session.removeAttribute("userGroups");
            session.removeAttribute("userGroupIds");
            session.removeAttribute("authorized");
            session.removeAttribute("requiredGroupName");
            session.removeAttribute("gui_user");
            session.removeAttribute("audioCache");
            log.info(ctx(sessionId, guser) + " logout | session cleared");
        } else {
            log.fine(ctx(sessionId) + " logout | no active session");
        }
        request.setAttribute("message", "You have been logged out successfully. Would you like to log in again?");
        request.getRequestDispatcher("/relogin.jsp").forward(request, response);
    }
    @SuppressWarnings("unchecked")
    private void cleanupAudioCache(HttpSession session, String sessionId, GenesysUser guser) {
        ConcurrentHashMap<String, byte[]> audioCache =
                (ConcurrentHashMap<String, byte[]>) session.getAttribute("audioCache");
        if (audioCache == null || audioCache.isEmpty()) {
            return;
        }
        int count = audioCache.size();
        audioCache.clear();
        log.info(ctx(sessionId, guser) + " audio | cache_cleared entries=" + count);
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
