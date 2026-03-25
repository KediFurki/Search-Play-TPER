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
		long runStart = System.currentTimeMillis();
		String startTimestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
		log.info("\n" +
			"┌──────────────────────────────────────────────────────────────┐\n" +
			"│          Elab Daily Task - RUNNING                          │\n" +
			"│  Start Time: " + padRight(startTimestamp, 46) + "│\n" +
			"└──────────────────────────────────────────────────────────────┘");
		try {
			Properties cs = ConfigServlet.getProperties();
			String FOLDER_PATH = cs.getProperty("report");
			String fileName = FOLDER_PATH + "Report_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".csv";
			log.info("Elab.run() - Report file: " + fileName);
			File file = new File(fileName);
			try {
			    File folder = new File(FOLDER_PATH);
			    if (!folder.exists()) {
			    	folder.mkdirs();
			    	log.info("Elab.run() - Report folder created: " + FOLDER_PATH);
			    }

			    if (!file.exists()) {
			        try (FileWriter writer = new FileWriter(file)) {
			            writer.write("Username;Timestamp;Azione\n");
			        }
			        log.info("Elab.run() - New report file created: " + fileName);
			    } else {
			    	log.info("Elab.run() - Report file already exists: " + fileName);
			    }
			} catch (Exception e) {
				log.log(Level.SEVERE, "Elab.run() - File operation error: " + e.getMessage(), e);
			}
			
			
		} catch (Throwable e) {
			log.log(Level.SEVERE, "Elab.run() Exception: " + e.getMessage(), e);
		} finally {
		}		
		elabServlet.nextExecutionDay();
		long elapsed = System.currentTimeMillis() - runStart;
		String endTimestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
		log.info("\n" +
			"┌──────────────────────────────────────────────────────────────┐\n" +
			"│          Elab Daily Task - COMPLETED                        │\n" +
			"│  End Time  : " + padRight(endTimestamp, 46) + "│\n" +
			"│  Duration  : " + padRight(elapsed + " ms", 46) + "│\n" +
			"└──────────────────────────────────────────────────────────────┘");
	}
	
	private static String padRight(String text, int length) {
		if (text == null) text = "N/A";
		if (text.length() > length) {
			text = text.substring(0, length - 3) + "...";
		}
		return String.format("%-" + length + "s", text);
	}
}