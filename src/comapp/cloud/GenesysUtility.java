package comapp.cloud;


import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import comapp.ConfigServlet;
import comapp.cloud.Genesys.AudioType;

public class GenesysUtility {
	static Logger log = Logger.getLogger(ConfigServlet.web_app);
	public static void donwloadRecorderByQueue(GenesysUser guser, String queueId, String destination) {
		boolean moreUser = true;
		int page = 0;
		try {
			// String destination = ConfigServlet.getProperties().getProperty("destination",
			// "c:\\tmp");
			do { 
				moreUser = false;
				Instant instant = Instant.now().minus(3, ChronoUnit.HOURS);

				JSONObject conversations = Genesys.getConnectionList(guser, queueId, instant,  (++page), 100);
				log.info("conversations-->" + conversations.toString());
				JSONArray jConversations = conversations.getJSONArray("conversations");

				if (jConversations != null && jConversations.length() > 0) {
					moreUser = true;
					for (int i = 0; i < jConversations.length(); i++) {
						String conversationId = jConversations.getJSONObject(i).getString("conversationId");
						if (!isNeedDownload(destination, conversationId)) {
							log.info("conversation " + conversationId + " the conversation has already been downloaded");
							continue;
						}
						JSONArray recorderList = Genesys.getRecorderList(guser, conversationId, Genesys.AudioType.WAV);
						if (recorderList.length() > 1) {
							throw new Exception("too much  recording");
						}
						File file = new File(destination + "\\" + conversationId + ".wav");
						// JSONObject jMessage = jConversations.getJSONObject(i);
						if (Genesys.downloadFile(guser, recorderList.getJSONObject(0), AudioType.WAV, file, null, null)) {

							File jsonfile = new File(destination + "\\" + conversationId + ".json");
							File file_name = new File(destination + "\\" + conversationId + "_name.wav");
							File file_notice = new File(destination + "\\" + conversationId + "_notice.wav");
							// START_REGISTRAZIONE
							// START_NOMINATIVO
							// STOP_NOMINATIVO
							// START_ANNUNCIO
							// STOP_ANNUNCIO

							JSONObject jo = getParticipantsAttribute(guser, conversationId, jsonfile);
							if (jo != null) {
								// Instant instant = Instant.now();
								DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX").withZone(ZoneId.of("UTC"));
								String START_REGISTRAZIONE = jo.getString("START_REGISTRAZIONE");
								String START_NOMINATIVO = jo.getString("START_NOMINATIVO");
								String STOP_NOMINATIVO = jo.getString("STOP_NOMINATIVO");
								String START_ANNUNCIO = jo.getString("START_ANNUNCIO");
								String STOP_ANNUNCIO = jo.getString("STOP_ANNUNCIO");

								long iSTART_REGISTRAZIONE = Instant.from(DATE_TIME_FORMATTER.parse(START_REGISTRAZIONE)).toEpochMilli();
								long iSTART_NOMINATIVO = Instant.from(DATE_TIME_FORMATTER.parse(START_NOMINATIVO)).toEpochMilli();
								long iSTOP_NOMINATIVO = Instant.from(DATE_TIME_FORMATTER.parse(STOP_NOMINATIVO)).toEpochMilli();
								long iSTART_ANNUNCIO = Instant.from(DATE_TIME_FORMATTER.parse(START_ANNUNCIO)).toEpochMilli();
								long iSTOP_ANNUNCIO = Instant.from(DATE_TIME_FORMATTER.parse(STOP_ANNUNCIO)).toEpochMilli();

								// int start_registrazione = 0;
								int start_nominativo = (int) (iSTART_NOMINATIVO - iSTART_REGISTRAZIONE) / 1000;
								int stop_nominativo = (int) (iSTOP_NOMINATIVO - iSTART_REGISTRAZIONE) / 1000;
								int start_annincio = (int) (iSTART_ANNUNCIO - iSTART_REGISTRAZIONE) / 1000;
								int stop_annincio = (int) (iSTOP_ANNUNCIO - iSTART_REGISTRAZIONE) / 1000;
								AudioInputStream audioInputStream = null;
								byte[] byteArray = null;
								long bit_in_a_second = 0;
								try {
									audioInputStream = AudioSystem.getAudioInputStream(file);
									bit_in_a_second = (long) (audioInputStream.getFormat().getSampleSizeInBits() * audioInputStream.getFormat().getSampleRate() * audioInputStream.getFormat().getChannels());
									byteArray = audioInputStream.readAllBytes();
								} catch (Exception e) {
									log.log(Level.WARNING,"", e);
								} finally {
									try {
										audioInputStream.close();
									} catch (Exception e) {
									}
									System.gc();
								}

								split(byteArray, (int) bit_in_a_second / 8, start_nominativo, stop_nominativo, file_name);
								split(byteArray, (int) bit_in_a_second / 8, start_annincio, stop_annincio, file_notice);

								markedDownloaded(destination, conversationId);
								// log.log(Level.WARNING,"*************** DELETE ***************** " + jMessage.toString(4));
							} else {
								log.log(Level.WARNING,"*************** Participants Attribute error ***************** " + jConversations.getJSONObject(i).toString(4));
							}

						} else {
							log.log(Level.WARNING,"*************** download error ***************** " + jConversations.getJSONObject(i).toString(4));
						}
					}
				} else {
					log.info("no message found");
				}

			} while (moreUser);

		} catch (Exception e) {
			log.log(Level.WARNING,"", e);
		}
	}
	private static void markedDownloaded(String destination, String conversationId) throws Exception {
		DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy_MM_dd");
		String todayStr = LocalDate.now(ZoneId.systemDefault()).format(fmt);
		File today = new File(destination + "//" + todayStr);

		String dayBeforeStr = LocalDate.now(ZoneId.systemDefault()).minusDays(2).format(fmt);
		File daybeforeyesterday = new File(destination + "//" + dayBeforeStr);
		try {
			daybeforeyesterday.delete();
		} catch (Exception e) {
		}
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(today, true))) {
			writer.append("\n" + conversationId);
		}
	}

	private static boolean isNeedDownload(String destination, String conversationId) throws Exception {
		DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy_MM_dd");
		String todayStr = LocalDate.now(ZoneId.systemDefault()).format(fmt);
		File today = new File(destination + "//" + todayStr);
		String str = FileUtils.readFileToString(today, "utf-8");
		if (StringUtils.containsIgnoreCase(str, conversationId))
			return false;

		String yesterdayStr = LocalDate.now(ZoneId.systemDefault()).minusDays(1).format(fmt);
		File yesterday = new File(destination + "//" + yesterdayStr);
		str = FileUtils.readFileToString(yesterday, "utf-8");

		return !StringUtils.containsIgnoreCase(str, conversationId);
	}

	public static void main(String[] args) throws Exception {
		AudioInputStream ai = AudioSystem.getAudioInputStream(new File("C:\\tmp\\12333def-0b09-4c9f-b536-1c314cfff4e5.wav"));
		InputStream is = null;
		JSONObject jo = null;
		try {
			is = new FileInputStream(new File("C:\\tmp\\12333def-0b09-4c9f-b536-1c314cfff4e5.json"));

			jo = new JSONObject(IOUtils.toString(is, Charset.defaultCharset()));

			is.close();
		} catch (Exception e) {
			log.log(Level.WARNING,"", e);
		} finally {
			try {
				is.close();
			} catch (Exception e) {
			}
		}

		DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX").withZone(ZoneId.of("UTC"));
		String START_REGISTRAZIONE = jo.getString("START_REGISTRAZIONE");
		String START_NOMINATIVO = jo.getString("START_NOMINATIVO");
		String STOP_NOMINATIVO = jo.getString("STOP_NOMINATIVO");
		String START_ANNUNCIO = jo.getString("START_ANNUNCIO");
		String STOP_ANNUNCIO = jo.getString("STOP_ANNUNCIO");

		long iSTART_REGISTRAZIONE = Instant.from(DATE_TIME_FORMATTER.parse(START_REGISTRAZIONE)).toEpochMilli();
		long iSTART_NOMINATIVO = Instant.from(DATE_TIME_FORMATTER.parse(START_NOMINATIVO)).toEpochMilli();
		long iSTOP_NOMINATIVO = Instant.from(DATE_TIME_FORMATTER.parse(STOP_NOMINATIVO)).toEpochMilli();
		long iSTART_ANNUNCIO = Instant.from(DATE_TIME_FORMATTER.parse(START_ANNUNCIO)).toEpochMilli();
		long iSTOP_ANNUNCIO = Instant.from(DATE_TIME_FORMATTER.parse(STOP_ANNUNCIO)).toEpochMilli();

		// int start_registrazione = 0;
		int start_nominativo = (int) (iSTART_NOMINATIVO - iSTART_REGISTRAZIONE) / 1000;
		int stop_nominativo = (int) (iSTOP_NOMINATIVO - iSTART_REGISTRAZIONE) / 1000;
		int start_annincio = (int) (iSTART_ANNUNCIO - iSTART_REGISTRAZIONE) / 1000;
		int stop_annincio = (int) (iSTOP_ANNUNCIO - iSTART_REGISTRAZIONE) / 1000;

		long bit_in_a_second = (long) (ai.getFormat().getSampleSizeInBits() * ai.getFormat().getSampleRate() * ai.getFormat().getChannels());
		byte[] byteArray = ai.readAllBytes();

		split(byteArray, (int) bit_in_a_second / 8, start_nominativo, stop_nominativo, new File("C:\\tmp\\splt1.wav"));
		split(byteArray, (int) bit_in_a_second / 8, start_annincio, stop_annincio, new File("C:\\tmp\\splt2.wav"));
	}
	public static void split(byte[] ba, int byte_in_a_second, int start, int stop, File out) throws IOException, UnsupportedAudioFileException {

		byte[] subByteArray = java.util.Arrays.copyOfRange(ba, (int) byte_in_a_second * start, (int) byte_in_a_second * stop);
		generateFile(subByteArray, out);
	}

	public static void generateFile(byte[] data, File outputFile) {
		try {
			AudioInputStream audioStream = getAudioStream(data);
			if (outputFile.getName().endsWith("wav")) {
				int nb = AudioSystem.write(audioStream, AudioFileFormat.Type.WAVE, new FileOutputStream(outputFile));
				log.fine("WAV file written to " + outputFile.getCanonicalPath() + " (" + (nb / 1000) + " kB)");
			} else {
				throw new RuntimeException("Unsupported encoding " + outputFile);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("could not generate file: " + e);
		}
	}

	public static AudioInputStream getAudioStream(byte[] byteArray) {
		try {
			try {
				ByteArrayInputStream byteStream = new ByteArrayInputStream(byteArray);
				return AudioSystem.getAudioInputStream(byteStream);
			} catch (UnsupportedAudioFileException e) {
				byteArray = addWavHeader(byteArray);
				ByteArrayInputStream byteStream = new ByteArrayInputStream(byteArray);
				return AudioSystem.getAudioInputStream(byteStream);
			}
		} catch (IOException | UnsupportedAudioFileException e) {
			throw new RuntimeException("cannot convert bytes to audio stream: " + e);
		}
	}

	private static byte[] addWavHeader(byte[] bytes) throws IOException {

		ByteBuffer bufferWithHeader = ByteBuffer.allocate(bytes.length + 44);
		bufferWithHeader.order(ByteOrder.LITTLE_ENDIAN);
		bufferWithHeader.put("RIFF".getBytes());
		bufferWithHeader.putInt(bytes.length + 36);
		bufferWithHeader.put("WAVE".getBytes());
		bufferWithHeader.put("fmt ".getBytes());
		bufferWithHeader.putInt(16);
		bufferWithHeader.putShort((short) 1);
		bufferWithHeader.putShort((short) 2);
		bufferWithHeader.putInt(8000);
		bufferWithHeader.putInt(8000 * 16 * 2 / 8);
		bufferWithHeader.putShort((short) 4);
		bufferWithHeader.putShort((short) 16);
		bufferWithHeader.put("data".getBytes());
		bufferWithHeader.putInt(bytes.length);
		bufferWithHeader.put(bytes);
		return bufferWithHeader.array();
	}

	public static void donwloadMailBoxMessages(GenesysUser guser, String queueId, String destination) {

		boolean moreUser = true;
		int page = 0;
		try {
			// String destination = ConfigServlet.getProperties().getProperty("destination",
			// "c:\\tmp");
			do {
				moreUser = false;
				JSONObject messages = Genesys.getMessagesList(guser, queueId,  (++page),  500);

				JSONArray jtmpMessage = messages.getJSONArray("entities");
				if (jtmpMessage != null && jtmpMessage.length() > 0) {
					moreUser = true;
					for (int i = 0; i < jtmpMessage.length(); i++) {
						String messageId = jtmpMessage.getJSONObject(i).getString("id");
						File file = new File(destination + "\\" + messageId + ".mp3");
						JSONObject jMessage = jtmpMessage.getJSONObject(i);
						if (Genesys.downloadMailboxFile(guser, jMessage.getString("id"), AudioType.MP3, file)) {
							String conversationId = jMessage.getJSONObject("conversation").getString("id");
							File jfile = new File(destination + "\\" + messageId + ".json");
							if (getParticipantsAttribute(guser, conversationId, jfile) != null) {
								log.log(Level.WARNING,"*************** DELETE ***************** " + jMessage.toString(4));
							} else {
								log.log(Level.WARNING,"*************** Participants Attribute error ***************** " + jMessage.toString(4));
							}

						} else {
							log.log(Level.WARNING,"*************** download error ***************** " + jMessage.toString(4));
						}
					}
				} else {
					log.info("no message found");
				}

			} while (moreUser);

		} catch (Exception e) {
			log.log(Level.WARNING,"", e);
		}
	}

	@SuppressWarnings("unchecked")
	public static JSONObject getParticipantsAttribute(GenesysUser guser, String conversationId, File jfile) {

		try {
			// String destination = ConfigServlet.getProperties().getProperty("destination",
			// "c:\\tmp");
			JSONObject jo = new JSONObject();

			JSONObject paticipant = Genesys.getParticipantsList(guser, conversationId);
			log.info("paticipant-->:" + paticipant.toString(4));
			JSONArray jParticipants = paticipant.getJSONArray("participants");

			if (jParticipants != null && jParticipants.length() > 0) {

				for (int i = 0; i < jParticipants.length(); i++) {
					JSONObject jParticipant = jParticipants.getJSONObject(i);
					if (!jParticipant.isNull("attributes")) {
						JSONObject jAttributes = jParticipant.getJSONObject("attributes");
						Iterator<String> it = jAttributes.keys();
						while (it.hasNext()) {
							String key = it.next();
							jo.put(key, jAttributes.get(key));
						}

					}

				}
			} else {
				log.info("no partecipant found");
			}

			FileWriter file = null;
			try {
				file = new FileWriter(jfile);
				file.write(jo.toString(4));
				return jo;
			} catch (Exception e) {
				log.log(Level.WARNING,"", e);
			} finally {
				try {
					file.close();
				} catch (Exception e) {

				}
			}

		} catch (Exception e) {
			log.log(Level.WARNING,"", e);
		}
		return null;
	}
	
	public static Hashtable<String, JSONObject> trasformArrayWithId(JSONArray ja) throws JSONException{
		Hashtable<String, JSONObject> ht = new Hashtable<String, JSONObject>();
		for (int i=0; i<ja.length(); i++) {
			JSONObject jo= ja.getJSONObject(i);
			String id = jo.getString("id");
			ht.put(id, jo);
		}
		return ht;
	}
	public static Hashtable<String, JSONObject> setHasSubCategory(Hashtable<String, JSONObject> ht) throws JSONException{
		Enumeration<JSONObject> el = ht.elements();
		while (el.hasMoreElements()) {
			JSONObject jo= el.nextElement();
			if (jo.has("parentCategory")) {
				String id = jo.getJSONObject("parentCategory").getString("id");
				ht.get(id).put("hasSubCategory", true);
			} 
		}
		
		return ht;
	}
	
	
	public static void filterJSON(String sessionid, JSONObject jo) {
		try {
			
			for(int j = 0; j<jo.getJSONArray("conversations").length(); j++) {
				int total = jo.getInt("totalHits");
				log.info("totalHits =  " + total);  
				String filter = jo.getJSONArray("conversations").getJSONObject(j).getJSONArray("participants").getJSONObject(0).getJSONArray("sessions").getJSONObject(0).getString("mediaType");
				log.info("Mediatype di " + (j+1) + "è  " + filter);  
			}
			
			
		}catch(Exception e) {
			log.log(Level.WARNING, "Unable to apply the filter ", e);
		}
	
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}