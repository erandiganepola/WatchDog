package ueg.watchdog.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ueg.watchdog.database.DbConnect;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Model class to run the logic for adding a new user
 *
 * @author erandi
 */
public class UserAccount {

    private static final Logger logger = LoggerFactory.getLogger(UserAccount.class);

    private final String username;
    private final String password;

    public UserAccount(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return this.username;
    }

    public String getPassword() {
        return this.password;
    }

    public static AccountStatus createUserAccount(UserAccount account) {
        try {
            Connection conn = DbConnect.getDBConnection();
            PreparedStatement statement
                    = conn.prepareStatement("SELECT count(*) FROM `user` WHERE `username`=?");
            statement.setString(1, account.getUsername());
            ResultSet rs = statement.executeQuery();
            /*
             * Advance to the next result. If a result exists, check whether the
             * first column's value (count(*)) is greater than 0. If yes,
             * username is already taken.
             */
            if (rs.next() && rs.getInt(1) > 0) {
                return AccountStatus.ALREADY_EXISTS;
            }

            PreparedStatement pst = conn
                    .prepareStatement("INSERT INTO `user`(`username`, `password`) VALUES (?,?)");
            pst.setString(1, account.getUsername());
            pst.setString(2, account.getPassword());
            pst.executeUpdate();
            conn.close();
            return AccountStatus.CREATED;
        } catch (SQLException e) {
            logger.error("Error when creating user account", e);
            return AccountStatus.FAILED;
        }
    }

    /**
     * The account creation status. This is an enumeration. Accessed in a way
     * similar to an inner class
     */
    public enum AccountStatus {
        CREATED,
        ALREADY_EXISTS,
        FAILED
    }
}
