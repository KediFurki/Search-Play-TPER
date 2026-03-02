<%@page import="comapp.cloud.CSVReport"%>
<%@page import="java.util.Locale"%>
<%@page import="java.time.Duration"%>
<%@page import="java.time.ZoneId"%>
<%@page import="java.time.ZonedDateTime"%>
<%@page import="java.time.LocalDateTime"%>
<%@page import="java.time.format.DateTimeFormatter"%>
<%@page import="java.util.logging.Level"%>
<%@page import="org.json.JSONArray"%>
<%@page import="comapp.cloud.Genesys"%>
<%@page import="org.apache.commons.lang3.StringUtils"%>
<%@page import="comapp.cloud.GenesysUser"%>
<%@page import="org.json.JSONObject"%>
<%@page import="java.util.Properties"%>
<%@page import="comapp.ConfigServlet"%>
<%@page import="java.util.logging.Logger"%>
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
/* .body { */
/*     margin: 0; */
/* 	font-family: 'Futura PT'', Medium; */
/*     display: flex; */
/*     height: 100vh; */
/*     width : 100%; */
/*     overflow: hidden; */
/* } */
.tableView {
    width: 100%;
    border-collapse: collapse;
    font-family: Arial, sans-serif;
}

thead th{
	top: 0;	
	position: sticky;
	z-index: 2;
	background-color: #004080;
	color: white;
	text-align: center;
	vertical-align: middle;
	padding: 8px 10px;
	border: 1px solid #ddd;
/* 	font-size: 14px; */
}

tbody tr {
    border-bottom: 1px solid #ddd;
}

tbody td{
	padding: 8px 10px;
	border: 1px solid #ddd;
	text-align: center;
	color: #333;
}

.bottom{
	position: flex;
  bottom: 0;
  width: 100%;
  height: 70px; 
  border-top: 1px #CCC solid;
  background: white; 
  display: flex; 
  justify-content: space-between; 
  margin-top: 50px;
}

.highlight {
        background-color: #83AF9B; /* Colore di evidenziazione */
    }

</style>

<body>

<%
Logger log = Logger.getLogger(ConfigServlet.web_app);
log.info("##################### [" + session.getId() + "] SearchCall starting.. #####################");


String authToken = null;
JSONObject me = (JSONObject) session.getAttribute("gui_user");
GenesysUser guser = null;
// try{
// 	log.info("[" + session.getId() + "]*******************security control *********************");
// 	Properties cs = ConfigServlet.getProperties();
// 	boolean enabled_security =  Boolean.parseBoolean(cs.getProperty("enabled_security", "true"));
// 	String code = request.getParameter("code");
// 	authToken = request.getParameter("authToken");
// 	if (StringUtils.isBlank(authToken)){
// 		authToken =(String) session.getAttribute("authToken");
// 	}
// 	log.info("[" + session.getId() + "] authToken: "+authToken+" code: "+code);
	
// 	if (enabled_security){
// 		if (me==null){	 
// 			try {
// 				String gui_clientId = cs.getProperty("gui_clientId", "");
// 				String gui_clientSecret = cs.getProperty("gui_clientSecret", "");
// 				String urlRegion =  cs.getProperty("urlRegion","");
// 				String redirect_uri = cs.getProperty("redirect_uri","");
// 				String urlAuthorizeString = cs.getProperty("urlAuthorizeString","https://login.mypurecloud.ie/oauth/authorize");		
				
// 				log.info("[" + session.getId() + "] clientId: "+gui_clientId+"  clientSecret: "+gui_clientSecret+" urlRegion: "+urlRegion+" urlAuthorizeString: "+urlAuthorizeString);
// 				guser  = new GenesysUser(session.getId(), gui_clientId, gui_clientSecret, urlRegion, redirect_uri, urlAuthorizeString);
// 				if (!StringUtils.isBlank(authToken)){
// 					guser.jToken= new JSONObject(authToken);	
// 				}else{
// 					if (!StringUtils.isBlank(code))
// 							guser.setCode(code);
// 						else
// 							response.sendRedirect("index.jsp");
// 				}
// 				authToken =  StringUtils.normalizeSpace( guser.getToken(false).toString());
// 				log.info("[" + session.getId() + "]  --> authToken: "+authToken);		
// 				me= Genesys.getUserMe(guser, "groups");
// 				JSONArray groups = me.getJSONArray("groups");
// 				log.info(groups.toString());
// 				String gui_AllowGroupAdmin = cs.getProperty("gui_AllowGroupAdmin", "*");
// 				String gui_AllowGroupUser = cs.getProperty("gui_AllowGroupUser", "*");
// 				boolean allowGroupAdmin =StringUtils.equalsIgnoreCase(gui_AllowGroupAdmin,"*");
// 				boolean allowGroupUser =StringUtils.equalsIgnoreCase(gui_AllowGroupUser,"*");
// 				log.info("allowGroupAdmin group: "+allowGroupAdmin);
// 				for (int i=0; !allowGroupUser && i<groups.length(); i++){
// 					log.info("Users group:"+groups.get(i));
// 					allowGroupUser = StringUtils.containsIgnoreCase( ""+groups.get(i), gui_AllowGroupUser);		
// 				}
// 				if (!allowGroupAdmin){
// 					for (int i=0; !allowGroupUser && i<groups.length(); i++){
// 						log.info("Users group:"+groups.get(i));
// 						allowGroupUser = StringUtils.containsIgnoreCase( ""+groups.get(i), gui_AllowGroupUser);		
// 					}
// 					if (!allowGroupUser){
// 						session.setAttribute("error", "Access Denied");
// 						response.sendRedirect("index.jsp");
// 						return;
// 					}
// 					session.setAttribute("gui_user", me);
					
// 				}else{
// 					session.setAttribute("gui_admin", me);
// 					session.setAttribute("gui_user", me);
// 				}	
// 			}catch (Exception e){
// 				log.log(Level.WARNING, "[" + session.getId() + "] Unauthorized access",e);
// 				response.sendRedirect("index.jsp");
				
// 				return;
// 		 	}
// 		}else{
// 			log.info("[" + session.getId() + "] me is not null: "+me.toString());
// 		}
// 	}else{			
// 		if (me==null){
// 			me = new JSONObject();
// 			me.put("name","admin");
// 			me.put("id","admin");
// 			session.setAttribute("gui_user",me);
// 		}
// 	}
	
// }catch (Exception e){
// 	log.log(Level.WARNING, "[" + session.getId() + "] unable to verify the security issue",e);
// }

Properties cs = ConfigServlet.getProperties();
int pageSize = cs.getProperty("pageSize") == null ? 1 : Integer.parseInt(cs.getProperty("pageSize", "10")); 
guser = (GenesysUser)session.getAttribute("guser");

if (guser == null) {
    log.info("[" + session.getId() + "] guser is null. Redirecting to index.jsp...");
    session.invalidate(); 
    response.sendRedirect("index.jsp");
    return;
}
try{
log.info("[" + session.getId() + "] guser = " + guser.toString());

String action = request.getParameter("action") == null ? "" : request.getParameter("action");
String order = request.getParameter("order") == null ? "desc" : request.getParameter("order");
String ani = request.getParameter("ani") == null ? "" : request.getParameter("ani");;
String dnis = request.getParameter("dnis") == null ? "" : request.getParameter("dnis");
String date_from = request.getParameter("from") == null ? "" : request.getParameter("from");
String date_to = request.getParameter("to") == null ? "" : request.getParameter("to");
int currentpage = request.getParameter("currentpage") == null ? 1 : Integer.parseInt(request.getParameter("currentpage"));
String date_from_formated = null;
String date_to_formated = null;
JSONObject jRes = null;

DateTimeFormatter localFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
DateTimeFormatter utcFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
LocalDateTime localDateTime;
ZonedDateTime localZonedDateTime;
ZonedDateTime utcDateTime;
if(StringUtils.isNotBlank(date_from)){
	localDateTime = LocalDateTime.parse(date_from, localFormatter);
	localZonedDateTime = localDateTime.atZone(ZoneId.of("Europe/Rome"));
    utcDateTime = localZonedDateTime.withZoneSameInstant(ZoneId.of("UTC"));
    date_from_formated = utcDateTime.format(utcFormatter);
}

if(StringUtils.isNotBlank(date_to)){
	localDateTime = LocalDateTime.parse(date_to, localFormatter);
	localZonedDateTime = localDateTime.atZone(ZoneId.of("Europe/Rome"));
    utcDateTime = localZonedDateTime.withZoneSameInstant(ZoneId.of("UTC"));
    date_to_formated = utcDateTime.format(utcFormatter);
}

log.info("["+ session.getId() +"] searchCall currentpage = " + currentpage);
log.info("["+ session.getId() +"] searchCall ani = " + ani);
log.info("["+ session.getId() +"] searchCall dnis = " + dnis);
log.info("["+ session.getId() +"] searchCall from = " + date_from);
log.info("["+ session.getId() +"] searchCall date_from_formated = " + date_from_formated);
log.info("["+ session.getId() +"] searchCall to = " + date_to);
log.info("["+ session.getId() +"] searchCall date_to_formated = " + date_to_formated);
log.info("["+ session.getId() +"] searchCall pageSize = " + pageSize); 
log.info("["+ session.getId() +"] searchCall guser = " + guser); 
log.info("["+ session.getId() +"] searchCall order = " + order); 

StringBuffer dettagli = new StringBuffer();
 
try{ 
	jRes = Genesys.getConversationList(session.getId(), guser, date_from_formated, date_to_formated, ani, dnis, currentpage, pageSize, order, dettagli); 
	if(StringUtils.compareIgnoreCase("search", action) == 0){
		log.info("["+ session.getId() +"] searchCall dettagli : " + dettagli); 
		JSONObject user = (JSONObject) session.getAttribute("gui_user");
		CSVReport.aggiungiLog(user.getString("name"), "Ricerca", dettagli.toString());
	}
}catch(Exception ex){
	log.log(Level.WARNING, "An error occured while using the getConversationList", ex);
}
// log.info("jRes = " + jRes.toString(4));
int totalItems = jRes.getInt("totalHits");
int totalPages = (int) Math.ceil((double) totalItems / pageSize);
log.info("["+ session.getId() +"] searchCall totalHits = " + totalItems);
if(totalItems == 0){
	out.println("<div style='text-align:center; border-top: 1px #CCC solid;'> <h2 style='color:red;'>Nessun elemento trovato!! </h2> </div>");
}else{
%>
<div class="mainContainer">
	<form action="SearchCall.jsp" id="searchCall" name="searchCall" method="POST">
		<table class="tableView">
			<thead>
				<tr>
					<th style="user-select: none; "><nobr>Scarica </nobr> <a style="text-decoration: none"></a> </th>
					<th style="user-select: none; "> CONVERSATION ID </th>
					<th style="cursor: pointer;" onclick="orderColumn()"> DATA INIZIO 
						<%
						if(StringUtils.compareIgnoreCase(order, "desc") == 0){
						%>
						<span id="icon-1" uk-icon="icon: arrow-down; ratio: 1.3"></span>
						<%	
						}else{
						%>
						<span id="icon-1" uk-icon="icon: arrow-up; ratio: 1.3"></span>
						<%	
						}
						%>
					
					
					</th>
					<th style="user-select: none; "> DURATA </th>
					<th style="user-select: none; "> ANI </th>
					<th style="user-select: none; "> DNIS </th>
					<th style="user-select: none; "> DIVIZIONE ID </th>
					<th style="user-select: none; "> DIREZIONE </th>
					<th style="user-select: none; ">PLAY</th>
				</tr>
			</thead>
			<tbody> 
			<%
				for(int i=0; i<jRes.getJSONArray("conversations").length(); i++){
					out.println("<tr id='row-" + i + "'>");
					
					out.println("<td>");
// 					out.println("<a style='text-decoration:none'>"
// 									+"<span style='width:15px' onclick='downloadAudio(\"row-" + i + "\", \""
// 									+ jRes.getJSONArray("conversations").getJSONObject(i).getString("conversationId") 		
// 									+"\")' uk-icon='icon: download;'> </span>"
// 								+"</a>");
					String convId = jRes.getJSONArray("conversations").getJSONObject(i).getString("conversationId") ;
// 					log.info("["+ session.getId() +"] searchCall convId  = " + convId);
					out.println("<a href='DownloadAudio?conversationId="+ convId +"'><span style='width:15px' onclick='downloadAudio(\"row-" + i + "\", \""
								+ convId 		
								+"\")' uk-icon='icon: download;'> </span></a></td>");
					
					out.println("</td>");
					
					out.println("<td>" + jRes.getJSONArray("conversations").getJSONObject(i).getString("conversationId") + "</td>");
					
					String utcStart = jRes.getJSONArray("conversations").getJSONObject(i).getString("conversationStart");
					utcDateTime = ZonedDateTime.parse(utcStart);
	    			ZonedDateTime zonedDateTime = utcDateTime.withZoneSameInstant(ZoneId.of("Europe/Rome"));
	    			DateTimeFormatter format = DateTimeFormatter.ofPattern("E, dd MMM, yyyy HH:mm", Locale.ITALIAN);
	    	        String formattedDate = zonedDateTime.format(format);
	    			String utcEnd = jRes.getJSONArray("conversations").getJSONObject(i).getString("conversationEnd");
	    			DateTimeFormatter formatUtc = DateTimeFormatter.ISO_ZONED_DATE_TIME;
	    			ZonedDateTime time1 = ZonedDateTime.parse(utcStart, formatUtc);
	    	        ZonedDateTime time2 = ZonedDateTime.parse(utcEnd, formatUtc);
	    	        Duration duration = Duration.between(time1, time2);
	    	        long minutes = duration.toMinutes()  ;
	    	        long seconds = duration.getSeconds() % 60;
	    			
	    	        out.println("<td>" + formattedDate + "</td>");
	    	        out.println("<td>"  + minutes +"min," + seconds +"sec </td>");					
					if(StringUtils.compareIgnoreCase(jRes.getJSONArray("conversations").getJSONObject(i).getJSONArray("participants").getJSONObject(0).getJSONArray("sessions").getJSONObject(0).getString("direction"), "outbound") == 0){
						out.println("<td>" + jRes.getJSONArray("conversations").getJSONObject(i).getJSONArray("participants").getJSONObject(0).getString("userId") + "</td>");
					}else{
						out.println("<td>" + jRes.getJSONArray("conversations").getJSONObject(i).getJSONArray("participants").getJSONObject(0).getJSONArray("sessions").getJSONObject(0).getString("ani") + "</td>");
					}
					

					out.println("<td>" + jRes.getJSONArray("conversations").getJSONObject(i).getJSONArray("participants").getJSONObject(0).getJSONArray("sessions").getJSONObject(0).getString("dnis") + "</td>");
					out.println("<td>");
					int j = 0;
					while(j<jRes.getJSONArray("conversations").getJSONObject(i).getJSONArray("divisionIds").length()){
						out.println(jRes.getJSONArray("conversations").getJSONObject(i).getJSONArray("divisionIds").getString(j) + "<br>");
						j++;
					}
					out.println("</td>");
					out.println("<td>" + jRes.getJSONArray("conversations").getJSONObject(i).getString("originatingDirection") + "</td>");
					  out.println("<td onclick='play(\"row-" + i + "\", \""
				                + jRes.getJSONArray("conversations").getJSONObject(i).getString("conversationId") 
				                + "\")' style='cursor:pointer; text-align:center;'>");
				    out.println("<span style='width:30px' uk-icon=\"icon:play-circle;\"></span></td>");
				    
					out.println("</tr>");
				}
	}

				%>
			</tbody>
		</table>
		<input type='hidden' id='currentpage' name='currentpage' value='<%=currentpage%>'>
		<input type='hidden' id='ani' name='ani' value='<%=ani%>'>
		<input type='hidden' id='order' name='order' value='<%=order%>'>
		<input type='hidden' id='dnis' name='dnis' value='<%=dnis%>'>
		<input type='hidden' id='from' name='from' value='<%=date_from%>'>
		<input type='hidden' id='to' name='to' value='<%=date_to%>'>
		
	</form>
</div>

<div id="interaction_search" uk-modal="bg-close: false">
    <div class="uk-modal-dialog uk-modal-body uk-align-center uk-transition-fade" style="width: fit-content; border-radius: 4px;">
        <div uk-spinner></div>
          Caricamento della pagina in corso...
          
	</div>
</div>


<div class="bottom">
	<div class="top-left">
		<% 	out.print((currentpage*pageSize>totalItems?(""+totalItems):currentpage*pageSize)+" / "+totalItems); %>	
	</div>
	
	<div class="top-center">
		<iframe id="frameplayer" name="frameplayer" src="" style="width:100%; height:100%; border:0;"></iframe> 	</nobr>
	</div>
	
	<div id="loadingModal" uk-modal>
		<div class="uk-modal-dialog uk-modal-body uk-text-center">
		     Ricerca dell'audio in corso...
		    <div uk-spinner></div>
		</div>
	</div>

	<div class="top-right" >
		<span>
		<%
			try {
			    String disabled = "";
			    
			    if (currentpage == 1) {
			        disabled = " disabled='true'";
			    }
			    
			    out.print("<input type='button' class='uk-button uk-button-default uk-button-small'" + disabled + " id='First' name='First' value='First' onclick=\"submitformpage('1');\">");
			    disabled = (currentpage == 1) ? " disabled='true'" : ""; 
			    out.print("<input type='button' class='uk-button uk-button-default uk-button-small'" + disabled + " id='Previous' name='Previous' value='Previous' onclick=\"submitformpage('" + (currentpage - 1) + "');\">");
			    
			    int startpage = Math.max(1, currentpage - 2);
			    int endpage = Math.min(totalPages, startpage + 6);

			    if (totalPages > 5) {
			        if (currentpage > 3) {
			            startpage = currentpage - 2; // Start from currentpage - 2
			        }
			        if (totalPages - startpage < 5) {
			            startpage = totalPages - 5; // Adjust to ensure showing 5 buttons at least
			        }
			    }

			    for (int i = startpage; i <= endpage; i++) {
			        if (i == currentpage) {
			            out.print("<input type='button' class='uk-button uk-button-default uk-button-small select' disabled id='" + i + "' name='" + i + "' value='" + i + "' >");
			        } else {
			            out.print("<input type='button' class='uk-button uk-button-default uk-button-small' id='" + i + "' name='" + i + "' value='" + i + "' onclick=\"submitformpage('" + i + "');\">");
			        }
			    }

			    if (totalPages > endpage) {
			        out.print("...");
			        out.print("<input type='button' class='uk-button uk-button-default uk-button-small' id='" + totalPages + "' name='" + totalPages + "' value='" + totalPages + "' onclick=\"submitformpage('" + totalPages + "');\">");
			    }

			    disabled = (currentpage == totalPages) ? " disabled='true'" : "";
			    out.print("<input type='button' class='uk-button uk-button-default uk-button-small'" + disabled + " id='Next' name='Next' value='Next' onclick=\"submitformpage('" + (currentpage + 1) + "');\">");
			    out.print("<input type='button' class='uk-button uk-button-default uk-button-small'" + disabled + " id='Last' name='Last' value='Last' onclick=\"submitformpage('" + totalPages + "');\">");

			} catch (Exception e) {
			    log.log(Level.WARNING, "[" + session.getId() + "]", e);
			}
}catch(Exception e){
	out.println("<h2 style='text-align:center; color:red;'> Un errore si è prodotto riprovare più tardi</h2>");
	log.log(Level.WARNING, "An error occured on a large scale", e);
	
}


			%>
		</span>
	</div>
	
</div>
<script type="text/javascript">

function play(rowId, conversationId) {
    UIkit.modal('#loadingModal').show();
    document.querySelectorAll("tr").forEach(row => row.classList.remove("highlight"));
    
    const row = document.getElementById(rowId);
    if (row) {
        row.classList.add("highlight");
    } else {
    }

    const audioPlayer = document.getElementById('frameplayer');
    if (audioPlayer) {
        const audioPageUrl = "AudioPlayer.jsp?conversationId=" + encodeURIComponent(conversationId);
        audioPlayer.src = audioPageUrl;
        audioPlayer.style.display = "block";
    } else {
    }
}


function submitformpage(page) {
    document.getElementById('currentpage').value = page;
    UIkit.modal("#interaction_search").show();
    document.forms['searchCall'].submit();  
}


function orderColumn() {
    const orderField = document.getElementById("order");
    orderField.value = (orderField.value === 'desc') ? 'asc' : 'desc';
    UIkit.modal("#interaction_search").show();
    document.forms['searchCall'].submit();
}


document.getElementById("frameplayer").onload = function() {
    UIkit.modal("#loadingModal").hide();
};


function downloadAudio(rowId, conversationId){
    document.querySelectorAll("tr").forEach(row => row.classList.remove("highlight"));
    const row = document.getElementById(rowId);
    if (row) {
        row.classList.add("highlight");
    } else {
    }
    document.getElementById('conversationId').value = conversationId;
    document.forms['downloadAudio'].submit();  
}




</script>
</body>
</html>