package comapp.cloud;

import java.util.Calendar;
//import java.util.logging.Level;
//import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;

import java.util.logging.Level;
import  java.util.logging.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import comapp.ConfigServlet;

public class GenesysUser  {

	public String clientId = "";
	public String clientSecret = "";
	public String urlRegion = "";
	public String redirect_uri = "https://93.38.116.140/AutomaticPATCB/index.jsp";
	public String urlAuthorizeString = "https://login.mypurecloud.de/oauth/authorize";
	static Logger log =  Logger.getLogger(ConfigServlet.web_app);

	public String code;
	public String agent;
	public String conversationid;
	public String authToken;
	public String sessionId;

	public JSONObject jToken = null;
	public boolean recording;
	private String authorization;
	public Object syncObjectUsers = new Object();



	String routingstatusTableName = "routingstatus";
	String presenceTableName = "presence";
	String conf_userTable = "conf_user";

	

	

	

	public String getCode() {
		return code;
	}

	public void setRecording(boolean recording) {
		this.recording = recording;
	}

	public Object getSyncObjectUsers() {
		return syncObjectUsers;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getAgent() {
		return agent;
	}

	public void setAgent(String agent) {
		this.agent = agent;
	}

	public String getConversationid() {
		return conversationid;
	}

	public void setConversationid(String conversationid) {
		this.conversationid = conversationid;
	}

	public String getAuthToken() {
		return authToken;
	}

	public void setAuthToken(String authToken) {
		this.authToken = authToken;
	}

	public GenesysUser(String session, String clientId, String clientSecret, String urlRegion, String redirect_uri, String urlAuthorizeString) {
		super();
		this.clientId = clientId;
		this.sessionId = session;
		this.clientSecret = clientSecret;
		this.urlRegion = urlRegion;
		this.redirect_uri = redirect_uri;
		this.urlAuthorizeString = urlAuthorizeString;
	}

	public GenesysUser(String session, String authorization, String urlRegion) {
		super();
		this.authorization = authorization;
		this.sessionId = session;
		this.urlRegion = urlRegion;
	}

	private String getTokenFromAuthorization() {
		return authorization.replace("bearer", "").replace("Bearer", "").trim();
	}

	public String getTokenString() {
		if (jToken == null)
			getToken(false);
		try {
			return jToken.getString("access_token");
		} catch (JSONException e) {
			return null;
		}
	}

	public JSONObject getToken(boolean force) {
		try {
			if (StringUtils.isNotBlank(authorization)) {
				log.info("" + new JSONObject().put("access_token", getTokenFromAuthorization()));
				return new JSONObject().put("access_token", getTokenFromAuthorization());
			}

			if (force)
				jToken = Genesys.getToken(urlRegion, clientId, clientSecret, code, redirect_uri);

			if (jToken == null) {
				jToken = Genesys.getToken(urlRegion, clientId, clientSecret, code, redirect_uri);
			}

			Calendar cal = Calendar.getInstance();
			if (cal.getTimeInMillis() > jToken.getLong("expires_at"))
				jToken = Genesys.getToken(urlRegion, clientId, clientSecret, code, redirect_uri);
		} catch (Exception e) {
			log.log(Level.SEVERE, "exception in getToken", e);
			jToken = Genesys.getToken(urlRegion, clientId, clientSecret, code, redirect_uri);
		}
		return jToken;
	}

}
