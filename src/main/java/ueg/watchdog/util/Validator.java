package ueg.watchdog.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ueg.watchdog.database.DbConnect;
import ueg.watchdog.model.UserAccount;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * A utility class for user input validation methods
 *
 * @author erandi
 */
public class Validator {

    private static final Logger logger = LoggerFactory.getLogger(UserAccount.class);

    private final static String DATE_FORMAT = "yyyy-MM-dd";
    private final static String PHONE_REGEX = "\\+\\d{2}(-\\d{9})";
    private final static String ID_REGEX = "[A-Z]{2}\\d{4}";

    public static boolean validateInputs(String input, int num) {
        switch (num) {
            case 0:
                if (validateDateFormat(input)) {
                    return true;
                }
            case 1:
                if (validateFutureDate(input)) {
                    return true;
                }
            case 2:
                if (validateContactNumber(input)) {
                    return true;
                }
                break;
            case 3:
                if (validateInputId(input)) {
                    return true;
                }
            case 4:
                if (validatePersonalId(input)) {
                    return true;
                }
        }
        return false;
    }

    private static boolean validateDateFormat(String inputDate) {
        Date date = null;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
            date = sdf.parse(inputDate);
            if (!inputDate.equals(sdf.format(date))) {
                date = null;
            }
        } catch (ParseException ex) {
            logger.error("Error when parsing date", ex);
        }
        if (date == null) {
            logger.debug("Invalid date format");
            return false;
        } else {
            logger.debug("Valid date format");
            return true;
        }

    }

    private static boolean validateFutureDate(String inputDate) {
        Calendar cal = Calendar.getInstance();
        Date today = cal.getTime();
        Date date;

        try {
            SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
            date = sdf.parse(inputDate);
            if (date.after(today)) {
                logger.debug("future date: " + inputDate);
                return true;
            } else {
                logger.debug("past date: " + inputDate);
                return false;
            }
        } catch (ParseException ex) {
            logger.error("date parsing error", ex);
            return false;
        }
    }

    private static boolean validateContactNumber(String inputContact) {
        if (inputContact.matches(PHONE_REGEX)) {
            logger.debug("Match contact");
            return true;
        } else {
            logger.debug("Not matching number");
            return false;
        }
    }

    private static boolean validateInputId(String inputId) {
        if (inputId.matches(ID_REGEX)) {
            logger.debug("Match ID");
            return true;
        } else {
            logger.debug("Not matching ID");
            return false;
        }
    }

    private static boolean validatePersonalId(String personalId) {
        try {
            Connection conn = DbConnect.getDBConnection();
            PreparedStatement statement = conn.prepareStatement("SELECT count(*) FROM `person_profile` WHERE `personal_id`=?");
            statement.setString(1, personalId);
            ResultSet rs = statement.executeQuery();
            /*
             * Advance to the next result. If a result exists, check whether the
             * first column's value (count(*)) is greater than 0. If yes,
             * username is already taken.
             */
            if (rs.next() && rs.getInt(1) > 0) {
                return true;
            }
        } catch (SQLException ex) {
            logger.error("Error when validating person ID", ex);
        }
        return false;
    }
}
