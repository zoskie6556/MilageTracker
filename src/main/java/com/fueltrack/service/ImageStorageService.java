package com.fueltrack.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.UUID;

@Service
public class ImageStorageService {

    private static final long MAX_UPLOAD_BYTES = 10L * 1024 * 1024;
    private static final long MAX_IMAGE_PIXELS = 40_000_000L;
    private static final int MAX_DIMENSION = 1600;
    private final Path uploadDirectory;

    public ImageStorageService(@Value("${app.upload-dir:./uploads}") String uploadDirectory) {
        this.uploadDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize();
    }

    /**
     * Validates, resizes and stores an uploaded image outside the web root.
     *
     * @param upload image received from the user
     * @return the generated UUID filename
     * @throws ImageUploadException when the image is unsupported, oversized or unreadable
     */
    public String store(MultipartFile upload) {
        if (upload == null || upload.isEmpty()) {
            throw new ImageUploadException("Choose a JPEG, PNG or WebP odometer photo.");
        }
        if (upload.getSize() > MAX_UPLOAD_BYTES) {
            throw new ImageUploadException("The photo must be 10 MB or smaller.");
        }

        try {
            byte[] original = upload.getBytes();
            if (original.length > MAX_UPLOAD_BYTES) {
                throw new ImageUploadException("The photo must be 10 MB or smaller.");
            }
            String format = detectFormat(original);
            BufferedImage image = decode(original, format);
            long pixels = (long) image.getWidth() * image.getHeight();
            if (pixels > MAX_IMAGE_PIXELS) {
                throw new ImageUploadException("The photo dimensions are too large to process.");
            }

            BufferedImage resized = resize(image);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            if (!ImageIO.write(resized, "jpeg", output)) {
                throw new ImageUploadException("The photo could not be processed.");
            }

            Files.createDirectories(uploadDirectory);
            String filename = UUID.randomUUID() + ".jpg";
            Files.write(uploadDirectory.resolve(filename), output.toByteArray());
            return filename;
        } catch (IOException exception) {
            throw new ImageUploadException("The photo could not be read or stored.", exception);
        }
    }

    /**
     * Reads a stored image after checking that its name is a generated UUID filename.
     *
     * @param filename generated stored filename
     * @return image bytes for the AI request
     */
    public byte[] read(String filename) {
        return readPath(filename);
    }

    /**
     * Loads a stored image for an authenticated photo response.
     *
     * @param filename generated stored filename
     * @return bytes from the upload directory
     */
    public byte[] load(String filename) {
        return readPath(filename);
    }

    private byte[] readPath(String filename) {
        if (filename == null || !filename.matches("[0-9a-fA-F-]{36}\\.jpg")) {
            throw new ImageUploadException("Photo not found.");
        }
        try {
            return Files.readAllBytes(uploadDirectory.resolve(filename).normalize());
        } catch (IOException exception) {
            throw new ImageUploadException("The stored photo could not be read.", exception);
        }
    }

    private static String detectFormat(byte[] bytes) {
        if (bytes.length >= 3 && (bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8
                && (bytes[2] & 0xff) == 0xff) {
            return "jpeg";
        }
        if (bytes.length >= 8 && (bytes[0] & 0xff) == 0x89 && bytes[1] == 0x50
                && bytes[2] == 0x4e && bytes[3] == 0x47 && bytes[4] == 0x0d
                && bytes[5] == 0x0a && bytes[6] == 0x1a && bytes[7] == 0x0a) {
            return "png";
        }
        if (bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F'
                && bytes[3] == 'F' && bytes[8] == 'W' && bytes[9] == 'E'
                && bytes[10] == 'B' && bytes[11] == 'P') {
            return "webp";
        }
        throw new ImageUploadException("Only JPEG, PNG and WebP photos are supported.");
    }

    private static BufferedImage decode(byte[] bytes, String expectedFormat) throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (input == null) {
                throw new ImageUploadException("The uploaded file is not a readable image.");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new ImageUploadException("The uploaded image format could not be decoded.");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                String actualFormat = reader.getFormatName().toLowerCase();
                if (!expectedFormat.equals(actualFormat)
                        && !(expectedFormat.equals("jpeg") && actualFormat.equals("jpg"))) {
                    throw new ImageUploadException("The uploaded image content does not match its format.");
                }
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || (long) width * height > MAX_IMAGE_PIXELS) {
                    throw new ImageUploadException("The photo dimensions are too large to process.");
                }
                return reader.read(0);
            } finally {
                reader.dispose();
            }
        }
    }

    private static BufferedImage resize(BufferedImage source) {
        double scale = Math.min(1.0, (double) MAX_DIMENSION / Math.max(source.getWidth(), source.getHeight()));
        int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = resized.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, width, height);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(source, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }
        return resized;
    }
}
