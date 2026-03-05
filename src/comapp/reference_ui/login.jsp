<%@page import="java.sql.Statement"%>
<%@page import="java.sql.SQLException"%>
<%@page import="javax.sql.DataSource"%>
<%@page import="javax.naming.InitialContext"%>
<%@page import="javax.naming.Context"%>
<%@page import="java.sql.ResultSet"%>
<%@page import="java.sql.Connection"%>
<%@page import="org.apache.logging.log4j.Level"%>
<%@page import="comapp.cloud.GenesysUser"%>
<%@page import="java.util.Enumeration"%>
<%@page import="comapp.ConfigServlet"%>
<%@page import="java.util.Properties"%>
<%@page import="org.apache.commons.lang3.StringUtils"%>
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
<body>
<%
	Logger log = LogManager.getLogger("comapp.Genesys" + this.getClass()); 
	String code = request.getParameter("code");
	TrackId trackId = new TrackId(session.getId(), code);
	log.info("{} ----------------------- login --------------------------", trackId);
	
	log.info("{} start agent login " + (StringUtils.isBlank(code) ?" step 1" : " step 2") + " ************************************", trackId);
	Connection connection = null;
	Statement stmt = null;
	ResultSet rs = null;
	try{
		Properties cs = ConfigServlet.getProperties();

		String gui_clientId = null;
		String gui_clientSecret = null;
		String urlRegion = null;
		String get_client_credential = cs.getProperty("sql.clientcredential", "");
		log.info("{} get_client_credential query = {}", trackId, get_client_credential);
		
		try{

			Context ctx = new InitialContext();
			log.log(Level.DEBUG, trackId+"get connection: java:comp/env/" + ConfigServlet.prefix + ConfigServlet.web_app);
			DataSource ds = (DataSource) ctx.lookup("java:comp/env/" + ConfigServlet.prefix + ConfigServlet.web_app);
			connection = ds.getConnection();
			stmt = connection.createStatement();
			rs = stmt.executeQuery(get_client_credential);
			while(rs.next()){
				gui_clientId = rs.getString("clientid");
				gui_clientSecret = rs.getString("clientsecret");
				urlRegion = rs.getString("region");
			}
			
		}catch(SQLException sqle){
			log.log(Level.WARN, "{} Errore durante la connessione con il database ", trackId);
		}finally{
			if(rs!=null) rs.close();
			if(stmt!=null) stmt.close();
			if(connection!=null) connection.close();
		}
		String redirect_uri = cs.getProperty("redirect_uri", "");
		boolean enabled_security = Boolean.parseBoolean(cs.getProperty("enabled_security", "true"));
		String urlAuthorizeString = cs.getProperty("urlAuthorizeString", "https://login.mypurecloud.de/oauth/authorize");
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
		if(!enabled_security){
			log.info("{} security is disabled ", trackId);
			response.sendRedirect("main.jsp");
			return;
		}
		session.setAttribute("gui_clientId", gui_clientId);
		session.setAttribute("gui_clientSecret", gui_clientSecret);
		session.setAttribute("urlRegion", urlRegion);
		
		if(StringUtils.isBlank(code)){
			String url_redirect = urlAuthorizeString + "?client_id=" + gui_clientId + "&response_type=code&redirect_uri=" + redirect_uri;
// 			log.info("{} login redirect to Genesys : " + url_redirect, trackId);
			response.sendRedirect(url_redirect);
			return;
		}
		
	}catch(Exception ex){
		log.log(Level.WARN, "", ex);
	}finally{
		log.info("{} login closed", trackId);
	}
	
	
	%>
	
</body>
</html>