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
package ueg.watchdog.util;

import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.io.FilenameUtils;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.bytedeco.javacpp.opencv_core.Mat;
import static ueg.watchdog.Constants.PROFILE_PICTURE_DIR;
import static ueg.watchdog.Constants.SEPARATOR;

/**
 * Utils to be used for images related tasks
 *
 * @author erandi
 */
public class ImageUtils {

    private static final Logger logger = LoggerFactory.getLogger(ImageUtils.class);
    private static Java2DFrameConverter frameConverter = new Java2DFrameConverter();
    private static OpenCVFrameConverter.ToMat matConverter = new OpenCVFrameConverter.ToMat();

    public static String copyImage(String path, int profileId, int fileNumber) throws IOException {
        String extension = FilenameUtils.getExtension(path);
        String destinationDirectory = PROFILE_PICTURE_DIR + SEPARATOR + Integer.toString(profileId);
        WatchDogUtils.createDirectoryIfNotExist(destinationDirectory);

        String destinationFile = destinationDirectory + SEPARATOR + fileNumber + "." + extension;
        try {
            Files.copy(new File(path).toPath(), new File(destinationFile).toPath(), REPLACE_EXISTING);
            return destinationFile;
        } catch (IOException ex) {
            logger.error("Error when copying image", ex);
            throw ex;
        }
    }

    /**
     * Method to resize and visualize the user selected image within the image
     * label
     *
     * @param imagePath Path to the image
     * @param width     expected weight
     * @param height    expected height
     * @return {@link ImageIcon}
     */
    public static ImageIcon resizeImage(String imagePath, int width, int height) {
        ImageIcon ic = new ImageIcon(imagePath);
        Image img = ic.getImage();
        Image newImage = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(newImage);
    }

    /**
     * Method to get resized buffered image when user passes the relevant frame and video panel.
     *
     * @param frame
     * @param videoPanel
     * @return
     */
    public static BufferedImage getResizedBufferedImage(Frame frame, JPanel videoPanel) {
        BufferedImage resizedImage = null;

        try {
            /*
             * We get notified about the frames that are being added. Then we pass each frame to BufferedImage. I have used
             * a library called Thumbnailator to achieve the resizing effect along with performance
             */
            resizedImage = Thumbnails.of(frameConverter.getBufferedImage(frame))
                    .size(videoPanel.getWidth(), videoPanel.getHeight())
                    .asBufferedImage();
        } catch (IOException e) {
            logger.error("Unable to convert the image to a buffered image", e);
        }

        return resizedImage;
    }

    /**
     * Convert a buffered image to an input stream. Used by database modes
     *
     * @param image buffered image
     * @return input stream
     * @throws IOException io exception
     */
    public static InputStream toInputStream(BufferedImage image) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", byteArrayOutputStream);
        return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
    }

    public static BufferedImage toBufferedImage(Frame frame) {
        return frameConverter.convert(frame);
    }

    public static BufferedImage toBufferedImage(Mat mat) {
        return frameConverter.convert(matConverter.convert(mat));
    }

}
