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

        log.info("[" + sessionId + "] searchCalls | range=" + sStart + "~" + sEnd
                + " ani=" + ani + " dnis=" + dnis + " convId=" + conversationId
                + " queue=" + queue + " page=" + pageNumber + "/" + pageSize);
        Map<String, Object> result = null;
        try {
            result = analyzerRepository.searchCallsInDatabase(
                    sStart, sEnd, ani, dnis,
                    conversationId, queue, operator,
                    userGroups, enableGroupFilter,
                    pageNumber, pageSize, order);

            if (result != null) {
                log.info("[" + sessionId + "] searchCalls | total=" + result.get("totalCount")
                        + " returned=" + ((List<?>) result.get("results")).size());
            } else {
                log.warning("[" + sessionId + "] searchCalls | db returned null");
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "[" + sessionId + "] searchCalls | failed", e);
        }
        return result;
    }
    public InputStream getMorphedAudioStream(String sessionId, GenesysUser guser,
                                             String conversationId) {
        log.info("[" + sessionId + "] getMorphedAudio | convId=" + conversationId);
        InputStream morphedStream = null;
        try {
            JSONArray recorderList = Genesys.getRecorderList(getTperSystemUser(sessionId), conversationId, AudioType.WAV);

            if (recorderList == null || recorderList.length() == 0) {
                log.warning("[" + sessionId + "] getMorphedAudio | no recordings, convId=" + conversationId);
                return null;
            }
            String audioUrl = Genesys.getAudioUrl(getTperSystemUser(sessionId), recorderList.getJSONObject(0),
                                                   AudioType.WAV, null, null);

            if (audioUrl == null || audioUrl.isBlank()) {
                log.warning("[" + sessionId + "] getMorphedAudio | no audio URL, convId=" + conversationId);
                return null;
            }
            audioUrl = audioUrl.replaceAll("\"", "");
            log.info("[" + sessionId + "] getMorphedAudio | " + recorderList.length() + " recording(s), morphing convId=" + conversationId);
            morphedStream = morphingService.processAudio(audioUrl);
            if (morphedStream == null) {
                log.warning("[" + sessionId + "] getMorphedAudio | morphing returned null, convId=" + conversationId);
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "[" + sessionId + "] getMorphedAudio | failed, convId=" + conversationId, e);
        }
        return morphedStream;
    }
    public boolean extendPersonalRetention(String sessionId, GenesysUser guser,
                                           String conversationId, String conversationStart) {
        log.info("[" + sessionId + "] extendRetention | convId=" + conversationId + " start=" + conversationStart);
        try {
            if (conversationId == null || conversationId.isBlank()) {
                log.warning("[" + sessionId + "] extendRetention | convId is blank");
                return false;
            }
            if (conversationStart == null || conversationStart.isBlank()) {
                log.warning("[" + sessionId + "] extendRetention | convStart is blank");
                return false;
            }
            int yearsToKeep = Integer.parseInt(
                    ConfigServlet.getProperties().getProperty("retention.years", "17"));
            if (yearsToKeep <= 0) {
                log.warning("[" + sessionId + "] extendRetention | invalid years=" + yearsToKeep);
                return false;
            }
            ZonedDateTime startDate = ZonedDateTime.parse(conversationStart,
                    DateTimeFormatter.ISO_DATE_TIME.withZone(ZoneOffset.UTC));
            String deleteDate = startDate.plusYears(yearsToKeep).format(DateTimeFormatter.ISO_INSTANT);

            JSONArray recordings = Genesys.getRecorderList(getTperSystemUser(sessionId), conversationId, Genesys.AudioType.NONE);
            if (recordings == null || recordings.length() == 0) {
                throw new Exception("No recordings found for conversation " + conversationId);
            }
            log.info("[" + sessionId + "] extendRetention | " + recordings.length() + " rec(s), deleteDate=" + deleteDate);
            for (int i = 0; i < recordings.length(); i++) {
                String recId = recordings.getJSONObject(i).getString("id");
                Genesys.updateRecordingRetention(getTperSystemUser(sessionId), conversationId, recId, deleteDate);
            }
            log.info("[" + sessionId + "] extendRetention | done, convId=" + conversationId);
            analyzerRepository.saveRetentionLocked(conversationId, true);
            return true;
        } catch (Exception e) {
            log.log(Level.SEVERE, "[" + sessionId + "] extendRetention | failed, convId=" + conversationId, e);
            return false;
        }
    }
    public boolean revertPersonalRetention(String sessionId, GenesysUser guser,
                                           String conversationId, String conversationStart) {
        log.info("[" + sessionId + "] revertRetention | convId=" + conversationId + " start=" + conversationStart);
        try {
            if (conversationId == null || conversationId.isBlank()) {
                log.warning("[" + sessionId + "] revertRetention | convId is blank");
                return false;
            }
            if (conversationStart == null || conversationStart.isBlank()) {
                log.warning("[" + sessionId + "] revertRetention | convStart is blank");
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
            log.info("[" + sessionId + "] revertRetention | " + recordings.length() + " rec(s), deleteDate=" + deleteDate + " (in " + daysFromNow + "d)");
            for (int i = 0; i < recordings.length(); i++) {
                String recId = recordings.getJSONObject(i).getString("id");
                Genesys.updateRecordingRetention(getTperSystemUser(sessionId), conversationId, recId, deleteDate);
            }
            log.info("[" + sessionId + "] revertRetention | done, convId=" + conversationId);
            analyzerRepository.saveRetentionLocked(conversationId, false);
            return true;
        } catch (Exception e) {
            log.log(Level.SEVERE, "[" + sessionId + "] revertRetention | failed, convId=" + conversationId, e);
            return false;
        }
    }
}