package comapp.cloud;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import comapp.ConfigServlet;

public class CSVReport {
	static Properties cs = ConfigServlet.getProperties();
	static String FOLDER_PATH = cs.getProperty("report");
	static Logger log = Logger.getLogger("comapp");

	public static void aggiungiLog(String username, String azione, String dettagli) {
        String fileName = FOLDER_PATH + "Report_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".csv";
        File file = new File(fileName);
       
        try {
            // Creare la directory dei log se non esiste
            File folder = new File(FOLDER_PATH);
            if (!folder.exists()) folder.mkdirs();

            // Validare input
            if (!isInputValido(username, azione)) {
                log.log(Level.WARNING, "Dati non validi: Username o Azione mancante");
                return;
            }

            // Creare il file CSV se non esiste e aggiungere l'intestazione
            if (!file.exists()) {
                try (FileWriter writer = new FileWriter(file, true)) {
                    writer.write("Username;Timestamp;Azione;Dettagli\n");
                }
            }

            // Aggiungi la nuova riga al file
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
            	 String nuovaRiga = null;
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                if(dettagli.isBlank())
                     nuovaRiga = String.format("%s;%s;%s;%s", username, timestamp.toString(), azione, "");
                else
                	nuovaRiga = String.format("%s;%s;%s;%s", username, timestamp.toString(), azione, dettagli);
 
                writer.write(nuovaRiga);
                writer.newLine(); // Andare a capo
            }

        } catch (IOException e) {
            System.err.println("Errore durante la scrittura nel file CSV: " + e.getMessage());
        }
    }

	// Validiamo l'input
	private static boolean isInputValido(String username, String azione) {
        return username != null && !username.trim().isEmpty() &&
               azione != null && !azione.trim().isEmpty();
    }

}
