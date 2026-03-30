package comapp.cloud;

import java.util.Calendar;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.servlet.Servlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;

@WebServlet(
		urlPatterns = "/Elab",
		loadOnStartup = 100
	)

public class ElabServlet extends HttpServlet implements Servlet {
	private static final long serialVersionUID = 1L;
	private ScheduledExecutorService scheduledExecutor = null;
	private ScheduledFuture<?> scheduleManager;
	private Elab elab = null;
	private String TimingStartAt = "00:00";

	
	Logger log = Logger.getLogger("comapp");

	public void init() throws ServletException {
		try {
			log.info("[ELAB] Scheduler started | daily run at " + TimingStartAt);

			elab = new Elab(this);
			scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
			scheduleManager = scheduledExecutor.scheduleAtFixedRate(elab, millisToStartDay(), 24 * 60 * 60 * 1000, TimeUnit.MILLISECONDS);

		} catch (Exception e) {
			log.log(Level.SEVERE,"init() Exception: "+e.getMessage(), e);
		}
	}
	public void destroy() {
	    if (scheduleManager != null) {
	    	scheduleManager.cancel(true);
	    }
	    if (scheduledExecutor != null && !scheduledExecutor.isShutdown()) {
	    	scheduledExecutor.shutdownNow();
	    	try {
	    		if (!scheduledExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
	    			log.warning("END-> ScheduledExecutorService did not terminate within 5 seconds.");
	    		}
	    	} catch (InterruptedException ie) {
	    		scheduledExecutor.shutdownNow();
	    		Thread.currentThread().interrupt();
	    	}
	    }
	    log.info("[ELAB] Scheduler stopped | all resources released.");
	}
		
	private long millisToStartDay() {
		int seconds = 0;
		Calendar cal = Calendar.getInstance();
		Calendar cal_start = Calendar.getInstance();
		cal_start.set(Calendar.HOUR_OF_DAY, Integer.parseInt(TimingStartAt.split(":")[0]));
		cal_start.set(Calendar.MINUTE, Integer.parseInt(TimingStartAt.split(":")[1]));
		cal_start.set(Calendar.SECOND, 0);
		if (cal.before(cal_start)) {
			seconds = (int)((cal_start.getTimeInMillis()-cal.getTimeInMillis())/1000);
		} else {
			cal_start.add(Calendar.DAY_OF_YEAR, 1);
			seconds = (int)((cal_start.getTimeInMillis()-cal.getTimeInMillis())/1000);
		}
		log.fine("INI-> Day Start run in hour:minutes:seconds: " + ((int)(seconds/3600)) + ":" + ((int) ((seconds/60) % 60)) + ":" + ((int)(seconds % 60)));
		return seconds * 1000;
	}
	
	private long millisToNextDay() {
		int seconds = 0;
		Calendar cal = Calendar.getInstance();
		Calendar cal_start = Calendar.getInstance();
		cal_start.set(Calendar.HOUR_OF_DAY, Integer.parseInt(TimingStartAt.split(":")[0]));
		cal_start.set(Calendar.MINUTE, Integer.parseInt(TimingStartAt.split(":")[1]));
		cal_start.set(Calendar.SECOND, 0);
		cal_start.add(Calendar.DAY_OF_YEAR, 1);
		seconds = (int)(cal_start.getTimeInMillis()-cal.getTimeInMillis())/1000;
		log.fine("MOD-> Day Next run in hour:minutes:seconds: " + ((int)(seconds/3600)) + ":" + ((int) ((seconds/60) % 60)) + ":" + ((int)(seconds % 60)));
		return seconds * 1000;
	}
	
	public void nextExecutionDay() {
	    if (scheduleManager!= null) {
	    	scheduleManager.cancel(true);
	    }
	    scheduleManager = scheduledExecutor.scheduleAtFixedRate(elab, millisToNextDay(), 24 * 60 * 60 * 1000, TimeUnit.MILLISECONDS);
	}
}
