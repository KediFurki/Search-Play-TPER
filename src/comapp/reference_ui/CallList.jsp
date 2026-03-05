<%@page import="java.time.ZonedDateTime"%>
<%@page import="java.time.LocalDateTime"%>
<%@page import="java.util.HashMap"%>
<%@page import="java.util.Map"%>
<%@page import="java.time.Duration"%>
<%@page import="java.time.ZoneId"%>
<%@page import="java.time.format.DateTimeFormatter"%>
<%@page import="java.time.Instant"%>
<%@page import="org.owasp.encoder.Encode"%>
<%@page import="java.sql.SQLException"%>
<%@page import="java.sql.PreparedStatement"%>
<%@page import="org.json.JSONArray"%>
<%@page import="comapp.cloud.Genesys"%>
<%@page import="org.json.JSONObject"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.List"%>
<%@page import="comapp.User"%>
<%@page import="javax.sql.DataSource"%>
<%@page import="javax.naming.InitialContext"%>
<%@page import="javax.naming.Context"%>
<%@page import="org.apache.logging.log4j.Level"%>
<%@page import="org.apache.commons.lang3.StringUtils"%>
<%@page import="java.sql.ResultSet"%>
<%@page import="java.sql.Connection"%>
<%@page import="java.util.TimeZone"%>
<%@page import="java.util.Locale"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="java.util.Properties"%>
<%@page import="comapp.ConfigServlet"%>
<%@page import="comapp.cloud.GenesysUser"%>
<%@page import="comapp.cloud.TrackId"%>
<%@page import="org.apache.logging.log4j.LogManager"%>
<%@page import="org.apache.logging.log4j.Logger"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Search And Play</title>
<link rel="icon" type="image/x-icon" href="img/favicon.ico">
<link rel="stylesheet" href="css/uikit.min.css" />
 <script src="js/uikit-icons.min.js"></script>
<script src="js/uikit.min.js"></script>
<script src="js/uikit-icons.min.js"></script> 
</head>
<style>
.overlay {
	position: fixed;
	top: 0;
	right: 0;
	height: 100%;
	width: 0;
	background-color: white;
	overflow: auto;
	z-index: 99;
	text-align: center;
	padding-top: 0;
	transition: width 1s;
	box-shadow: -5px 0px 3px 0px #e1e1e1;
}
.overlay_show {
	width: 70%;
	padding-top: 75px;
	/* 			   min-width: 400px; */
	transition: width 1s, padding-left 1s;
}
.highlight {
	background-color: #83AF9B; /* Colore di evidenziazione */
}
.uk-button-small {
	padding: 0;
	padding-left: 7px;
	padding-right: 7px;
	border: 0px;
	text-transform: none;
}	
.uk-button:hover:enabled {
	color: #fff !important;
	background-color: #585858;
	background: linear-gradient(to bottom, #585858 0%, #111 100%);
}
.uk-button:disabled {
	color: grey;
}
.select {
	border: 1px #ccc solid !important;
	color: #CCC;
}
html, body {
	font: 12px "Roboto", Cairo, Sans-serif;
	line-height: normal;
	line-height: 20px;
	color: #555;
	margin: 0
}
td {
	padding: 5px;
	border-bottom: 1px #CCC solid;
	text-overflow: ellipsis;
	white-space: nowrap;
 	max-width: 2px;
	overflow: hidden;
	text-align: left;
	cursor: pointer;
}
.uk-table th {
	vertical-align: middle;
	padding-left: 0px;
	padding-right: 0px;
	text-align: center;
}
.bottom {
	position: fixed;
	bottom: 0;
	width: 100%;
	height: 90px;
	border-top: 1px #CCC solid;
	background: white;
}
.resizer {
	/* 			    position: absolute; */
	/* 			    top: 0; */
	/* 			    right: 0; */
	/* 			    width: 2px; */
	cursor: col-resize;
	user-select: none;
	min-width: 3px;
	max-width: 3px;
	vertical-align: middle;
}
.resizer div {
	background-color: gray;
	height: 24px;
	border-radius: 2px;
	cursor: col-resize;
	width: 3px;
	margin-top: 8px;
	cursor: col-resize
}
.resizerTarget {
	border: 0px;
}
.resizer:hover, .resizing {
	cursor: col-resize;
}
.overlay {
	position: fixed;
	top: 0;
	right: 0;
	height: 100%;
	width: 0;
	background-color: white;
	overflow: auto;
	z-index: 99;
	text-align: center;
	padding-top: 0;
	transition: width 1s;
	box-shadow: -5px 0px 3px 0px #e1e1e1;
}
.overlay_show {
	width: 70%;
	padding-top: 75px;
	/* 			   min-width: 400px; */
	transition: width 1s, padding-left 1s;
}

.uk-table td {
	padding-top: 5px;
	padding-bottom: 5px;
}

.uk-switch {
	position: relative;
	display: inline-block;
	height: 13px;
	width: 24px;
}

/* Hide default HTML checkbox */
.uk-switch input {
	display: none;
}
/* Slider */
.uk-switch-slider {
	background-color: rgba(0, 0, 0, 0.22);
	position: absolute;
	top: 0;
	left: 0;
	right: 0;
	border-radius: 200px;
	bottom: 0;
	cursor: pointer;
	transition-property: background-color;
	transition-duration: .2s;
	box-shadow: inset 0 0 2px rgba(0, 0, 0, 0.07);
}
/* Switch pointer */
.uk-switch-slider:before {
	content: '';
	background-color: #fff;
	position: absolute;
	width: 12px;
	height: 12px;
	left: 1px;
	bottom: 1px;
	border-radius: 50%;
	transition-property: transform, box-shadow;
	transition-duration: .2s;
}

nput:checked+.uk-switch-slider {
	background-color: #39f !important;
}
/* Pointer active animation */
input:checked+.uk-switch-slider:before {
	transform: translateX(12px);
}
.uk-switch input:checked + .uk-switch-slider {
    background-color: #1e87f0 !important; /* Questo è il blu standard UIkit */
}

/* Modifiers */
.uk-switch-slider.uk-switch-on-off {
	background-color: #f0506e;
}

input:checked+.uk-switch-slider.uk-switch-on-off {
	background-color: #32d296 !important;
}

/* Style Modifier */
.uk-switch-slider.uk-switch-big:before {
	transform: scale(1.2);
	box-shadow: 0 0 6px rgba(0, 0, 0, 0.22);
}

.uk-switch-slider.uk-switch-small:before {
	box-shadow: 0 0 6px rgba(0, 0, 0, 0.22);
}

input:checked+.uk-switch-slider.uk-switch-big:before {
	transform: translateX(12px) scale(1.2);
}

/* Inverse Modifier - affects only default */
.uk-light .uk-switch-slider:not(.uk-switch-on-off) {
	background-color: rgba(255, 255, 255, 0.22);
}

.uk-notification {
	width: auto;
}

.rowselected {
	background-color: rgb(176, 255, 194);/*lightgreen;*/
}

</style>
<body>

<%!

public String buildFullQuery(String sql, List<String> params) {
    String fullQuery = sql;
    for (String p : params) {
        fullQuery = fullQuery.replaceFirst("\\?", "'" + p + "'");
    }
    return fullQuery;
}
%>

<%
Logger log = LogManager.getLogger("comapp.Genesys" + this.getClass()); 
String sessionid = session.getId();
User user = (User) session.getAttribute("user");
GenesysUser guser = (GenesysUser)session.getAttribute("guser"); 
if(guser==null || user==null){
	out.println("<script type=\"text/javascript\">location='login.jsp'</script>");
    return;
}
String code = guser.getCode();
TrackId trackId = new TrackId(sessionid, code);
trackId.add(user.getUserName());
log.info("{} ----------------------- CallList --------------------------", trackId);

Properties cs = ConfigServlet.getProperties();
String action = request.getParameter("action") == null ? "" : request.getParameter("action");

int currentpage = request.getParameter("currentpage") == null ? 1 : Integer.parseInt(request.getParameter("currentpage"));

int pageSize = Integer.parseInt(cs.getProperty("pageSize", "20"));

long totalHits =  request.getParameter("totalHits") == null ? -1 : Integer.parseInt(request.getParameter("totalHits"));;

//**************************************************************
// Parametri di ricerca
//**************************************************************
String connid = request.getParameter("connid") == null ? "" : request.getParameter("connid");
String agentname = request.getParameter("agentname") == null ? "" : request.getParameter("agentname");
String ani = request.getParameter("ani") == null ? "" : request.getParameter("ani");
String dnis = request.getParameter("dnis") == null ? "" : request.getParameter("dnis");
String from = request.getParameter("from") == null ? "" : request.getParameter("from");
String to = request.getParameter("to") == null ? "" : request.getParameter("to");
String select = request.getParameter("select") == null ? "" : request.getParameter("select");
HashMap<String, String> orderBy= new HashMap<>();
String order_column = request.getParameter("order_column");


log.log(Level.INFO, trackId+"==== Parameters");
log.log(Level.INFO, trackId+"totalHits  : "+totalHits);
log.log(Level.INFO, trackId+"currentpage: "+currentpage);
log.log(Level.INFO, trackId+"pageSize   : "+pageSize);
log.log(Level.INFO, trackId+"connid  	: "+connid);
log.log(Level.INFO, trackId+"agentname	: "+agentname);
log.log(Level.INFO, trackId+"ani  		: "+ani);
log.log(Level.INFO, trackId+"dnis  		: "+dnis);
log.log(Level.INFO, trackId+"from		: "+from);
log.log(Level.INFO, trackId+"to   		: "+to);
log.log(Level.INFO, trackId+"==== Filters");
log.log(Level.INFO, trackId+"select		: "+select);



if(StringUtils.equalsIgnoreCase(action, "search")){
	session.setAttribute("connid", connid);
	session.setAttribute("agentname", agentname);
	session.setAttribute("ani", ani);
	session.setAttribute("dnis", dnis);
	
	if(StringUtils.isNotBlank(from)){
		LocalDateTime ldt = LocalDateTime.parse(from);
		ZonedDateTime dataLocale = ldt.atZone(ZoneId.systemDefault());
	    ZonedDateTime dataUTC = dataLocale.withZoneSameInstant(ZoneId.of("UTC"));
	    from = dataUTC.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
	}
    session.setAttribute("from", from);
    
    if(StringUtils.isNotBlank(to)){
		LocalDateTime ldt = LocalDateTime.parse(to);
		ZonedDateTime dataLocale = ldt.atZone(ZoneId.systemDefault());
	    ZonedDateTime dataUTC = dataLocale.withZoneSameInstant(ZoneId.of("UTC"));
	    to = dataUTC.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
	}
	session.setAttribute("to", to);
// 	orderBy.put("part.starttime", "desc");
	session.setAttribute("orderBy", orderBy);
}else{
	connid = (String) session.getAttribute("connid");
	agentname = (String) session.getAttribute("agentname");
	ani = (String) session.getAttribute("ani");
	dnis = (String) session.getAttribute("dnis");
	from = (String) session.getAttribute("from");
	to = (String) session.getAttribute("to");
	Object sessionObj = session.getAttribute("orderBy");
	if (sessionObj instanceof HashMap<?, ?>) {
		@SuppressWarnings("unchecked")
		HashMap<String, String> tempMap = (HashMap<String, String>) sessionObj;
	    orderBy = tempMap;
	}
	if(StringUtils.isNotBlank(order_column)){
		if(!orderBy.containsKey(order_column)){
			orderBy.put(order_column, "asc");
		}else{
			if(StringUtils.equalsIgnoreCase(orderBy.get(order_column), "asc"))
				orderBy.put(order_column, "desc");
			else
				orderBy.put(order_column, "asc");
		}
		session.setAttribute("orderBy", orderBy);
	}
}



//**************************************************************
//Trovare chi fa parte del/dei gruppo/i dell'utente
//**************************************************************
List<String> user_authgrp_id = new ArrayList<>(user.authGroups().values());
log.log(Level.INFO, trackId+" user_authgrp_id : "+user_authgrp_id);
List<String> user_grp_members = new ArrayList<String>();
for(String str : user_authgrp_id){
	JSONObject jo = new JSONObject();
	jo = Genesys.getGroupMembers(trackId.toString(), guser, str, pageSize, 1); 
	int pageCount = jo.getInt("pageCount");
	JSONArray entities = new JSONArray();
	entities = jo.getJSONArray("entities");
	for(int i=0; i<entities.length(); i++){
		if(!user_grp_members.contains(entities.getJSONObject(i).getString("username"))){
			user_grp_members.add(entities.getJSONObject(i).getString("username"));
		}
	}
	if(pageCount > 1){
		for(int i=2; i<pageCount; i++){
			jo = Genesys.getGroupMembers(trackId.toString(), guser, str, pageSize, i); 
			entities = jo.getJSONArray("entities");
			for(int j=0; j<entities.length(); j++){
				if(!user_grp_members.contains(entities.getJSONObject(j).getString("username"))){
					user_grp_members.add(entities.getJSONObject(j).getString("username"));
				}
			}
		}
	}
}
log.log(Level.INFO,"{} user_team_members = {}", trackId, user_grp_members );

//*******************************************************************************
//Costruire la query di ricerca basata sui vari parametri e filtri inseriti
//*******************************************************************************
List<String> params = new ArrayList<>();
StringBuilder filtriSql = new StringBuilder();

if(StringUtils.isNotBlank(connid)){
	filtriSql.append(" and part.conversationid = ?");
	params.add(connid);
}
if(StringUtils.isNotBlank(agentname)){
	filtriSql.append(" AND UPPER(TRIM(us.name)) LIKE UPPER(TRIM('%' + ? + '%')) ");	
	params.add(agentname);
}
if(StringUtils.isNotBlank(ani)){
	filtriSql.append(" and sess.ani = ?");
	params.add(ani);
}
if(StringUtils.isNotBlank(dnis)){
	filtriSql.append(" and sess.dnis = ?");
	params.add(dnis);
}
if(StringUtils.isNotBlank(from) && StringUtils.isNotBlank(to)){
	filtriSql.append(" and part.starttime between ? and  ? ");
	params.add(from);
	params.add(to);
}else if(StringUtils.isNotBlank(from)){
	filtriSql.append(" and part.starttime >= ? ");
	params.add(from);
}else if(StringUtils.isNotBlank(to)){
	filtriSql.append(" and part.starttime <= ? ");
	params.add(to);
}
if(!user_grp_members.isEmpty()){
	filtriSql.append(" and username in (");
	for(int i=0; i<user_grp_members.size(); i++){
		filtriSql.append("?");
		if (i < user_grp_members.size() - 1) {
			filtriSql.append(",");
        }
		params.add(user_grp_members.get(i));
	}
	filtriSql.append(")");
}

StringBuffer order_col= new StringBuffer();
if(!orderBy.isEmpty()){
	order_col.append(" ORDER BY ");
	int j = 0;
	int size = orderBy.size();
	for(String str : orderBy.keySet()){		
		order_col.append(str + " " + orderBy.get(str));
		if(j < size - 1){
            order_col.append(", ");
        }
		j++;
	}
}

String dir = null;

%>


<!-- ************************************************************** -->
<!--  Rappresentazione grafica della tabella dei risultati          -->
<!-- ************************************************************** -->

<div class="" style="height:100%">
	<form action="CallList.jsp" method="post" id="callList">
		<table id='resizeMe' class="uk-table uk-table-hover uk-table-divider" style='margin-bottom: 85px; margin-top: 10px'>
			<thead style='width: 100%; table-layout: fixed; position: stiky; padding-bottom: 0px'  >
				<tr>
				<%
					if(user.getDownloadStatus()){
				%>
					<th class='resizerTarget' style='padding-bottom: 2px;padding-top: 4px; width: 60px; min-width: 60px; max-width: 60px; text-align: center; vertical-align: top'>
						<a style="text-decoration:none" title="Scarica"><span style='width:30px' uk-icon="icon: download;  " onclick="executeDonwload()"></span> </a>
		           	</th>
				<%		
					}
				%>
		           	
		           	<th class='' style='padding: 0px;  width: 0px '></th>
		           	<th class='' style='padding-bottom: 2px; padding-top: 4px; vertical-align: top ; width: 60px; min-width: 60px; max-width: 60px;  text-align: center '><nobr>Play</nobr>	</th>
		           	
<!-- 		           	<th class='resizer' style='padding: 0px; cursor: default; width: 0px'><div style='min-width: 0px; width: 0px; max-width: 0px'></div></th> -->
		        	<th class='resizer' style='padding: 0px; cursor: default; width: 0px'><div  ></div></th>
		        	<% dir = null;
		           		if(orderBy.containsKey("part.conversationid"))
		           			if(StringUtils.equalsAnyIgnoreCase("asc", orderBy.get("part.conversationid"))) dir = "up";
		           			else dir = "down";
		           	%>
					<th class='resizerTarget' style='padding-bottom: 2px; padding-top: 4px; cursor: pointer; min-width: 70px; text-align: center' onclick="orderColumn('part.conversationid')"><nobr  style="padding-right: 5px;">CONVERSATION ID<span class="uk-margin-xsmall-right" uk-icon="icon: arrow-<%=dir%>; ratio: 1" ></span> </nobr></th>
				
		        	<th class='resizer' style='padding: 0px; cursor: default; width: 0px'><div  ></div></th>
		        	<% dir = null;
		           		if(orderBy.containsKey("part.starttime"))
		           			if(StringUtils.equalsAnyIgnoreCase("asc", orderBy.get("part.starttime"))) dir = "up";
		           			else dir = "down";
		           	%>
    				<th class='resizerTarget' style='padding-bottom: 2px; padding-top: 4px; cursor: pointer; min-width: 70px; text-align: center'  onclick="orderColumn('part.starttime')"><nobr style="padding-right: 5px;">DATA ORA INIZIO<span class="uk-margin-xsmall-right" uk-icon="icon: arrow-<%=dir%>; ratio: 1" ></span> </nobr></th>
					
		            <th class='resizer' style='padding: 0px; cursor: default; width: 0px'><div  ></div></th>
	           	 	<th class='resizerTarget' style='padding-bottom: 2px; padding-top: 4px; cursor: default; min-width: 70px; text-align: center'><nobr style="padding-right: 35px;">DURATA</nobr></th>
	           	 	
		        	<th class='resizer' style='padding: 0px; cursor: default; width: 0px'><div  ></div></th>
		        	<% dir = null;
		           		if(orderBy.containsKey("sess.ani"))
		           			if(StringUtils.equalsAnyIgnoreCase("asc", orderBy.get("sess.ani"))) dir = "up";
		           			else dir = "down";
		           	%>
		            <th class='resizerTarget' style='padding-bottom: 2px; padding-top: 4px; cursor: pointer; min-width: 70px; text-align: center' onclick="orderColumn('sess.ani')" ><nobr style="  padding-right: 5px;">NUMERO CHIAMANTE<span class="uk-margin-xsmall-right" uk-icon="icon: arrow-<%=dir%>; ratio: 1" ></span></nobr></th>	
		            
					<th class='resizer' style='padding: 0px; cursor: default; width: 0px'><div  ></div></th>
					<% dir = null;
		           		if(orderBy.containsKey("sess.dnis"))
		           			if(StringUtils.equalsAnyIgnoreCase("asc", orderBy.get("sess.dnis"))) dir = "up";
		           			else dir = "down";
		           	%>
		            <th class='resizerTarget' style='padding-bottom: 2px; padding-top: 4px; cursor: pointer; min-width: 70px; text-align: center' onclick="orderColumn('sess.dnis')"><nobr  style="  padding-right: 5px;">NUMERO CHIAMATO<span class="uk-margin-xsmall-right" uk-icon="icon: arrow-<%=dir%>; ratio: 1" ></span></nobr></th>	
		            
		             <th class='resizer' style='padding: 0px; cursor: default; width: 0px'><div  ></div></th>
	           	 	<th class='resizerTarget' style='padding-bottom: 2px; padding-top: 4px; cursor: default; min-width: 70px; text-align: center'><nobr style="padding-right: 35px;">DIREZIONE</nobr></th>
	           	 	
		        	<th class='resizer' style='padding: 0px; cursor: default; width: 0px '><div  ></div></th>
		        	<% dir = null;
		           		if(orderBy.containsKey("us.email"))
		           			if(StringUtils.equalsAnyIgnoreCase("asc", orderBy.get("us.email"))) dir = "up";
		           			else dir = "down";
		           	%>
		            <th class='resizerTarget' style='padding-bottom: 2px; padding-top: 4px; cursor: pointer; min-width: 70px; text-align: center' onclick="orderColumn('us.email')"><nobr  style="  padding-right: 5px;">EMAIL<span class="uk-margin-xsmall-right" uk-icon="icon: arrow-<%=dir%>; ratio: 1" ></span></nobr>	</th>
		            
		        	<th class='resizer' style='padding: 0px; cursor: default; width: 0px '><div  ></div></th>

				</tr>
			</thead>		
			<tbody>
			<%
				Connection connection = null;
				Connection connection_aud = null;
				PreparedStatement pstmt = null;
				PreparedStatement pstmt_aud = null;
				ResultSet rs = null;
				
				String query_search = cs.getProperty("sql.query.search");
				String query_count = cs.getProperty("sql.query.count");
				String sql_search = String.format(query_search, filtriSql.toString(), order_col.toString()) ;
				String sql_count = String.format(query_count, filtriSql.toString());
				log.log(Level.INFO, "{} sql_count = {}", trackId, sql_count);
				log.log(Level.INFO, "{} sql_search = {}", trackId, sql_search);
				
				try{
					Context ctx = new InitialContext();
// 					log.log(Level.DEBUG, trackId+"get connection: java:comp/env/" + ConfigServlet.prefix + ConfigServlet.web_app);
					DataSource ds = (DataSource) ctx.lookup("java:comp/env/" + ConfigServlet.prefix + ConfigServlet.web_app);
					connection = ds.getConnection();
					
					//**************************************************************
					// Conteggiamo il numero di elementi
					//**************************************************************
					
					pstmt = connection.prepareStatement(sql_count);
					for(int i=0; i<params.size(); i++){
						pstmt.setString(i+1, params.get(i));
					}
					ResultSet rsCount = pstmt.executeQuery();
					if(rsCount.next()){
						totalHits = rsCount.getLong(1);
						log.log(Level.INFO, "{} totalHits = {}", trackId, totalHits);
					}
					rsCount.close();
					pstmt.close();
					
					
					//**************************************************************
					// Recuperiamo gli elementi
					//**************************************************************
					if(totalHits > 0){
						pstmt = connection.prepareStatement(sql_search);
						for(int i=0; i<params.size(); i++){
							pstmt.setString(i+1, params.get(i));
						}
						int offset = (currentpage-1) * pageSize;
						pstmt.setInt(params.size() + 1, offset);
						pstmt.setInt(params.size() + 2, pageSize);
						
						rs = pstmt.executeQuery();
												
						String checked;
						int index = 0;
						while(rs.next()){
							String rs_connid 	= rs.getString("conversationid");
							String rs_email		= rs.getString("email");
							String rs_direzione = rs.getString("direction");
							String rs_ani		= rs.getString("ani");
							if(StringUtils.isNotBlank(rs_ani)){
								rs_ani = rs_ani.replace("sip:", "");
								rs_ani = rs_ani.replace("tel:", "");
								if (rs_ani.matches("\\+?\\d{7,}@\\d{1,3}(\\.\\d{1,3}){3}.*")) {
									rs_ani = rs_ani.replaceAll("(\\+?\\d+)@.*", "$1");
						        }else if (StringUtils.compareIgnoreCase(rs_direzione, "outbound")==0 && rs_ani.contains("@")) { 
					            	rs_ani = rs.getString("name");
					            } else if (rs_ani.contains("@")) {
					            	rs_ani = rs_ani.replace("@", "-");
					            } 
							}
							
							
							String rs_dnis		= rs.getString("dnis");
							if(StringUtils.isNotBlank(rs_dnis)){
								rs_dnis = rs_dnis.replace("tel:", "");
								rs_dnis = rs_dnis.replace("sip:", "");
								if (rs_dnis.matches("\\+?\\d{7,}@\\d{1,3}(\\.\\d{1,3}){3}.*")) {
									rs_dnis = rs_dnis.replaceAll("(\\+?\\d+)@.*", "$1");
						        }else if (StringUtils.compareIgnoreCase(rs_direzione, "inbound")==0 && rs_dnis.contains("@")) { 
						        	rs_dnis = rs.getString("name");
					            } else if (rs_dnis.contains("@")) {
					            	rs_dnis = rs_dnis.replace("@", "-");
					            } 
							}
							String rs_starttime		= rs.getString("starttime");
							String rs_endtime		= rs.getString("endtime");
							Instant start = Instant.parse(rs_starttime);
							Instant end = Instant.parse(rs_endtime);
							
							DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
				                    .withZone(ZoneId.systemDefault());
							Duration duration = Duration.between(start, end);
							long seconds = duration.getSeconds();
							String hms = String.format("%02d:%02d:%02d", 
					                seconds / 3600, 
					                (seconds % 3600) / 60, 
					                seconds % 60);
							
							checked = StringUtils.containsIgnoreCase(select, rs_connid)?" checked='true' ":" ";
							out.println("<tr id='row-" + index + "'>");
							if(user.canDownload()){
								out.println("<td>");
								out.println("<label class=\"uk-switch uk-margin-right\" _con='"+rs_connid+"' onclick=\"  changeTo(event,this,'"+rs_connid+"')\">");
								out.println("<input type=\"checkbox\" "+checked+" >");
								out.println("<div class=\"uk-switch-slider uk-switch-big\"></div>");
								out.println("</label>");
								out.println("</td>");
							}
							out.println("<td></td>");
							out.println("<td  onclick='play(\"row-" + index + "\", \""  
								    + rs_connid
								    + "\")'><span style='width:30px' uk-icon=\"icon:play-circle;\"></span></td>");
							out.println("<td> </td>");
							out.println("<td  style='text-align:center;'>" + Encode.forHtml(rs_connid) + "</td>");
							out.println("<td> </td>");
							out.println("<td  style='text-align:center;'>" + Encode.forHtml(formatter.format(start)) + "</td>");
							out.println("<td> </td>");
							out.println("<td  style='text-align:center;'>" + Encode.forHtml(hms) + "</td>");
							out.println("<td> </td>");
							out.println("<td  style='text-align:center;'>" + Encode.forHtml(rs_ani) + "</td>");
							out.println("<td> </td>");
							out.println("<td  style='text-align:center;'>" + Encode.forHtml(rs_dnis) + "</td>");
							out.println("<td> </td>");
							out.println("<td  style='text-align:center;'>" + Encode.forHtml(rs_direzione) + "</td>");
							out.println("<td> </td>");
							out.println("<td  style='text-align:center;'>" + Encode.forHtml(rs_email) + "</td>");
							out.println("</tr>");
							index++;
							
							
						}					
						
						
					}else{
						out.println("<tr style='text-align:center; color:red'> Nessun dato trovato </tr>");
					}
					
					//-------------------------------------------------------------------------------
					// Inserire la ricerca dell'utente sul database analyzeraudit
					//-------------------------------------------------------------------------------
					String audit_query = buildFullQuery(sql_search, params);
					try{
						Context ctx_aud = new InitialContext();
// 						log.log(Level.DEBUG, trackId + " get connection: java:comp/env/" + ConfigServlet.prefix + "SearchAndPlay");
						DataSource ds_aud = (DataSource) ctx.lookup("java:comp/env/" + ConfigServlet.prefix +  "SearchAndPlay");
						connection_aud = ds_aud.getConnection();
						connection_aud.setAutoCommit(false);
						
						String audit_insert = cs.getProperty("sql.audit.insert");
						pstmt_aud = connection_aud.prepareStatement(audit_insert);
						pstmt_aud.setString(1, user.getUserName());
						pstmt_aud.setString(2, "SEARCH");
						pstmt_aud.setString(3, audit_query);
						pstmt_aud.setString(4, "SUCCESS");
						pstmt_aud.setBoolean(5, false);
						
						 pstmt_aud.executeUpdate();
						connection_aud.commit();
					}catch(SQLException sqle){
						if (connection_aud != null) {
					        try { 
					            connection_aud.rollback(); 
					            log.debug(trackId + " Rollback eseguito correttamente");
					        } catch (SQLException ex) { 
					            log.error(trackId + " Errore durante il rollback", ex); 
					        }
					    }
						log.error(trackId + " Errore durante l'inserimento dell'audit: ", sqle);
					}finally{
						if (pstmt_aud != null) try { pstmt_aud.close(); } catch (SQLException e) { }
					    if (connection_aud != null) try { connection_aud.close(); } catch (SQLException e) { }
					}
					
					
				}catch(Exception e){
					out.println("<h3 style='color:red; align-text:center;'> Si è Verificato un errore, Riprovare più tardi </h3>");
					log.log(Level.WARN, "{} Si è verificato un errore durante l'esecuzione dell'applicazione", trackId, e);
				}finally{
					try { rs.close(); } catch (Exception e) {}
					try { pstmt.close(); } catch (Exception e) {}
					try { connection.close(); } catch (Exception e) {}
				}
			%>
			</tbody>
		</table>
		<input type='hidden' id='currentpage' name='currentpage' value='<%=currentpage%>'>
		<input type='hidden' id="order_column" name="order_column" value=''>
		<input type='hidden' id='select' name='select' value='<%=select%>'>
		
	</form>
	
	<div class="bottom">
		<table style='width:100%' class="uk-margin-small-top">
			<tr>
				<td style='border:0px; text-align: left'>
					<button class="class='uk-button uk-button-link uk-button-small ">
					<% 
						if(totalHits<0){
							out.print("0/0");
						}else{
							 out.print((currentpage*pageSize>totalHits?(""+totalHits):currentpage*pageSize)+" / "+totalHits);
						}
						%>
					</button> 		
				</td>		
				<td style='border:0px; text-align: center'>
					<audio controls preload="none" id="control" style="width: 100%;">
						<source id='_player' src="" type="audio/mpeg">
					</audio>
					<div id="modal-attesa-audio" uk-modal="bg-close: false; esc-close: false;">
		        		<div class="uk-modal-dialog uk-modal-body uk-text-center">
		            		<h2 class="uk-modal-title">Attendere...</h2>
		            		<p>Il file audio è in fase di preparazione...</p>
		            		<div uk-spinner="ratio: 2"></div>
		        		</div>
		    		</div>
				</td>
				<td style='border:0px; text-align: right'>
					<span>
<%
	try {
		String disabled = "";
		if (currentpage == 1) {
			disabled = " disabled='true'";
		}
		int numberofpage = (int) Math.ceil((double) totalHits / (double) pageSize); 
		if(numberofpage == 0) numberofpage=1;
// 		log.info("{} numberofpage = {} ",trackId, numberofpage);
%>
						<input type='button' class='uk-button uk-button-default uk-button-small' <%=disabled%> id='First' name='First' value='First' onclick="submitformpage('1');">
						<input type='button' class='uk-button uk-button-default uk-button-small' <%=disabled%> id='Previous' name='Previous' value='Previous' onclick="submitformpage('<%=(currentpage - 1)%>');">
<%
		int startpage = 1;	
		if (numberofpage > 5) {
			startpage = currentpage - 2;
			if (startpage <= 0)
				startpage = 1;
			int offset = numberofpage - startpage;
			if (offset < 6) {
				startpage = numberofpage - 6;
			}
		}
	
		for (int i = startpage; i <= numberofpage && i <= (startpage + 5); i++) {
			if (i == currentpage) {
%>
		<input type='button' class='uk-button uk-button-default uk-button-small select' disabled id='<%=i%>' name='<%=i%>'  value='<%=i%>' >
<%
			} else {
%>
		<input type='button' class='uk-button uk-button-default uk-button-small'  id='<%=i%>' name='<%=i%>' value='<%=i%>' onclick="submitformpage('<%=i%>');">
<%
			}
		}
	
		if (numberofpage > 5) {
			if (startpage < (numberofpage - 6)) {
%>
				...
<%
			}
%>
			<input type='button' class='uk-button uk-button-default uk-button-small'  id='<%=numberofpage%>' name='<%=numberofpage%>' value='<%=numberofpage%>' onclick="submitformpage('<%=numberofpage%>');">
<%
		}
		disabled = "";
		if (currentpage == numberofpage) {
			disabled = " disabled='true'";
		}
%>
		<input type='button' class='uk-button uk-button-default uk-button-small' <%=disabled%> id='Next' name='Next' value='Next' onclick="submitformpage('<%=(currentpage + 1)%>');">
		<input type='button' class='uk-button uk-button-default uk-button-small' <%=disabled%> id='Last' name='Last' value='Last' onclick="submitformpage('<%=numberofpage%>');">
<%
	} catch (Exception e) {
		log.log(Level.WARN, "[" + session.getId() + "]", e);
	}

%>
			</span></td>	
			</tr>
		</table>
	</div>
</div>

<div id="call_search" uk-modal bg-close=false>
   		<div class="uk-modal-dialog uk-modal-body "  class='uk-align-center uk-transition-fade '  style='width: fit-content;border-radius: 4px' >		      
    		<div uk-spinner>Ricerca in corso...</div>
	    </div>
	</div>

<div id="call_down" uk-modal="bg-close: false; esc-close: false;">
    <div class="uk-modal-dialog uk-modal-body uk-text-center" style="width: fit-content; border-radius: 4px">
        <div uk-spinner></div>
        <div class="uk-margin-small-top">Download in corso...</div>
    </div>
</div>

<!-- <form id="form_down_call" action="PlayAudio" method="post" class="uk-hidden"> -->
<!--     <input type="hidden" name="action" id="action" value="DOWNLOAD"> -->
<!--     <input type="hidden" id="file_down" name="file" value=""> -->
<!-- </form> -->
<iframe name="hidden_download_frame" id="hidden_download_frame" style="display:none;"></iframe>

<form id="form_down_call" action="PlayAudio" method="post" class="uk-hidden" target="hidden_download_frame">
    <input type="hidden" name="action" id="action" value="DOWNLOAD">
    <input type="hidden" id="file_down" name="file" value="">
</form>
	
<script type="text/javascript">
UIkit.modal(document.getElementById('modal-attesa-audio')).hide();

submitformpage = function (page) {
	document.getElementById("currentpage").value = page; 
	submitform();
}

submitform = function () {
	UIkit.modal(document.getElementById("call_search")).show();
	document.getElementById("callList").submit();
}

function orderColumn(order_column){
	document.getElementById("order_column").value = order_column;
	submitform("CallList.jsp");
}



function play(rowId, file){
	document.querySelectorAll("tr").forEach(row => row.classList.remove("highlight"));
    const row = document.getElementById(rowId);
    if (row) row.classList.add("highlight");
    var audioControl = document.getElementById("control");
    var audioPlayer = document.getElementById("_player");

    UIkit.modal('#modal-attesa-audio').show();

//     var url = "/SearchAndPlay/playAudio.jsp?action=play&file=" + encodeURIComponent(file);
    var url = "/SearchAndPlay/PlayAudio?action=PLAY&file=" + encodeURIComponent(file);
    audioControl.src = url;
    audioPlayer.src = url;
    audioControl.load();
    
    audioControl.oncanplay = function() {
        UIkit.modal('#modal-attesa-audio').hide();
    };
    
    audioControl.onerror = function() {
        UIkit.modal('#modal-attesa-audio').hide();
        UIkit.modal.alert('Errore durante il caricamento o l\'elaborazione dell\'audio.');
    };
    
    audioControl.play().catch(e => {
        UIkit.modal.alert('Riproduzione automatica bloccata o errore.').then(function() {
        });
        return;
    });
    addAudit(file, "PLAY");
}


// function executeDonwload(){
// 	var fileValue = document.getElementById("file_down").value;
// 	fileValue = document.getElementById("select").value
     
// 	if (!fileValue || fileValue === "" || fileValue === "null") {
//         UIkit.modal.alert('Attenzione: Seleziona almeno una riga dalla tabella prima di procedere con il download.').then(function() {
//         });
//         return;
//     }
// 	UIkit.modal("#call_down").show();
//     document.getElementById("form_down_call").submit();
//     setTimeout(function() {
//         UIkit.modal("#call_down").hide();
//     }, 3000);
// 	addAudit(fileValue,"DOWNLOAD");
// }

function executeDonwload(){
    var selectedFiles = document.getElementById("select").value;
     
    if (!selectedFiles || selectedFiles === "" || selectedFiles === "null") {
        UIkit.modal.alert('Attenzione: Seleziona almeno una riga dalla tabella prima di procedere.');
        return;
    }

    document.getElementById("file_down").value = selectedFiles;

    
    UIkit.modal("#call_down").show();


    document.getElementById("form_down_call").submit();

    setTimeout(function() {
        UIkit.modal("#call_down").hide();
    }, 5000);

    addAudit(selectedFiles, "DOWNLOAD");
}

// ------------------------------------------------------------------
addAudit = function (file, action) {
	const xhr = new XMLHttpRequest();
	xhr.open('POST', 'audit.jsp', false);
	xhr.setRequestHeader('Content-Type', 'application/json');
	const jsonPayload = JSON.stringify({ file: file, action: action});
	xhr.send(jsonPayload);
	if (xhr.status === 200) {
		var responseJson = JSON.parse(xhr.responseText)
		if (responseJson.result!="OK") {
			toastDanger('&nbsp&nbsp&nbspErrore Audit:&nbsp&nbsp&nbsp'+responseJson.description+'&nbsp&nbsp&nbsp');
		} else {
		}
	} else {
		toastDanger('&nbsp&nbsp&nbspErrore Audit:&nbsp&nbsp&nbspHTTPStatus '+xhr.status+'&nbsp&nbsp&nbsp');
	}
}
// ------------------------------------------------------------------

changeTo = function(event, obj, key){
	event.preventDefault();
	
var inp = obj.getElementsByTagName("input")[0];

if (document.getElementById("select").value.includes(key)){
	document.getElementById("select").value = document.getElementById("select").value.replace(key+",",'');
	inp.checked = false;
	return false;
}
	
document.getElementById("select").value =  document.getElementById("select").value+key+",";
inp.checked = true;

return false;
}

class resizerElement{
	constructor(target, resizer, k){
		this.target=target;
		this.resizer=resizer;
		this.k=k;
		if (this.resizer!=null)
			this.resizer.onmousedown = this.onmousedown.bind(this);
	}
	onmousedown = function(e){
		this.start= e.clientX;
		this.mm =  this.mouseMoveHandler.bind(this);
		this.mu= this.mouseUpHandler.bind(this);
	    document.addEventListener('mousemove', this.mm);
        document.addEventListener('mouseup', this.mu);
	}
	mouseMoveHandler=function(e){
		var offset =  e.clientX - this.start
		this.target.style.width =(this.target.clientWidth + offset)+"px";
		this.start = e.clientX;
	}
	mouseUpHandler=function(e){
		document.removeEventListener('mousemove', this.mm);
		document.removeEventListener('mouseup', this.mu);
	}
}

class resizerContainer {
	constructor(tableId){
		try {
			this.re=new Array();
			this.tabel = document.getElementById(tableId);
							
			this.theader = this.tabel.getElementsByTagName("thead")[0];
			this.firstRow=this.theader.getElementsByTagName("tr")[0];
			var allTDs = this.firstRow.getElementsByTagName('th');
			var k=0;
			for (var i=0; i<allTDs.length-1; i=i+2) {
				this.re[k]=new resizerElement(allTDs[i],allTDs[i+1],k);
				k++;
			}
			this.re[k]=new resizerElement(allTDs[allTDs.length-1],null);
		} catch (e) {
			console.log("resizerContainer-->"+e);
		}
	}
}
document.addEventListener("DOMContentLoaded", function() {
	new resizerContainer("resizeMe");
}); 

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




// var prev_idrow = "";
// toogleRow = function(_idrow) {
// 	if (prev_idrow!="" && prev_idrow!=_idrow) {
// 		document.getElementById("rowid_"+prev_idrow).classList.remove('rowselected');
// 	}
// 	document.getElementById("rowid_"+_idrow).classList.add('rowselected');
// 	prev_idrow = _idrow;
// }



</script>