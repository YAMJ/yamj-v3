/*
 *      Copyright (c) 2004-2015 YAMJ Members
 *      https://github.com/organizations/YAMJ/teams
 *
 *      This file is part of the Yet Another Media Jukebox (YAMJ).
 *
 *      YAMJ is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      any later version.
 *
 *      YAMJ is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU General Public License for more details.
 *
 *      You should have received a copy of the GNU General Public License
 *      along with YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: https://github.com/YAMJ/yamj-v3
 *
 */
package org.yamj.core.tools.image;

import com.jhlabs.image.PerspectiveFilter;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.apache.sanselan.ImageReadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GraphicTools {

    private static final Logger LOG = LoggerFactory.getLogger(GraphicTools.class);

    private GraphicTools() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static Dimension getDimension(String url) throws IOException {
        try (InputStream in = new URL(url).openStream();
             ImageInputStream iis = ImageIO.createImageInputStream(in))
         {

            final Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (readers.hasNext()) {
                ImageReader reader = readers.next();
                try {
                    reader.setInput(iis, Boolean.TRUE);
                    return new Dimension(reader.getWidth(0), reader.getHeight(0));
                } finally {
                    try {
                        reader.dispose();
                    } catch (Exception ignore) {
                        // ignore any error on dispose
                    }
                }
            }
        }
        return new Dimension();
    }

    /**
     * Load a JPG image from a file
     *
     * @param fileImage
     * @return
     * @throws IOException
     * @throws org.apache.sanselan.ImageReadException
     */
    public static BufferedImage loadJPEGImage(File fileImage) throws IOException, ImageReadException {
        if (fileImage.exists()) {
            JpegReader jr = new JpegReader();
            BufferedImage bi = jr.readImage(fileImage);
            if (bi == null) {
                // null means: the image is invalid and couldn't be loaded
                throw new ImageReadException("Image file '" + fileImage.getName() + "' is invalid");
            }
            return bi;
        } else {
            throw new FileNotFoundException("Image file '" + fileImage.getName() + "' does not exist");
        }
    }

    /**
     * Load a JPG image from an URL
     *
     * @param url
     * @return
     */
    public static BufferedImage loadJPEGImage(URL url) {
        try {
            return ImageIO.read(url);
        } catch (IOException ex) {
            LOG.error("Error reading image file. Possibly corrupt image.");
            return null;
        }
    }

    public static BufferedImage createBlankImage(int width, int height) {
        return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    }

    /**
     * Bi-cubic image scaling
     *
     * @param nMaxWidth
     * @param nMaxHeight
     * @param imgSrc
     * @return
     */
    public static BufferedImage scaleToSize(int nMaxWidth, int nMaxHeight, BufferedImage imgSrc) {
        /* determine thumbnail size from WIDTH and HEIGHT */
        int imageWidth = imgSrc.getWidth(null);
        int imageHeight = imgSrc.getHeight(null);

        int tempWidth;
        int tempHeight;
        int y = 0;

        tempWidth = nMaxWidth;
        tempHeight = (int) (((double) imageHeight * (double) nMaxWidth) / (double) imageWidth);

        if (nMaxHeight > tempHeight) {
            y = nMaxHeight - tempHeight;
        }

        Image temp1 = imgSrc.getScaledInstance(tempWidth, tempHeight, Image.SCALE_SMOOTH);
        BufferedImage bi = new BufferedImage(nMaxWidth, nMaxHeight, BufferedImage.TYPE_INT_ARGB);
        bi.createGraphics().drawImage(temp1, 0, y, null);
        return bi;
    }

    public static BufferedImage scaleToSizeStretch(int nMaxWidth, int nMaxHeight, BufferedImage imgSrc) {
        /* determine thumbnail size from WIDTH and HEIGHT */

        int tempWidth;
        int tempHeight;

        tempWidth = nMaxWidth;
        tempHeight = nMaxHeight;

        Image temp1 = imgSrc.getScaledInstance(tempWidth, tempHeight, Image.SCALE_SMOOTH);
        BufferedImage bi = new BufferedImage(nMaxWidth, nMaxHeight, BufferedImage.TYPE_INT_ARGB);
        bi.createGraphics().drawImage(temp1, 0, 0, null);
        return bi;
    }

    public static BufferedImage scaleToSizeBestFit(int nMaxWidth, int nMaxHeight, BufferedImage imgSrc) {
        // determine thumbnail size from WIDTH and HEIGHT
        int imageWidth = imgSrc.getWidth(null);
        int imageHeight = imgSrc.getHeight(null);

        int tempWidth;
        int tempHeight;

        tempWidth = nMaxWidth;
        tempHeight = (int) (((double) imageHeight * (double) nMaxWidth) / (double) imageWidth);

        Image temp1 = imgSrc.getScaledInstance(tempWidth, tempHeight, Image.SCALE_SMOOTH);
        BufferedImage bi = new BufferedImage(tempWidth, tempHeight, BufferedImage.TYPE_INT_ARGB);
        bi.createGraphics().drawImage(temp1, 0, 0, null);
        return bi;
    }

    public static BufferedImage scaleToSizeNormalized(int nMaxWidth, int nMaxHeight, BufferedImage imgSrc) {
        // determine thumbnail size from WIDTH and HEIGHT
        int imageWidth = imgSrc.getWidth(null);
        int imageHeight = imgSrc.getHeight(null);

        double imageRatio = (double) imageHeight / (double) imageWidth;
        double thumbnailRatio = (double) nMaxHeight / (double) nMaxWidth;

        int tempWidth;
        int tempHeight;

        if (imageRatio > thumbnailRatio) {
            tempWidth = nMaxWidth;
            tempHeight = (int) (((double) imageHeight * (double) nMaxWidth) / (double) imageWidth);
        } else {
            tempWidth = (int) (((double) imageWidth * (double) nMaxHeight) / (double) imageHeight);
            tempHeight = nMaxHeight;
        }

        Image temp1 = imgSrc.getScaledInstance(tempWidth, tempHeight, Image.SCALE_SMOOTH);
        BufferedImage bi = new BufferedImage(tempWidth, tempHeight, BufferedImage.TYPE_INT_ARGB);
        bi.createGraphics().drawImage(temp1, 0, 0, null);
        return cropToSize(nMaxWidth, nMaxHeight, bi);
    }

    public static BufferedImage cropToSize(int nMaxWidth, int nMaxHeight, BufferedImage imgSrc) {
        int nHeight = imgSrc.getHeight();
        int nWidth = imgSrc.getWidth();

        int x1 = 0;
        if (nWidth > nMaxWidth) {
            x1 = (nWidth - nMaxWidth) / 2;
        }

        int l = nMaxWidth;
        if (nWidth < nMaxWidth) {
            l = nWidth;
        }

        int y1 = 0;
        if (nHeight > nMaxHeight) {
            y1 = (nHeight - nMaxHeight) / 2;
        }

        int h = nMaxHeight;
        if (nHeight < nMaxHeight) {
            h = nHeight;
        }

        return imgSrc.getSubimage(x1, y1, l, h);
    }

    /**
     * Creates the reflection effect
     *
     * graphicType should be "posters", "thumbnails" or "videoimage" and is used to determine the settings that are extracted from
     * the skin.properties file.
     *
     * @param avatar
     * @param graphicType
     * @return
     */
    public static BufferedImage createReflectedPicture(BufferedImage avatar, String graphicType) {
        int avatarWidth = avatar.getWidth();
        int avatarHeight = avatar.getHeight();

        // TODO: read from artwork profile
        float reflectionHeight = 12.5f;

        BufferedImage gradient = createGradientMask(avatarWidth, avatarHeight, reflectionHeight, graphicType);
        BufferedImage buffer = createReflection(avatar, avatarWidth, avatarHeight, reflectionHeight);

        applyAlphaMask(gradient, buffer, avatarWidth, avatarHeight);

        return buffer;
    }

    /**
     * Create a gradient mask for the image
     *
     * @param avatarWidth
     * @param avatarHeight
     * @param reflectionHeight
     * @param graphicType
     * @return
     */
    public static BufferedImage createGradientMask(int avatarWidth, int avatarHeight, float reflectionHeight, String graphicType) {
        BufferedImage gradient = new BufferedImage(avatarWidth, avatarHeight, BufferedImage.TYPE_4BYTE_ABGR_PRE);
        Graphics2D g = gradient.createGraphics();

        float reflectionStart, reflectionEnd, opacityStart, opacityEnd;
        float reflectionHeightAbsolute = avatarHeight * (reflectionHeight / 100);

        // TODO read from artwork profile
        reflectionStart = 0.0f * reflectionHeightAbsolute;
        reflectionEnd = 100.0f * reflectionHeightAbsolute;
        opacityStart = 30.0f / 100;
        opacityEnd = 100.0f / 100;

        GradientPaint painter = new GradientPaint(0.0f, reflectionStart, new Color(1.0f, 1.0f, 1.0f, opacityStart), 0.0f, reflectionEnd, new Color(1.0f, 1.0f, 1.0f, opacityEnd));
        g.setPaint(painter);
        g.fill(new Rectangle2D.Double(0, 0, avatarWidth, avatarHeight));

        g.dispose();
        gradient.flush();

        return gradient;
    }

    /**
     * Create the reflection effect for the image
     *
     * @param avatar
     * @param avatarWidth
     * @param avatarHeight
     * @param reflectionHeight
     * @return
     */
    public static BufferedImage createReflection(BufferedImage avatar, int avatarWidth, int avatarHeight, float reflectionHeight) {
        // Increase the height of the image to cater for the reflection.
        int newHeight = (int) (avatarHeight * (1 + (reflectionHeight / 100)));

        BufferedImage buffer = new BufferedImage(avatarWidth, newHeight, BufferedImage.TYPE_4BYTE_ABGR_PRE);
        Graphics2D g = buffer.createGraphics();

        g.drawImage(avatar, null, null);
        g.translate(0, (avatarHeight << 1) + 2);

        AffineTransform reflectTransform = AffineTransform.getScaleInstance(1.0, -1.0);
        g.drawImage(avatar, reflectTransform, null);
        g.translate(0, -(avatarHeight << 1));

        g.dispose();

        return buffer;
    }

    public static void applyAlphaMask(BufferedImage gradient, BufferedImage buffer, int avatarWidth, int avatarHeight) {
        Graphics2D g2 = buffer.createGraphics();
        g2.setComposite(AlphaComposite.DstOut);
        g2.drawImage(gradient, null, 0, avatarHeight);
        g2.dispose();
    }

    /**
     * Creates the 3D effect
     *
     * graphicType should be "posters", "thumbnails" or "videoimage" and is used to determine the settings that are extracted from
     * the skin.properties file.
     *
     * @param bi
     * @param graphicType
     * @param perspectiveDirection
     * @return
     */
    public static BufferedImage create3DPicture(BufferedImage bi, String graphicType, String perspectiveDirection) {
        int w = bi.getWidth();
        int h = bi.getHeight();
        float perspectiveTop = 3f;
        float perspectiveBottom = 3f;

        // TODO read from artwork profile
        int top3d = (int) (h * perspectiveTop / 100);
        int bot3d = (int) (h * perspectiveBottom / 100);

        PerspectiveFilter perspectiveFilter = new PerspectiveFilter();
        // Top Left (x/y), Top Right (x/y), Bottom Right (x/y), Bottom Left (x/y)

        if (perspectiveDirection.equalsIgnoreCase("right")) {
            perspectiveFilter.setCorners(0, 0, w, top3d, w, h - bot3d, 0, h);
        } else {
            perspectiveFilter.setCorners(0, top3d, w, 0, w, h, 0, h - bot3d);
        }
        return perspectiveFilter.filter(bi, null);
    }
}
