package ueg.watchdog.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ueg.watchdog.core.configuration.WatchDogContext;
import ueg.watchdog.database.DbConnect;
import ueg.watchdog.model.UserAccount;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static ueg.watchdog.core.configuration.WatchDogContext.*;

/**
 * A utility class to save settings related to watch dog.
 *
 * @author Erandi Ganepola
 */
public class SettingsHandler {

    private static final Logger logger = LoggerFactory.getLogger(UserAccount.class);


    private static int configId = 1;

    public static boolean saveSettings(WatchDogContext wdc) {
        OperatingMode operatingMode = wdc.getOperatingMode();
        String storagePath = wdc.getStoragePath();
        String compressionMode = wdc.getCompressionMode();

        try {
            Connection conn = DbConnect.getDBConnection();

            PreparedStatement pst = conn.prepareStatement("UPDATE `config` SET `operating_mode`=?,`storage_location`=?, `compression_mode`=? WHERE `id`=?");
            pst.setInt(1, operatingMode.ordinal());
            pst.setString(2, storagePath);
            pst.setString(3, compressionMode);
            pst.setInt(4, configId);
            pst.executeUpdate();

            conn.close();
            return true;
        } catch (SQLException e) {
            logger.error("Error when saving settings", e);
            return false;
        }
    }

    public static WatchDogContext getSettings() {
        WatchDogContext wdc = WatchDogContext.getInstance();
        try {
            Connection conn = DbConnect.getDBConnection();
            PreparedStatement pst = conn.prepareStatement("SELECT `operating_mode`, `storage_location`, `compression_mode` FROM `config` WHERE `id`=?;");
            pst.setInt(1, configId);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                wdc.setOperatingMode(OperatingMode.values()[rs.getInt(1)]);
                wdc.setStoragePath(rs.getString(2));
                wdc.setCompressionMode(rs.getString(3));
            }
            conn.close();
            return wdc;
        } catch (SQLException e) {
            logger.error("Error when fetching settings", e);
            return null;
        }
    }
}
