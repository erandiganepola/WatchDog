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
import ueg.watchdog.util.WatchDogUtils;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Model class for video's in WatchDog
 *
 * @author Erandi Ganepola,
 */
public class Video {

    private static final Logger logger = LoggerFactory.getLogger(Video.class);

    private static final String TABLE = "video";

    private int id;
    private String fileName;
    private String filePath;
    private boolean processed;
    private LocalDateTime from;
    private LocalDateTime to;
    private boolean deleted;

    public Video(String fileName, String filePath, boolean processed, LocalDateTime from, LocalDateTime to) {
        this(0, fileName, filePath, processed, from, to, false);
    }

    public Video(int id, String fileName, String filePath, boolean processed, LocalDateTime from, LocalDateTime to, boolean deleted) {
        this.id = id;
        this.fileName = fileName;
        this.filePath = filePath;
        this.processed = processed;
        this.from = from;
        this.to = to;
    }

    public int getId() {
        return id;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public boolean isProcessed() {
        return processed;
    }

    public LocalDateTime getFrom() {
        return from;
    }

    public LocalDateTime getTo() {
        return to;
    }

    public boolean isSaved() {
        return id != 0;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public boolean exists() {
        File videoFile = new File(filePath);
        return videoFile.exists() && videoFile.isFile();
    }

    public boolean save() {
        String query = "INSERT INTO `" + TABLE + "` (file_name,processed,file_path,start_time,end_time,deleted) VALUES(?,?,?,?,?,?)";
        Connection connection = DbConnect.getDBConnection();
        try {
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, getFileName());
            statement.setBoolean(2, isProcessed());
            statement.setString(3, getFilePath());
            statement.setTimestamp(4, WatchDogUtils.toMySQLDate(getFrom()));
            statement.setTimestamp(5, WatchDogUtils.toMySQLDate(getTo()));
            statement.setBoolean(6, false);
            statement.execute();
            String fetchIdQuery = "SELECT `id` FROM `" + TABLE + "` ORDER BY `id`  DESC LIMIT 1";
            PreparedStatement fetchIdStatement = connection.prepareStatement(fetchIdQuery);
            ResultSet resultSet = fetchIdStatement.executeQuery();
            resultSet.first();
            this.id = resultSet.getInt("id");
        } catch (SQLException e) {
            logger.error("Error occurred when saving video ({},{}) details to DB", getFileName(), getFilePath(), e);
            return false;
        } finally {
            DbUtils.closeQuietly(connection);
        }
        return true;
    }

    public boolean markProcessed(boolean processed) {
        String query = "UPDATE `" + TABLE + "` SET processed=? WHERE id=?";
        Connection connection = DbConnect.getDBConnection();
        try {
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setBoolean(1, processed);
            statement.setInt(2, getId());
            if (statement.executeUpdate() > 0) {
                this.processed = processed;
                return true;
            }
        } catch (SQLException e) {
            logger.error("Error occurred when marking video ({},{}) as {}", getFileName(), getFilePath(),
                    processed ? "processed" : "unprocessed", e);
        } finally {
            DbUtils.closeQuietly(connection);
        }
        return false;
    }

    public boolean softDelete() {
        String query = "UPDATE `" + TABLE + "`SET deleted=? WHERE id=?";
        Connection connection = DbConnect.getDBConnection();
        try {
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setBoolean(1, true);
            statement.setInt(2, getId());
            statement.execute();
        } catch (SQLException e) {
            logger.error("Error occurred when soft deleting video ({},{})", getFileName(), getFilePath(), e);
            return false;
        } finally {
            DbUtils.closeQuietly(connection);
        }
        return true;
    }

    /**
     * Deletes the original video.
     *
     * @return true if successfully deleted
     */
    public boolean deleteRawFile() {
        if (exists()) {
            File videoFile = new File(filePath);
            return videoFile.delete();
        }
        return false;
    }

    public static Video getVideoById(int id) {
        String query = "SELECT * FROM `" + TABLE + "` WHERE id=?";
        Connection connection = DbConnect.getDBConnection();
        try {
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, id);
            ResultSet resultSet = statement.executeQuery();
            resultSet.first();
            return loadData(resultSet);
        } catch (SQLException e) {
            logger.error("Error occurred when fetching video with id : {} from DB", id, e);
            return null;
        } finally {
            DbUtils.closeQuietly(connection);
        }
    }

    /**
     * Get the videos filtered by {@link #processed} flag.
     *
     * @param finishedProcessing if true, return the processed videos
     * @param deleted            if true, deleted videos will be searched with other filters
     * @return list of videos matching the filter {@link #processed}
     */
    public static List<Video> getVideos(boolean finishedProcessing, boolean deleted) {
        List<Video> videos = new ArrayList<>();
        String query = "SELECT * FROM `" + TABLE + "` WHERE processed=? AND deleted=?";
        Connection connection = DbConnect.getDBConnection();
        try {
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setBoolean(1, finishedProcessing);
            statement.setBoolean(2, deleted);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                videos.add(loadData(resultSet));
            }
        } catch (Exception e) {
            logger.error("Error occurred when fetching the {} videos", finishedProcessing ? "processed" : "unprocessed", e);
        } finally {
            DbUtils.closeQuietly(connection);
        }
        return videos;
    }

    public static Video createVideo(String fileName, String filePath) {
        String withoutExtension = fileName.substring(0, fileName.lastIndexOf("."));
        String timestamps[] = withoutExtension.split("-");
        LocalDateTime from = WatchDogUtils.getLocalDateTime(timestamps[0]);
        LocalDateTime to = WatchDogUtils.getLocalDateTime(timestamps[1]);
        return new Video(fileName, filePath, false, from, to);
    }

    private static Video loadData(ResultSet resultSet) throws SQLException {
        return new Video(resultSet.getInt("id"),
                resultSet.getString("file_name"),
                resultSet.getString("file_path"),
                resultSet.getBoolean("processed"),
                WatchDogUtils.fromMySQLDate(resultSet.getTimestamp("start_time")),
                WatchDogUtils.fromMySQLDate(resultSet.getTimestamp("end_time")),
                resultSet.getBoolean("deleted"));
    }
}
