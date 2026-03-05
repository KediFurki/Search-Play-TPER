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

        log.info("MorphingService.processAudio() - ENTRY - audioUrl=" + audioUrl);

        Properties cs = ConfigServlet.getProperties();

        int nSeconds = Integer.parseInt(cs.getProperty("secondToMuted", "0"));
        float pitchFactor = Float.parseFloat(cs.getProperty("pitchFactor", "0"));
        float camouflageFactor = Float.parseFloat(cs.getProperty("camouflageFactor", "0"));

        log.info("MorphingService.processAudio() - Config loaded: "
                + "secondToMuted=" + nSeconds
                + ", pitchFactor=" + pitchFactor
                + ", camouflageFactor=" + camouflageFactor);
        log.info("MorphingService.processAudio() - Installing TrustAllCerts SSL bypass...");
        installTrustAllCerts();
        log.info("MorphingService.processAudio() - TrustAllCerts installed successfully.");
        AudioInputStream orgAudio = null;
        AudioInputStream audioMono = null;
        AudioInputStream silenceAudio = null;
        AudioInputStream voiceChanged = null;

        try {
            log.info("MorphingService.processAudio() - Opening URL stream: " + audioUrl);
            URL url = new URL(audioUrl);
            InputStream urlStream = url.openStream();
            orgAudio = AudioSystem.getAudioInputStream(new BufferedInputStream(urlStream));
            
            AudioFormat audioFormat = orgAudio.getFormat();
            log.info("MorphingService.processAudio() - Original AudioFormat: " + audioFormat);
            log.info("MorphingService.processAudio() - Converting to mono...");
            audioMono = convertingToMono(orgAudio, audioFormat);
            audioFormat = audioMono.getFormat();
            log.info("MorphingService.processAudio() - AudioFormat after convertingToMono: " + audioFormat);
            log.info("MorphingService.processAudio() - Removing first " + nSeconds + " seconds...");
            silenceAudio = removeNSeconds(audioMono, nSeconds);
            audioFormat = silenceAudio.getFormat();
            log.info("MorphingService.processAudio() - AudioFormat after removeNSeconds: " + audioFormat);
            log.info("MorphingService.processAudio() - Applying changeVoice "
                    + "(pitchFactor=" + pitchFactor + ", camouflageFactor=" + camouflageFactor + ")...");
            try {
                voiceChanged = changeVoice(silenceAudio, pitchFactor, camouflageFactor);
            } catch (Exception ex) {
                log.log(Level.WARNING,
                        "MorphingService.processAudio() - Unable to change the voice of the audio", ex);
                return null;
            }
            audioFormat = voiceChanged.getFormat();
            log.info("MorphingService.processAudio() - AudioFormat after changeVoice: " + audioFormat);
            log.info("MorphingService.processAudio() - Writing morphed audio to ByteArrayOutputStream...");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            AudioSystem.write(voiceChanged, AudioFileFormat.Type.WAVE, baos);
            
            byte[] resultBytes = baos.toByteArray();
            log.info("MorphingService.processAudio() - EXIT - Morphed WAV size=" + resultBytes.length + " bytes");

            return new ByteArrayInputStream(resultBytes);

        } catch (Exception e) {
            log.log(Level.SEVERE,
                    "MorphingService.processAudio() - Exception while processing audio from URL: " + audioUrl, e);
            return null;
        } finally {
            closeQuietly(voiceChanged, "voiceChanged");
            closeQuietly(silenceAudio, "silenceAudio");
            closeQuietly(audioMono, "audioMono");
            closeQuietly(orgAudio, "orgAudio");
        }
    }
    AudioInputStream convertingToMono(AudioInputStream orgAudio, AudioFormat audioFormat) {
        log.info("MorphingService.convertingToMono() - ENTRY - channels=" + audioFormat.getChannels());

        if (audioFormat.getChannels() == 1) {
            log.info("MorphingService.convertingToMono() - Already mono, returning original stream.");
            return orgAudio;
        }
        AudioFormat monoFormat = new AudioFormat(
                audioFormat.getSampleRate(),
                audioFormat.getSampleSizeInBits(),
                1,
                true, 
                audioFormat.isBigEndian());

        AudioInputStream mono = AudioSystem.getAudioInputStream(monoFormat, orgAudio);
        log.info("MorphingService.convertingToMono() - EXIT - converted to mono: " + monoFormat);
        return mono;
    }
    AudioInputStream removeNSeconds(AudioInputStream audioMono, int nSeconds) throws Exception {
        log.info("MorphingService.removeNSeconds() - ENTRY - nSeconds=" + nSeconds);

        AudioFormat audioFormat = audioMono.getFormat();
        int bytesPerSecond = (int) (audioFormat.getFrameRate() * audioFormat.getFrameSize());
        int removeLength = bytesPerSecond * nSeconds;
        byte[] audioBytes = audioMono.readAllBytes();
        log.info("MorphingService.removeNSeconds() - Original byte length=" + audioBytes.length
                + ", removeLength=" + removeLength);

        if (audioBytes.length <= removeLength) {
            log.warning("MorphingService.removeNSeconds() - Audio shorter than trim window, returning empty stream.");
            return new AudioInputStream(new ByteArrayInputStream(new byte[0]), audioFormat, 0);
        }

        byte[] trimmedBytes = new byte[audioBytes.length - removeLength];
        System.arraycopy(audioBytes, removeLength, trimmedBytes, 0, trimmedBytes.length);
        log.info("MorphingService.removeNSeconds() - EXIT - trimmedBytes length=" + trimmedBytes.length);
        return new AudioInputStream(
                new ByteArrayInputStream(trimmedBytes),
                audioFormat,
                trimmedBytes.length / audioFormat.getFrameSize());
    }
    AudioInputStream changeVoice(AudioInputStream silenceAudio, float pitchFactor,
                                 float camouflageFactor) throws Exception {

        log.info("MorphingService.changeVoice() - ENTRY - pitchFactor=" + pitchFactor
                + ", camouflageFactor=" + camouflageFactor);

        AudioFormat format = silenceAudio.getFormat();
        byte[] audioBytes = silenceAudio.readAllBytes();
        log.info("MorphingService.changeVoice() - Read " + audioBytes.length + " bytes from silenceAudio.");

        byte[] pitchedAudio = null;
        try {
            pitchedAudio = changePitch(audioBytes, format, pitchFactor);
            log.info("MorphingService.changeVoice() - changePitch completed. "
                    + "outputLength=" + pitchedAudio.length);
        } catch (Exception e) {
            log.log(Level.WARNING, "MorphingService.changeVoice() - Problem with changePitch: " + e.getMessage(), e);
            throw e;
        }

        double[] audioSamples = null;
        try {
            audioSamples = byteArrayToDoubleArray(pitchedAudio, format);
            log.info("MorphingService.changeVoice() - byteArrayToDoubleArray completed. "
                    + "samplesLength=" + audioSamples.length);
        } catch (Exception e) {
            log.log(Level.WARNING, "MorphingService.changeVoice() - Problem with byteArrayToDoubleArray: " + e.getMessage(), e);
            throw e;
        }
        try {
            log.info("MorphingService.changeVoice() - Applying FFT forward...");
            DoubleFFT_1D fft = new DoubleFFT_1D(audioSamples.length);
            fft.realForward(audioSamples);

            log.info("MorphingService.changeVoice() - Applying camouflageFrequencies (factor=" + camouflageFactor + ")...");
            camouflageFrequencies(audioSamples, camouflageFactor);

            log.info("MorphingService.changeVoice() - Applying FFT inverse...");
            fft.realInverse(audioSamples, true);

            log.info("MorphingService.changeVoice() - FFT pipeline completed.");
        } catch (Exception e) {
            log.log(Level.WARNING, "MorphingService.changeVoice() - Problem in FFT pipeline: " + e.getMessage(), e);
            throw e;
        }
        byte[] processedBytes = doubleArrayToByteArray(audioSamples, format);
        log.info("MorphingService.changeVoice() - EXIT - processedBytes length=" + processedBytes.length);

        return new AudioInputStream(
                new ByteArrayInputStream(processedBytes),
                format,
                processedBytes.length / format.getFrameSize());
    }

    static byte[] changePitch(byte[] audioData, AudioFormat format, float pitchFactor) {
        int sampleSize = format.getSampleSizeInBits() / 8;
        byte[] newAudioData = new byte[(int) (audioData.length / pitchFactor)];
        for (int i = 0; i < newAudioData.length; i++) {
            int sampleIndex = (int) (i * pitchFactor) * sampleSize;
            if (sampleIndex + sampleSize < audioData.length) {
                for (int j = 0; j < sampleSize; j++) {
                    newAudioData[i * sampleSize + j] = audioData[sampleIndex + j];
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
        for (int i = 0; i < audioSamples.length; i++) {
            if (i > 10 && i < audioSamples.length / 2) {
                audioSamples[i] *= factor;
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
            log.log(Level.WARNING, "MorphingService.installTrustAllCerts() - Failed to install TrustAllCerts", e);
        }
    }
    private void closeQuietly(AudioInputStream stream, String name) {
        if (stream != null) {
            try {
                stream.close();
                log.fine("MorphingService.closeQuietly() - Closed stream: " + name);
            } catch (Exception e) {
                log.log(Level.WARNING,
                        "MorphingService.closeQuietly() - Error closing stream '" + name + "'", e);
            }
        }
    }
}