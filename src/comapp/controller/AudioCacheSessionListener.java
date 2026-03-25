package comapp.controller;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;

@WebListener
public class AudioCacheSessionListener implements HttpSessionListener {

    private static final Logger log = Logger.getLogger("comapp");

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        log.fine("AudioCacheSessionListener.sessionCreated() - sessionId=" + se.getSession().getId());
    }

    @Override
    @SuppressWarnings("unchecked")
    public void sessionDestroyed(HttpSessionEvent se) {
        String sessionId = se.getSession().getId();
        log.info("AudioCacheSessionListener.sessionDestroyed() - ENTRY - sessionId=" + sessionId);

        ConcurrentHashMap<String, File> audioCache =
                (ConcurrentHashMap<String, File>) se.getSession().getAttribute("audioCache");

        if (audioCache == null || audioCache.isEmpty()) {
            log.info("AudioCacheSessionListener.sessionDestroyed() - No audio cache found for sessionId=" + sessionId);
            return;
        }

        int deleted = 0;
        int failed = 0;
        for (Map.Entry<String, File> entry : audioCache.entrySet()) {
            File tempFile = entry.getValue();
            if (tempFile != null && tempFile.exists()) {
                if (tempFile.delete()) {
                    deleted++;
                    log.fine("AudioCacheSessionListener - Deleted temp file: "
                            + tempFile.getAbsolutePath() + " (convId=" + entry.getKey() + ")");
                } else {
                    failed++;
                    log.warning("AudioCacheSessionListener - Failed to delete temp file: "
                            + tempFile.getAbsolutePath() + " (convId=" + entry.getKey() + ")");
                }
            }
        }
        audioCache.clear();

        log.info("AudioCacheSessionListener.sessionDestroyed() - EXIT - sessionId=" + sessionId
                + ", deletedFiles=" + deleted + ", failedFiles=" + failed);
    }
}
