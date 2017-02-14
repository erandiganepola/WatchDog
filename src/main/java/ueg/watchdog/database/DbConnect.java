package ueg.watchdog.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Class to create database connection
 *
 * @author erandi
 */
public class DbConnect {

    private static final Logger logger = LoggerFactory.getLogger(DbConnect.class);
    private static Connection conn;

    public static Connection getDBConnection() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/watchdog", "root", "root");
        } catch (ClassNotFoundException | SQLException ex) {
            logger.error("Error when creating DB connection", ex);
        }
        return conn;
    }
}
