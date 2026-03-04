package comapp.service;

import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import comapp.cloud.Genesys;
import comapp.cloud.Genesys.AudioType;
import comapp.cloud.GenesysUser;

public class TperService {

    private static final Logger log = Logger.getLogger("comapp");

    private final MorphingService morphingService = new MorphingService();

    public JSONObject searchCalls(String sessionId, GenesysUser guser,
                                  String sStart, String sEnd,
                                  String ani, String dnis,
                                  int pageNumber, int pageSize, String order) {

        log.info("[" + sessionId + "] TperService.searchCalls() - ENTRY - "
                + "sessionId=" + sessionId
                + ", guser=" + guser
                + ", sStart=" + sStart
                + ", sEnd=" + sEnd
                + ", ani=" + ani
                + ", dnis=" + dnis
                + ", pageNumber=" + pageNumber
                + ", pageSize=" + pageSize
                + ", order=" + order);

        JSONObject result = null;
        StringBuffer details = new StringBuffer();

        try {
            log.info("[" + sessionId + "] TperService.searchCalls() - Sending request to Genesys.getConversationList...");

            result = Genesys.getConversationList(sessionId, guser, sStart, sEnd,
                                                  ani, dnis, pageNumber, pageSize,
                                                  order, details);

            if (result != null) {
                int totalHits = result.optInt("totalHits", 0);
                log.info("[" + sessionId + "] TperService.searchCalls() - Response received successfully. "
                        + "totalHits=" + totalHits
                        + ", details=" + details);
            } else {
                log.warning("[" + sessionId + "] TperService.searchCalls() - Genesys returned null response. "
                        + "details=" + details);
            }

        } catch (Exception e) {
            log.log(Level.SEVERE,
                    "[" + sessionId + "] TperService.searchCalls() - Exception while calling Genesys.getConversationList", e);
        }

        log.info("[" + sessionId + "] TperService.searchCalls() - EXIT - "
                + "resultIsNull=" + (result == null)
                + ", details=" + details);

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
                                           String conversationId, int yearsToKeep) {

        log.info("[" + sessionId + "] TperService.extendPersonalRetention() - ENTRY - "
                + "sessionId=" + sessionId
                + ", guser=" + guser
                + ", conversationId=" + conversationId
                + ", yearsToKeep=" + yearsToKeep);

        boolean success = false;

        try {
            if (conversationId == null || conversationId.isBlank()) {
                log.warning("[" + sessionId + "] TperService.extendPersonalRetention() - "
                        + "conversationId is null or blank. Aborting retention extension.");
                return false;
            }

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
                + ", yearsToKeep=" + yearsToKeep
                + ", success=" + success);

        return success;
    }
}
