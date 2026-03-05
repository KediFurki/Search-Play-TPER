<%@page import="javax.sql.DataSource"%>
<%@page import="javax.naming.InitialContext"%>
<%@page import="javax.naming.Context"%>
<%@page import="java.sql.ResultSet"%>
<%@page import="java.sql.PreparedStatement"%>
<%@page import="java.sql.Connection"%>
<%@page import="comapp.ManageUser"%>
<%@page import="comapp.User"%>
<%@page import="java.util.Calendar"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="org.json.JSONArray"%>
<%@page import="org.apache.logging.log4j.Level"%>
<%@page import="comapp.cloud.Genesys"%>
<%@page import="comapp.ConfigServlet"%>
<%@page import="java.util.Properties"%>
<%@page import="java.util.Enumeration"%>
<%@page import="comapp.cloud.GenesysUser"%>
<%@page import="org.json.JSONObject"%>
<%@page import="org.apache.commons.lang3.StringUtils"%>
<%@page import="comapp.cloud.TrackId"%>
<%@page import="org.apache.logging.log4j.LogManager"%>
<%@page import="org.apache.logging.log4j.Logger"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<title>Search And Play</title>
<link rel="icon" type="image/x-icon" href="img/favicon.ico">
<link rel="stylesheet" href="css/uikit.min.css" />
 <script src="js/uikit-icons.min.js"></script>
<script src="js/uikit.min.js"></script>
<script src="js/uikit-icons.min.js"></script> 
</head>
<style>
.uk-tile {
	padding: 10px;
}

.uk-button-group {
    display: flex;            
    justify-content: center;   
    align-items: center;      
    gap: 10px;                 
    width: 100%;              
}

html, body {
	font: 12px "Roboto", Cairo, Sans-serif;
	line-height: normal;
	line-height: 20px;
	color: #555;
}

.clear-all {
	background-color: white;
	color: red;

}

.uk-button-small {
	padding: 0;
	padding-left: 7px;
	padding-right: 7px;
	cursor: pointer;
	border: 0px;
	text-transform: none;
}


</style>

<body>
<%
Logger log = LogManager.getLogger("comapp.Genesys" + this.getClass()); 
String code = request.getParameter("code");

String sessionid = session.getId();
TrackId trackId = new TrackId(sessionid, code);

Enumeration<String> es = request.getHeaderNames();
log.info("{} header ********** ", trackId);
while(es.hasMoreElements()){
	String key = es.nextElement();
	String value = request.getHeader(key);
	log.info(trackId + " header key:" + key + " value:" + value);
}

Enumeration<String> enume = request.getParameterNames(); 
log.info(trackId + " parameters *************** ");
while(enume.hasMoreElements()){
	String key = enume.nextElement();
	String value = request.getParameter(key);
	log.info("{} parameter key:" + key + " value:" + value, trackId);
}



log.info("{} ----------------------- main --------------------------", trackId);
log.info("{} start agent login " + (StringUtils.isBlank(code) ?" step 1" : " step 2") + " ************************************", trackId);


JSONObject me = null;
String name="-";
String username="-";

GenesysUser guser = (GenesysUser)session.getAttribute("guser") == null ? null : (GenesysUser)session.getAttribute("guser");

log.log(Level.INFO, "{} guser = " + guser, trackId);

try{
	Properties cs = ConfigServlet.getProperties();			

	String gui_clientId = (String) session.getAttribute("gui_clientId");
	String gui_clientSecret = (String) session.getAttribute("gui_clientSecret");
	String urlRegion = (String) session.getAttribute("urlRegion");
	String redirect_uri = cs.getProperty("redirect_uri","");
	boolean enabled_security =  Boolean.parseBoolean(cs.getProperty("enabled_security", "true"));			
	String urlAuthorizeString = cs.getProperty("urlAuthorizeString","https://login.mypurecloud.de/oauth/authorize");	
	String allowedGroup = cs.getProperty("allowedGroup", "SP");
	
	if(enabled_security){
		try{
			me = (JSONObject) session.getAttribute("gui_user");
			if(me == null){
				if(StringUtils.isBlank(code)){
					out.println("<script type=\"text/javascript\">location='login.jsp'</script>");
                    return;
				}
	            guser = new GenesysUser(sessionid, gui_clientId, gui_clientSecret, urlRegion, redirect_uri, urlAuthorizeString);
	            guser.setCode(code);
	           
	            me = Genesys.getUserMe(sessionid ,guser, "groups");
	            session.setAttribute("guser", guser);
	            JSONArray groups = me.getJSONArray("groups"); 
	            username = me.getString("username");
	            String gui_AllowGroupAdmin = cs.getProperty("gui_AllowGroupAdmin", "SP");
	            String gui_AllowGroupUser = cs.getProperty("gui_AllowGroupUser", "SP");
	            boolean allowGroupAdmin = StringUtils.equalsIgnoreCase(gui_AllowGroupAdmin, allowedGroup);
	            boolean allowGroupUser = StringUtils.equalsIgnoreCase(gui_AllowGroupUser, allowedGroup);
	            
	            
	            for (int i = 0; !allowGroupAdmin && i < groups.length(); i++) {
	                allowGroupAdmin = StringUtils.containsIgnoreCase("" + groups.get(i), gui_AllowGroupAdmin);
	            }
	            
	            if (!allowGroupAdmin) {
	            	for (int i = 0; !allowGroupUser && i < groups.length(); i++) {
	                    allowGroupUser = StringUtils.containsIgnoreCase("" + groups.get(i), gui_AllowGroupUser);
	                }
	            	if (!allowGroupUser) {
	                    session.setAttribute("error", "Access Denied");
	                    log.log(Level.WARN, "{} Utente " + username + " non è amesso");
	                    out.println("<p> <h2 style='text-align:center; color:red;'> Utente non è ammissible a questa applicazione </h2> </p>");
	                    return;
	                }
	            	session.setAttribute("gui_user", me);	            	
	            }else{
	            	session.setAttribute("gui_admin", me);
	                session.setAttribute("gui_user", me);
	            }
			}else{
				
			}
		}catch(Exception ex){
			log.log(Level.WARN, "{} response.sendError(401)", trackId, ex);
    		response.sendError(401);
    		return;
		}
	}else{
		log.info("security is not enabled ");
		String clientId = cs.getProperty("clientId", "");
    	String clientSecret = cs.getProperty("clientSecret", "");
    	String region = cs.getProperty("region", "");
    	guser = new GenesysUser(sessionid, clientId, clientSecret, region, null, null);
    	name = "No Security enabled";
    	session.setAttribute("guser", guser);
	}
	
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
	String s_dateFrom = sdf.format(Calendar.getInstance().getTime());
	name = me.getString("name");
	User user = null;
	if(session.getAttribute("user") == null){
		ManageUser manageUser = new ManageUser();
		user = manageUser.setUserConfig(trackId.toString(), guser, username);
		trackId.add(user.username());
		log.info("{} info utente [username: {}, authGroups: "+ user.authGroups().keySet() +", " + user.authGroups().values() + ", canDownload: {}, Morphing: {}]",  trackId, user.username(), user.canDownload(), user.morph_enabled());	
		session.setAttribute("user", user);
		
		//-------------------------------------------------------------------------------
		// Inserire il login dell'utente sul database analyzeraudit
		//-------------------------------------------------------------------------------
		Connection connection = null;
		PreparedStatement psmt = null;
		String audit_insert = cs.getProperty("sql.audit.insert");
		log.info("{} main audit_insert : {}", trackId, audit_insert);
	 	try{
	 		Context ctx = new InitialContext();
	 		log.log(Level.DEBUG, trackId + " get connection: " + ConfigServlet.prefix + "SearchAndPlay");
	 		DataSource ds = (DataSource) ctx.lookup("java:comp/env/" + ConfigServlet.prefix +  "SearchAndPlay");
	 		connection = ds.getConnection();
	 		connection.setAutoCommit(false);
			
	 		psmt = connection.prepareStatement(audit_insert);
	 		psmt.setString(1, user.getUserName());
	 		psmt.setString(2, "LOGIN");
	 		psmt.setString(3, "-");
	 		psmt.setString(4, "SUCCESS");
	 		psmt.setBoolean(5, false);
	 		
	 		 psmt.executeUpdate();
	 		connection.commit();
// 	 		log.info("{} parametri inseriti perfettamente...", trackId);
	 	}catch(Exception e){
	 		connection.rollback();
	 		log.error("{} errore durante l'inserimento dell'audit: ", trackId, e);
	 	}finally{
	 		psmt.close();
	 		connection.close();
	 	}
		
	}else{
		user = (User) session.getAttribute("user");
		log.info("{} info utente [username: {}, authGroups: "+ user.authGroups().keySet() +", " + user.authGroups().values() + ", canDownload: {}, Morphing: {}]",  trackId, user.username(), user.canDownload(), user.morph_enabled());	
	}
	
	%>
		<div class="container">
		<div id="call_search" uk-modal bg-close="false">
			<div class="uk-modal-dialog uk-modal-body" class="uk-align-center uk-transition-fade" style="width: fit-content; border-radius:4px">
				<div uk-spinner> Ricerca in corso...</div>
			</div>
		</div>
		
		<table style="width:100%; padding-left: 10px; padding-right: 10px">
			<tr>
				<th style="min-width: 250px;">
					<div style="width: 250px; text-align: left;" class="uk-title uk-title-muted">
						<a style="text-decoration: none;">
							<span id="icon-arrow" uk-icon="icon: triangle-left" style="border-radius: 10px; background-color: #44444E; color: white;" uk-toggle="target: #my_id; animation: uk-animation-slide-left, uk-animation-slide-left" onclick="toggle(this);">
							</span>
						</a>
					</div>
				</th> 
				<th style="width: 100%; text-align: center;">
					<div style="width: 100%; user-select: none;" class="uk-title uk-title-muted">
						<span><%=ConfigServlet.web_app%></span>
						<span style="font-size: xx-small;"> &nbsp;&nbsp;&nbsp; <%=ConfigServlet.version%></span>
					</div>
				</th>
				<th style="width: 150px">
					<form action="logout.jsp" method="post" id="sign_out">
						<div style="width: 150px; user-select: none;" class="uk-title uk-title-muted">
							<%=name%>
							<a style="text-decoration: none" onclick="document.getElementById('sign_out').submit()"> 
								<span uk-icon="icon: sign-out" style="color: black"> </span>
							</a>
						</div>
					</form>
				</th>
			</tr>
			<tr>
				<td style="vertical-align: top; width:250px;" id="my_id">
					<form id="callList" name="callList" action="CallList.jsp" method="post" target="_iframe" style="height:100%">
						<input type="hidden" value="" id="action" name="action">
						<div class="uk-tile uk-tile-primary" style="user-select:none"> SEARCH </div>
						<div class="filters">
							<br> <label for="conversationid">CONVERSATION ID:</label> <br><input class="uk-input" type="text" id="connid" name="connid" placeholder="Conversation Id">
							<br> <label for="agentname">NOME DELL'AGENTE :</label> <br><input class="uk-input" type="text" id="agentname" name="agentname" placeholder="Nome dell'agente">
							<br> <label for="abi">NUMERO CHIAMANTE:</label> <br><input class="uk-input" type="text" id="ani" name="ani" placeholder="Numero chiamante">
							<br> <label for="dnis">NUMERO CHIAMATO:</label> <br><input class="uk-input" type="text" id="dnis" name="dnis" placeholder="Numero chiamato">
							<br> <label for="from">DATA E ORA INIZIO:</label> <br><input class="uk-input" type="datetime-local" id="from" name="from" >
							<br> <label for="to">DATA E ORA FINE:</label> <br><input class="uk-input" type="datetime-local" id="to" name="to" >
						</div>
						<br>
						<div class="uk-button-group" >
							<button class="uk-button uk-button-primary" id="btn_s" type="button" onclick="search()">Search</button>
							<button id="clear-all-btn" class="clear-all uk-button " type="button" onclick="clear()">Clear</button>
						</div>
					</form>
				</td>
				<td id="my_center" colspan="3">
					<iframe id="_iframe" name="_iframe" src="" style="width: 100%; height: 90vh; user-select: none;"></iframe>
				</td>
			</tr>
			
			
			
			
		</table>
	</div>
	<%
	
}catch(Exception gen){
	
}

%>
<script type="text/javascript">
document.getElementById('clear-all-btn').addEventListener('click', function () {
	const inputs = document.querySelectorAll('.filters .uk-input');
  	inputs.forEach(input => {
    	input.value = ''; 
  	});
});

hide = function () {
	UIkit.modal(document.getElementById("call_search")).hide();
}

toggle = function (_this) {
	var my_center = document.getElementById("my_center");
	if (_this.getAttribute('uk-icon')=='icon: triangle-right') {
		my_center.setAttribute("colspan","2");
		_this.setAttribute('uk-icon','icon: triangle-left');
	} else {
		 my_center.setAttribute("colspan","3");
		_this.setAttribute('uk-icon','icon: triangle-right' );
	}
}

function search(){
	document.getElementById("action").value="search";
	UIkit.modal(document.getElementById("call_search")).show();
	document.getElementById("callList").submit(); 
}

document.getElementById("_iframe").onload = function() {
    UIkit.modal("#call_search").hide();
};

function scadenzaSessione() {
    localStorage.clear(); 
    sessionStorage.clear();

    UIkit.modal.alert('Sessione scaduta per inattività. Verrai reindirizzato alla pagina di login.')
        .then(function() {
            window.location.href = 'logout.jsp?reason=sessionescaduta';
        });
}

// const sessionTimeoutMs = (1 * 60 * 1000) ; //+ 5000; 
// let timeoutTimer;

// function resetTimer() {
//     clearTimeout(timeoutTimer);
//     timeoutTimer = setTimeout(scadenzaSessione, sessionTimeoutMs);
// }

// document.onmousemove = resetTimer;
// document.onkeypress = resetTimer;
// document.onclick = resetTimer;

// resetTimer();


</script>
</body>
</html>