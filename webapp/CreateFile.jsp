<%@page import="java.io.FileWriter"%>
<%@page import="java.io.File"%>
<%@page import="java.time.format.DateTimeFormatter"%>
<%@page import="java.time.LocalDateTime"%>
<%@page import="java.util.Properties"%>
<%@page import="comapp.ConfigServlet"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="ISO-8859-1">
<title>Create File</title>
</head>
<body>
<%
Properties cs = ConfigServlet.getProperties();
String FOLDER_PATH = cs.getProperty("report");
String fileName = FOLDER_PATH + "Report_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".csv";

File file = new File(fileName);
try {
    File folder = new File(FOLDER_PATH);
    if (!folder.exists()) folder.mkdirs();

    if (!file.exists()) {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("Username;Timestamp;Azione;Dettagli\n");
        }
    }
}catch (Exception e){
	e.printStackTrace();
}

%>
</body>
</html>