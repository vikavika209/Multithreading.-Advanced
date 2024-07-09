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
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

@Service
public class ImageProcessingService {
    private final SvgOptimizationService svgOptimizationService;
    private final ExecutorService executorService;

    public ImageProcessingService(SvgOptimizationService svgOptimizationService) {
        this.svgOptimizationService = svgOptimizationService;
        this.executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    private Color findDominantColor(BufferedImage image, int margin) {
        int width = image.getWidth();
        int height = image.getHeight();
        ConcurrentHashMap<Color, AtomicInteger> colorCount = new ConcurrentHashMap<>();

        for (int y = 0; y < margin; y++) {
            for (int x = 0; x < margin; x++) {
                addColorCount(colorCount, new Color(image.getRGB(x, y)));
                addColorCount(colorCount, new Color(image.getRGB(width - 1 - x, y)));
                addColorCount(colorCount, new Color(image.getRGB(x, height - 1 - y)));
                addColorCount(colorCount, new Color(image.getRGB(width - 1 - x, height - 1 - y)));
            }
        }

        return colorCount.entrySet().stream()
                .max(Map.Entry.comparingByValue(Comparator.comparingInt(AtomicInteger::get)))
                .map(Map.Entry::getKey)
                .orElse(Color.WHITE);
    }

    private void addColorCount(ConcurrentHashMap<Color, AtomicInteger> colorCount, Color color) {
        colorCount.computeIfAbsent(color, k -> new AtomicInteger()).incrementAndGet();
    }

    private boolean isBackgroundColor(Color color, Color bgColor, int tolerance) {
        int rDiff = Math.abs(color.getRed() - bgColor.getRed());
        int gDiff = Math.abs(color.getGreen() - bgColor.getGreen());
        int bDiff = Math.abs(color.getBlue() - bgColor.getBlue());
        return rDiff < tolerance && gDiff < tolerance && bDiff < tolerance;
    }

    private BufferedImage removeBackground(BufferedImage source) throws IOException {
        int width = source.getWidth();
        int height = source.getHeight();
        BufferedImage outputImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        int margin = Math.min(width, height) / 10;
        Color backgroundColor = findDominantColor(source, margin);
        int tolerance = 30;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = source.getRGB(x, y);
                Color color = new Color(rgb, true);

                if (isBackgroundColor(color, backgroundColor, tolerance)) {
                    outputImage.setRGB(x, y, 0x00FFFFFF);
                } else {
                    outputImage.setRGB(x, y, rgb);
                }
            }
        }
        return outputImage;
    }

    private byte[] bufferedImageToSvg(BufferedImage image) throws IOException {
        int width = image.getWidth();
        int height = image.getHeight();

        // Создаем новый документ для SVG
        DOMImplementation domImpl = GenericDOMImplementation.getDOMImplementation();
        Document document = domImpl.createDocument(null, "svg", null);

        // Создаем SVGGraphics2D для рисования SVG
        SVGGraphics2D svgGenerator = new SVGGraphics2D(document);

        // Рисуем изображение на SVGGraphics2D
        svgGenerator.drawImage(image, 0, 0, null);

        // Получаем корневой элемент SVG и устанавливаем атрибуты
        org.w3c.dom.Element root = svgGenerator.getRoot();
        root.setAttributeNS(null, "width", String.valueOf(width));
        root.setAttributeNS(null, "height", String.valueOf(height));
        root.setAttributeNS(null, "viewBox", "0 0 " + width + " " + height);

        // Генерация SVG контента в виде строки
        StringWriter writer = new StringWriter();
        svgGenerator.stream(root, writer);

        // Конвертация строки в байты
        return writer.toString().getBytes(StandardCharsets.UTF_8);
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

            // Убедитесь, что размеры результирующего изображения совпадают с исходными
            int width = inputImage.getWidth();
            int height = inputImage.getHeight();

            BufferedImage result = removeBackground(inputImage);

            // После удаления фона проверим, что размеры совпадают
            if (result.getWidth() != width || result.getHeight() != height) {
                BufferedImage correctedResult = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = correctedResult.createGraphics();
                g.drawImage(result, 0, 0, width, height, null);
                g.dispose();
                result = correctedResult;
            }

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

    public byte[][] convertImagesParallel(MultipartFile[] files, String format, float quality) throws IOException {
        List<Future<byte[]>> futures = IntStream.range(0, files.length)
                .mapToObj(i -> executorService.submit(() -> {
                    try {
                        BufferedImage inputImage = ImageIO.read(files[i].getInputStream());
                        if (inputImage == null) {
                            throw new IOException("Could not open or find the image at index " + i);
                        }

                        // Убедитесь, что размеры результирующего изображения совпадают с исходными
                        int width = inputImage.getWidth();
                        int height = inputImage.getHeight();

                        BufferedImage result = removeBackground(inputImage);

                        // После удаления фона проверим, что размеры совпадают
                        if (result.getWidth() != width || result.getHeight() != height) {
                            BufferedImage correctedResult = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                            Graphics2D g = correctedResult.createGraphics();
                            g.drawImage(result, 0, 0, width, height, null);
                            g.dispose();
                            result = correctedResult;
                        }

                        if ("svg".equalsIgnoreCase(format)) {
                            byte[] svgBytes = bufferedImageToSvg(result);
                            try {
                                return svgOptimizationService.optimizeSvg(svgBytes);
                            } catch (Exception e) {
                                throw new IOException("SVG optimization failed for image at index " + i, e);
                            }
                        } else {
                            return bufferedImageToByteArray(result, format, quality);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }))
                .collect(Collectors.toList());

        byte[][] results = new byte[files.length][];
        for (int i = 0; i < files.length; i++) {
            try {
                results[i] = futures.get(i).get();
            } catch (InterruptedException | ExecutionException e) {
                throw new IOException("Error processing image at index " + i, e);
            }
        }

        return results;
    }
}