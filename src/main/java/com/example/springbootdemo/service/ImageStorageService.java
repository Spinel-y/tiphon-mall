package com.example.springbootdemo.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class ImageStorageService {

    private static final Path IMAGES_DIR = Paths.get("ai-images");
    private static final int MAX_WIDTH = 800;
    private static final int MAX_HEIGHT = 800;

    private Path getImagesDir() throws IOException {
        if (!Files.exists(IMAGES_DIR)) {
            Files.createDirectories(IMAGES_DIR);
        }
        return IMAGES_DIR;
    }

    public String storeUploadedImage(MultipartFile file) throws IOException {
        String extension = ".png";
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && originalFilename.contains(".")) {
            String ext = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
            if (ext.equals(".gif")) {
                extension = ".gif";
            }
        }
        String filename = UUID.randomUUID().toString() + extension;
        Path targetPath = getImagesDir().resolve(filename);

        byte[] imageBytes = file.getBytes();
        if (!extension.equals(".gif")) {
            imageBytes = resizeImage(imageBytes);
        }
        Files.write(targetPath, imageBytes);
        return filename;
    }

    public String storeImageFromUrl(String remoteUrl) throws IOException {
        String filename = UUID.randomUUID().toString() + ".png";
        byte[] imageBytes;
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new URL(remoteUrl).openConnection();
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(30000);
        try (InputStream in = conn.getInputStream()) {
            imageBytes = in.readAllBytes();
        } finally {
            conn.disconnect();
        }
        return saveImage(filename, imageBytes);
    }

    public String storeFromBase64(String base64Data) throws IOException {
        String filename = UUID.randomUUID().toString() + ".png";
        byte[] imageBytes = Base64.getDecoder().decode(base64Data);
        return saveImage(filename, imageBytes);
    }

    private String saveImage(String filename, byte[] imageBytes) throws IOException {
        Path targetPath = getImagesDir().resolve(filename);
        byte[] resized = resizeImage(imageBytes);
        Files.write(targetPath, resized);
        return filename;
    }

    public List<String> listImages() throws IOException {
        List<String> filenames = new ArrayList<>();
        try (Stream<Path> stream = Files.list(getImagesDir())) {
            stream.filter(Files::isRegularFile)
                    .map(p -> p.getFileName().toString())
                    .filter(name -> name.endsWith(".png") || name.endsWith(".jpg")
                            || name.endsWith(".jpeg") || name.endsWith(".gif") || name.endsWith(".webp"))
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(filenames::add);
        }
        return filenames;
    }

    public Path resolve(String filename) throws IOException {
        if (filename.contains("/") || filename.contains("\\") || filename.contains("..")) {
            throw new IOException("Invalid filename");
        }
        return getImagesDir().resolve(filename);
    }

    private byte[] resizeImage(byte[] imageBytes) throws IOException {
        BufferedImage original = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (original == null) {
            return imageBytes;
        }
        int w = original.getWidth();
        int h = original.getHeight();
        if (w <= MAX_WIDTH && h <= MAX_HEIGHT) {
            return imageBytes;
        }
        double scale = Math.min((double) MAX_WIDTH / w, (double) MAX_HEIGHT / h);
        int newW = (int) (w * scale);
        int newH = (int) (h * scale);
        BufferedImage resized = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(original, 0, 0, newW, newH, null);
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(resized, "png", out);
        return out.toByteArray();
    }
}
