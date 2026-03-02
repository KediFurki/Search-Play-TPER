package comapp.cloud;

import java.io.File;
import java.io.FileWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import comapp.ConfigServlet;

public class Elab implements Runnable {
	Logger log = Logger.getLogger("comapp");
	private ElabServlet elabServlet = null;
	
	public Elab(ElabServlet eServlet) {
		elabServlet = eServlet;
	}

	public void run() {
		log.info("################################################");
		log.info("START    - Elab ServLet");
		log.info("################################################");
		try {
			Properties cs = ConfigServlet.getProperties();
			String FOLDER_PATH = cs.getProperty("report");
			String fileName = FOLDER_PATH + "Report_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".csv";
			log.info("START    - Elab ServLet fileName = " + fileName);
			File file = new File(fileName);
			try {
			    File folder = new File(FOLDER_PATH);
			    if (!folder.exists()) folder.mkdirs();

			    if (!file.exists()) {
			        try (FileWriter writer = new FileWriter(file)) {
			            writer.write("Username;Timestamp;Azione\n");
			        }
			    }
			}catch (Exception e){
				e.printStackTrace();
			}
			
			
		} catch (Throwable e) {
			log.log(Level.SEVERE,"Elab() Exception: "+e.getMessage(), e);
		} finally {
		}		
		elabServlet.nextExecutionDay();
		log.info("################################################");
		log.info("STOP     - Elab ServLet");
		log.info("################################################");
	}
	
	
	
	
	
	
}