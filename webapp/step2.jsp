<%@page import="comapp.cloud.CSVReport"%>
<%@page import="org.apache.commons.csv.CSVRecord"%>
<%@page import="org.apache.commons.csv.CSVFormat"%>
<%@page import="java.time.LocalDate"%>
<%@page import="java.io.FileWriter"%>
<%@page import="java.io.File"%>
<%@page import="java.util.logging.Level"%>
<%@page import="org.json.JSONArray"%>
<%@page import="comapp.cloud.Genesys"%>
<%@page import="org.apache.commons.lang3.StringUtils"%>
<%@page import="java.util.Properties"%>
<%@page import="comapp.cloud.GenesysUser"%>
<%@page import="org.json.JSONObject"%>
<%@page import="java.util.logging.Logger"%>
<%@page import="comapp.ConfigServlet"%> 
<%@ page language="java" contentType="text/html; charset=UTF-8" 
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>SP_Lite</title>
<link rel="stylesheet" href="css/uikit.min.css" />
<script src="js/uikit.min.js"></script>
<script src="js/uikit-icons.min.js"></script>
</head>

<style>
header {
    padding: 10px;
    font-family: Alegreya, Sans Medium; 
    font-weight: bold;
  display: flex; 
  justify-content: space-between; 
/*   margin-top: 5px; */
}

header .h4{
	text-align:left;
    margin-left: 40px;
	font-family: 'Alegreya', Sans Medium;
	font-weight: bolder;
}
header .logout{
	text-align:right;
    margin-right: 30px;
}

body {
    margin: 0;
	font-family: 'Futura PT'', Medium;
    display: flex;
    height: 100vh;
    overflow: hidden;
}

.main_container {
    flex: 1;
    display: flex;
    flex-direction: column;
    height: 100vh;
    overflow-y: auto; 
}

iframe {
    border: none;
    width: 100%;
    height: 100%;
}

.search_parameters {
    display: flex;
    flex-direction: column; 
    align-items: center; 
    gap: 20px; 
}

.parameters {
    display: flex; 
    justify-content: center; 
    align-items: center; 
    gap: 10px; 
}


.parameters label {
    margin: 0;
}


.uk-input {
    margin: 0; 
    height: 35px; 
}


.searchButton {
    display: flex; 
    justify-content: center; 
    gap: 20px; 
    margin: 15px 5px; 
    
    text-align: center; 
}
.uk-button{
	border-radius: 15px; 
    height: 40px; 
}

</style>

<body>
<% 
Logger log = Logger.getLogger(ConfigServlet.web_app);
log.info("["+ session.getId() +"] ##################### [" + session.getId() + "] step2 start.. #####################");
// log.info("[" + session.getId() + "] start agent login "+(StringUtils.isBlank(code)?" step 1":" step 2")+" *********************");

// Manage the authorization access
String authToken = null;
JSONObject me = (JSONObject) session.getAttribute("gui_user");
GenesysUser guser  = null;
try{
	log.info("[" + session.getId() + "]*******************security control *********************");
	Properties cs = ConfigServlet.getProperties();
	boolean enabled_security =  Boolean.parseBoolean(cs.getProperty("enabled_security", "false"));
	String code = request.getParameter("code");
	authToken = request.getParameter("authToken");
	if (StringUtils.isBlank(authToken)){
		authToken =(String) session.getAttribute("authToken");
	}
	log.info("[" + session.getId() + "] authToken: "+authToken+" code: "+code);
	
	if (enabled_security){
		if (me==null){	 
			try {					
				String gui_clientId = cs.getProperty("gui_clientId", "");
				String gui_clientSecret = cs.getProperty("gui_clientSecret", "");
				String urlRegion =  cs.getProperty("urlRegion","");
				String redirect_uri = cs.getProperty("redirect_uri","");
				String urlAuthorizeString = cs.getProperty("urlAuthorizeString","https://login.mypurecloud.ie/oauth/authorize");		
				
				
				
				log.info("[" + session.getId() + "] clientId: "+gui_clientId+"  clientSecret: "+gui_clientSecret+" urlRegion: "+urlRegion+" urlAuthorizeString: "+urlAuthorizeString);
				 guser  = new GenesysUser(session.getId(), gui_clientId, gui_clientSecret, urlRegion, redirect_uri, urlAuthorizeString);
				 guser.setCode(code);
				 log.info("[" + session.getId() +"] guser = " + guser.toString());
				 session.setAttribute("guser", guser);   // Set the session's guser
				 try{
					if (!StringUtils.isBlank(authToken)){
						guser.jToken= new JSONObject(authToken);	
					}else{
						if (!StringUtils.isBlank(code))
								guser.setCode(code);
							else
								response.sendRedirect("index.jsp");
					}
				}catch(Exception ex){
					response.sendRedirect("index.jsp");
				}
				 
				authToken =  StringUtils.normalizeSpace( guser.getToken(false).toString());
				log.info("[" + session.getId() + "]  --> authToken: "+authToken);		
				me= Genesys.getUserMe(guser, "groups");
				JSONArray groups = me.getJSONArray("groups");
				log.info(groups.toString());
				String gui_AllowGroupAdmin = cs.getProperty("gui_AllowGroupAdmin", "*");
				String gui_AllowGroupUser = cs.getProperty("gui_AllowGroupUser", "*");
				boolean allowGroupAdmin =StringUtils.equalsIgnoreCase(gui_AllowGroupAdmin,"*");
				boolean allowGroupUser =StringUtils.equalsIgnoreCase(gui_AllowGroupUser,"*");
				log.info("allowGroupAdmin group: "+allowGroupAdmin);
				for (int i=0; !allowGroupUser && i<groups.length(); i++){
					log.info("Users group:"+groups.get(i));
					allowGroupUser = StringUtils.containsIgnoreCase( ""+groups.get(i), gui_AllowGroupUser);		
				}
				if (!allowGroupAdmin){
					for (int i=0; !allowGroupUser && i<groups.length(); i++){
						log.info("Users group:"+groups.get(i));
						allowGroupUser = StringUtils.containsIgnoreCase( ""+groups.get(i), gui_AllowGroupUser);		
					}
					if (!allowGroupUser){
						session.setAttribute("error", "Access Denied");
						response.sendRedirect("index.jsp");
						return;
					}
					session.setAttribute("gui_user", me);
					
				}else{
					session.setAttribute("gui_admin", me);
					session.setAttribute("gui_user", me);
				}	
			}catch (Exception e){
				log.log(Level.WARNING, "[" + session.getId() + "] Unauthorized access",e);
				response.sendRedirect("index.jsp");
				
				return;
		 	}
		}else{
			log.info("[" + session.getId() + "] me is not null: "+me.toString());
			log.info("[" + session.getId() +"] guser = " + guser.toString());
		}
	}else{	
		if (me==null){
			me = new JSONObject();
			me.put("name","admin");
			me.put("id","admin");
		}
		 

	}
	
}catch (Exception e){
	log.log(Level.WARNING, "[" + session.getId() + "] unable to verify the security issue",e);
}

String access = (String) session.getAttribute("access") ;
log.info("[" + session.getId() + "] Step 2 access vale = " + access);
JSONObject user = (JSONObject) session.getAttribute("gui_user");
if(StringUtils.compareIgnoreCase("first_access", access) == 0){
	session.setAttribute("access", "Reload");
	CSVReport.aggiungiLog(user.getString("name"), "Login", "");
}


%>

<div class="main_container">
	<header>
		<h4>Search&Play <span style="font-size:xx-small;"><%=ConfigServlet.version%></span></h4>
		<div class="logout">
			<span class="user-name" ><%=user.getString("name")%></span>
			<a href="Logout.jsp" class="uk-icon-button uk-margin-small-left" style="text-decoration:none"> <span uk-icon="icon: sign-out"></span></a>
<!-- 			<form id="logout" name="logout" action="Logout.jsp" method="post"> -->
<%-- 				<span class="user-name" ><%=user.getString("name")%></span>  --%>
<!-- 				<span uk-icon="icon: sign-out" onclick="logout()"></span> -->
<!-- 			</form> -->
		</div>
	</header>
	
	<div class="search_parameters">
		<form id="searchCall" name="searchCall" action="SearchCall.jsp" method="POST" target="_iframe">
			<div class="parameters">
				<label style="margin-left:10px">ANI:</label>
		        <input class="uk-input" type="text" id="ani" name="ani">
		        <label  style="margin-left:10px">DNIS:</label>
		        <input class="uk-input" type="text" id="dnis" name="dnis">
		         <label style="margin-left:10px">DAL:</label> 
		        <input class="uk-input" type="datetime-local" id="from" name="from">
		        <label style="margin-left:10px">AL:</label>
		        <input class="uk-input" type="datetime-local" id="to" name="to">
	
			</div>
			 <div class="searchButton">
		        	<button class="uk-button uk-button-default" type="button" onclick="clearAll()" > Cancella </button>
		        	<button class="uk-button uk-button-primary" type="button" onclick="searchForm()">Ricerca</button>
		        </div>
			<input id="username" name="username" type="hidden" value="<%=me%>"> 
			<input id="action" name="action" type="hidden" > 
		</form>
	</div>
	
	<iframe id="_iframe" name="_iframe" src=""></iframe>
	
</div>

<div id="call_search" uk-modal="bg-close: false">
    <div class="uk-modal-dialog uk-modal-body uk-align-center uk-transition-fade" style="width: fit-content; border-radius: 4px;">
        <div uk-spinner></div>
          Ricerca in corso...
          <p>Attendere il caricamento dei risultati.</p>
    </div>
</div>


<script type="text/javascript">
function clearAll(){
	document.getElementById("ani").value = "";
	document.getElementById("dnis").value = "";
	document.getElementById("email").value = "";
    document.getElementById("from").value = "";
	document.getElementById("to").value = "";
}

function searchForm(){
	document.getElementById("action").value = "search";
	 UIkit.modal("#call_search").show();
	    document.getElementById("searchCall").submit();
}
function logout(){
	document.getElementById("logout").submit();
}

document.getElementById("_iframe").onload = function() {
    UIkit.modal("#call_search").hide();
};
</script>
</body>
</html>