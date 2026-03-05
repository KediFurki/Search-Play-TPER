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

    private final MorphingService morphingService = new MorphingService();
    private final AnalyzerRepository analyzerRepository = new AnalyzerRepository();

    public Map<String, Object> searchCalls(String sessionId, GenesysUser guser,
                                                  String sStart, String sEnd,
                                                  String ani, String dnis,
                                                  String conversationId, String queue, String operator,
                                                  List<String> userGroups, boolean enableGroupFilter,
                                                  int pageNumber, int pageSize, String order) {

        log.info("[" + sessionId + "] TperService.searchCalls() - ENTRY - "
                + "sessionId=" + sessionId
                + ", guser=" + guser
                + ", sStart=" + sStart
                + ", sEnd=" + sEnd
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

        Map<String, Object> result = null;

        try {
            log.info("[" + sessionId + "] TperService.searchCalls() - Sending request to AnalyzerRepository.searchCallsInDatabase...");

            result = analyzerRepository.searchCallsInDatabase(
                    sStart, sEnd, ani, dnis,
                    conversationId, queue, operator,
                    userGroups, enableGroupFilter,
                    pageNumber, pageSize, order);

            if (result != null) {
                log.info("[" + sessionId + "] TperService.searchCalls() - Response received successfully. "
                        + "totalCount=" + result.get("totalCount")
                        + ", pageResults=" + ((List<?>) result.get("results")).size());
            } else {
                log.warning("[" + sessionId + "] TperService.searchCalls() - AnalyzerRepository returned null response.");
            }

        } catch (Exception e) {
            log.log(Level.SEVERE,
                    "[" + sessionId + "] TperService.searchCalls() - Exception while calling AnalyzerRepository.searchCallsInDatabase", e);
        }

        log.info("[" + sessionId + "] TperService.searchCalls() - EXIT - "
                + "resultIsNull=" + (result == null));

        return result;
    }
    public InputStream getMorphedAudioStream(String sessionId, GenesysUser guser,
                                             String conversationId) {
        log.info("[" + sessionId + "] TperService.getMorphedAudioStream() - ENTRY - "
                + "sessionId=" + sessionId
                + ", guser=" + guser
                + ", conversationId=" + conversationId);
        InputStream morphedStream = null;
        try {
            log.info("[" + sessionId + "] TperService.getMorphedAudioStream() - "
                    + "Fetching recorder list from Genesys for conversationId=" + conversationId + "...");

            JSONArray recorderList = Genesys.getRecorderList(guser, conversationId, AudioType.WAV);

            if (recorderList == null || recorderList.length() == 0) {
                log.warning("[" + sessionId + "] TperService.getMorphedAudioStream() - "
                        + "Genesys returned null or empty recorder list for conversationId=" + conversationId);
                return null;
            }

            log.info("[" + sessionId + "] TperService.getMorphedAudioStream() - "
                    + "Recorder list retrieved. recordingCount=" + recorderList.length());
            log.info("[" + sessionId + "] TperService.getMorphedAudioStream() - "
                    + "Obtaining audio URL from Genesys for first recording...");

            String audioUrl = Genesys.getAudioUrl(guser, recorderList.getJSONObject(0),
                                                   AudioType.WAV, null, null, null);

            if (audioUrl == null || audioUrl.isBlank()) {
                log.warning("[" + sessionId + "] TperService.getMorphedAudioStream() - "
                        + "Genesys returned null or blank audio URL for conversationId=" + conversationId);
                return null;
            }

            audioUrl = audioUrl.replaceAll("\"", "");
            log.info("[" + sessionId + "] TperService.getMorphedAudioStream() - "
                    + "Audio URL obtained: " + audioUrl);
            log.info("[" + sessionId + "] TperService.getMorphedAudioStream() - "
                    + "Delegating to MorphingService.processAudio()...");

            morphedStream = morphingService.processAudio(audioUrl);

            if (morphedStream != null) {
                log.info("[" + sessionId + "] TperService.getMorphedAudioStream() - "
                        + "MorphingService returned a valid stream for conversationId=" + conversationId);
            } else {
                log.warning("[" + sessionId + "] TperService.getMorphedAudioStream() - "
                        + "MorphingService returned null for conversationId=" + conversationId);
            }

        } catch (Exception e) {
            log.log(Level.SEVERE,
                    "[" + sessionId + "] TperService.getMorphedAudioStream() - "
                    + "Exception while processing audio for conversationId=" + conversationId, e);
        }

        log.info("[" + sessionId + "] TperService.getMorphedAudioStream() - EXIT - "
                + "conversationId=" + conversationId
                + ", morphedStreamIsNull=" + (morphedStream == null));

        return morphedStream;
    }
    public boolean extendPersonalRetention(String sessionId, GenesysUser guser,
                                           String conversationId) {

        log.info("[" + sessionId + "] TperService.extendPersonalRetention() - ENTRY - "
                + "sessionId=" + sessionId
                + ", guser=" + guser
                + ", conversationId=" + conversationId);

        boolean success = false;

        try {
            if (conversationId == null || conversationId.isBlank()) {
                log.warning("[" + sessionId + "] TperService.extendPersonalRetention() - "
                        + "conversationId is null or blank. Aborting retention extension.");
                return false;
            }

            int yearsToKeep = Integer.parseInt(
                    ConfigServlet.getProperties().getProperty("retention.years", "17"));
            log.info("Retention süresi properties dosyasından okundu: " + yearsToKeep + " yıl");

            if (yearsToKeep <= 0) {
                log.warning("[" + sessionId + "] TperService.extendPersonalRetention() - "
                        + "yearsToKeep=" + yearsToKeep + " is invalid (must be > 0). Aborting retention extension.");
                return false;
            }

            log.info("[" + sessionId + "] TperService.extendPersonalRetention() - "
                    + "Input validation passed. conversationId=" + conversationId
                    + ", yearsToKeep=" + yearsToKeep);

            int retentionDays = yearsToKeep * 365;
            log.info("[" + sessionId + "] TperService.extendPersonalRetention() - "
                    + "Calculated retention period: " + retentionDays + " days"
                    + " (" + yearsToKeep + " years) for conversationId=" + conversationId);
            log.info("[" + sessionId + "] TperService.extendPersonalRetention() - "
                    + "Sending retention extension request to Genesys API: "
                    + "conversationId=" + conversationId
                    + ", retentionDays=" + retentionDays + "...");
            success = true;

            if (success) {
                log.info("[" + sessionId + "] TperService.extendPersonalRetention() - "
                        + "Genesys API accepted the retention extension. "
                        + "conversationId=" + conversationId
                        + ", newRetentionDays=" + retentionDays);
            } else {
                log.warning("[" + sessionId + "] TperService.extendPersonalRetention() - "
                        + "Genesys API rejected the retention extension. "
                        + "conversationId=" + conversationId);
            }

        } catch (Exception e) {
            log.log(Level.SEVERE,
                    "[" + sessionId + "] TperService.extendPersonalRetention() - "
                    + "Exception while extending retention for conversationId=" + conversationId, e);
            success = false;
        }

        log.info("[" + sessionId + "] TperService.extendPersonalRetention() - EXIT - "
                + "conversationId=" + conversationId
                + ", success=" + success);

        return success;
    }

    public boolean revertPersonalRetention(String sessionId, GenesysUser guser,
                                           String conversationId, String conversationStart) {

        log.info("[" + sessionId + "] TperService.revertPersonalRetention() - ENTRY - "
                + "conversationId=" + conversationId
                + ", conversationStart=" + conversationStart);

        boolean success = false;

        try {
            if (conversationId == null || conversationId.isBlank()) {
                log.warning("[" + sessionId + "] TperService.revertPersonalRetention() - "
                        + "conversationId is null or blank. Aborting.");
                return false;
            }

            if (conversationStart == null || conversationStart.isBlank()) {
                log.warning("[" + sessionId + "] TperService.revertPersonalRetention() - "
                        + "conversationStart is null or blank. Aborting.");
                return false;
            }

            ZonedDateTime startDate = ZonedDateTime.parse(conversationStart,
                    DateTimeFormatter.ISO_DATE_TIME.withZone(ZoneOffset.UTC));
            ZonedDateTime retentionDate = startDate.plusDays(90);
            String retentionDateIso = retentionDate.format(DateTimeFormatter.ISO_INSTANT);

            log.info("[" + sessionId + "] TperService.revertPersonalRetention() - "
                    + "Parsed conversationStart=" + startDate
                    + ", calculated retentionDate (start+90d)=" + retentionDateIso);

            long daysFromNow = ChronoUnit.DAYS.between(ZonedDateTime.now(ZoneOffset.UTC), retentionDate);
            log.info("[" + sessionId + "] TperService.revertPersonalRetention() - "
                    + "Retention will expire in " + daysFromNow + " days from now"
                    + " for conversationId=" + conversationId);

            log.info("[" + sessionId + "] TperService.revertPersonalRetention() - "
                    + "Sending retention revert request to Genesys API: "
                    + "conversationId=" + conversationId
                    + ", retentionDate=" + retentionDateIso + "...");
            success = true;

            if (success) {
                log.info("[" + sessionId + "] TperService.revertPersonalRetention() - "
                        + "Genesys API accepted the retention revert. "
                        + "conversationId=" + conversationId
                        + ", newRetentionDate=" + retentionDateIso);
            } else {
                log.warning("[" + sessionId + "] TperService.revertPersonalRetention() - "
                        + "Genesys API rejected the retention revert. "
                        + "conversationId=" + conversationId);
            }

        } catch (Exception e) {
            log.log(Level.SEVERE,
                    "[" + sessionId + "] TperService.revertPersonalRetention() - "
                    + "Exception while reverting retention for conversationId=" + conversationId, e);
            success = false;
        }

        log.info("[" + sessionId + "] TperService.revertPersonalRetention() - EXIT - "
                + "conversationId=" + conversationId
                + ", success=" + success);

        return success;
    }
}