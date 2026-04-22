package comapp.cloud;

import java.io.IOException;
import java.util.logging.Logger;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


@WebServlet("/DownloadAudio")
public class DownloadAudio extends HttpServlet {
	private static final long serialVersionUID = 1L;
	static Logger log = Logger.getLogger("comapp");

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String sessionId = (request.getSession(false) != null) ? request.getSession(false).getId() : "NO_SESSION";
		log.warning("[" + sessionId + "] DownloadAudio - Audio download attempt BLOCKED. "
				+ "conversationId=" + request.getParameter("conversationId"));
		response.sendError(HttpServletResponse.SC_FORBIDDEN, "Audio download is not permitted.");
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}
}