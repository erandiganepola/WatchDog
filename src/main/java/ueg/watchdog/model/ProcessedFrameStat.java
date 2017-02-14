/*
 * The MIT License (MIT)
 * Copyright (c) 2016 Erandi Ganepola
 * <p>
 * Permission is hereby granted, free of charge,
 * to any person obtaining a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation the rights to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS",
 * WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package ueg.watchdog.model;

import org.apache.commons.dbutils.DbUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ueg.watchdog.database.DbConnect;
import ueg.watchdog.util.ImageUtils;
import ueg.watchdog.util.WatchDogUtils;

import java.awt.image.BufferedImage;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;

/**
 * The model class to represent the processed information of a given frame in a video
 *
 * @author Erandi Ganepola
 */
public class ProcessedFrameStat {

    private static final Logger logger = LoggerFactory.getLogger(ProcessedFrameStat.class);

    private static final String TABLE = "video_stat";

    private int id;
    private int videoId;
    private LocalDateTime timestamp;
    private String description;
    private BufferedImage face;
    private String profileId;

    public ProcessedFrameStat(int videoId, LocalDateTime timestamp, String gender, BufferedImage face, String profileId) {
        this.videoId = videoId;
        this.timestamp = timestamp;
        this.description = gender;
        this.face = face;
        this.profileId = profileId;
    }

    public void checkForDuplicatesAndSave() {
        String query = "SELECT profile_id FROM `" + TABLE + "` WHERE video_id=? ORDER BY id DESC LIMIT 1";
        Connection connection = DbConnect.getDBConnection();
        try {
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, getVideoId());
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next() && resultSet.getString(1) != null && resultSet.getString(1).equals(getProfileId())) {
                // Don't save
                logger.debug("Not saving the frame stat since the last frame is of the same profile : {}", getProfileId());
            } else {
                save();
            }
        } catch (Exception e) {
            logger.error("Error occurred when checking duplications of the frame : {}", e);
        }
    }

    public boolean save() {
        String query = "INSERT INTO `" + TABLE + "` (video_id,occurred_timestamp,description,face,profile_id) VALUES(?,?,?,?,?)";
        Connection connection = DbConnect.getDBConnection();
        try {
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, getVideoId());
            statement.setTimestamp(2, WatchDogUtils.toMySQLDate(getTimestamp()));
            statement.setString(3, getDescription());
            statement.setBinaryStream(4, ImageUtils.toInputStream(getFace()));
            statement.setString(5, getProfileId());
            statement.execute();
            String fetchIdQuery = "SELECT `id` FROM `" + TABLE + "` ORDER BY `id`  DESC LIMIT 1";
            PreparedStatement fetchIdStatement = connection.prepareStatement(fetchIdQuery);
            ResultSet resultSet = fetchIdStatement.executeQuery();
            resultSet.first();
            this.id = resultSet.getInt("id");
        } catch (Exception e) {
            logger.error("Error occurred when saving stat to database", e);
            return false;
        } finally {
            DbUtils.closeQuietly(connection);
        }
        logger.debug("Successfully saved processed frame stats");
        return true;
    }

    public int getId() {
        return id;
    }

    public int getVideoId() {
        return videoId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public BufferedImage getFace() {
        return face;
    }

    public String getProfileId() {
        return profileId;
    }

    public String getDescription() {
        return description;
    }
}
