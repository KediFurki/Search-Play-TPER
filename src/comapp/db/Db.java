package comapp.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import comapp.ConfigServlet;

public class Db {

    private static final Logger log = Logger.getLogger("comapp");

    private static volatile DataSource ds;

    private static DataSource lookupDataSource() throws SQLException {
        if (ds != null) return ds;

        synchronized (Db.class) {
            if (ds != null) return ds;

            try {
                Properties props = ConfigServlet.getProperties();
                String jndiName = (props != null)
                        ? props.getProperty("jndi.name", "jdbc/SP_Lite")
                        : "jdbc/SP_Lite";
                log.info("Db.lookupDataSource() - Looking up JNDI name: " + jndiName);

                InitialContext ic = new InitialContext();
                Context env = (Context) ic.lookup("java:comp/env");
                ds = (DataSource) env.lookup(jndiName);
                log.info("Db.lookupDataSource() - DataSource '" + jndiName + "' obtained from JNDI successfully.");
                return ds;
            } catch (NamingException e) {
                log.log(Level.SEVERE, "Db.lookupDataSource() - JNDI lookup failed: " + e.getMessage(), e);
                throw new SQLException("Cannot get DataSource from JNDI", e);
            } catch (Exception e) {
                log.log(Level.SEVERE, "Db.lookupDataSource() - General DataSource lookup error", e);
                throw new SQLException("Cannot get DataSource", e);
            }
        }
    }
    public static Connection open() throws SQLException {
        DataSource dataSource = lookupDataSource();
        Connection cn = dataSource.getConnection();
        log.fine("Db.open() - Got connection from pool: " + cn);
        return cn;
    }
}