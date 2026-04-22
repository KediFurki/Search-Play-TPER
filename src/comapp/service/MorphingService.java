package comapp.service;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.cert.X509Certificate;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.jtransforms.fft.DoubleFFT_1D;

import comapp.ConfigServlet;

public class MorphingService {
    private static final Logger log = Logger.getLogger("comapp");

    public InputStream processAudio(String audioUrl) {
        Properties cs = ConfigServlet.getProperties();
        int nSeconds = Integer.parseInt(cs.getProperty("secondToMuted", "0"));
        float pitchFactor = Float.parseFloat(cs.getProperty("pitchFactor", "1.15"));
        float camouflageFactor = Float.parseFloat(cs.getProperty("camouflageFactor", "0.85"));
        log.info("processAudio | mute=" + nSeconds + "s pitch=" + pitchFactor + " camouflage=" + camouflageFactor);
        installTrustAllCerts();
        AudioInputStream orgAudio = null;
        AudioInputStream audioMono = null;
        AudioInputStream silenceAudio = null;
        AudioInputStream voiceChanged = null;
        try {
            URL url = new URL(audioUrl);
            InputStream urlStream = url.openStream();
            orgAudio = AudioSystem.getAudioInputStream(new BufferedInputStream(urlStream));
            AudioFormat audioFormat = orgAudio.getFormat();
            log.info("processAudio | format=" + audioFormat);
            audioMono = convertingToMono(orgAudio, audioFormat);
            silenceAudio = removeNSeconds(audioMono, nSeconds);
            if (silenceAudio.getFrameLength() == 0) {
                log.warning("processAudio | empty after trimming " + nSeconds + "s");
                return null;
            }
            try {
                voiceChanged = changeVoice(silenceAudio, pitchFactor, camouflageFactor);
            } catch (Exception ex) {
                log.log(Level.WARNING, "processAudio | voice change failed", ex);
                return null;
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            AudioSystem.write(voiceChanged, AudioFileFormat.Type.WAVE, baos);
            byte[] resultBytes = baos.toByteArray();
            log.info("processAudio | done, size=" + resultBytes.length + "B");
            return new ByteArrayInputStream(resultBytes);
        } catch (Exception e) {
            log.log(Level.SEVERE, "processAudio | failed, url=" + audioUrl, e);
            return null;
        } finally {
            closeQuietly(voiceChanged, "voiceChanged");
            closeQuietly(silenceAudio, "silenceAudio");
            closeQuietly(audioMono, "audioMono");
            closeQuietly(orgAudio, "orgAudio");
        }
    }
    AudioInputStream convertingToMono(AudioInputStream orgAudio, AudioFormat audioFormat) {
        if (audioFormat.getChannels() == 1) {
            return orgAudio;
        }
        AudioFormat monoFormat = new AudioFormat(
                audioFormat.getSampleRate(),
                audioFormat.getSampleSizeInBits(),
                1,
                true, 
                audioFormat.isBigEndian());
        AudioInputStream mono = AudioSystem.getAudioInputStream(monoFormat, orgAudio);
        log.info("convertingToMono | converted, format=" + monoFormat);
        return mono;
    }
    AudioInputStream removeNSeconds(AudioInputStream audioMono, int nSeconds) throws Exception {
        AudioFormat audioFormat = audioMono.getFormat();
        int bytesPerSecond = (int) (audioFormat.getFrameRate() * audioFormat.getFrameSize());
        int removeLength = bytesPerSecond * nSeconds;
        byte[] audioBytes = audioMono.readAllBytes();
        if (audioBytes.length <= removeLength) {
            log.warning("removeNSeconds | audio shorter than " + nSeconds + "s, returning empty");
            return new AudioInputStream(new ByteArrayInputStream(new byte[0]), audioFormat, 0);
        }
        byte[] trimmedBytes = new byte[audioBytes.length - removeLength];
        System.arraycopy(audioBytes, removeLength, trimmedBytes, 0, trimmedBytes.length);
        log.fine("removeNSeconds - Trimmed " + removeLength + " bytes, remaining=" + trimmedBytes.length);
        return new AudioInputStream(
                new ByteArrayInputStream(trimmedBytes),
                audioFormat,
                trimmedBytes.length / audioFormat.getFrameSize());
    }
    AudioInputStream changeVoice(AudioInputStream silenceAudio, float pitchFactor,
                                 float camouflageFactor) throws Exception {
        AudioFormat format = silenceAudio.getFormat();
        if (pitchFactor == 0.0f && camouflageFactor == 0.0f) {
            log.info("changeVoice | both factors 0, skipping");
            return silenceAudio;
        }
        byte[] audioBytes = silenceAudio.readAllBytes();
        if (audioBytes.length == 0) {
            log.warning("changeVoice | empty input");
            return new AudioInputStream(new ByteArrayInputStream(new byte[0]), format, 0);
        }
        try {
            byte[] pitchedAudio = changePitch(audioBytes, format, pitchFactor);
            double[] audioSamples = byteArrayToDoubleArray(pitchedAudio, format);
            DoubleFFT_1D fft = new DoubleFFT_1D(audioSamples.length);
            fft.realForward(audioSamples);
            camouflageFrequencies(audioSamples, camouflageFactor);
            fft.realInverse(audioSamples, true);
            byte[] processedBytes = doubleArrayToByteArray(audioSamples, format);
            log.info("changeVoice | done, in=" + audioBytes.length + "B out=" + processedBytes.length + "B");
            return new AudioInputStream(
                    new ByteArrayInputStream(processedBytes),
                    format,
                    processedBytes.length / format.getFrameSize());
        } catch (Exception e) {
            log.log(Level.WARNING, "changeVoice | failed", e);
            throw e;
        }
    }
    static byte[] changePitch(byte[] audioData, AudioFormat format, float pitchFactor) {
        if (pitchFactor <= 0.0f || pitchFactor == 1.0f) {
            if (pitchFactor <= 0.0f) {
                log.warning("changePitch | invalid pitchFactor=" + pitchFactor);
            }
            return audioData;
        }
        int sampleSize = format.getSampleSizeInBits() / 8;
        int totalSamples = audioData.length / sampleSize;
        int newTotalSamples = (int) (totalSamples / pitchFactor);
        byte[] newAudioData = new byte[newTotalSamples * sampleSize];
        for (int i = 0; i < newTotalSamples; i++) {
            double srcPos = i * pitchFactor;
            int srcIndex = (int) srcPos;
            double frac = srcPos - srcIndex;
            if (srcIndex + 1 < totalSamples) {
                for (int j = 0; j < sampleSize; j++) {
                    int s0 = audioData[srcIndex * sampleSize + j];
                    int s1 = audioData[(srcIndex + 1) * sampleSize + j];
                    newAudioData[i * sampleSize + j] = (byte) (s0 + frac * (s1 - s0));
                }
            } else if (srcIndex < totalSamples) {
                for (int j = 0; j < sampleSize; j++) {
                    newAudioData[i * sampleSize + j] = audioData[srcIndex * sampleSize + j];
                }
            }
        }
        return newAudioData;
    }
    static byte[] addEcho(byte[] audioData, AudioFormat format, int delayMs, float decay) {
        int sampleSize = format.getSampleSizeInBits() / 8;
        int numSamples = audioData.length / sampleSize;
        int delaySamples = (int) (format.getSampleRate() * delayMs / 1000);
        byte[] echoedAudio = new byte[audioData.length];
        for (int i = 0; i < numSamples; i++) {
            for (int j = 0; j < sampleSize; j++) {
                echoedAudio[i * sampleSize + j] = audioData[i * sampleSize + j];
            }
            if (i - delaySamples >= 0) {
                for (int j = 0; j < sampleSize; j++) {
                    int echoSampleIndex = (i - delaySamples) * sampleSize + j;
                    int originalSample = audioData[i * sampleSize + j];
                    int echoSample = (int) (audioData[echoSampleIndex] * decay);
                    int result = originalSample + echoSample;
                    echoedAudio[i * sampleSize + j] =
                            (byte) Math.min(Math.max(result, Byte.MIN_VALUE), Byte.MAX_VALUE);
                }
            }
        }
        return echoedAudio;
    }
    static double[] byteArrayToDoubleArray(byte[] audioBytes, AudioFormat format) {
        ByteBuffer buffer = ByteBuffer.wrap(audioBytes).order(ByteOrder.LITTLE_ENDIAN);
        double[] audioSamples = new double[audioBytes.length / 2];
        for (int i = 0; i < audioSamples.length; i++) {
            audioSamples[i] = buffer.getShort() / 32768.0;
        }
        return audioSamples;
    }
    static void camouflageFrequencies(double[] audioSamples, double factor) {
        float sampleRate = 8000.0f;
        int n = audioSamples.length;
        int binLow  = (int) (300.0  * n / sampleRate);
        int binHigh = (int) (3400.0 * n / sampleRate);
        int halfN = n / 2;
        for (int i = binLow; i <= Math.min(binHigh, halfN); i++) {
            audioSamples[i] *= factor;
            if (n - i < n) {
                audioSamples[n - i] *= factor;
            }
        }
    }
    static byte[] doubleArrayToByteArray(double[] audioSamples, AudioFormat format) {
        byte[] audioBytes = new byte[audioSamples.length * 2];
        ByteBuffer buffer = ByteBuffer.wrap(audioBytes).order(ByteOrder.LITTLE_ENDIAN);
        for (double sample : audioSamples) {
            buffer.putShort((short) (sample * 32767));
        }
        return audioBytes;
    }
    private void installTrustAllCerts() {
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
            public void checkClientTrusted(X509Certificate[] certs, String authType) { }
            public void checkServerTrusted(X509Certificate[] certs, String authType) { }
        } };
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            log.log(Level.WARNING, "installTrustAllCerts | failed", e);
        }
    }
    private void closeQuietly(AudioInputStream stream, String name) {
        if (stream != null) {
            try {
                stream.close();
            } catch (Exception e) {
                log.log(Level.WARNING, "closeQuietly | error closing " + name, e);
            }
        }
    }
}