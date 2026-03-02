<%@page import="java.util.logging.Logger"%>

<%@page import="comapp.ConfigServlet"%>
<%@page import="java.util.Properties"%>
<%@page import="java.util.Hashtable"%>
<%@page import="comapp.cloud.GenesysUser"%>

<%@page import="org.json.JSONArray"%>
<%@page import="java.util.Enumeration"%>
<%@page import="comapp.cloud.Genesys"%>
<%@page import="java.net.URLEncoder"%>
<%@page import="java.util.Base64"%>
<%@page import="java.util.logging.Level"%>
<%@page import="org.json.JSONObject"%>
<%@page import="java.nio.charset.StandardCharsets"%>
<%@page import="org.apache.commons.io.IOUtils"%>
<%@page import="org.apache.http.client.methods.CloseableHttpResponse"%>
<%@page import="org.apache.http.client.entity.UrlEncodedFormEntity"%>
<%@page import="java.util.ArrayList"%>
<%@page import="org.apache.http.message.BasicNameValuePair"%>
<%@page import="java.util.List"%>
<%@page import="org.apache.http.client.methods.HttpPost"%>
<%@page import="org.apache.http.impl.client.HttpClientBuilder"%>
<%@page import="org.apache.http.impl.client.CloseableHttpClient"%>
<%@page import="java.util.Calendar"%>
<%@page import="org.apache.http.client.config.RequestConfig"%>

<%@page import="org.apache.commons.lang3.StringUtils"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="ISO-8859-1">
<link rel="icon" type="image/png" href="img/comapp.png">
<title>Login</title>
</head>
<body>
	<%
	//5386925e-55f4-4f69-aff3-60b0cb6e06aa
		Logger log = Logger.getLogger("comapp." + this.getClass());
		String code = request.getParameter("code");
		log.info("[ "+ session.getId() +"]##################### index start #####################");
		log.info("[" + session.getId() + "] start agent login "+(StringUtils.isBlank(code)?" step 1":" step 2")+" *********************");
		session.setAttribute("access", "first_access");
		try {    
			Properties cs = ConfigServlet.getProperties();			
			String gui_clientId = cs.getProperty("gui_clientId", "");//"5386925e-55f4-4f69-aff3-60b0cb6e06aa";
			String gui_clientSecret = cs.getProperty("gui_clientSecret", "");//"G85_lwng7DkPQghh2jKPPz0XZFI6zHCs0kx7C5AjGgw";
			String urlRegion =  cs.getProperty("urlRegion","");
			String redirect_uri = cs.getProperty("redirect_uri","");
			boolean enabled_security =  Boolean.parseBoolean(cs.getProperty("enabled_security", "true"));			
			String urlAuthorizeString = cs.getProperty("urlAuthorizeString","https://login.mypurecloud.ie/oauth/authorize");			
			Enumeration<String> es = request.getHeaderNames();
			log.info("[" + session.getId() + "]  header ******* " );
			while (es.hasMoreElements()) {
				String key = es.nextElement();
				String value = request.getHeader(key);
				log.info("[" + session.getId() + "]  header key: " + key + " value:" + value);
			}
			Enumeration<String> enume = request.getParameterNames();
			log.info("[" + session.getId() + "]  parameter ******* " );
			while (enume.hasMoreElements()) {
				String key = enume.nextElement();
				String value = request.getParameter(key);
				log.info("[" + session.getId() + "]  parameter key:" + key + " value:" + value);
			}
			if (!enabled_security){
				log.info("security is not enabled ");
				response.sendRedirect("step2.jsp");
				return;
			}
			GenesysUser guser = null;			
			if (StringUtils.isBlank(code)) {
				// Let's try to initialize guser here
				
				
				String url_redirect =  urlAuthorizeString + "?client_id=" + gui_clientId + "&response_type=code&redirect_uri=" + redirect_uri;
				log.info("[" + session.getId() + "] index redirect to Genesys:" +url_redirect);
				response.sendRedirect(url_redirect);
				return;
			}

		} catch (Exception e) {
			log.log(Level.WARNING, "", e);
		} finally {
			out.print("</body>	</html>");
		}
	%>