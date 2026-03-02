package comapp.cloud;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import comapp.ConfigServlet;
import comapp.cloud.Genesys.AudioType;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/DownloadAudio")


public class DownloadAudio extends HttpServlet {
	private static final long serialVersionUID = 1L;
	static Logger log = Logger.getLogger("comapp");
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

//        Properties cs = ConfigServlet.getProperties();
//        String downloadPath = cs.getProperty("downloadPath");
    	JSONObject user = (JSONObject) request.getSession().getAttribute("gui_user");
    	String conversationId = request.getParameter("conversationId");
    	if (conversationId == null || conversationId.isEmpty()) {
    	    log.warning("conversationId non ricevuto o vuoto.");
    	    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Il parametro conversationId è mancante.");
    	    return;
    	}
    	log.info("[" + request.getRequestedSessionId() + "] conversationId = " + conversationId);
//    	log.info("[" + request.getRequestedSessionId() + "] downloadPath = " + downloadPath);
    	
    	response.setContentType("audio/mpeg");
        response.setHeader("Content-Disposition", "attachment; filename=\""+conversationId+".wav\"");
    	
    	GenesysUser guser = (GenesysUser)request.getSession().getAttribute("guser");
    	
    	JSONArray recorderList = Genesys.getRecorderList(guser, conversationId, Genesys.AudioType.WAV);
    	String urlAudio = null;
    	InputStream inputStream = null;
    	try {
			urlAudio = Genesys.getAudioUrl(guser, recorderList.getJSONObject(0), AudioType.WAV, null, null, null);
			URI uri = new URI(urlAudio);
			URL url = uri.toURL();
			inputStream = url.openStream();   
			OutputStream outputStream = response.getOutputStream();
			byte[] buffer = new byte[1024];
		    int bytesRead;
		    while ((bytesRead = inputStream.read(buffer)) != -1) {
		        outputStream.write(buffer, 0, bytesRead);
		    }
		    String dettagli = "conversationId: " + conversationId;
			CSVReport.aggiungiLog(user.getString("name"), "Scarica", dettagli);
		} catch (Exception e) {
			e.printStackTrace();
			
		}finally {
		    if (inputStream != null) {
		        inputStream.close();
		    }
		}
        
//        
//        InputStream inputStream = null;
//        try {
//            inputStream = new URL(fileUrl).openStream();
//             OutputStream outputStream = response.getOutputStream() ;
//             
//            byte[] buffer = new byte[1024];
//            int bytesRead;
//            while ((bytesRead = inputStream.read(buffer)) != -1) {
//                outputStream.write(buffer, 0, bytesRead);
//            }
//        }finally {
//            if (inputStream != null) {
//                inputStream.close();
//            }
//        }
    }
}
