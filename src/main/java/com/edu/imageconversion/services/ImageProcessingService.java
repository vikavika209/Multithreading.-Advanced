package com.edu.imageconversion.services;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
public class ImageProcessingService {

    private Color findBackgroundColor(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        Map<Color, Integer> colorCount = new HashMap<>();

        int margin = 5;
        for (int y = 0; y < margin; y++) {
            for (int x = 0; x < margin; x++) {
                addColorCount(colorCount, new Color(image.getRGB(x, y)));
                addColorCount(colorCount, new Color(image.getRGB(width - 1 - x, y)));
                addColorCount(colorCount, new Color(image.getRGB(x, height - 1 - y)));
                addColorCount(colorCount, new Color(image.getRGB(width - 1 - x, height - 1 - y)));
            }
        }

        return colorCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(Color.WHITE);
    }

    private void addColorCount(Map<Color, Integer> colorCount, Color color) {
        colorCount.put(color, colorCount.getOrDefault(color, 0) + 1);
    }

    private BufferedImage removeBackground(BufferedImage source) throws IOException {
        int width = source.getWidth();
        int height = source.getHeight();
        BufferedImage outputImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        Color backgroundColor = findBackgroundColor(source);
        int tolerance = 30;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = source.getRGB(x, y);
                Color color = new Color(rgb, true);

                if (isBackground(color, backgroundColor, tolerance)) {
                    outputImage.setRGB(x, y, 0x00FFFFFF);
                } else {
                    outputImage.setRGB(x, y, rgb);
                }
            }
        }
        return outputImage;
    }

    private boolean isBackground(Color color, Color backgroundColor, int tolerance) {
        int rDiff = Math.abs(color.getRed() - backgroundColor.getRed());
        int gDiff = Math.abs(color.getGreen() - backgroundColor.getGreen());
        int bDiff = Math.abs(color.getBlue() - backgroundColor.getBlue());
        return rDiff < tolerance && gDiff < tolerance && bDiff < tolerance;
    }

    private byte[] bufferedImageToSvg(BufferedImage image) throws IOException {
        // Простейшая реализация конвертации в SVG, для качественной векторизации лучше использовать специализированные библиотеки
        StringBuilder svgBuilder = new StringBuilder();
        svgBuilder.append("<svg xmlns='http://www.w3.org/2000/svg' width='")
                .append(image.getWidth())
                .append("' height='")
                .append(image.getHeight())
                .append("'>");

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                int alpha = (rgb >> 24) & 0xff;
                if (alpha == 0) {
                    continue; // Skip transparent pixels
                }
                String hex = String.format("#%02x%02x%02x",
                        (rgb >> 16) & 0xff, // Red
                        (rgb >> 8) & 0xff,  // Green
                        (rgb) & 0xff);      // Blue
                svgBuilder.append("<rect x='")
                        .append(x)
                        .append("' y='")
                        .append(y)
                        .append("' width='1' height='1' fill='")
                        .append(hex)
                        .append("'/>");
            }
        }

        svgBuilder.append("</svg>");
        return svgBuilder.toString().getBytes();
    }

    private byte[] bufferedImageToByteArray(BufferedImage image, String format) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ImageIO.write(image, format, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    // Метод для конвертации изображения
    public byte[] convertImage(MultipartFile file, String format) throws IOException {
        BufferedImage inputImage = ImageIO.read(file.getInputStream());
        if (inputImage == null) {
            throw new IOException("Could not open or find the image");
        }

        BufferedImage result = removeBackground(inputImage);

        if ("svg".equalsIgnoreCase(format)) {
            return bufferedImageToSvg(result);
        }

        return bufferedImageToByteArray(result, "png");
    }
}