package com.edu.imageconversion.services;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
public class ImageProcessingService {
    private final SvgOptimizationService svgOptimizationService;

    public ImageProcessingService(SvgOptimizationService svgOptimizationService) {
        this.svgOptimizationService = svgOptimizationService;
    }

    private Map<Color, Integer> findBackgroundColors(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        Map<Color, Integer> colorCount = new HashMap<>();

        int margin = Math.min(width, height) / 10; // Для анализа 10% от краев изображения
        for (int y = 0; y < margin; y++) {
            for (int x = 0; x < margin; x++) {
                addColorCount(colorCount, new Color(image.getRGB(x, y)));
                addColorCount(colorCount, new Color(image.getRGB(width - 1 - x, y)));
                addColorCount(colorCount, new Color(image.getRGB(x, height - 1 - y)));
                addColorCount(colorCount, new Color(image.getRGB(width - 1 - x, height - 1 - y)));
            }
        }

        return colorCount;
    }

    private void addColorCount(Map<Color, Integer> colorCount, Color color) {
        colorCount.put(color, colorCount.getOrDefault(color, 0) + 1);
    }

    private boolean isBackgroundColor(Color color, Color[] bgColors, int tolerance) {
        for (Color bgColor : bgColors) {
            int rDiff = Math.abs(color.getRed() - bgColor.getRed());
            int gDiff = Math.abs(color.getGreen() - bgColor.getGreen());
            int bDiff = Math.abs(color.getBlue() - bgColor.getBlue());
            if (rDiff < tolerance && gDiff < tolerance && bDiff < tolerance) {
                return true;
            }
        }
        return false;
    }

    private BufferedImage removeBackground(BufferedImage source) throws IOException {
        int width = source.getWidth();
        int height = source.getHeight();
        BufferedImage outputImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        Map<Color, Integer> backgroundColorsMap = findBackgroundColors(source);
        // Найдем два самых доминирующих цвета
        Color[] backgroundColors = backgroundColorsMap.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(2)
                .map(Map.Entry::getKey)
                .toArray(Color[]::new);

        int tolerance = 30;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = source.getRGB(x, y);
                Color color = new Color(rgb, true);

                if (isBackgroundColor(color, backgroundColors, tolerance)) {
                    outputImage.setRGB(x, y, 0x00FFFFFF);
                } else {
                    outputImage.setRGB(x, y, rgb);
                }
            }
        }
        return outputImage;
    }

    private byte[] bufferedImageToSvg(BufferedImage image) throws IOException {
        StringBuilder svgBuilder = new StringBuilder();
        svgBuilder.append("<svg xmlns='http://www.w3.org/2000/svg' width='")
                .append(image.getWidth())
                .append("' height='")
                .append(image.getHeight())
                .append("'>");

        Map<Color, StringBuilder> colorToPathsMap = new HashMap<>();

        boolean[][] visited = new boolean[image.getWidth()][image.getHeight()];

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (!visited[x][y]) {
                    int color = image.getRGB(x, y);
                    if ((color >> 24) == 0x00) {
                        // Skip transparent pixels
                        visited[x][y] = true;
                        continue;
                    }

                    Color keyColor = new Color(color, true);
                    StringBuilder path = colorToPathsMap.computeIfAbsent(keyColor, k -> new StringBuilder());

                    int height = 1;
                    int width = 1;

                    // Find the width of the block
                    for (int wx = x + 1; wx < image.getWidth() && image.getRGB(wx, y) == color; wx++) {
                        visited[wx][y] = true;
                        width++;
                    }

                    // Check vertically
                    boolean expandHeight = true;
                    while (expandHeight && (y + height) < image.getHeight()) {
                        for (int wx = x; wx < x + width; wx++) {
                            if (image.getRGB(wx, y + height) != color) {
                                expandHeight = false;
                                break;
                            }
                        }

                        if (expandHeight) {
                            for (int wx = x; wx < x + width; wx++) {
                                visited[wx][y + height] = true;
                            }
                            height++;
                        }
                    }

                    addPath(path, x, y, width, height);
                }
            }
        }

        colorToPathsMap.forEach((color, path) -> {
            String hex = String.format("#%02x%02x%02x",
                    color.getRed(),
                    color.getGreen(),
                    color.getBlue());
            svgBuilder.append("<path fill='")
                    .append(hex)
                    .append("' d='")
                    .append(path)
                    .append("'/>");
        });

        svgBuilder.append("</svg>");
        return svgBuilder.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void addPath(StringBuilder pathBuilder, int x, int y, int width, int height) {
        pathBuilder.append("M")
                .append(x)
                .append(",")
                .append(y)
                .append("h")
                .append(width)
                .append("v")
                .append(height)
                .append("h")
                .append(-width)
                .append("z");
    }

    private byte[] bufferedImageToByteArray(BufferedImage image, String format, float quality) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        if ("jpeg".equalsIgnoreCase(format) || "jpg".equalsIgnoreCase(format)) {
            ImageWriter jpegWriter = ImageIO.getImageWritersByFormatName("jpeg").next();
            ImageWriteParam jpegWriteParam = jpegWriter.getDefaultWriteParam();
            if (jpegWriteParam.canWriteCompressed()) {
                jpegWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                jpegWriteParam.setCompressionQuality(quality);
            }

            try (ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(byteArrayOutputStream)) {
                jpegWriter.setOutput(imageOutputStream);
                jpegWriter.write(null, new IIOImage(image, null, null), jpegWriteParam);
                jpegWriter.dispose();
            }
        } else if ("png".equalsIgnoreCase(format)) {
            ImageIO.write(image, format, byteArrayOutputStream);
        }

        return byteArrayOutputStream.toByteArray();
    }

    public byte[][] convertImages(MultipartFile[] files, String format, float quality) throws IOException {
        byte[][] results = new byte[files.length][];
        for (int i = 0; i < files.length; i++) {
            BufferedImage inputImage = ImageIO.read(files[i].getInputStream());
            if (inputImage == null) {
                throw new IOException("Could not open or find the image at index " + i);
            }

            BufferedImage result = removeBackground(inputImage);

            if ("svg".equalsIgnoreCase(format)) {
                byte[] svgBytes = bufferedImageToSvg(result);
                try {
                    results[i] = svgOptimizationService.optimizeSvg(svgBytes);
                } catch (Exception e) {
                    throw new IOException("SVG optimization failed for image at index " + i, e);
                }
            } else {
                results[i] = bufferedImageToByteArray(result, format, quality);
            }
        }
        return results;
    }
}