<%@page import="comapp.cloud.CSVReport"%>
<%@page import="org.apache.commons.lang3.StringUtils"%>
<%@page import="java.util.logging.Level"%>
<%@page import="comapp.cloud.Genesys.AudioType"%>
<%@page import="comapp.cloud.GenesysUser"%>
<%@page import="comapp.cloud.Genesys"%>
<%@page import="org.json.JSONArray"%>
<%@page import="org.json.JSONObject"%>
<%@page import="comapp.ConfigServlet"%>
<%@page import="java.util.logging.Logger"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="ISO-8859-1">
<title>Insert title here</title>
</head>
<style>

body {
    display: flex;
    justify-content: center;
    align-items: center;
    width: 100%;
    height: 100%; 
    overflow: hidden; 
    position: relative; 
}
</style>
<body>

<%
Logger log = Logger.getLogger(ConfigServlet.web_app);
log.info("##################### [" + session.getId() + "] AudioPlayer starting.. #####################");
GenesysUser guser = (GenesysUser)session.getAttribute("guser");
JSONObject user = (JSONObject) session.getAttribute("gui_user");
String conversationId = request.getParameter("conversationId");
log.info("##################### [" + session.getId() + "] conversationId = " + conversationId);
String audioUrl = null;
try{
	JSONArray recorderList = Genesys.getRecorderList(guser, conversationId, Genesys.AudioType.WAV);
	audioUrl = Genesys.getAudioUrl(guser, recorderList.getJSONObject(0), AudioType.WAV, null, null, null);
	log.info("##################### [" + session.getId() + "] audioUrl = " + audioUrl);
// 	if(audioUrl==null) throw new Exception();
	if(StringUtils.isNotBlank(audioUrl)){
		String dettagli = "conversationId: " + conversationId;
		CSVReport.aggiungiLog(user.getString("name"), "Ascolto", dettagli);
		%>
			<audio controls id="audio_control" >
			  	<source id="_player" src="<%=audioUrl%>" type="audio/wav">
			  	I suo browser not supporta il file audio
	   		 </audio>
		<%
	}else{
		out.println("No audio found");
	}

}catch(Exception e){
	log.log(Level.WARNING, "An error occured while trying to retreive the audio url", e);
}
%>

</body>
</html>