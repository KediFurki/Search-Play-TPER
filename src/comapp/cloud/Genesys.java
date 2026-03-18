package comapp.cloud;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import comapp.ConfigServlet;



public class Genesys {
	static Logger log = Logger.getLogger("comapp");
	public static java.util.concurrent.ConcurrentHashMap<String, Boolean> cancelMap = new java.util.concurrent.ConcurrentHashMap<>();
	public static String version = "1.0.6";
	public static String prefixLogin = "https://login.";
	public static String prefixApi = "https://api.";

	public static JSONObject getToken(String urlRegion, String clientId, String clientSecret, String code, String redirect_uri) {

		// String urlString = "https://login." + urlRegion + "/oauth/token";
		String urlString = prefixLogin + urlRegion + "/oauth/token";
		CloseableHttpClient httpClient = null;
		CloseableHttpResponse res = null;
		try {
			RequestConfig conf = RequestConfig.custom().setConnectTimeout(3 * 1000).setConnectionRequestTimeout(3 * 1000).setSocketTimeout(3 * 1000).build(); // fix 23/09/2021

			httpClient = HttpClientBuilder.create().setDefaultRequestConfig(conf).build();
			log.log(Level.INFO, "urlString:" + urlString);
			HttpPost httppost = new HttpPost(urlString);
			List<BasicNameValuePair> params = new ArrayList<>();
			if (code == null) {
				params.add(new BasicNameValuePair("grant_type", "client_credentials"));
				log.log(Level.INFO, "client_credentials");
			} else {
				params.add(new BasicNameValuePair("grant_type", "authorization_code"));
				log.log(Level.INFO, "client_credentials: authorization_code code: " + code + " redirect_uri:" + redirect_uri);
				params.add(new BasicNameValuePair("code", code));
				params.add(new BasicNameValuePair("redirect_uri", redirect_uri));
			}
			// params.add(new BasicNameValuePair("grant_type", "authorization_code"));
			// params.add(new BasicNameValuePair("grant_type", "client_credentials"));

			httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
			log.log(Level.INFO, "authorization: Basic " + Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes()));
			httppost.addHeader("authorization", "Basic " + Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes()));
			httppost.addHeader("content-type", "application/x-www-form-urlencoded");

			res = httpClient.execute(httppost);
			// {
			// "access_token": "token",
			// "token_type": "bearer",
			// "expires_in": 86400,
			// "error": "optional-error-message"
			// }
			String jsonString = IOUtils.toString(res.getEntity().getContent(), StandardCharsets.UTF_8);
			log.log(Level.INFO, jsonString);
			if (jsonString != null) {
				JSONObject jo = new JSONObject(jsonString);
				Calendar cal = Calendar.getInstance();
				cal.add(Calendar.SECOND, jo.getInt("expires_in") - 60);
				jo.put("expires_at", cal.getTimeInMillis());
				log.log(Level.INFO, "get token end: " + jo.getString("access_token"));
				return jo;
			}

		} catch (Exception e) {
			log.log(Level.INFO, "retrieve token", e);
		} finally {
			try {
				res.close();
			} catch (Exception e) {
			}
			try {
				httpClient.close();
			} catch (Exception e) {
			}
		}
		return null;
	}

	// public static JSONObject acceptCallBack( String sessionId, String urlRegion,
	// String clientId, String clientSecret, String code, String redirect_uri,
	// String conversationId, String participantId) {
	// try {
	// String urlString = prefixApi + urlRegion + "/api/v2/conversations/callbacks/"
	// + conversationId + "/participants/" + participantId;
	// log.log(Level.INFO,"[" + sessionId + "] urlString:" + urlString);
	//
	// String token = getMasterToken(urlRegion, clientId, clientSecret, code,
	// redirect_uri);
	// JSONObject jo = new JSONObject();
	// jo.put("state", "connected");
	// StringEntity he = new StringEntity(jo.toString());
	// return refinePerformRequestPatch(sessionId, urlString, urlRegion, clientId,
	// clientSecret, code, redirect_uri, token, he, "application/json;
	// charset=UTF-8", 0);
	//
	// } catch (Exception e) {
	// log.log(Level.INFO, "[" + sessionId + "] - acceptCallBack ", e);
	// } finally {}
	// return null;
	// // refinePerformRequestPatch( sessionId, url, token, timeOutInSeconds, he)
	// }
	public static JSONObject acceptCallBack(GenesysUser guser, String conversationId, String participantId) {
		try {
			String urlString = prefixApi + guser.urlRegion + "/api/v2/conversations/callbacks/" + conversationId + "/participants/" + participantId;
			log.log(Level.INFO, "[" + guser.sessionId + "," + conversationId + "] urlString:" + urlString);

			JSONObject jo = new JSONObject();
			jo.put("state", "connected");
			StringEntity he = new StringEntity(jo.toString(), "UTF-8");
			return refinePerformRequestPatch(guser, urlString, he, "application/json", 0, conversationId);

		} catch (Exception e) {
			log.log(Level.INFO, "[" + guser.sessionId + "," + conversationId + "] - acceptCallBack ", e);
		} finally {
		}
		return null;
		// refinePerformRequestPatch( sessionId, url, token, timeOutInSeconds, he)
	}

	public static JSONObject getUserMe(GenesysUser guser, String expand) {
		try {
			String urlString = prefixApi + guser.urlRegion + "/api/v2/users/me";
			if (StringUtils.isNotBlank(expand)) {
				urlString += "?expand=" + expand;
			}
			log.log(Level.INFO, "[" + guser.sessionId + ",getUserMe] urlString:" + urlString + " ");

			return (JSONObject) refinePerformRequestGet(guser, urlString, 0, "getUserMe");

		} catch (Exception e) {
			log.log(Level.INFO, "[" + guser.sessionId + "] - recorder ", e);
		} finally {
		}
		return null;
		// refinePerformRequestPatch( sessionId, url, token, timeOutInSeconds, he)

	}

	public static void fetchUserGroups(GenesysUser guser) {
		List<String> groups = new ArrayList<>();
		try {
			log.log(Level.INFO, "[" + guser.sessionId + "] Genesys.fetchUserGroups() - "
					+ "Fetching user groups from /api/v2/users/me?expand=groups ...");

			JSONObject userMe = getUserMe(guser, "groups");

			if (userMe == null) {
				log.warning("[" + guser.sessionId + "] Genesys.fetchUserGroups() - "
						+ "getUserMe returned null. Assigning empty group list.");
				guser.setUserGroups(groups);
				return;
			}

			JSONArray groupsArray = userMe.optJSONArray("groups");
			if (groupsArray != null && groupsArray.length() > 0) {
				for (int i = 0; i < groupsArray.length(); i++) {
					JSONObject groupObj = groupsArray.optJSONObject(i);
					if (groupObj != null) {
						String groupName = groupObj.optString("name", "");
						if (StringUtils.isNotBlank(groupName)) {
							groups.add(groupName);
						}
					}
				}
			}

			log.info("[" + guser.sessionId + "] Genesys.fetchUserGroups() - "
					+ "Groups parsed successfully. count=" + groups.size()
					+ ", groups=" + groups);

		} catch (Exception e) {
			log.log(Level.SEVERE, "[" + guser.sessionId + "] Genesys.fetchUserGroups() - "
					+ "Error while fetching/parsing user groups. Assigning empty list.", e);
			groups = new ArrayList<>();
		}
		guser.setUserGroups(groups);
	}

	public static JSONObject recorderCommand(GenesysUser guser, String conversationId, String recordingState) {
		try {
			String urlString = prefixApi + guser.urlRegion + "/api/v2/conversations/calls/" + conversationId;
			log.log(Level.INFO, "[" + guser.sessionId + "," + conversationId + "] urlString:" + urlString + " ");

			JSONObject jo = new JSONObject();
			jo.put("recordingState", recordingState);
			StringEntity he = new StringEntity(jo.toString());
			log.log(Level.INFO, "[" + guser.sessionId + "," + conversationId + "] " + jo.toString());
			return refinePerformRequestPatch(guser, urlString, he, "application/json; charset=UTF-8", 0, conversationId);

		} catch (Exception e) {
			log.log(Level.INFO, "[" + guser.sessionId + "] - recorder ", e);
		} finally {
		}
		return null;
		// refinePerformRequestPatch( sessionId, url, token, timeOutInSeconds, he)
	}

	public static enum AudioType {
		WAV, WEBM, WAV_ULAW, OGG_VORBIS, OGG_OPUS, MP3, NONE
	}

	public static Calendar convertToUCT(String dateStr) throws Exception {
		DateTimeFormatter formatter = DateTimeFormatter
				.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
				.withZone(ZoneOffset.UTC);
		Instant instant = Instant.from(formatter.parse(dateStr));
		Calendar cal = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"));
		cal.setTimeInMillis(instant.toEpochMilli());
		return cal;
	}

	public static JSONArray getRecorderList(GenesysUser guser, String conversationId, AudioType format) {
		try {
			// String urlString = "https://api." + urlRegion + "/api/v2/conversations/" +
			// conversationId + "/recordings";
			String urlString = prefixApi + guser.urlRegion + "/api/v2/conversations/" + conversationId + "/recordings";
			URIBuilder builder = null;
			try {
				builder = new URIBuilder(urlString);
				builder.setParameter("maxWaitMs", "20000");
				builder.setParameter("formatId", "NONE");
			} catch (Exception e) {
				log.log(Level.SEVERE, "", e);
				return null;
			}
			urlString = builder.toString();
			log.log(Level.INFO, "[" + guser.sessionId + "," + conversationId + "] urlString:" + urlString + " ");
			JSONArray jsonStringArray = (JSONArray) refinePerformRequestGet(guser, urlString, 0, conversationId);
			log.log(Level.INFO, "\n" + jsonStringArray.toString());
			return jsonStringArray;
		} catch (Exception e) {
			log.log(Level.SEVERE, "[" + guser.sessionId + "," + conversationId + "] - recorder ", e);
		} finally {
		}
		return null;
	}

	public static boolean downloadFile(GenesysUser guser, JSONObject jo, AudioType format, File file, String urlDownload, String oriUrlDownload) throws Exception {
		try {
			String selfUri = jo.getString("selfUri");
			String id = jo.getString("id");
			URIBuilder builder = null;
			try {
				builder = new URIBuilder(prefixApi + guser.urlRegion + selfUri);
				// builder.setParameter("maxWaitMs", "20000");
				builder.setParameter("formatId", format.name());
				builder.setParameter("download", "true");
				builder.setParameter("mediaFormats", format.name());
				builder.setParameter("fileName", id);
			} catch (Exception e) {
				log.log(Level.SEVERE, "[" + guser.sessionId + "," + id + "]", e);
				return false;
			}
			String urlString = builder.toString();
			log.log(Level.INFO, "[" + guser.sessionId + "," + id + "] urlString:" + urlString + " ");

			JSONObject jsonString = null;
			boolean retry = true;
			while (retry) {
				try {
					jsonString = (JSONObject) refinePerformRequestGet(guser, urlString, 0, id);
					retry = false;
				} catch (GenesysCloud202Exception e) {
					if (e.jRes != null && e.jRes.optString("jsonString", "").contains("\"fileState\":\"DELETED\"")) {
						log.log(Level.WARNING, "[" + guser.sessionId + "," + id + "] - fileState is DELETED. Breaking retry loop.");
						return false;
					}
					log.log(Level.INFO, "[" + guser.sessionId + "," + id + "] - response is 202 sleep for retry");
					retry = true;
					Thread.sleep(2000);

				}
			}
			// JSONObject jsonString = new JSONObject(jo.getString("jsonString"));
			String mediaUri = jsonString.getJSONObject("mediaUris").getJSONObject("S").getString("mediaUri");
			String mediaUri2 = mediaUri;
			if (StringUtils.isNotBlank(oriUrlDownload) && StringUtils.isNotBlank(urlDownload))
				mediaUri2 = StringUtils.replace(mediaUri, oriUrlDownload, urlDownload);
			log.log(Level.INFO, "[" + guser.sessionId + "," + id + "] - download: " + mediaUri2 + "\n    ori: " + mediaUri);
			downloadFile(file, mediaUri2);
			log.log(Level.INFO, "[" + guser.sessionId + "," + id + "] - copy completed");
			return true;
		} catch (Exception e) {
			log.log(Level.SEVERE, "", e);
			return false;
		}
	}

	public static boolean downloadMailboxFile(GenesysUser guser, String messageId, AudioType format, File file) throws Exception {
		try {
			String urlString = prefixApi + guser.urlRegion + "/api/v2/voicemail/messages/" + messageId + "/media";
			getRecorderList(guser, urlString, format);

			URIBuilder builder = null;
			try {
				builder = new URIBuilder(urlString);
				// builder.setParameter("maxWaitMs", "20000");
				builder.setParameter("formatId", format.name());

			} catch (Exception e) {
				log.log(Level.SEVERE, "[" + guser.sessionId + "," + messageId + "] - ", e);
				return false;
			}
			urlString = builder.toString();
			log.log(Level.INFO, "[" + guser.sessionId + "," + messageId + "] urlString:" + urlString + " ");
			JSONObject jsonString = null;
			boolean retry = true;
			while (retry) {
				try {
					jsonString = (JSONObject) refinePerformRequestGet(guser, urlString, 0, messageId);
					retry = false;
				} catch (GenesysCloud202Exception e) {
					log.log(Level.INFO, "[" + guser.sessionId + "," + messageId + "] response is 202 sleep for retry");
					retry = true;
					Thread.sleep(2000);
				}
			}
			//
			String mediaUri = jsonString.getString("mediaFileUri");

			log.log(Level.INFO, "[" + guser.sessionId + "," + messageId + "] - download: \n    ori: " + mediaUri);
			downloadFile(file, mediaUri);
			log.log(Level.INFO, "[" + guser.sessionId + "," + messageId + "] - copy completed");
			return true;
		} catch (Exception e) {
			log.log(Level.SEVERE, "", e);
			return false;
		}
	}

	private static void downloadFile(File file, String mediaUri_trace) throws Exception {
		int i = 0;

		log.log(Level.INFO, "downloadFile: " + file.getName() + " mediaUri_trace:" + mediaUri_trace);
		HttpResponse response = null;
		HttpClient httpClient = HttpClientBuilder.create().build();
		HttpRequestBase request = null;
		InputStream is = null;
		FileOutputStream targetFile = null;
		try {
			try {

				log.log(Level.INFO, "target: " + file.getCanonicalPath());

				file.getParentFile().mkdirs();
			} catch (Exception e) {
				log.log(Level.SEVERE, "Error in create directory : ", e);
				throw e;
			}
			log.log(Level.INFO, "download file:" + mediaUri_trace);
			do {
				i++;
				request = new HttpGet(mediaUri_trace);
				response = httpClient.execute(request);
				log.log(Level.INFO, "Response status " + response.getStatusLine().getStatusCode());
				if (response.getStatusLine().getStatusCode() == 429 || response.getStatusLine().getStatusCode() == 202) {
					log.log(Level.INFO, "sleep");
					Thread.sleep(5000);
					log.log(Level.INFO, "wake up");
				}
				Header[] ha = response.getAllHeaders();
				for (int x = 0; x < ha.length; x++) {
					log.log(Level.INFO, ha[x].getName() + "," + ha[x].getValue());
				}
			} while ((response.getStatusLine().getStatusCode() == 429 || response.getStatusLine().getStatusCode() == 202) && i < 20);
			targetFile = new FileOutputStream(file);
			is = response.getEntity().getContent();
			IOUtils.copy(is, targetFile);
			log.log(Level.INFO, "file: " + file.getPath());

		} finally {
			try {
				is.close();
			} catch (Exception e) {

			}
			try {
				targetFile.close();
			} catch (Exception e) {

			}

		}

	}

	public static JSONObject makeCallVoice(GenesysUser guser, String callNumber, String callFromQueueId) {
		try {
			String urlString = prefixApi + guser.urlRegion + "/api/v2/conversations/calls";
			log.log(Level.INFO, "[" + guser.sessionId + "," + callNumber + "] urlString:" + urlString + " ");

			JSONObject jo = new JSONObject();
			jo.put("callFromQueueId", callFromQueueId);
			// jo.put("callQueueId", callFromQueueId);
			JSONArray ja = new JSONArray();
			ja.put(new JSONObject().put("address", callNumber));
			jo.put("phoneNumber", callNumber);
			StringEntity he = new StringEntity(jo.toString());
			log.log(Level.INFO, "[" + guser.sessionId + "," + callNumber + "] " + jo.toString());
			try {
				return refinePerformRequestPost(guser, urlString, he, "application/json; charset=UTF-8", 0, callNumber);

			} catch (GenesysCloud202Exception e) {
				return e.jRes;

			}
		} catch (Exception e) {
			log.log(Level.INFO, "[" + guser.sessionId + "," + callNumber + "] - makeCallVoice ", e);
		} finally {
		}
		return null;

	}

	public static JSONObject confCallVoice(GenesysUser guser, String callNumber, String id, String ex) {
		try {
			String urlString = prefixApi + guser.urlRegion + "/api/v2/conversations/calls/" + id + "/participants";
			log.log(Level.INFO, "[" + guser.sessionId + "," + callNumber + "] urlString:" + urlString + " ");

			JSONObject jo = new JSONObject();
			jo.put("externalTag", ex);
			JSONArray ja = new JSONArray();
			jo.put("participants", ja);

			JSONObject address = new JSONObject();
			address.put("address", callNumber);
			address.put("dnis", ex);
			ja.put(address);

			StringEntity he = new StringEntity(jo.toString());
			log.log(Level.INFO, "[" + guser.sessionId + "," + callNumber + "] " + jo.toString());
			try {
				return refinePerformRequestPost(guser, urlString, he, "application/json; charset=UTF-8", 0, callNumber);

			} catch (GenesysCloud202Exception e) {
				return e.jRes;

			}
		} catch (Exception e) {
			log.log(Level.INFO, "[" + guser.sessionId + "," + callNumber + "] - confCallVoice ", e);
		} finally {
		}
		return null;

	}

	public static JSONObject makeCallFromCallBack(GenesysUser guser, String conversationId, String callNumber) {
		try {
			String urlString = prefixApi + guser.urlRegion + "/api/v2/conversations/calls/" + conversationId;
			log.log(Level.INFO, "[" + guser.sessionId + "," + conversationId + "] urlString:" + urlString + " ");

			JSONObject jo = new JSONObject();
			jo.put("callNumber", callNumber);
			StringEntity he = new StringEntity(jo.toString());
			log.log(Level.INFO, "[" + guser.sessionId + "," + conversationId + "] " + jo.toString());
			return refinePerformRequestPost(guser, urlString, he, "application/json; charset=UTF-8", 0, conversationId);

		} catch (Exception e) {
			log.log(Level.INFO, "[" + guser.sessionId + "," + conversationId + "] - makeCallFromCallBack ", e);
		} finally {
		}
		return null;
		// refinePerformRequestPatch( sessionId, url, token, timeOutInSeconds, he)
	}

	public static JSONObject convesationStatus(GenesysUser guser, String conversationId) {
		try {
			String urlString = prefixApi + guser.urlRegion + "/api/v2/conversations/calls/" + conversationId;
			log.log(Level.INFO, "[" + guser.sessionId + "," + conversationId + "] urlString:" + urlString + " ");

			return (JSONObject) refinePerformRequestGet(guser, urlString, 0, conversationId);

		} catch (Exception e) {
			log.log(Level.INFO, "[" + guser.sessionId + "] - convesationStatus ", e);
		} finally {
		}
		return null;
		// refinePerformRequestPatch( sessionId, url, token, timeOutInSeconds, he)
	}

	public static JSONObject getUserList(GenesysUser guser, int pageNumber, int pageSize) {
		try {
			String urlString = prefixApi + guser.urlRegion + "/api/v2/users?pageSize=" + pageSize + "&pageNumber=" + pageNumber;
			log.log(Level.INFO, "[" + guser.sessionId + ",userList] urlString:" + urlString + " ");

			return (JSONObject) refinePerformRequestGet(guser, urlString, 0, "userList");

		} catch (Exception e) {
			log.log(Level.INFO, "[" + guser.sessionId + ",userList] - getUserList ", e);
		} finally {
		}
		return null;
		// refinePerformRequestPatch( sessionId, url, token, timeOutInSeconds, he)
	}
	
	public static JSONObject getUser(GenesysUser guser, String userid) {
		try {
			String urlString = prefixApi + guser.urlRegion + "/api/v2/users/" + userid ;
			log.log(Level.INFO, "[" + guser.sessionId + ",userList] urlString:" + urlString + " ");

			return (JSONObject) refinePerformRequestGet(guser, urlString, 0, "getUser");

		} catch (Exception e) {
			log.log(Level.INFO, "[" + guser.sessionId + ",getUser] - getUser ", e);
		} finally {
		}
		return null;
		// refinePerformRequestPatch( sessionId, url, token, timeOutInSeconds, he)
	}
	public static JSONArray getAllUserList(GenesysUser guser) {
		JSONArray jaRes = new JSONArray();
		JSONArray jaRows = new JSONArray();
		JSONObject jRow = new JSONObject();
		try {
			int page = 0;
			do {
				jRow = getUserList(guser, (++page), 500);
				if (jRow.has("entities")) {
					jaRows = jRow.getJSONArray("entities");
					if (jaRows.length() > 0) {
						for (int i = 0; i < jaRows.length(); i++) {
							jaRes.put(jaRows.getJSONObject(i));
						}
					}
				}
			} while (jRow.has("entities") && jaRows.length() > 0);
			return jaRes;
		} catch (Exception e) {
			log.log(Level.WARNING, "[" + guser.sessionId + "] - getAllQueueList ", e);
		} finally {
		}
		return null;
	}

	public static JSONObject getQueueList(GenesysUser guser, String name, String pageNumber, String pageSize) {
		try {

			String urlString = prefixApi + guser.urlRegion + "/api/v2/routing/queues?pageSize=" + pageSize + "&pageNumber=" + pageNumber;
			if (!StringUtils.isBlank(name)) {
				name = URLEncoder.encode(name, StandardCharsets.UTF_8);
				urlString += "&name=" + name;
			}
			log.log(Level.INFO, "[" + guser.sessionId + ",getQueueList] urlString:" + urlString + " ");

			return (JSONObject) refinePerformRequestGet(guser, urlString, 0, "getQueueList");

		} catch (Exception e) {
			log.log(Level.INFO, "[" + guser.sessionId + ",getQueueList] - getQueueList ", e);
		} finally {
		}
		return null;
		// refinePerformRequestPatch( sessionId, url, token, timeOutInSeconds, he)
	}

	public static JSONArray getAllQueueList(GenesysUser guser, String name) {
		JSONArray jaRes = new JSONArray();
		JSONArray jaRows = new JSONArray();
		JSONObject jRow = new JSONObject();
		try {
			int page = 0;
			do {
				jRow = getQueueList(guser, name, "" + (++page), "" + 500);
				if (jRow.has("entities")) {
					jaRows = jRow.getJSONArray("entities");
					if (jaRows.length() > 0) {
						for (int i = 0; i < jaRows.length(); i++) {
							jaRes.put(jaRows.getJSONObject(i));
						}
					}
				}
			} while (jRow.has("entities") && jaRows.length() > 0);
			return jaRes;
		} catch (Exception e) {
			log.log(Level.WARNING, "[" + guser.sessionId + "] - getAllQueueList ", e);
		} finally {
		}
		return null;
	}

	public static JSONObject getDivisionList(GenesysUser guser, String pageNumber, String pageSize) {
		try {
			String urlString = prefixApi + guser.urlRegion + "/api/v2/authorization/divisions?pageSize=" + pageSize + "&pageNumber=" + pageNumber;
			log.log(Level.INFO, "[" + guser.sessionId + ",getDivisionList] urlString:" + urlString + " ");
			return (JSONObject) refinePerformRequestGet(guser, urlString, 0, "getDivisionList");
		} catch (Exception e) {
			log.log(Level.INFO, "[" + guser.sessionId + ",getDivisionList] - getDivisionList ", e);
		} finally {
		}
		return null;
		// refinePerformRequestPatch( sessionId, url, token, timeOutInSeconds, he)
	}

	public static JSONObject getDatabaseRows(GenesysUser guser, String databaseId, int pageNumber, int pageSize) {
		try {
			String urlString = prefixApi + guser.urlRegion + "/api/v2/flows/datatables/" + databaseId + "/rows?pageSize=" + pageSize + "&pageNumber=" + pageNumber + "&&showbrief=false";
			log.log(Level.INFO, "[" + guser.sessionId + "," + databaseId + "] urlString:" + urlString + " ");
			return (JSONObject) refinePerformRequestGet(guser, urlString, 0, databaseId);
		} catch (Exception e) {
			log.log(Level.INFO, "[" + guser.sessionId + "," + databaseId + "] - getDatabaseRows ", e);
		} finally {
		}
		return null;
	}

	public static boolean deleteExternalContact(GenesysUser guser, String id) {

		try {
			String urlString = prefixApi + guser.urlRegion + "/api/v2/externalcontacts/contacts/" + id;
			log.log(Level.INFO, "[" + guser.sessionId + "," + id + "] urlString:" + urlString + " ");
			try {

				return refinePerformRequestDelete(guser, urlString, 0, id) != null;
			} catch (GenesysCloud204Exception e) {
				return true;
			}
		} catch (Exception e) {
			log.log(Level.INFO, "[" + guser.sessionId + "," + id + "] - deleteExternalContact ", e);
			return false;
		} finally {
		}

	}

	public static JSONArray getKnowledgeSearchDocument(GenesysUser guser, String knowledgeBaseId, String q) {
		try {
			String urlString = prefixApi + guser.urlRegion + "/api/v2/knowledge/knowledgebases/" + knowledgeBaseId + "/documents/search";
			log.log(Level.INFO, "[" + guser.sessionId + "," + knowledgeBaseId + "] urlString:" + urlString + " q=" + q);
			StringEntity he = new StringEntity(new JSONObject().put("query", q).toString());
			JSONObject jo = refinePerformRequestPost(guser, urlString, he, "application/json; charset=UTF-8", 0, knowledgeBaseId);

			return jo.getJSONArray("results");

		} catch (Exception e) {
			log.log(Level.INFO, "[" + guser.sessionId + "," + knowledgeBaseId + "] - getSearchDocument ", e);
		} finally {
		}
		return null;
	}
	public static JSONArray getKnowledgeSessionSearchDocument(String urlRegion, String sessionid, String q) {
		try {
			String urlString = prefixApi + urlRegion + "/api/v2/knowledge/guest/sessions/"+sessionid+"/documents/search";
			//String urlString="http://localhost:8085/FAQ/NewFile.jsp";
			log.log(Level.INFO, "[" + sessionid + ",] urlString:" + urlString );
			StringEntity he = new StringEntity(new JSONObject().put("query", q).toString(), "UTF-8");
			log.log(Level.INFO, "[" + sessionid + ",] he:" + he.toString() );
			JSONObject jo = refinePerformRequestPost(null, urlString, he, "application/json", 0, sessionid);

			return jo.getJSONArray("results");

		} catch (Exception e) {
			log.log(Level.INFO, "[" + sessionid + "] - getKnowledgeSessionSearchDocument ", e);
		} finally {
		}
		return null;
	}
	public static JSONArray getKnowledgeSessionSearchDocumentSuggestions(String urlRegion, String sessionid, String q) {
		try {
			String urlString = prefixApi + urlRegion + "/api/v2/knowledge/guest/sessions/"+sessionid+"/documents/search/suggestions";
			log.log(Level.INFO, "[" + sessionid + ",] urlString:" + urlString + " q=" + q);
			StringEntity he = new StringEntity(new JSONObject().put("query", q).toString(), "UTF-8");
			JSONObject jo = refinePerformRequestPost(null, urlString, he, "application/json; charset=UTF-8", 0, sessionid);

			return jo.getJSONArray("results");

		} catch (Exception e) {
			log.log(Level.INFO, "[" + sessionid + "] - getKnowledgeSessionSearchDocument ", e);
		} finally {
		}
		return null;
	}
	public static JSONArray getKnowledgeSearchDocumentSuggestions(GenesysUser guser, String knowledgeBaseId, String q) {
		try {
			String urlString = prefixApi + guser.urlRegion + "/api/v2/knowledge/knowledgebases/" + knowledgeBaseId + "/documents/search/Suggestions";
			log.log(Level.INFO, "[" + guser.sessionId + "," + knowledgeBaseId + "] urlString:" + urlString + " q=" + q);
			StringEntity he = new StringEntity(new JSONObject().put("query", q).toString(), "UTF-8");
			JSONObject jo = refinePerformRequestPost(guser, urlString, he, "application/json; charset=UTF-8", 0, knowledgeBaseId);

			return jo.getJSONArray("results");

		} catch (Exception e) {
			log.log(Level.INFO, "[" + guser.sessionId + "," + knowledgeBaseId + "] - getKnowledgeSearchDocumentSuggestions ", e);
		} finally {
		}
		return null;
	}
	public static JSONArray getKnowledgeDocuments(GenesysUser guser, String knowledgeBaseId, String category) {
		try {
			String urlString = prefixApi + guser.urlRegion + "/api/v2/knowledge/knowledgebases/" + knowledgeBaseId + "/documents?includeDrafts=false" ;
			if (StringUtils.isNotBlank(category)) {
				urlString = urlString+"&categoryId="+category;
			}
			log.log(Level.INFO, "[" + guser.sessionId + "," + knowledgeBaseId + "] urlString:" + urlString);
			JSONObject jo =(JSONObject) refinePerformRequestGet(guser, urlString, 0, knowledgeBaseId + " - " + category);
			return jo.getJSONArray("entities");
		} catch (Exception e) {
			log.log(Level.INFO, "[" + guser.sessionId + "," + knowledgeBaseId + "] - getKnowledgeDocuments ", e);
		} finally {
		}
		return null;
	}
	public static JSONArray getKnowledgeSessionDocuments(String urlRegion,String sessionId,  String category) {
		try {
			String urlString = prefixApi + urlRegion + "/api/v2/knowledge/guest/sessions/"+sessionId+"/documents" ;
			if (StringUtils.isNotBlank(category)) {
				urlString = urlString+"?categoryId="+category;
			}
			log.log(Level.INFO, "[" + sessionId + "] urlString:" + urlString);
			JSONObject jo =(JSONObject) refinePerformRequestGet(null, urlString, 0, sessionId + " - " + category);
			return jo.getJSONArray("entities");
		} catch (Exception e) {
			log.log(Level.INFO, "[" + sessionId + "] - getKnowledgeSessionDocuments ", e);
		} finally {
		}
		return null;
	}
	
	public static JSONObject getKnowledgeSessionDocumentDetails(String urlRegion, String sessionid, String documentId) {
		try {
			String urlString = prefixApi +  urlRegion + "/api/v2/knowledge/guest/sessions/"+sessionid+"/documents/"+documentId;
			log.log(Level.INFO, "[" + sessionid + "] urlString:" + urlString);
			return (JSONObject) refinePerformRequestGet(null, urlString, 0, sessionid + " - " + documentId);
		} catch (Exception e) {
			log.log(Level.INFO, "[" + sessionid + "] - getKnowledgeDocument ", e);
		} finally {
		}
		return null;
	}

	public static JSONObject getKnowledgeDocumentDetails(GenesysUser guser, String knowledgeBaseId, String documentId) {
		try {
			String urlString = prefixApi + guser.urlRegion + "/api/v2/knowledge/knowledgebases/" + knowledgeBaseId + "/documents/" + documentId+"?expand=variations";
			log.log(Level.INFO, "[" + guser.sessionId + "," + knowledgeBaseId + "] urlString:" + urlString);
			return (JSONObject) refinePerformRequestGet(guser, urlString, 0, knowledgeBaseId + " - " + documentId);
		} catch (Exception e) {
			log.log(Level.INFO, "[" + guser.sessionId + "," + knowledgeBaseId + "] - getKnowledgeDocument ", e);
		} finally {
		}
		return null;
	}

	public enum Reating { Negative,Positive}
	public static boolean postKnowledgeFeedback(GenesysUser guser, String knowledgeBaseId, String  documentId,String variationid, Reating reating) {

		try {
			String urlString = prefixApi + guser.urlRegion + "/api/v2/knowledge/knowledgebases/"+knowledgeBaseId+"/documents/"+documentId+"/feedback";
			log.log(Level.INFO, "[" + guser.sessionId + "," + knowledgeBaseId + "] urlString:" + urlString + " ");
			
			JSONObject jo = new JSONObject().put("rating", reating.name());
			jo.put("documentVariation", new JSONObject().put("id", variationid));
			log.log(Level.INFO, "[" + guser.sessionId + "," + knowledgeBaseId + "] " + jo.toString());
			StringEntity he = new StringEntity(jo.toString(), "UTF-8");

			return refinePerformRequestPost(guser, urlString, he, "application/json; charset=UTF-8", 0, knowledgeBaseId) != null;
		} catch (Exception e) {
			log.log(Level.INFO, "[" + guser.sessionId + "," + knowledgeBaseId + "] - postKnowledgeFeedback ", e);
			return false;
		} finally {
		}

	}
	public enum Reasons { DocumentContent,SearchResults}
	public static boolean postKnowledgeSessionFeedback(String urlRegion, String sessionid, String  documentId,String variationid,String versionid, Reating reating,Reasons reason,String comment ) {

		try {
			String urlString = prefixApi + urlRegion + "/api/v2/knowledge/guest/sessions/"+sessionid+"/documents/"+documentId+"/feedback";
			log.log(Level.INFO, "[" + sessionid+ "] urlString:" + urlString + " ");
			
			JSONObject jo = new JSONObject().put("rating", reating.name());
			jo.put("documentVariation", new JSONObject().put("id", variationid));
			jo.put("document", new JSONObject().put("versionId", versionid));
			if (reason!=null) {
				jo.put("reason",reason.name());
			}
			if (StringUtils.isNotBlank(comment)) {
				jo.put("comment",comment);
			}
			log.log(Level.INFO, "[" + sessionid + "] " + jo.toString());
			StringEntity he = new StringEntity(jo.toString(), "UTF-8");

			return refinePerformRequestPost(null, urlString, he, "application/json; charset=UTF-8", 0, sessionid) != null;
		} catch (Exception e) {
			log.log(Level.INFO, "[" + sessionid+ "] - postKnowledgeSessionFeedback ", e);
			return false;
		} finally {
		}

	}
	
	
	public static JSONObject createCallBack(GenesysUser guser, String phoneNumber, String queueId, String callbackUserName) {
		try {
		
			String urlString = prefixApi + guser.urlRegion + "/api/v2/conversations/callbacks";
			JSONObject jo = new JSONObject();
			JSONArray ja = new JSONArray();
			ja.put(phoneNumber);
			jo.put("callbackNumbers", ja);
			jo.put("queueId", queueId);
			jo.put("callbackUserName", callbackUserName);
			
		
			HttpEntity he = new StringEntity(jo.toString(),  "UTF-8");
			// HttpEntity he = new UrlEncodedFormEntity(params,
			// ContentType.APPLICATION_JSON);
			log.info("[" + guser.sessionId + "] " +he);
			return refinePerformRequestPost(guser, urlString, he, "application/json ", 0, "createCallBack");

		} catch (Exception e) {
			log.log(Level.INFO, "[" + guser.sessionId + "] - createCallBack ", e);
		} finally {
		}
		return null;
	}
	
	
	public static JSONObject getKnowledgeSession(String urlRegion, String deploymentId, String  type,String customerId) {

		try {
			String urlString = prefixApi + urlRegion + "/api/v2/knowledge/guest/sessions";
			log.log(Level.INFO, "[" + customerId + "," + deploymentId + ","+type+"] urlString:" + urlString + " ");
			
			JSONObject jo = new JSONObject().put("app", new JSONObject().put("deploymentId", deploymentId).put("type",type));
			jo.put("customerId", customerId);
			log.log(Level.INFO, "[" + customerId + "," + deploymentId + "] " + jo.toString());
			StringEntity he = new StringEntity(jo.toString(), "UTF-8");

			return refinePerformRequestPost(null, urlString, he, "application/json; charset=UTF-8", 0, deploymentId);
		} catch (Exception e) {
			log.log(Level.INFO, "[" + customerId + "," + deploymentId + "] - getKnowledgeSession ", e);
			return null;
		} finally {
		}

	}
	public static JSONArray getKnowledgeGetegories100(GenesysUser guser, String knowledgeBaseId) {
		try {
			String urlString = prefixApi + guser.urlRegion + "/api/v2/knowledge/knowledgebases/" + knowledgeBaseId + "/categories?pageSize=100";
			log.log(Level.INFO, "[" + guser.sessionId + "," + knowledgeBaseId + "] urlString:" + urlString);
			JSONObject jo = (JSONObject) refinePerformRequestGet(guser, urlString, 0, knowledgeBaseId);
			
			return jo.getJSONArray("entities");
		} catch (Exception e) {
			log.log(Level.INFO, "[" + guser.sessionId + "," + knowledgeBaseId + "] - getKnowledgeGetegories100 ", e);
		} finally {
		}
		return null;
	}
	public static JSONArray getKnowledgeSessionGetegories100(String urlRegion, String sessionid, String knowledgeBaseId) {
		try {
			String urlString = prefixApi + urlRegion + "/api/v2/knowledge/guest/sessions/"+sessionid+"/categories?pageSize=100";
			log.log(Level.INFO, "[" + sessionid + "," + knowledgeBaseId + "] urlString:" + urlString);
			JSONObject jo = (JSONObject) refinePerformRequestGet(null, urlString, 0, knowledgeBaseId);
			
			return jo.getJSONArray("entities");
		} catch (Exception e) {
			log.log(Level.INFO, "[" + sessionid + "," + knowledgeBaseId + "] - getKnowledgeSessionGetegories100 ", e);
		} finally {
		}
		return null;
	}
	public static JSONObject getDatabaseRowById(GenesysUser guser, String databaseId, String rowId) {
		try {
			String urlString = prefixApi + guser.urlRegion + "/api/v2/flows/datatables/" + databaseId + "/rows/" + rowId + "?showbrief=false";
			log.log(Level.INFO, "[" + guser.sessionId + "," + databaseId + "] urlString:" + urlString + " ");
			return (JSONObject) refinePerformRequestGet(guser, urlString, 0, rowId);
		} catch (Exception e) {
			log.log(Level.INFO, "[" + guser.sessionId + "," + databaseId + "] - getDatabaseRowById ", e);
		} finally {
		}
		return null;
	}

	public static boolean putDatabaseRow(GenesysUser guser, String databaseId, JSONObject jo) {

		try {
			String urlString = prefixApi + guser.urlRegion + "/api/v2/flows/datatables/" + databaseId + "/rows/" + jo.getString("key");
			log.log(Level.INFO, "[" + guser.sessionId + "," + databaseId + "] urlString:" + urlString + " ");

			StringEntity he = new StringEntity(jo.toString(), "UTF-8");

			return refinePerformRequestPut(guser, urlString, he, "application/json; charset=UTF-8", 0, databaseId) != null;
		} catch (Exception e) {
			log.log(Level.INFO, "[" + guser.sessionId + "," + databaseId + "]- putDatabaseRow ", e);
			return false;
		} finally {
		}

	}

	public static boolean updateEdge(GenesysUser guser, String id, JSONObject jStatusCode) {
		try {
			String urlString = prefixApi + guser.urlRegion + "/api/v2/telephony/providers/edges/" + id;
			log.log(Level.INFO, "[" + guser.sessionId + "," + id + "] urlString:" + urlString + " ");
			StringEntity he = new StringEntity(jStatusCode.toString(), "UTF-8");
			return refinePerformRequestPut(guser, urlString, he, "application/json; charset=UTF-8", 0, id) != null;
		} catch (Exception e) {
			log.log(Level.INFO, "[" + guser.sessionId + "," + id + "] - updateEdge jStatusCode: " + jStatusCode + " id:" + id, e);
			return false;
		} finally {
		}

	}

	public static boolean putEdgeInOutService(GenesysUser guser, String id, boolean inservive) {
		try {
			String urlString = prefixApi + guser.urlRegion + "/api/v2/telephony/providers/edges/" + id + "/statuscode";
			log.log(Level.INFO, "[" + guser.sessionId + "," + id + "] urlString:" + urlString + " inservive:" + inservive);

			StringEntity he = new StringEntity(new JSONObject().put("inService", inservive).toString(), "UTF-8");
			return refinePerformRequestPost(guser, urlString, he, "application/json; charset=UTF-8", 0, id) != null;
		} catch (Exception e) {
			log.log(Level.INFO, "[" + guser.sessionId + "," + id + "] - putEdgeInOutService inservive: " + inservive + " id:" + id, e);
			return false;
		} finally {
		}

	}

	public static boolean putExternalcontacts(GenesysUser guser, JSONObject jo) {
		try {
			String urlString = prefixApi + guser.urlRegion + "/api/v2/externalcontacts/contacts";
			log.log(Level.INFO, "[" + guser.sessionId + ",putExternalcontacts] urlString:" + urlString + " parameters:" + jo);

			StringEntity he = new StringEntity(jo.toString(), "UTF-8");
			return refinePerformRequestPost(guser, urlString, he, "application/json; charset=UTF-8", 0, "putExternalcontacts") != null;
		} catch (Exception e) {
			log.log(Level.INFO, "[" + guser.sessionId + ",putExternalcontacts] - putExternalcontacts ", e);
			return false;
		} finally {
		}

	}

	public static boolean updateExternalcontacts(GenesysUser guser, String id, JSONObject jo) {
		try {
			String urlString = prefixApi + guser.urlRegion + "/api/v2/externalcontacts/contacts/" + id;
			log.log(Level.INFO, "[" + guser.sessionId + ",updateExternalcontacts] urlString:" + urlString + " parameters:" + jo);

			StringEntity he = new StringEntity(jo.toString(), "UTF-8");
			return refinePerformRequestPut(guser, urlString, he, "application/json; charset=UTF-8", 0, "updateExternalcontacts") != null;
		} catch (Exception e) {
			log.log(Level.INFO, "[" + guser.sessionId + ",updateExternalcontacts] -  ", e);
			return false;
		} finally {
		}

	}

	public static boolean updateRecordingRetention(GenesysUser guser, String conversationId, String recordingId, String deleteDate) {
		try {
			String urlString = prefixApi + guser.urlRegion + "/api/v2/conversations/" + conversationId + "/recordings/" + recordingId;
			log.log(Level.INFO, "[" + guser.sessionId + ",updateRecordingRetention] urlString:" + urlString
					+ " conversationId=" + conversationId + " recordingId=" + recordingId + " deleteDate=" + deleteDate);

			JSONObject body = new JSONObject();
			body.put("deleteDate", deleteDate);

			StringEntity he = new StringEntity(body.toString(), "UTF-8");
			return refinePerformRequestPut(guser, urlString, he, "application/json; charset=UTF-8", 0, "updateRecordingRetention") != null;
		} catch (Exception e) {
			log.log(Level.INFO, "[" + guser.sessionId + ",updateRecordingRetention] - conversationId=" + conversationId
					+ " recordingId=" + recordingId, e);
			return false;
		} finally {
		}
	}

	public static boolean createDatabaseRow(GenesysUser guser, String databaseId, JSONObject jo) {

		try {
			String urlString = prefixApi + guser.urlRegion + "/api/v2/flows/datatables/" + databaseId + "/rows";
			log.log(Level.INFO, "[" + guser.sessionId + "," + databaseId + "] urlString:" + urlString + " ");
			log.log(Level.INFO, "[" + guser.sessionId + "," + databaseId + "] " + jo.toString());
			StringEntity he = new StringEntity(jo.toString(), "UTF-8");

			return refinePerformRequestPost(guser, urlString, he, "application/json; charset=UTF-8", 0, databaseId) != null;
		} catch (Exception e) {
			log.log(Level.INFO, "[" + guser.sessionId + "," + databaseId + "] - putDatabaseRow ", e);
			return false;
		} finally {
		}

	}

	public static boolean deleteDatabaseRow(GenesysUser guser, String datatableId, String id) {

		try {
			String urlString = prefixApi + guser.urlRegion + "/api/v2/flows/datatables/" + datatableId + "/rows/" + id;
			log.log(Level.INFO, "[" + guser.sessionId + "," + datatableId + "] urlString:" + urlString + " ");
			try {

				return refinePerformRequestDelete(guser, urlString, 0, datatableId) != null;
			} catch (GenesysCloud204Exception e) {
				return true;
			}
		} catch (Exception e) {
			log.log(Level.INFO, "[" + guser.sessionId + "," + datatableId + "] - deleteDatabaseRow ", e);
			return false;
		} finally {
		}

	}

	public static JSONArray getAllDatabaseRows(GenesysUser guser, String databaseId) throws JSONException {

		JSONArray jaRes = new JSONArray();
		JSONArray jaRows = new JSONArray();
		JSONObject jRow = new JSONObject();
		try {
			int page = 0;
			do {
				jRow = getDatabaseRows(guser, databaseId, ++page, 500);
				if (jRow.has("entities")) {
					jaRows = jRow.getJSONArray("entities");
					if (jaRows.length() > 0) {
						for (int i = 0; i < jaRows.length(); i++) {
							jaRes.put(jaRows.getJSONObject(i));
						}
					}
				}
			} while (jRow.has("entities") && jaRows.length() > 0);
			return jaRes;

		} catch (Exception e) {
			// log.log(Level.WARNING,"" + jRow.toString());
			log.log(Level.WARNING, "[" + guser.sessionId + "] - getAllDatabaseRows ", e);
		} finally {
		}
		return null;
	}

	public static JSONArray getAllExternalContactsMax1000(GenesysUser guser, String q, String order) throws JSONException {

		JSONArray jaRes = new JSONArray();
		JSONArray jaRows = new JSONArray();
		JSONObject jRow = new JSONObject();
		try {
			int page = 0;
			do {
				jaRows = getExternalContacts(guser, q, order, ++page, 100);

				if (jaRows.length() > 0) {
					for (int i = 0; i < jaRows.length(); i++) {
						jaRes.put(jaRows.getJSONObject(i));

					}
					// System.out.println(jaRes.length());
				}

			} while (jRow.has("entities") && jaRows.length() > 0);
			return jaRes;

		} catch (Exception e) {
			// log.log(Level.WARNING,"" + jRow.toString());
			log.log(Level.WARNING, "[" + guser.sessionId + "] - getAllDatabaseRows ", e);
		} finally {
		}
		return jaRes;
	}

	public static JSONArray getAllEdge(GenesysUser guser) throws JSONException {

		JSONArray jaRes = new JSONArray();
		JSONArray jaRows = new JSONArray();
		JSONObject jRow = new JSONObject();
		try {
			int page = 0;
			do {
				jRow = getEdges(guser, ++page, 500);
				if (jRow.has("entities")) {
					jaRows = jRow.getJSONArray("entities");
					if (jaRows.length() > 0) {
						for (int i = 0; i < jaRows.length(); i++) {
							jaRes.put(jaRows.getJSONObject(i));
						}
					}
				}
			} while (jRow.has("entities") && jaRows.length() > 0);
			return jaRes;

		} catch (Exception e) {
			// log.log(Level.WARNING,"" + jRow.toString());
			log.log(Level.WARNING, "[" + guser.sessionId + "] - getAllDatabaseRows ", e);
		} finally {
		}
		return null;
	}
	public static JSONObject getEdge( GenesysUser guser, String edgeid) {
		try {
			String urlString = prefixApi + guser.urlRegion + "/api/v2/telephony/providers/edges/"+edgeid;
			log.log(Level.INFO, "[" + edgeid + "] urlString:" + urlString + " ");
			return (JSONObject) refinePerformRequestGet( guser, urlString, 0, edgeid);
		} catch (Exception e) {
			log.log(Level.INFO, "[" + edgeid+"] - getEdge ", e);
		} finally {
		}
		return null;
	}

	public static JSONObject getEdges(GenesysUser guser, int pageNumber, int pageSize) {
		try {
			String urlString = prefixApi + guser.urlRegion + "/api/v2/telephony/providers/edges?pageSize=" + pageSize + "&pageNumber=" + pageNumber + "&showbrief=false";
			log.log(Level.INFO, "[" + guser.sessionId + ",getEdges] urlString:" + urlString + " ");
			return (JSONObject) refinePerformRequestGet(guser, urlString, 0, "getEdges");
		} catch (Exception e) {
			log.log(Level.INFO, "[" + guser.sessionId + ",getEdges] - getEdges ", e);
		} finally {
		}
		return null;
	}
	
	public static JSONObject postEdgeReboot(String sessionid, GenesysUser guser, String idEdge) {
		try {
			String urlString = prefixApi + guser.urlRegion + "/api/v2/telephony/providers/edges/"+idEdge+"/reboot";
			log.log(Level.INFO, "[" + sessionid + "] urlString:" + urlString + " ");
			StringEntity he = new StringEntity("{ \"callDrainingWaitTimeSeconds\": 0}", "UTF-8");

			return refinePerformRequestPost(guser, urlString, he, "application/json; charset=UTF-8", 0, sessionid);

		} catch (Exception e) {
			log.log(Level.INFO, "[" + sessionid + "] - postEdgeReboot ", e);
		} finally {
		}
		return null;
	}
	public static JSONArray getExternalContacts(GenesysUser guser, String q, String order, int pageNumber, int pageSize) {
		try {
			JSONArray jaRows = new JSONArray();
			String urlString = prefixApi + guser.urlRegion + "/api/v2/externalcontacts/contacts?pageSize=" + pageSize + "&pageNumber=" + pageNumber;
			if (StringUtils.isNoneBlank(order)) {
				urlString += "&sortOrder=" + order;
			}
			if (StringUtils.isNotBlank(q)) {

				q = URLEncoder.encode(q, StandardCharsets.UTF_8);
				urlString += "&q=" + q;
			}
			log.log(Level.INFO, "[" + guser.sessionId + ",getexternalcontacts v2] urlString:" + urlString + " ");
			JSONObject jRow = (JSONObject) refinePerformRequestGet(guser, urlString, 0, "getexternalcontacts");
			if (jRow.has("entities"))
				jaRows = jRow.getJSONArray("entities");
			return jaRows;
		} catch (Exception e) {
			log.log(Level.INFO, "[" + guser.sessionId + ",getEdges] - getexternalcontacts ", e);
		} finally {
		}
		return null;
	}

	public static JSONObject getExternalContact(GenesysUser guser, String id) {
		try {

			String urlString = prefixApi + guser.urlRegion + "/api/v2/externalcontacts/contacts/" + id;

			log.log(Level.INFO, "[" + guser.sessionId + ",getexternalcontacts v2] urlString:" + urlString + " ");

			return (JSONObject) refinePerformRequestGet(guser, urlString, 0, "getexternalcontacts");
		} catch (Exception e) {
			log.log(Level.INFO, "[" + guser.sessionId + ",getEdges] - getexternalcontacts ", e);
		} finally {
		}
		return null;
	}

	public static JSONObject getConversationEmail(GenesysUser guser, String id) {
		try {

			//String urlString = prefixApi + guser.urlRegion + "/api/v2/conversations/emails/" + id + "/messages"; // non
																													// c'era
																													// /messages
			String urlString = prefixApi + guser.urlRegion + "/api/v2/conversations/emails/" + id;
			
			log.log(Level.INFO, "[" + guser.sessionId + ",getConversationEmail] urlString:" + urlString + " ");

			return (JSONObject) refinePerformRequestGet(guser, urlString, 0, "getConversationEmail");
		} catch (Exception e) {
			log.log(Level.INFO, "[" + guser.sessionId + ",getEdges] - getConversationEmail ", e);
		} finally {
		}
		return null;
	}

	public static JSONArray getConversationMessageAddDetails(GenesysUser guser, JSONArray messages) {
		try {
			for (int i = 0; i < messages.length(); i++) {
				JSONObject message = messages.getJSONObject(i);
				String urlString = prefixApi + guser.urlRegion + message.getString("selfUri");
				JSONObject jo = (JSONObject) refinePerformRequestGet(guser, urlString, 0, "getConversationMessageAddDetails");
				message.put("details", jo);
				log.log(Level.INFO, "[" + guser.sessionId + ",getConversationMessageAddDetails] urlString:" + urlString + " ");
			}
			log.log(Level.INFO, "[" + guser.sessionId + ",getConversationMessageAddDetails] messages:" + messages.toString() + " ");
			return messages;
		} catch (Exception e) {
			log.log(Level.INFO, "[" + guser.sessionId + ",getEdges] - getConversationEmail ", e);
		} finally {
		}
		return null;
	}
//	
//	public static JSONObject getAllDatabaseRow(GenesysUser guser,String databaseId) throws JSONException{
//		boolean moreUser=false;
//		int page = 0;
//		JSONArray all = new JSONArray();
//		do {
//			JSONObject row = Genesys.getDatabaseRow(guser, databaseId, "" + (++page), "" + 500);
//			JSONArray jaRows = row.getJSONArray("entities");
//			if (jaRows.length() > 0) {
//				all.
//			}
//		} while  (moreUser);
//		return null;
//	}

	public static JSONObject getUserRoutingStatus(GenesysUser guser, String userId) {
		try {
			String urlString = prefixApi + guser.urlRegion + "/api/v2/users/" + userId + "/routingstatus";
			log.log(Level.INFO, "[" + guser.sessionId + ",getUserRoutingStatus] urlString:" + urlString + " ");
			return (JSONObject) refinePerformRequestGet(guser, urlString, 0, "getUserRoutingStatus");
		} catch (Exception e) {
			log.log(Level.INFO, "[" + guser.sessionId + ",getUserRoutingStatus] -  ", e);
		} finally {
		}
		return null;
		// refinePerformRequestPatch( sessionId, url, token, timeOutInSeconds, he)
	}

	public static JSONObject getUserPresenceStatus(GenesysUser guser, String userId) {
		try {
			String urlString = prefixApi + guser.urlRegion + "/api/v2/users/" + userId + "/presences/purecloud";
			log.log(Level.INFO, "[" + guser.sessionId + ",getUserPresenceStatus] urlString:" + urlString + " ");
			return (JSONObject) refinePerformRequestGet(guser, urlString, 0, "getUserPresenceStatus");
		} catch (Exception e) {
			log.log(Level.INFO, "[" + guser.sessionId + ",getUserPresenceStatus] -  ", e);
		} finally {
		}
		return null;
		// refinePerformRequestPatch( sessionId, url, token, timeOutInSeconds, he)
	}

	// public static String getMasterToken(String urlRegion, String clientId, String
	// clientSecret) throws Exception {
	// return getMasterToken( urlRegion, clientId, clientSecret, null, null, false);
	// }
	//
	// public static String getMasterToken( String urlRegion, String clientId,
	// String clientSecret, String code, String redirect_uri) throws Exception {
	// return getMasterToken( urlRegion, clientId, clientSecret, code, redirect_uri,
	// false);
	// }

	// public static String getMasterToken( String urlRegion, String clientId,
	// String clientSecret, String code, String redirect_uri, boolean force) throws
	// Exception {
	// JSONObject jo = null;
	// try {
	//// jo = masterTokenArray.get(clientId);
	//// if (jo != null && !force) {
	////
	//// Calendar cal = Calendar.getInstance();
	//// if (cal.getTimeInMillis() < jo.getLong("expires_at"))
	//// return jo.getString("access_token");
	////
	//// }
	// jo = getToken( urlRegion, clientId, clientSecret, code, redirect_uri);
	// log.log(Level.INFO,"token:" + jo.toString());
	// //masterTokenArray.put(clientId, jo);
	// return jo.getString("access_token");
	// } catch (Exception e) {
	// log.log(Level.WARNING, "", e);
	// return null;
	// } finally {
	// log.log(Level.INFO,"token:" + jo.getString("access_token"));
	// }
	//
	// }

	private static JSONObject performRequestPost(String sessionId, String url, String token, int timeOutInSeconds, Object he, String contenttype) throws Exception {
		// List<BasicNameValuePair>
		JSONObject jRes = new JSONObject();
		jRes.put("http_status_code", -1);
		timeOutInSeconds = Math.max(timeOutInSeconds, 5);
		if (StringUtils.isBlank(sessionId)) {
			sessionId = "performRequestPost";
		}
		log.fine("[" + sessionId + "] - setConnectTimeout(" + (timeOutInSeconds * 1000) + ") setConnectionRequestTimeout(" + (timeOutInSeconds * 1000) + ") setSocketTimeout(" + (timeOutInSeconds * 1000) + ") ");
		RequestConfig config = RequestConfig.custom().setConnectTimeout(timeOutInSeconds * 1000).setConnectionRequestTimeout(timeOutInSeconds * 1000).setSocketTimeout(timeOutInSeconds * 1000).build();
		CloseableHttpResponse response = null;
		HttpPost httppost = null;
		CloseableHttpClient httpClient = HttpClientBuilder.create().setDefaultRequestConfig(config).build();
		log.log(Level.INFO, "[" + sessionId + "] - url: " + url);
		httppost = new HttpPost(new URI(url));
		if (he != null) {
			if (he instanceof StringEntity) {
				httppost.setEntity((StringEntity) he);
			}
			if (he instanceof HttpEntity) {
				httppost.setEntity((HttpEntity) he);
			}

		} else {
			log.log(Level.INFO, "[" + sessionId + "] - params:  isNull ");
		}

		httppost.addHeader("content-type", contenttype);
		
		if (StringUtils.isNotBlank(token)) {
			log.log(Level.INFO, "httppost.addHeader(\"Authorization\", \"Bearer " + token + ");");
			httppost.addHeader("Authorization", "Bearer " + "" + token);
		}
		int status = 0;
		int sleepMillisecond = 0;
		String jsonString = "";
		timeOutInSeconds = timeOutInSeconds < 3 ? 3 : timeOutInSeconds;
		while (status == 408 || status == 503 || status == 504 || status == 429 || status == 0) {
			try {
				try {
					Thread.sleep(sleepMillisecond);
				} catch (Exception e) {
				}
				sleepMillisecond = timeOutInSeconds * 1000;
				response = httpClient.execute(httppost);
				status = response.getStatusLine().getStatusCode();
				log.log(Level.INFO, "[" + sessionId + "] - status code: " + status);
				jsonString = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
			} catch (Exception e) {
				if (status == 204) {
					jsonString = "{}";
				} else {
					log.log(Level.WARNING, "[" + sessionId + "] - generic error", e);
					status = -1;
				}

			} finally {

				try {
					response.close();
				} catch (Exception e) {
				}

			}
		}
		try {
			httpClient.close();
		} catch (Exception e) {
		}
		jRes.put("http_status_code", status);
		jRes.put("jsonString", jsonString);
		log.log(Level.INFO, "[" + sessionId + "] - engage url: " + url + " end response : " + jRes);

		return jRes;
	}

	private static JSONObject performRequestGet(String sessionId, String url, String token, int timeOutInSeconds) throws JSONException {

		CloseableHttpClient httpClient = null;
		CloseableHttpResponse res = null;
		JSONObject jres = new JSONObject();
		RequestConfig conf = RequestConfig.custom().setConnectTimeout(3 * 1000).setConnectionRequestTimeout(3 * 1000).setSocketTimeout(3 * 1000).build(); // fix 23/09/2021

		httpClient = null;
		log.log(Level.INFO, "urlString:" + url);

		int status = 0;
		int sleepMillisecond = 0;
		String jsonString = "";
		timeOutInSeconds = timeOutInSeconds < 3 ? 3 : timeOutInSeconds;
		while (status == 408 || status == 503 || status == 504 || status == 429 || status == 0) {
			try {
				httpClient = HttpClientBuilder.create().setDefaultRequestConfig(conf).build();
				HttpGet httpget = new HttpGet(url);
				httpget.addHeader("Accept-Charset", "utf-8");
				if (StringUtils.isNotBlank(token)) {
					log.log(Level.INFO, "httpget.addHeader(\"Authorization\", \"Bearer " + token + ");");
					httpget.addHeader("Authorization", "Bearer " + "" + token);
				}
				httpget.addHeader("content-type", "application/json");
				try {
					Thread.sleep(sleepMillisecond);
				} catch (Exception e) {
				}
				sleepMillisecond = timeOutInSeconds * 1000;
				res = httpClient.execute(httpget);
				status = res.getStatusLine().getStatusCode();
				log.log(Level.INFO, "[" + sessionId + "] - status code: " + status);
				jsonString = IOUtils.toString(res.getEntity().getContent(), StandardCharsets.UTF_8);
				//log.log(Level.INFO, "[" + sessionId + "] \n"+jsonString);
			} catch (Exception e) {
				log.log(Level.WARNING, "[" + sessionId + "] - generic error", e);
				status = -1;
			} finally {
				try {
					res.close();
				} catch (Exception e) {
				}

			}
		}
		try {
			httpClient.close();
		} catch (Exception e) {
		}
		jres.put("http_status_code", status);
		jres.put("jsonString", jsonString);
		log.log(Level.INFO, "[" + sessionId + "] - engage url: " + url + " end status : " + status + " jsonString: " + jsonString);

		return jres;
	}

	private static JSONObject performRequestPut(String sessionId, String url, String token, int timeOutInSeconds, Object he, String contenttype) throws Exception {
		JSONObject jRes = new JSONObject();
		jRes.put("http_status_code", -1);
		timeOutInSeconds = Math.max(timeOutInSeconds, 5);
		if (StringUtils.isBlank(sessionId)) {
			sessionId = "performRequestPatch";
		}
		log.log(Level.INFO, "[" + sessionId + "] - setConnectTimeout(" + (timeOutInSeconds * 1000) + ") setConnectionRequestTimeout(" + (timeOutInSeconds * 1000) + ") setSocketTimeout(" + (timeOutInSeconds * 1000) + ") ");
		RequestConfig config = RequestConfig.custom().setConnectTimeout(timeOutInSeconds * 1000).setConnectionRequestTimeout(timeOutInSeconds * 1000).setSocketTimeout(timeOutInSeconds * 1000).build();
		CloseableHttpResponse response = null;
		HttpPut httpPut = null;
		CloseableHttpClient httpClient = HttpClientBuilder.create().setDefaultRequestConfig(config).build();
		log.log(Level.INFO, "[" + sessionId + "] - url: " + url);
		httpPut = new HttpPut(new URI(url));

		if (he != null) {
			if (he instanceof StringEntity) {
				httpPut.setEntity((StringEntity) he);
			}
			if (he instanceof HttpEntity) {
				httpPut.setEntity((HttpEntity) he);
			}

		} else {
			log.log(Level.INFO, "[" + sessionId + "] - params:  isNull ");
		}

		httpPut.addHeader("content-type", contenttype);
		if (StringUtils.isNotBlank(token)) {
			log.log(Level.INFO, "httpPut.addHeader(\"Authorization\", \"Bearer " + token + ");");
			httpPut.addHeader("Authorization", "Bearer " + "" + token);
		}
		int status = 0;
		int sleepMillisecond = 0;
		String jsonString = "";
		timeOutInSeconds = timeOutInSeconds < 3 ? 3 : timeOutInSeconds;
		while (status == 408 || status == 503 || status == 504 || status == 429 || status == 0) {
			try {
				try {
					Thread.sleep(sleepMillisecond);
				} catch (Exception e) {
				}
				sleepMillisecond = timeOutInSeconds * 1000;
				response = httpClient.execute(httpPut);
				status = response.getStatusLine().getStatusCode();
				log.log(Level.INFO, "[" + sessionId + "] - status code: " + status);
				jsonString = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
			} catch (Exception e) {
				log.log(Level.WARNING, "[" + sessionId + "] - generic error", e);
				status = -1;
			} finally {
				try {
					response.close();
				} catch (Exception e) {
				}

			}
		}
		try {
			httpClient.close();
		} catch (Exception e) {
		}
		jRes.put("http_status_code", status);
		jRes.put("jsonString", jsonString);
		log.log(Level.INFO, "[" + sessionId + "] - engage url: " + url + " end response : " + jRes);
		return jRes;

	}

	private static JSONObject performRequestPatch(String sessionId, String url, String token, int timeOutInSeconds, Object he, String contenttype) throws Exception {
		// List<BasicNameValuePair>
		JSONObject jRes = new JSONObject();
		jRes.put("http_status_code", -1);
		timeOutInSeconds = Math.max(timeOutInSeconds, 5);
		if (StringUtils.isBlank(sessionId)) {
			sessionId = "performRequestPatch";
		}
		log.log(Level.INFO, "[" + sessionId + "] - setConnectTimeout(" + (timeOutInSeconds * 1000) + ") setConnectionRequestTimeout(" + (timeOutInSeconds * 1000) + ") setSocketTimeout(" + (timeOutInSeconds * 1000) + ") ");
		RequestConfig config = RequestConfig.custom().setConnectTimeout(timeOutInSeconds * 1000).setConnectionRequestTimeout(timeOutInSeconds * 1000).setSocketTimeout(timeOutInSeconds * 1000).build();
		CloseableHttpResponse response = null;
		HttpPatch httpPatch = null;
		CloseableHttpClient httpClient = HttpClientBuilder.create().setDefaultRequestConfig(config).build();
		log.log(Level.INFO, "[" + sessionId + "] - url: " + url);
		httpPatch = new HttpPatch(new URI(url));
		// if (he != null) {
		// httpPatch.setEntity(he);
		// } else {
		// log.log(Level.INFO,"[" + sessionId + "] - params: isNull ");
		// }
		// StringEntity entity = new StringEntity("{\"state\": \"connected\" }",
		// ContentType.APPLICATION_JSON);
		if (he != null) {
			if (he instanceof StringEntity) {
				httpPatch.setEntity((StringEntity) he);
			}
			if (he instanceof HttpEntity) {
				httpPatch.setEntity((HttpEntity) he);
			}

		} else {
			log.log(Level.INFO, "[" + sessionId + "] - params:  isNull ");
		}

		httpPatch.addHeader("content-type", contenttype);
		if (StringUtils.isNotBlank(token)) {
			log.log(Level.INFO, "httpPatch.addHeader(\"Authorization\", \"Bearer " + token + ");");
			httpPatch.addHeader("Authorization", "Bearer " + "" + token);
		}
		int status = 0;
		int sleepMillisecond = 0;
		String jsonString = "";
		timeOutInSeconds = timeOutInSeconds < 3 ? 3 : timeOutInSeconds;
		while (status == 408 || status == 503 || status == 504 || status == 429 || status == 0) {
			try {
				try {
					Thread.sleep(sleepMillisecond);
				} catch (Exception e) {
				}
				sleepMillisecond = timeOutInSeconds * 1000;
				response = httpClient.execute(httpPatch);
				status = response.getStatusLine().getStatusCode();
				log.log(Level.INFO, "[" + sessionId + "] - status code: " + status);
				jsonString = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
			} catch (Exception e) {
				log.log(Level.WARNING, "[" + sessionId + "] - generic error", e);
				status = -1;
			} finally {
				try {
					response.close();
				} catch (Exception e) {
				}

			}
		}
		try {
			httpClient.close();
		} catch (Exception e) {
		}
		jRes.put("http_status_code", status);
		jRes.put("jsonString", jsonString);
		log.log(Level.INFO, "[" + sessionId + "] - engage url: " + url + " end response : " + jRes);
		return jRes;
	}

	private static JSONObject performRequestDelete(String sessionId, String url, String token, int timeOutInSeconds) throws Exception {
	 CloseableHttpClient httpClient = null;
		CloseableHttpResponse res = null;
		JSONObject jres = new JSONObject();
		RequestConfig conf = RequestConfig.custom().setConnectTimeout(3 * 1000).setConnectionRequestTimeout(3 * 1000).setSocketTimeout(3 * 1000).build(); // fix 23/09/2021

		httpClient = null;
		log.log(Level.INFO, "httpDelete urlString:" + url);

		int status = 0;
		int sleepMillisecond = 0;
		String jsonString = "";
		timeOutInSeconds = timeOutInSeconds < 3 ? 3 : timeOutInSeconds;
		while (status == 408 || status == 503 || status == 504 || status == 429 || status == 0) {
			try {
				httpClient = HttpClientBuilder.create().setDefaultRequestConfig(conf).build();
				HttpDelete httpDelete = new HttpDelete(new URI(url));
				if (StringUtils.isNotBlank(token)) {
					log.log(Level.INFO, "httpDelete.addHeader(\"Authorization\", \"Bearer " + token + ");");
					httpDelete.addHeader("Authorization", "Bearer " + "" + token);
				}
				httpDelete.addHeader("content-type", "application/json; charset=UTF-8");
				try {
					Thread.sleep(sleepMillisecond);
				} catch (Exception e) {
				}
				sleepMillisecond = timeOutInSeconds * 1000;
				res = httpClient.execute(httpDelete);
				status = res.getStatusLine().getStatusCode();
				log.log(Level.INFO, "[" + sessionId + "] - status code: " + status);
				jsonString = IOUtils.toString(res.getEntity().getContent(), StandardCharsets.UTF_8);
			} catch (Exception e) {
				if (status == 204) {
					jsonString = "{}";
				} else {
					log.log(Level.WARNING, "[" + sessionId + "] - generic error", e);
					status = -1;
				}

			} finally {
				try {
					res.close();
				} catch (Exception e) {
				}

			}
		}
		try {
			httpClient.close();
		} catch (Exception e) {
		}
		jres.put("http_status_code", status);
		jres.put("jsonString", jsonString);
		log.log(Level.INFO, "[" + sessionId + "] - engage url: " + url + " end status : " + status + " jsonString: " + jsonString);

		return jres;

	}

	private static JSONObject refinePerformRequestPatch(GenesysUser guser, String urlString, Object he, String contenttype, int timeOutInSeconds, String id) throws Exception {

		log.log(Level.INFO, "[" + id + "] - urlString: " + urlString);
		JSONObject token = null;
		String stoken=null;
		if (guser!=null) {
			token = guser.getToken(false);
			stoken =token.getString("access_token");
		}


		JSONObject jRes = performRequestPatch(id, urlString, stoken, 0, he, contenttype);
		if (jRes.getInt("http_status_code") == 403 || jRes.getInt("http_status_code") == 401) {
			log.log(Level.WARNING, "http_status_code:" + jRes.getInt("http_status_code") + " try to generate token");
		
			if (guser!=null) {
				token = guser.getToken(false);
				stoken =token.getString("access_token");
			}

			jRes = performRequestPatch(id, urlString, stoken, 0, he, contenttype);
		}
		if (jRes.getInt("http_status_code") != 200 && jRes.getInt("http_status_code") != 202)
			throw new GenesysCloudException(jRes);
		// 200 - successful operation
		// 400 - The request could not be understood by the server due to malformed
		// syntax.
		// 401 - No authentication bearer token specified in authorization header.
		// 403 - You are not authorized to perform the requested action.
		// 404 - The requested resource was not found.
		// 408 - The client did not produce a request within the server timeout limit.
		// This can be caused by a slow network connection and/or large payloads.
		// 413 - The request is over the size limit. Content-Length: %s, Maximum bytes:
		// %s
		// 415 - Unsupported Media Type - Unsupported or incorrect media type, such as
		// an incorrect Content-Type value in the header.
		// 429 - Rate limit exceeded the maximum. Retry the request in [%s] seconds
		// 500 - The server encountered an unexpected condition which prevented it from
		// fulfilling the request.
		// 503 - Service Unavailable - The server is currently unavailable (because it
		// is overloaded or down for maintenance).
		// 504 - The request timed out.
		return jRes;
	}

	private static Object refinePerformRequestDelete(GenesysUser guser, String urlString, int timeOutInSeconds, String id) throws Exception {
		log.log(Level.INFO, "[" + id + "] - urlString: " + urlString);
		JSONObject token = null;
		String stoken=null;
		if (guser!=null) {
			token = guser.getToken(false);
			stoken =token.getString("access_token");
		}

		JSONObject jRes = performRequestDelete(id, urlString, stoken, 0);
		if (jRes.getInt("http_status_code") == 403 || jRes.getInt("http_status_code") == 401) {
			log.log(Level.WARNING, "[" + id + "] - http_status_code:" + jRes.getInt("http_status_code") + " try to generate token");
			if (guser!=null) {
				token = guser.getToken(false);
				stoken =token.getString("access_token");
			}

			jRes = performRequestGet(id + "," + id, urlString,stoken, 0);
		}
		if (jRes.getInt("http_status_code") != 200 && jRes.getInt("http_status_code") != 202 && jRes.getInt("http_status_code") != 204)
			throw new GenesysCloudException(jRes);

		if (jRes.getInt("http_status_code") == 202) {
			throw new GenesysCloud202Exception(jRes);
		}
		if (jRes.getInt("http_status_code") == 204) {
			throw new GenesysCloud204Exception(jRes);
		}

		try {
			return new JSONObject(jRes.getString("jsonString"));
		} catch (Exception e) {
			try {
				return new JSONArray(jRes.getString("jsonString"));
			} catch (Exception ex) {
				throw new GenesysCloud200(jRes);
			}
		}
	}

	private static Object refinePerformRequestGet(GenesysUser guser, String urlString, int timeOutInSeconds, String id) throws Exception {
		log.log(Level.INFO, "[" + id + "] - urlString: " + urlString);
		JSONObject token = null;
		String stoken=null;
		if (guser!=null) {
			token = guser.getToken(false);
			stoken =token.getString("access_token");
		}


		JSONObject jRes = performRequestGet(id, urlString, stoken, 0);
		if (jRes.getInt("http_status_code") == 403 || jRes.getInt("http_status_code") == 401) {
			log.log(Level.WARNING, "[" + id + "] - http_status_code:" + jRes.getInt("http_status_code") + " try to generate token");
			
			if (guser!=null) {
				token = guser.getToken(false);
				stoken =token.getString("access_token");
			}

			jRes = performRequestGet(id + "," + id, urlString, stoken, 0);
		}
		if (jRes.getInt("http_status_code") != 200 && jRes.getInt("http_status_code") != 202)
			throw new GenesysCloudException(jRes);
		if (jRes.getInt("http_status_code") == 202) {
			throw new GenesysCloud202Exception(jRes);
		}
		// 200 - successful operation
		// 400 - The request could not be understood by the server due to malformed
		// syntax.
		// 401 - No authentication bearer token specified in authorization header.
		// 403 - You are not authorized to perform the requested action.
		// 404 - The requested resource was not found.
		// 408 - The client did not produce a request within the server timeout limit.
		// This can be caused by a slow network connection and/or large payloads.
		// 413 - The request is over the size limit. Content-Length: %s, Maximum bytes:
		// %s
		// 415 - Unsupported Media Type - Unsupported or incorrect media type, such as
		// an incorrect Content-Type value in the header.
		// 429 - Rate limit exceeded the maximum. Retry the request in [%s] seconds
		// 500 - The server encountered an unexpected condition which prevented it from
		// fulfilling the request.
		// 503 - Service Unavailable - The server is currently unavailable (because it
		// is overloaded or down for maintenance).
		// 504 - The request timed out.
		try {
			return new JSONObject(jRes.getString("jsonString"));
		} catch (Exception e) {
			return new JSONArray(jRes.getString("jsonString"));
		}
	}

	private static JSONObject refinePerformRequestPost(GenesysUser guser, String urlString, Object he, String contenttype, int timeOutInSeconds, String id) throws Exception {

		log.log(Level.INFO, "[" + id + "] - urlString: " + urlString);
		
		JSONObject token = null;
		String stoken=null;
		if (guser!=null) {
			token = guser.getToken(false);
			stoken =token.getString("access_token");
		}
		JSONObject jRes = performRequestPost(id, urlString,stoken , 0, he, contenttype);
		if (jRes.getInt("http_status_code") == 403 || jRes.getInt("http_status_code") == 401) {
			log.log(Level.WARNING, "[" + id + "] - http_status_code:" + jRes.getInt("http_status_code") + " try to generate token");
			if (guser!=null) {
				token = guser.getToken(false);
				stoken =token.getString("access_token");
			}
			jRes = performRequestPost(id, urlString, stoken, 0, he, contenttype);
		}
		if (jRes.getInt("http_status_code") != 200 && jRes.getInt("http_status_code") != 202&& jRes.getInt("http_status_code") != 201)
			throw new GenesysCloudException(jRes);
		// 200 - successful operation
		// 400 - The request could not be understood by the server due to malformed
		// syntax.
		// 401 - No authentication bearer token specified in authorization header.
		// 403 - You are not authorized to perform the requested action.
		// 404 - The requested resource was not found.
		// 408 - The client did not produce a request within the server timeout limit.
		// This can be caused by a slow network connection and/or large payloads.
		// 413 - The request is over the size limit. Content-Length: %s, Maximum bytes:
		// %s
		// 415 - Unsupported Media Type - Unsupported or incorrect media type, such as
		// an incorrect Content-Type value in the header.
		// 429 - Rate limit exceeded the maximum. Retry the request in [%s] seconds
		// 500 - The server encountered an unexpected condition which prevented it from
		// fulfilling the request.
		// 503 - Service Unavailable - The server is currently unavailable (because it
		// is overloaded or down for maintenance).
		// 504 - The request timed out.
		return new JSONObject(jRes.getString("jsonString"));
	}

	private static JSONObject refinePerformRequestPut(GenesysUser guser, String urlString, Object he, String contenttype, int timeOutInSeconds, String id) throws Exception {

		log.log(Level.INFO, "[" + id + "] - urlString: " + urlString);
		JSONObject token = null;
		String stoken=null;
		if (guser!=null) {
			token = guser.getToken(false);
			stoken =token.getString("access_token");
		}

		JSONObject jRes = performRequestPut(id + "," + id, urlString, stoken, 0, he, contenttype);
		if (jRes.getInt("http_status_code") == 403 || jRes.getInt("http_status_code") == 401) {
			log.log(Level.WARNING, "[" + id + "] - http_status_code:" + jRes.getInt("http_status_code") + " try to generate token");
			if (guser!=null) {
				token = guser.getToken(false);
				stoken =token.getString("access_token");
			}
			jRes = performRequestPut(id + "," + id, urlString, stoken, 0, he, contenttype);
		}
		if (jRes.getInt("http_status_code") != 200 && jRes.getInt("http_status_code") != 202)
			throw new GenesysCloudException(jRes);
		// 200 - successful operation
		// 400 - The request could not be understood by the server due to malformed
		// syntax.
		// 401 - No authentication bearer token specified in authorization header.
		// 403 - You are not authorized to perform the requested action.
		// 404 - The requested resource was not found.
		// 408 - The client did not produce a request within the server timeout limit.
		// This can be caused by a slow network connection and/or large payloads.
		// 413 - The request is over the size limit. Content-Length: %s, Maximum bytes:
		// %s
		// 415 - Unsupported Media Type - Unsupported or incorrect media type, such as
		// an incorrect Content-Type value in the header.
		// 429 - Rate limit exceeded the maximum. Retry the request in [%s] seconds
		// 500 - The server encountered an unexpected condition which prevented it from
		// fulfilling the request.
		// 503 - Service Unavailable - The server is currently unavailable (because it
		// is overloaded or down for maintenance).
		// 504 - The request timed out.
		return new JSONObject(jRes.getString("jsonString"));
	}

	public static JSONObject createChannel(GenesysUser guser) {
		try {
			String urlString = prefixApi + guser.urlRegion + "/api/v2/notifications/channels";
			log.log(Level.INFO, "urlString:" + urlString);
			List<BasicNameValuePair> params = new ArrayList<>();
			params.add(new BasicNameValuePair("grant_type", "client_credentials"));
			HttpEntity he = new UrlEncodedFormEntity(params, "UTF-8");

			return refinePerformRequestPost(guser, urlString, he, "application/x-www-form-urlencoded", 0, "createChannel");
		} catch (Exception e) {
			log.log(Level.INFO, "createChannel ", e);
		} finally {
		}
		return null;
	}

	public static JSONObject subcription(GenesysUser guser, String channel, String statistic) {
		try {

			String urlString = prefixApi + guser.urlRegion + "/api/v2/notifications/channels/" + channel + "/subscriptions";
			// RequestConfig conf = RequestConfig.custom().setConnectTimeout(3 *
			// 1000).setConnectionRequestTimeout(3 * 1000).setSocketTimeout(3 *
			// 1000).build(); // fix 23/09/2021
			// httpClient =
			// HttpClientBuilder.create().setDefaultRequestConfig(conf).build();
			JSONArray ja = new JSONArray();
			ja.put(statistic);
			log.log(Level.INFO, "[" + channel + "] urlString:" + urlString);
			log.log(Level.INFO, "[" + channel + "] subcription:" + ja.toString());
			HttpEntity he = new StringEntity(ja.toString(),  "UTF-8");
			// HttpEntity he = new UrlEncodedFormEntity(params,
			// ContentType.APPLICATION_JSON);

			return refinePerformRequestPost(guser, urlString, he, "application/json ", 0, channel);

			// 200 - successful operation
			// 400 - The request could not be understood by the server due to malformed
			// syntax.
			// 401 - No authentication bearer token specified in authorization header.
			// 403 - You are not authorized to perform the requested action.
			// 404 - The requested resource was not found.
			// 408 - The client did not produce a request within the server timeout limit.
			// This can be caused by a slow network connection and/or large payloads.
			// 413 - The request is over the size limit. Content-Length: %s, Maximum bytes:
			// %s
			// 415 - Unsupported Media Type - Unsupported or incorrect media type, such as
			// an incorrect Content-Type value in the header.
			// 429 - Rate limit exceeded the maximum. Retry the request in [%s] seconds
			// 500 - The server encountered an unexpected condition which prevented it from
			// fulfilling the request.
			// 503 - Service Unavailable - The server is currently unavailable (because it
			// is overloaded or down for maintenance).
			// 504 - The request timed out.

		} catch (Exception e) {
			log.log(Level.INFO, "retrieve token", e);
		} finally {
		}
		return null;
	}

	public static JSONObject convesationCallBackStatus(GenesysUser guser, String conversationid) {
		try {
			String urlString = prefixApi + guser.urlRegion + "/api/v2/conversations/callbacks/" + conversationid;
			log.log(Level.INFO, "[" + conversationid + "]  urlString:" + urlString + " ");

			return (JSONObject) refinePerformRequestGet(guser, urlString, 0, conversationid);
		} catch (Exception e) {
			log.log(Level.INFO, "[" + conversationid + "] - convesationCallBackStatus ", e);
		} finally {
		}
		return null;
	}

	public static void createCall(String phoneNumber, String queue) {
		// TODO Auto-generated method stub

	}

	public static JSONObject getMessagesList(GenesysUser guser, String qeueId, int pageNumber, int pageSize) {
		try {
			String urlString = prefixApi + guser.urlRegion + "/api/v2/voicemail/queues/" + qeueId + "/messages?pageSize=" + pageSize + "&pageNumber=" + pageNumber;
			log.log(Level.INFO, "[" + qeueId + "] urlString:" + urlString + " ");
			return (JSONObject) refinePerformRequestGet(guser, urlString, 0, qeueId);
		} catch (Exception e) {
			log.log(Level.INFO, "[" + qeueId + "] - getMessagesList ", e);
		} finally {
		}
		return null;
	}

	public static boolean invalideteToken(GenesysUser guser, String userid) {
		try {// log out
				// https://api.mypurecloud.de/api/v2/tokens/d5ce2065-fb67-442b-a9b6-37be8b31367f
			String urlString = prefixApi + guser.urlRegion + "/api/v2/tokens/" + userid;
			log.log(Level.INFO, "[" + guser.sessionId + "," + userid + "] urlString:" + urlString + " ");
			refinePerformRequestDelete(guser, urlString, 0, userid);
		} catch (GenesysCloud200 e) {
			log.log(Level.INFO, "[" + guser.sessionId + "," + userid + "] get 200 from cloud bat body is empty:" + e.jRes.toString() + " ");
			return true;
		} catch (Exception e) {
			log.log(Level.INFO, "[" + guser.sessionId + "," + userid + "] - invalideteToken ", e);
		}
		return false;
	}

	public static JSONObject getParticipantsList(GenesysUser guser, String conversationId) {
		try {
			String urlString = prefixApi + guser.urlRegion + "/api/v2/conversations/calls/" + conversationId;
			log.log(Level.INFO, "[" + guser.sessionId + "," + conversationId + "] urlString:" + urlString + " ");
			return (JSONObject) refinePerformRequestGet(guser, urlString, 0, conversationId);
		} catch (Exception e) {
			log.log(Level.INFO, "[" + guser.sessionId + "," + conversationId + "] - getParticipantsList ", e);
		} finally {
		}
		return null;

	}

	public static JSONObject getConnectionList(GenesysUser guser, String queueId, Instant instant, int pageNumber, int pageSize) {
		try {
			DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX").withZone(ZoneId.of("UTC"));
			String sStart = DATE_TIME_FORMATTER.format(instant);
			String sEnd = DATE_TIME_FORMATTER.format(Instant.now());
			String urlString = prefixApi + guser.urlRegion + "/api/v2/analytics/conversations/details/query";
			JSONObject jo = new JSONObject();

			jo.put("interval", sStart + "/" + sEnd).put("order", "asc").put("orderBy", "conversationStart");
			JSONObject jpaging = new JSONObject();
			jpaging.put("pageSize", pageSize);
			jpaging.put("pageNumber", pageNumber);
			jo.put("paging", jpaging);
			JSONArray jSegmentFilters = new JSONArray();
			jo.put("segmentFilters", jSegmentFilters);
			JSONObject jos = new JSONObject();
			jSegmentFilters.put(jos);
			jos.put("type", "or");
			JSONArray jPredicates = new JSONArray();
			jos.put("predicates", jPredicates);
			JSONObject jop = new JSONObject();
			jPredicates.put(jop);
			jop.put("type", "dimension");
			jop.put("dimension", "queueId");
			jop.put("operator", "matches");
			jop.put("value", queueId);

			log.log(Level.INFO, "[" + guser.sessionId + "," + queueId + "] - getConnectionList request: " + jo.toString());
			HttpEntity he = new StringEntity(jo.toString(),  "UTF-8");
			// HttpEntity he = new UrlEncodedFormEntity(params,
			// ContentType.APPLICATION_JSON);

			return refinePerformRequestPost(guser, urlString, he, "application/json ", 0, queueId);

		} catch (Exception e) {
			log.log(Level.INFO, "[" + guser.sessionId + "] - getConnectionList ", e);
		} finally {
		}
		return null;
	}

	
	public static JSONObject getConversationList(String sessionid, GenesysUser guser, String sStart, String sEnd, String ani, String dnis,int pageNumber, int pageSize, String order, StringBuffer dettagli) {
		try {
			log.info("[" + sessionid +"] getConversationList("+ sessionid +", " + guser + ", " + sStart + ", " + sEnd + ", " + ani + ", " + dnis + ", " + pageNumber + ", " + pageSize + ", " + order + ", " + dettagli + ")" );
			DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX").withZone(ZoneId.of("UTC"));
			
			if(!StringUtils.isNotBlank(sEnd) && !StringUtils.isNotBlank(sStart)) {
				sEnd = DATE_TIME_FORMATTER.format(Instant.now());
				sStart = DATE_TIME_FORMATTER.format(Instant.now().minus(7, ChronoUnit.DAYS))  ;
			}else if(!StringUtils.isNotBlank(sStart) && StringUtils.isNotBlank(sEnd)) {
				sStart = DATE_TIME_FORMATTER.format(Instant.parse(sEnd).minus(7, ChronoUnit.DAYS))  ;
			}else if(StringUtils.isNotBlank(sStart) && !StringUtils.isNotBlank(sEnd)){
				sEnd = DATE_TIME_FORMATTER.format(Instant.parse(sStart).plus(7, ChronoUnit.DAYS))  ;
			}
			if(StringUtils.isNotBlank(dettagli))
				dettagli.append(", intervallo = [" + sStart + " / " + sEnd + "]");
			else 
				dettagli.append("intervallo = [" + sStart + " / " + sEnd + "]");
			
			String urlString = prefixApi + guser.urlRegion + "/api/v2/analytics/conversations/details/query";
			JSONObject jo = new JSONObject();

			jo.put("interval", sStart + "/" + sEnd).put("order", order).put("orderBy", "conversationStart");
			JSONObject jpaging = new JSONObject();
			jpaging.put("pageSize", pageSize);
			jpaging.put("pageNumber", pageNumber);
			jo.put("paging", jpaging);
			
			JSONArray segmentFilters = new JSONArray();
			
			if(StringUtils.isNotBlank(ani)) {
				JSONObject seg = new JSONObject();
				JSONArray predicates = new JSONArray();
				JSONObject pred_body = new JSONObject();
				pred_body.put("type", "dimension");
				pred_body.put("dimension", "ani");
				pred_body.put("operator", "matches");
				pred_body.put("value", ani);
				predicates.put(pred_body);
				seg.put("type", "and");
				seg.put("predicates", predicates);
				segmentFilters.put(seg);
				if(StringUtils.isNotBlank(dettagli))
					dettagli.append(", ani = "+ani);
				else 
					dettagli.append("ani = "+ani) ;
			}
			
			if(StringUtils.isNotBlank(dnis)) {
				JSONObject seg = new JSONObject();
				JSONArray predicates = new JSONArray();
				JSONObject pred_body = new JSONObject();
				pred_body.put("type", "dimension");
				pred_body.put("dimension", "dnis");
				pred_body.put("operator", "matches");
				pred_body.put("value", dnis);
				predicates.put(pred_body);
				seg.put("type", "and");
				seg.put("predicates", predicates);
				segmentFilters.put(seg);
				if(StringUtils.isNotBlank(dettagli))
					dettagli.append(", dnis = "+dnis);
				else 
					dettagli.append("dnis = "+dnis) ;
			}
			
			JSONArray predicatesVoiceReg = new JSONArray();
			JSONObject segVR = new JSONObject();
			segVR.put("type", "and");
			
			JSONObject voice = new JSONObject();
			voice.put("type", "dimension");
			voice.put("dimension", "mediaType");
			voice.put("operator", "matches");
			voice.put("value", "voice");
			predicatesVoiceReg.put(voice);
			
			JSONObject reg = new JSONObject();
			reg.put("type", "dimension");
			reg.put("dimension", "recording");
			reg.put("operator", "exists");
			predicatesVoiceReg.put(reg);
			
			segVR.put("predicates", predicatesVoiceReg);
			segmentFilters.put(segVR);
			
			JSONObject agent = new JSONObject();
			agent.put("type", "dimension");
			agent.put("dimension", "purpose");
			agent.put("operator", "matches");
			agent.put("value", "agent");
			JSONArray predicatesAgent = new JSONArray();
			predicatesAgent.put(agent);
			JSONObject segAgent = new JSONObject();
			segAgent.put("type", "and");
			segAgent.put("predicates", predicatesAgent);
			segmentFilters.put(segAgent);
				
			jo.put("segmentFilters", segmentFilters);
			log.info("[" + sessionid +"] jo = " + jo.toString(4));
			
			log.log(Level.INFO, "[" + guser.sessionId + ","  + "] - getConversationList request: " + jo.toString());
			HttpEntity he = new StringEntity(jo.toString(),  "UTF-8");
			// HttpEntity he = new UrlEncodedFormEntity(params,
			// ContentType.APPLICATION_JSON);

			return refinePerformRequestPost(guser, urlString, he, "application/json ", 0, null);

		} catch (Exception e) {
			log.log(Level.INFO, "[" + guser.sessionId + "] - getConversationList ", e);
		} finally {
		}
		return null;
	}
	
	
	public static JSONObject getConversationEmailList(GenesysUser guser, Instant date_from, Instant date_to, String queueId, String con_id, String userId, String addressFrom, String addressTo, String subject, int pageNumber, int pageSize) {
		try {
			DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX").withZone(ZoneId.of("UTC"));
			String sStart = DATE_TIME_FORMATTER.format(date_from);
			String sEnd = DATE_TIME_FORMATTER.format(date_to);
			String urlString = prefixApi + guser.urlRegion + "/api/v2/analytics/conversations/details/query";
			JSONObject jo = new JSONObject();

			jo.put("interval", sStart + "/" + sEnd).put("order", "asc").put("orderBy", "conversationStart");
			JSONObject jpaging = new JSONObject();
			jpaging.put("pageSize", pageSize);
			jpaging.put("pageNumber", pageNumber);
			jo.put("paging", jpaging);
			if (StringUtils.isNotBlank(con_id)) {
				JSONArray jconversationFiltersFilters = new JSONArray();
				jo.put("conversationFilters", jconversationFiltersFilters);

				JSONArray jPredicates = getPredicates(jconversationFiltersFilters);
				JSONObject jop = getDimensionPredicates("conversationId", "matches", con_id);
				jPredicates.put(jop);
			}

			JSONArray jSegmentFilters = new JSONArray();
			jo.put("segmentFilters", jSegmentFilters);
			JSONArray jPredicates = getPredicates(jSegmentFilters);
			JSONObject jop = getDimensionPredicates("mediaType", "matches", "email");
			jPredicates.put(jop);

			if (StringUtils.isNotBlank(queueId)) {
				jPredicates = getPredicates(jSegmentFilters);
				jop = getDimensionPredicates("queueId", "matches", queueId);
				jPredicates.put(jop);
			}
			if (StringUtils.isNotBlank(addressFrom)) {
				jPredicates = getPredicates(jSegmentFilters);
				jop = getDimensionPredicates("addressFrom", "matches", addressFrom);
				jPredicates.put(jop);
			}
			if (StringUtils.isNotBlank(addressTo)) {
				jPredicates = getPredicates(jSegmentFilters);
				jop = getDimensionPredicates("addressTo", "matches", addressTo);
				jPredicates.put(jop);
			}
			if (StringUtils.isNotBlank(userId)) {
				jPredicates = getPredicates(jSegmentFilters);
				jop = getDimensionPredicates("userId", "matches", userId);
				jPredicates.put(jop);
			}
			if (StringUtils.isNotBlank(subject)) {
				jPredicates = getPredicates(jSegmentFilters);
				jop = getDimensionPredicates("subject", "matches", subject);
				jPredicates.put(jop);
			}
			log.log(Level.INFO, "[" + guser.sessionId + "," + queueId + "] - getConnectionList request: " + jo.toString());
			HttpEntity he = new StringEntity(jo.toString(),  "UTF-8");
			// HttpEntity he = new UrlEncodedFormEntity(params,
			// ContentType.APPLICATION_JSON);

			return refinePerformRequestPost(guser, urlString, he, "application/json ", 0, queueId);

		} catch (Exception e) {
			log.log(Level.INFO, "[" + guser.sessionId + "] - getConnectionList ", e);
		} finally {
		}
		return null;
	}

	public static JSONObject getConversationEmailListNotEnd(GenesysUser guser, Instant date_from, Instant date_to, String queueId, String con_id, String userId, String addressFrom, String addressTo, String subject, int pageNumber, int pageSize) {
		try {
			DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX").withZone(ZoneId.of("UTC"));
			String sStart = DATE_TIME_FORMATTER.format(date_from);
			String sEnd = DATE_TIME_FORMATTER.format(date_to);
			String urlString = prefixApi + guser.urlRegion + "/api/v2/analytics/conversations/details/query";
			JSONObject jo = new JSONObject();

			jo.put("interval", sStart + "/" + sEnd).put("order", "asc").put("orderBy", "conversationStart");
			JSONObject jpaging = new JSONObject();
			jpaging.put("pageSize", pageSize);
			jpaging.put("pageNumber", pageNumber);
			jo.put("paging", jpaging);

			JSONArray jconversationFiltersFilters = new JSONArray();
			jo.put("conversationFilters", jconversationFiltersFilters);
			JSONArray jPredicates = getPredicates(jconversationFiltersFilters);
			JSONObject jop = getDimensionPredicates("conversationEnd", "notExists", null);
			jPredicates.put(jop);
			if (StringUtils.isNotBlank(con_id)) {

				jPredicates = getPredicates(jconversationFiltersFilters);
				jop = getDimensionPredicates("conversationId", "matches", con_id);
				jPredicates.put(jop);
			}

			JSONArray jSegmentFilters = new JSONArray();
			jo.put("segmentFilters", jSegmentFilters);
			jPredicates = getPredicates(jSegmentFilters);
			jop = getDimensionPredicates("mediaType", "matches", "email");
			jPredicates.put(jop);

			if (StringUtils.isNotBlank(queueId)) {
				jPredicates = getPredicates(jSegmentFilters);
				jop = getDimensionPredicates("queueId", "matches", queueId);
				jPredicates.put(jop);
			}
			if (StringUtils.isNotBlank(addressFrom)) {
				jPredicates = getPredicates(jSegmentFilters);
				jop = getDimensionPredicates("addressFrom", "matches", addressFrom);
				jPredicates.put(jop);
			}
			if (StringUtils.isNotBlank(addressTo)) {
				jPredicates = getPredicates(jSegmentFilters);
				jop = getDimensionPredicates("addressTo", "matches", addressTo);
				jPredicates.put(jop);
			}
			if (StringUtils.isNotBlank(userId)) {
				jPredicates = getPredicates(jSegmentFilters);
				jop = getDimensionPredicates("userId", "matches", userId);
				jPredicates.put(jop);
			}
			if (StringUtils.isNotBlank(subject)) {
				jPredicates = getPredicates(jSegmentFilters);
				jop = getDimensionPredicates("subject", "matches", subject);
				jPredicates.put(jop);
			}

			log.log(Level.INFO, "[" + guser.sessionId + "," + queueId + "] - getConnectionList request: " + jo.toString());
			HttpEntity he = new StringEntity(jo.toString(),  "UTF-8");
			// HttpEntity he = new UrlEncodedFormEntity(params,
			// ContentType.APPLICATION_JSON);

			return refinePerformRequestPost(guser, urlString, he, "application/json ", 0, queueId);

		} catch (Exception e) {
			log.log(Level.INFO, "[" + guser.sessionId + "] - getConnectionList ", e);
		} finally {
		}
		return null;
	}

	private static JSONObject getDimensionPredicates(String dimension, String operator, String value) throws JSONException {
		JSONObject jop = new JSONObject();

		jop.put("type", "dimension");
		jop.put("dimension", dimension);
		jop.put("operator", operator);
		jop.put("value", value == null ? JSONObject.NULL : value);
		return jop;
	}

	private static JSONArray getPredicates(JSONArray jSegmentFilters) throws JSONException {

		// jo.put("segmentFilters", jSegmentFilters);
		JSONObject jo = null;
		try {
			jo = jSegmentFilters.getJSONObject(0);

		} catch (JSONException e) {
		}
		if (jo == null) {
			jo = new JSONObject();
			jSegmentFilters.put(jo);
			jo.put("type", "and");
			JSONArray jClauses = new JSONArray();
			jo.put("clauses", jClauses);
			JSONObject jc = new JSONObject();
			jClauses.put(jc);
			jc.put("type", "and");
			JSONArray jPredicates = new JSONArray();
			jc.put("predicates", jPredicates);
		}
		return jo.getJSONArray("clauses").getJSONObject(0).getJSONArray("predicates");
	}

	public static JSONObject getConversationEmailListIsEnd(GenesysUser guser, Instant date_from, Instant date_to, String queueId, String con_id, String userId, String addressFrom, String addressTo, String subject, int pageNumber, int pageSize) {
		try {
			DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX").withZone(ZoneId.of("UTC"));
			String sStart = DATE_TIME_FORMATTER.format(date_from);
			String sEnd = DATE_TIME_FORMATTER.format(date_to);
			String urlString = prefixApi + guser.urlRegion + "/api/v2/analytics/conversations/details/query";
			JSONObject jo = new JSONObject();

			jo.put("interval", sStart + "/" + sEnd).put("order", "asc").put("orderBy", "conversationStart");
			JSONObject jpaging = new JSONObject();
			jpaging.put("pageSize", pageSize);
			jpaging.put("pageNumber", pageNumber);
			jo.put("paging", jpaging);

			JSONArray jconversationFiltersFilters = new JSONArray();
			jo.put("conversationFilters", jconversationFiltersFilters);
			JSONArray jPredicates = getPredicates(jconversationFiltersFilters);
			JSONObject jop = getDimensionPredicates("conversationEnd", "exists", null);
			jPredicates.put(jop);
			if (StringUtils.isNotBlank(con_id)) {
				jPredicates = getPredicates(jconversationFiltersFilters);
				jop = getDimensionPredicates("conversationId", "matches", con_id);
				jPredicates.put(jop);
			}
			JSONArray jSegmentFilters = new JSONArray();
			jo.put("segmentFilters", jSegmentFilters);
			jPredicates = getPredicates(jSegmentFilters);
			jop = getDimensionPredicates("mediaType", "matches", "email");
			jPredicates.put(jop);
			if (StringUtils.isNotBlank(queueId)) {
				jPredicates = getPredicates(jSegmentFilters);
				jop = getDimensionPredicates("queueId", "matches", queueId);
				jPredicates.put(jop);
			}
			if (StringUtils.isNotBlank(addressFrom)) {
				jPredicates = getPredicates(jSegmentFilters);
				jop = getDimensionPredicates("addressFrom", "matches", addressFrom);
				jPredicates.put(jop);
			}
			if (StringUtils.isNotBlank(addressTo)) {
				jPredicates = getPredicates(jSegmentFilters);
				jop = getDimensionPredicates("addressTo", "matches", addressTo);
				jPredicates.put(jop);
			}
			if (StringUtils.isNotBlank(userId)) {
				jPredicates = getPredicates(jSegmentFilters);
				jop = getDimensionPredicates("userId", "matches", userId);
				jPredicates.put(jop);
			}
			if (StringUtils.isNotBlank(subject)) {
				jPredicates = getPredicates(jSegmentFilters);
				jop = getDimensionPredicates("subject", "matches", subject);
				jPredicates.put(jop);
			}

			log.log(Level.INFO, "[" + guser.sessionId + "," + queueId + "] - getConnectionList request: " + jo.toString());
			HttpEntity he = new StringEntity(jo.toString(), "UTF-8");
			// HttpEntity he = new UrlEncodedFormEntity(params,
			// ContentType.APPLICATION_JSON);

			return refinePerformRequestPost(guser, urlString, he, "application/json ", 0, queueId);

		} catch (Exception e) {
			log.log(Level.INFO, "[" + guser.sessionId + "] - getConnectionList ", e);
		} finally {
		}
		return null;
	}

	public static JSONObject disconnect(String session, GenesysUser guser, JSONObject jo) {
		try {

			String conversationId = jo.getString("conversationId");

			String urlString = prefixApi + guser.urlRegion + "/api/v2/conversations/" + conversationId + "/disconnect";
			JSONObject jrequest = new JSONObject();
			log.log(Level.INFO, "[" + session + "] - disconnect request: " + jo.toString());
			HttpEntity he = new StringEntity(jrequest.toString(),  "UTF-8");

			return refinePerformRequestPost(guser, urlString, he, "application/json ", 0, session);
		} catch (Exception e) {
			log.log(Level.WARNING, "[" + session + "]", e);
		}
		return null;
	}
	
//	public static JSONObject getDetailsConv(String session, GenesysUser guser, String conversationId) {
//		try {
//
//			
//
//			String urlString = prefixApi + guser.urlRegion + "/api/v2/conversations/" + conversationId + "/details";
//			JSONObject jrequest = new JSONObject();
//			log.log(Level.INFO, "[" + session + "] - disconnect request: " + jo.toString());
//			HttpEntity he = new StringEntity(jrequest.toString(),  "UTF-8");
//
//			return (JSONObject) refinePerformRequestGet(guser, urlString,  0, session);
//		} catch (Exception e) {
//			log.log(Level.WARNING, "[" + session + "]", e);
//		}
//		return null;
//	}
	

	public static JSONObject transfer(String sessionid, GenesysUser guser, JSONObject jo, String queueId) {
		// post
		// /api/v2/conversations/emails/{conversationId}/participants/{participantId}/replace
		try {

			String partecipantId = getPartecipandACDId(sessionid, jo);
			String conversationId = jo.getString("conversationId");

			String urlString = prefixApi + guser.urlRegion + "/api/v2/conversations/emails/" + conversationId + "/participants/" + partecipantId + "/replace";
			JSONObject jrequest = new JSONObject();
			jrequest.put("queue", queueId);

			log.log(Level.INFO, "[" + sessionid + "," + queueId + "] - transfer request: " + jo.toString());
			HttpEntity he = new StringEntity(jrequest.toString(),  "UTF-8");
			// HttpEntity he = new UrlEncodedFormEntity(params,
			// ContentType.APPLICATION_JSON);

			return refinePerformRequestPost(guser, urlString, he, "application/json ", 0, sessionid);
		} catch (Exception e) {
			log.log(Level.WARNING, "[" + sessionid + "," + queueId + "]", e);
		}
		return null;
	}

	private static String getPartecipandACDId(String sessionid, JSONObject jo) throws JSONException {
		JSONArray ja = jo.getJSONArray("participants");
		for (int i = 0; i < ja.length(); i++) {
			JSONObject jparticipant = ja.getJSONObject(i);
			String purpose = jparticipant.getString("purpose");
			if (StringUtils.equalsIgnoreCase(purpose, "acd")) {
				JSONArray jasessions = jparticipant.getJSONArray("sessions");
				for (int j = 0; j < jasessions.length(); j++) {
					JSONObject jsession = jasessions.getJSONObject(j);
					log.log(Level.INFO, "[" + sessionid + "] + session: " + jsession.toString());
				}
				// fix get session only connect
				return jparticipant.getString("participantId");
			}

		}
		return null;
	}
	

	public static JSONObject createEmail(GenesysUser guser, String subj, String qId, JSONObject jo ,JSONObject attributes) {
		try {
			
			String urlString = prefixApi + guser.urlRegion + "/api/v2/conversations/emails";
			log.log(Level.INFO, "[" + guser.sessionId   + "] urlString:" + urlString + " ");
			
			//JSONObject jo = new JSONObject();
							
			jo.put("subject", "ExternalId " + subj);
			//jo.put("subject", subj);
			jo.put("queueId", qId);
			jo.put("provider", "Comapp");
			jo.put("attributes", attributes);
			StringEntity he = new StringEntity(jo.toString());
			log.info("jo : " + jo.toString(4));
			return refinePerformRequestPost(guser, urlString, he, "application/json; charset=UTF-8", 0, "POST REQUEST");
			
		}catch (Exception e) {
			log.log(Level.INFO, "[" + guser.sessionId + "," +  "] - createEmail ", e);
		}
		return null;
		
	}
	

	public static String getAudioUrl(GenesysUser guser, JSONObject jo, AudioType format, File file, String urlDownload, String oriUrlDownload) throws Exception {
		try {
			String selfUri = jo.getString("selfUri");
			String id = jo.getString("id");
			URIBuilder builder = null;
			try {
				builder = new URIBuilder(prefixApi + guser.urlRegion + selfUri);
				// builder.setParameter("maxWaitMs", "20000");
				builder.setParameter("formatId", format.name());
				builder.setParameter("download", "true");
				builder.setParameter("mediaFormats", format.name());
				builder.setParameter("fileName", id);
			} catch (Exception e) {
				log.log(Level.SEVERE, "[" + guser.sessionId + "," + id + "]", e);
				return null;
			}
			String urlString = builder.toString();
			log.log(Level.INFO, "[" + guser.sessionId + "," + id + "] urlString:" + urlString + " ");

			JSONObject jsonString = null;
			boolean retry = true;
			int retryCount = 0;
			int maxRetries = 15;
			while (retry) {
				String convIdForCancel = jo.optString("conversationId", id);
				if (cancelMap.getOrDefault(convIdForCancel, false)) {
					log.log(Level.INFO, "[" + guser.sessionId + "," + id + "] - Audio preparation cancelled by user. Breaking loop.");
					cancelMap.remove(convIdForCancel);
					return null;
				}
				if (retryCount >= maxRetries) {
					log.log(Level.WARNING, "[" + guser.sessionId + "," + id + "] - Max retry limit (" + maxRetries + ") reached. Breaking loop.");
					return null;
				}
				retryCount++;
				try {
					jsonString = (JSONObject) refinePerformRequestGet(guser, urlString, 0, id);
					retry = false;
				} catch (GenesysCloud202Exception e) {
					if (e.jRes != null && e.jRes.optString("jsonString", "").contains("\"fileState\":\"DELETED\"")) {
						log.log(Level.WARNING, "[" + guser.sessionId + "," + id + "] - fileState is DELETED. Breaking retry loop.");
						return null;
					}
					log.log(Level.INFO, "[" + guser.sessionId + "," + id + "] - response is 202 sleep for retry (" + retryCount + "/" + maxRetries + ")");
					retry = true;
					Thread.sleep(2000);

				}
			}
			// JSONObject jsonString = new JSONObject(jo.getString("jsonString"));
			String mediaUri = jsonString.getJSONObject("mediaUris").getJSONObject("S").getString("mediaUri");
			String mediaUri2 = mediaUri;
			if (StringUtils.isNotBlank(oriUrlDownload) && StringUtils.isNotBlank(urlDownload))
				mediaUri2 = StringUtils.replace(mediaUri, oriUrlDownload, urlDownload);
			log.log(Level.INFO, "[" + guser.sessionId + "," + id + "] - download: " + mediaUri2 + "\n    ori: " + mediaUri);
//			downloadFile(file, mediaUri2);
			log.log(Level.INFO, "[" + guser.sessionId + "," + id + "] - copy completed");
			return mediaUri2;
		} catch (Exception e) {
			log.log(Level.SEVERE, "", e);
			return null;
		}
	}
	
	
	
	
	

	
	
	
	
	
	
	
	
	
	
	
	
		
	
}
