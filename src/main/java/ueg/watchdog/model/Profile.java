package ueg.watchdog.model;

import org.apache.commons.dbutils.DbUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ueg.watchdog.database.DbConnect;
import ueg.watchdog.util.ImageUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Model to represent a Person profile in the system
 *
 * @author erandi
 */
public class Profile {

    private static final Logger logger = LoggerFactory.getLogger(Profile.class);

    private int id;
    private String personalId;
    private String firstName;
    private String lastName;
    private String dob;
    private String gender;
    private String contact;
    private String address;
    private List<String> imagePaths;
    private String occurrences;

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setPersonalId(String personalId) {
        this.personalId = personalId;
    }

    public String getPersonalId() {
        return personalId;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setDob(String dob) {
        this.dob = dob;
    }

    public String getDob() {
        return dob;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getGender() {
        return gender;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public String getContact() {
        return contact;
    }

    public void setAdress(String address) {
        this.address = address;
    }

    public String getAddress() {
        return address;
    }

    public void setImagePaths(List<String> imagePaths) {
        this.imagePaths = imagePaths;
    }

    public List<String> getImagePaths() {
        return imagePaths;
    }

    public void setOccurrences(String occurrences) {
        this.occurrences = occurrences;
    }

    public String getOccurrences() {
        return occurrences;
    }

    /**
     * Saves a given Profile into the database. Returns the success status of
     * the operation
     *
     * @param profile Profile to be saved
     * @return true if the operation was successful
     */
    public static boolean saveUserProfile(Profile profile) {
        String id = profile.getPersonalId();
        String firstName = profile.getFirstName();
        String lastName = profile.getLastName();
        String dob = profile.getDob();
        String gender = profile.getGender();
        String contact = profile.getContact();
        String address = profile.getAddress();
        List<String> imagePaths = profile.getImagePaths();

        Connection conn = DbConnect.getDBConnection();
        try {
            conn.setAutoCommit(false); //transaction block start
            PreparedStatement pst = conn.prepareStatement("INSERT INTO `person_profile`(`personal_id`, `first_name`, `last_name`, `dob`, `gender`, `contact_num`, `address`) VALUES (?,?,?,?,?,?,?)");
            pst.setString(1, id);
            pst.setString(2, firstName);
            pst.setString(3, lastName);
            pst.setString(4, dob);
            pst.setString(5, gender);
            pst.setString(6, contact);
            pst.setString(7, address);
            pst.executeUpdate();

            PreparedStatement pstId = conn.prepareStatement("SELECT `id` FROM `person_profile` WHERE `personal_id`=?");
            pstId.setString(1, profile.getPersonalId());
            ResultSet rs = pstId.executeQuery();
            if (rs.next()) {
                int saveId = Integer.parseInt(rs.getString(1));
                String paths = imagePaths.stream().collect(Collectors.joining(","));
                for (int i = 0; i < imagePaths.size(); i++) {
                    ImageUtils.copyImage(imagePaths.get(i), saveId, i);
                }

                PreparedStatement pstPath = conn.prepareStatement("UPDATE `person_profile` SET `image_path`=? WHERE `id`=?");
                pstPath.setString(1, paths);
                pstPath.setInt(2, saveId);
                pstPath.executeUpdate();
            }

            //transaction block end
            DbUtils.commitAndCloseQuietly(conn);
            DbUtils.closeQuietly(rs);
            DbUtils.closeQuietly(pst);
            DbUtils.closeQuietly(pstId);
            return true;
        } catch (Exception e) {
            DbUtils.rollbackAndCloseQuietly(conn);
            logger.error("Error occurred when saving user profile for profile : {} {}", profile.getFirstName(), profile.getLastName());
            return false;
        }
    }

    public static boolean updateUserProfile(Profile profile) {
        int id = profile.getId();
        String personalId = profile.getPersonalId();
        String firstName = profile.getFirstName();
        String lastName = profile.getLastName();
        String dob = profile.getDob();
        String gender = profile.getGender();
        String contact = profile.getContact();
        String address = profile.getAddress();
        List<String> imagePaths = profile.getImagePaths();

        Connection conn = DbConnect.getDBConnection();
        try {
            String paths = imagePaths.stream().collect(Collectors.joining(","));
            for (int i = 0; i < imagePaths.size(); i++) {
                ImageUtils.copyImage(imagePaths.get(i), id, i);
            }

            conn.setAutoCommit(false); //transaction block start
            PreparedStatement pst = conn.prepareStatement("UPDATE `person_profile` SET `personal_id`=?,`first_name`=?,`last_name`=?,`dob`=?,`gender`=?,`contact_num`=?,`address`=?,`image_path`=? WHERE `id`=?");
            pst.setString(1, personalId);
            pst.setString(2, firstName);
            pst.setString(3, lastName);
            pst.setString(4, dob);
            pst.setString(5, gender);
            pst.setString(6, contact);
            pst.setString(7, address);
            pst.setString(8, paths);
            pst.setInt(9, id);
            pst.executeUpdate();

            DbUtils.commitAndCloseQuietly(conn);
            DbUtils.closeQuietly(pst);
            return true;
        } catch (Exception e) {
            DbUtils.rollbackAndCloseQuietly(conn);
            logger.error("Error occurred when updating user profile", e);
            return false;
        }
    }

    public static Profile getProfileById(int id) {
        Connection conn = DbConnect.getDBConnection();
        try {
            PreparedStatement pst = conn.prepareStatement("SELECT * FROM `person_profile` WHERE `id`=?;");
            pst.setInt(1, id);
            ResultSet rs = pst.executeQuery();

            while (rs.first()) {
                return loadData(rs);
            }
        } catch (SQLException e) {
            logger.error("Error occurred", e);
        } finally {
            DbUtils.closeQuietly(conn);
        }
        return null;
    }

    public static ArrayList searchProfileByName(String name) {
        ArrayList<Profile> list = new ArrayList<Profile>();
        Connection conn = DbConnect.getDBConnection();
        try {
            PreparedStatement pst = conn.prepareStatement("SELECT  * FROM  `person_profile` WHERE  `first_name` LIKE  ? OR  `last_name` LIKE  ?;");
            pst.setString(1, name + "%");
            pst.setString(2, name + "%");
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                list.add(loadData(rs));
            }
        } catch (SQLException e) {
            logger.error("Error occurred when searching profiles by name : {}", name, e);
        } finally {
            DbUtils.closeQuietly(conn);
        }
        return list;
    }

    public static boolean deleteProfileById(int id) {
        Connection conn = DbConnect.getDBConnection();
        PreparedStatement pst;
        try {
            pst = conn.prepareStatement("DELETE FROM `person_profile` WHERE `id`=?;");
            pst.setInt(1, id);
            pst.execute();
            conn.close();
            return true;
        } catch (SQLException ex) {
            logger.error("Error when deleting profile with id : {}", id, ex);
            return false;
        }
    }

    private static Profile loadData(ResultSet resultSet) throws SQLException {
        Profile profile = new Profile();
        profile.setId(resultSet.getInt("id"));
        profile.setPersonalId(resultSet.getString("personal_id"));
        profile.setFirstName(resultSet.getString("first_name"));
        profile.setLastName(resultSet.getString("last_name"));
        profile.setDob(resultSet.getString("dob"));
        profile.setGender(resultSet.getString("gender"));
        profile.setContact(resultSet.getString("contact_num"));
        profile.setAdress(resultSet.getString("address"));
        profile.setImagePaths(Arrays.asList(resultSet.getString("image_path").split(",")));
        profile.setOccurrences(resultSet.getString("last_occurence"));
        return profile;
    }
}
