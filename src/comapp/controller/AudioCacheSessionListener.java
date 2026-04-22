package comapp.controller;

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

        ConcurrentHashMap<String, byte[]> audioCache =
                (ConcurrentHashMap<String, byte[]>) se.getSession().getAttribute("audioCache");

        if (audioCache == null || audioCache.isEmpty()) {
            log.info("AudioCacheSessionListener.sessionDestroyed() - No audio cache found for sessionId=" + sessionId);
            return;
        }

        int count = audioCache.size();
        audioCache.clear();

        log.info("AudioCacheSessionListener.sessionDestroyed() - EXIT - sessionId=" + sessionId
                + ", clearedEntries=" + count);
    }
}