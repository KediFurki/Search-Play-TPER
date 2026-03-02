package comapp;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;


import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;



public class ConfigServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	public static String version = "1.1.0";
	public static String ConfigLocation;
	public static Logger log = Logger.getLogger("comapp." + ConfigServlet.class.getName());
	public static String web_app = "";	
	public ConfigServlet() {
		super();
	}

	@Override
	public void init(ServletConfig config) throws ServletException {		
		try {
			Properties prop = new Properties();
			prop.load(config.getServletContext().getResourceAsStream("/META-INF/MANIFEST.MF"));
			String _version = prop.getProperty("Implementation-Version");
			if (StringUtils.isNotBlank(_version)) {
				version = _version;
			}
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		ConfigLocation = config.getInitParameter("config-properties-location") + "/"+ config.getServletContext().getContextPath() + ".properties";
		web_app = config.getServletContext().getContextPath().replaceAll("/", "");
		Properties filecs = new Properties();
		try {
			filecs = getProperties();
			String Log4jLocation = filecs.getProperty("java-logging-properties-location");
			if (Log4jLocation != null) {
			
				File file = new File(Log4jLocation);
				if (file.exists()) {
					InputStream sti = new FileInputStream(file);
					LogManager.getLogManager().readConfiguration(sti);
					sti.close();
				} 
			}
			log = Logger.getLogger("comapp");
			
			
			log.info("************** Start: " + web_app + " " + version + " *******************");
			
							
		} catch (Exception e) {
			try {
				log.log(Level.WARNING,"config", e);
			} catch (Exception ee) {}
			e.printStackTrace();
		}
		super.init(config);
	}	

	public static Properties getProperties() {
		try {
			Properties cs = new Properties();
			log.info("conf file:" + ConfigServlet.ConfigLocation);
			FileInputStream is = new FileInputStream(new File(ConfigServlet.ConfigLocation));
			cs.load(is);
			is.close();
			return cs;
		} catch (Exception e) {
			log.log(Level.SEVERE, "Config Servlet Exception : ", e);
			return null;
		}
	}
	
	
	
	public static String quote(String value) {
		return ""+((value!=null) ? StringUtils.normalizeSpace(value).replaceAll("\"", "'").replaceAll("\\\\", "\\\\\\\\") : "")+"";
	}
	
}
