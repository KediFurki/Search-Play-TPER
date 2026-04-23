package comapp.service;

import java.io.InputStream;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import comapp.ConfigServlet;
import comapp.cloud.Genesys;
import comapp.cloud.Genesys.AudioType;
import comapp.cloud.GenesysUser;
import comapp.db.AnalyzerRepository;

public class TperService {

    private static final Logger log = Logger.getLogger("comapp");

    private static comapp.cloud.GenesysUser tperSystemUser = null;
    private final MorphingService morphingService = new MorphingService();
    private final AnalyzerRepository analyzerRepository = new AnalyzerRepository();

    private static String shortSid(String sid) {
        if (sid == null || sid.length() <= 8) return sid == null ? "-" : sid;
        return sid.substring(sid.length() - 8);
    }
    private static String ctx(String sid, GenesysUser g) {
        String who = (g == null) ? "-" : g.getLogId();
        return "[sid=" + shortSid(sid) + " user=" + who + "]";
    }

    private synchronized comapp.cloud.GenesysUser getTperSystemUser(String sessionId) throws Exception {
        if (tperSystemUser == null) {
            java.util.Properties props = comapp.ConfigServlet.getProperties();
            String tperClientId = props.getProperty("tper_clientId");
            String tperClientSecret = props.getProperty("tper_clientSecret");
            String urlRegion = props.getProperty("urlRegion");
            if (org.apache.commons.lang3.StringUtils.isBlank(tperClientId) || org.apache.commons.lang3.StringUtils.isBlank(tperClientSecret)) {
                throw new Exception("tper_clientId or tper_clientSecret is missing in SP_Lite.properties");
            }
            tperSystemUser = new comapp.cloud.GenesysUser("SYSTEM_" + sessionId, tperClientId, tperClientSecret, urlRegion, "", "");
            tperSystemUser.setCode(null);
        }
        tperSystemUser.getToken(false);
        return tperSystemUser;
    }

    public Map<String, Object> searchCalls(String sessionId, GenesysUser guser,
                                                  String sStart, String sEnd,
                                                  String ani, String dnis,
                                                  String conversationId, String queue, String operator,
                                                  List<String> userGroups, boolean enableGroupFilter,
                                                  int pageNumber, int pageSize, String order) {

        log.info(ctx(sessionId, guser) + " search.db | START range=" + sStart + "~" + sEnd
                + " ani=" + (ani==null||ani.isEmpty()?"-":ani)
                + " dnis=" + (dnis==null||dnis.isEmpty()?"-":dnis)
                + " convId=" + (conversationId==null||conversationId.isEmpty()?"-":conversationId)
                + " queue=" + (queue==null||queue.isEmpty()?"-":queue)
                + " page=" + pageNumber + "/" + pageSize
                + " groupFilter=" + enableGroupFilter);
        Map<String, Object> result = null;
        long t0 = System.currentTimeMillis();
        try {
            result = analyzerRepository.searchCallsInDatabase(
                    sStart, sEnd, ani, dnis,
                    conversationId, queue, operator,
                    userGroups, enableGroupFilter,
                    pageNumber, pageSize, order);

            long dur = System.currentTimeMillis() - t0;
            if (result != null) {
                log.info(ctx(sessionId, guser) + " search.db | DONE total=" + result.get("totalCount")
                        + " returned=" + ((List<?>) result.get("results")).size()
                        + " took=" + dur + "ms");
            } else {
                log.warning(ctx(sessionId, guser) + " search.db | DONE result=null took=" + dur + "ms");
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, ctx(sessionId, guser) + " search.db | FAILED took="
                    + (System.currentTimeMillis() - t0) + "ms", e);
        }
        return result;
    }
    public InputStream getMorphedAudioStream(String sessionId, GenesysUser guser,
                                             String conversationId) {
        log.info(ctx(sessionId, guser) + " audio.fetch | START convId=" + conversationId);
        InputStream morphedStream = null;
        long t0 = System.currentTimeMillis();
        try {
            JSONArray recorderList = Genesys.getRecorderList(getTperSystemUser(sessionId), conversationId, AudioType.WAV);

            if (recorderList == null || recorderList.length() == 0) {
                log.warning(ctx(sessionId, guser) + " audio.fetch | no recordings convId=" + conversationId);
                return null;
            }
            String audioUrl = Genesys.getAudioUrl(getTperSystemUser(sessionId), recorderList.getJSONObject(0),
                                                   AudioType.WAV, null, null);

            if (audioUrl == null || audioUrl.isBlank()) {
                log.warning(ctx(sessionId, guser) + " audio.fetch | no audio URL convId=" + conversationId);
                return null;
            }
            audioUrl = audioUrl.replaceAll("\"", "");
            log.info(ctx(sessionId, guser) + " audio.fetch | recordings=" + recorderList.length()
                    + " morphing convId=" + conversationId);
            morphedStream = morphingService.processAudio(audioUrl);
            long dur = System.currentTimeMillis() - t0;
            if (morphedStream == null) {
                log.warning(ctx(sessionId, guser) + " audio.fetch | morph=null convId=" + conversationId
                        + " took=" + dur + "ms");
            } else {
                log.info(ctx(sessionId, guser) + " audio.fetch | DONE convId=" + conversationId
                        + " took=" + dur + "ms");
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, ctx(sessionId, guser) + " audio.fetch | FAILED convId=" + conversationId, e);
        }
        return morphedStream;
    }
    public boolean extendPersonalRetention(String sessionId, GenesysUser guser,
                                           String conversationId, String conversationStart) {
        log.info(ctx(sessionId, guser) + " retention.lock | START convId=" + conversationId
                + " start=" + conversationStart);
        try {
            if (conversationId == null || conversationId.isBlank()) {
                log.warning(ctx(sessionId, guser) + " retention.lock | convId blank");
                return false;
            }
            if (conversationStart == null || conversationStart.isBlank()) {
                log.warning(ctx(sessionId, guser) + " retention.lock | convStart blank");
                return false;
            }
            int yearsToKeep = Integer.parseInt(
                    ConfigServlet.getProperties().getProperty("retention.years", "17"));
            if (yearsToKeep <= 0) {
                log.warning(ctx(sessionId, guser) + " retention.lock | invalid years=" + yearsToKeep);
                return false;
            }
            ZonedDateTime startDate = ZonedDateTime.parse(conversationStart,
                    DateTimeFormatter.ISO_DATE_TIME.withZone(ZoneOffset.UTC));
            String deleteDate = startDate.plusYears(yearsToKeep).format(DateTimeFormatter.ISO_INSTANT);

            JSONArray recordings = Genesys.getRecorderList(getTperSystemUser(sessionId), conversationId, Genesys.AudioType.NONE);
            if (recordings == null || recordings.length() == 0) {
                throw new Exception("No recordings found for conversation " + conversationId);
            }
            log.info(ctx(sessionId, guser) + " retention.lock | recordings=" + recordings.length()
                    + " deleteDate=" + deleteDate);
            for (int i = 0; i < recordings.length(); i++) {
                String recId = recordings.getJSONObject(i).getString("id");
                Genesys.updateRecordingRetention(getTperSystemUser(sessionId), conversationId, recId, deleteDate);
            }
            analyzerRepository.saveRetentionLocked(conversationId, true);
            log.info(ctx(sessionId, guser) + " retention.lock | DONE convId=" + conversationId);
            return true;
        } catch (Exception e) {
            log.log(Level.SEVERE, ctx(sessionId, guser) + " retention.lock | FAILED convId=" + conversationId, e);
            return false;
        }
    }
    public boolean revertPersonalRetention(String sessionId, GenesysUser guser,
                                           String conversationId, String conversationStart) {
        log.info(ctx(sessionId, guser) + " retention.unlock | START convId=" + conversationId
                + " start=" + conversationStart);
        try {
            if (conversationId == null || conversationId.isBlank()) {
                log.warning(ctx(sessionId, guser) + " retention.unlock | convId blank");
                return false;
            }
            if (conversationStart == null || conversationStart.isBlank()) {
                log.warning(ctx(sessionId, guser) + " retention.unlock | convStart blank");
                return false;
            }
            ZonedDateTime startDate = ZonedDateTime.parse(conversationStart,
                    DateTimeFormatter.ISO_DATE_TIME.withZone(ZoneOffset.UTC));
            ZonedDateTime retentionDate = startDate.plusDays(90);
            String deleteDate = retentionDate.format(DateTimeFormatter.ISO_INSTANT);
            long daysFromNow = ChronoUnit.DAYS.between(ZonedDateTime.now(ZoneOffset.UTC), retentionDate);

            JSONArray recordings = Genesys.getRecorderList(getTperSystemUser(sessionId), conversationId, Genesys.AudioType.NONE);
            if (recordings == null || recordings.length() == 0) {
                throw new Exception("No recordings found for conversation " + conversationId);
            }
            log.info(ctx(sessionId, guser) + " retention.unlock | recordings=" + recordings.length()
                    + " deleteDate=" + deleteDate + " (in " + daysFromNow + "d)");
            for (int i = 0; i < recordings.length(); i++) {
                String recId = recordings.getJSONObject(i).getString("id");
                Genesys.updateRecordingRetention(getTperSystemUser(sessionId), conversationId, recId, deleteDate);
            }
            analyzerRepository.saveRetentionLocked(conversationId, false);
            log.info(ctx(sessionId, guser) + " retention.unlock | DONE convId=" + conversationId);
            return true;
        } catch (Exception e) {
            log.log(Level.SEVERE, ctx(sessionId, guser) + " retention.unlock | FAILED convId=" + conversationId, e);
            return false;
        }
    }
}