<%@page import="comapp.ConfigServlet"%>
<%@page import="java.util.logging.Logger"%>
<%@page import="comapp.cloud.CSVReport"%>
<%@page import="org.apache.commons.lang3.StringUtils"%>
<%@page import="org.json.JSONObject"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="ISO-8859-1">
<title>Logout</title>
</head>
<body>
<%
	Logger log = Logger.getLogger(ConfigServlet.web_app);
	log.info("[" + session.getId() + "]############## Login out #####################");
	JSONObject user = (JSONObject) session.getAttribute("gui_user");
	session.setAttribute("access", "logout");
	CSVReport.aggiungiLog(user.getString("name"), "Logout", "");
	session.invalidate();
	response.sendRedirect("index.jsp");
%>

</body>
</html>