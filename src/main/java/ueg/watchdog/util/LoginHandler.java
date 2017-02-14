package ueg.watchdog.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ueg.watchdog.database.DbConnect;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Model class to check user credentials
 *
 * @author erandi
 */
public class LoginHandler {

    private static final Logger logger = LoggerFactory.getLogger(LoginHandler.class);

    private LoginHandler() {
    }

    public static boolean checkCredentials(String username, String password) {
        Connection conn = DbConnect.getDBConnection();
        try {
            PreparedStatement pst = conn.prepareStatement("SELECT `username` , `password` FROM `user` WHERE `username` = ? AND `password` = ?");
            pst.setString(1, username);
            pst.setString(2, password);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                return true;
            } else {
                return false;
            }
        } catch (SQLException e) {
            logger.error("Error occurred when checking credentials", e);
            return false;
        } finally {
            try { conn.close();} catch (SQLException ignored) {}
        }
    }
}
