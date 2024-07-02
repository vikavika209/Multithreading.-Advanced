package com.edu.imageconversion.services;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class ImageProcessingService {

    private BufferedImage removeBackground(BufferedImage source) throws IOException {
        int width = source.getWidth();
        int height = source.getHeight();
        BufferedImage outputImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        Color backgroundColor = Color.WHITE;  // Задаём цвет фона (например, белый)
        int tolerance = 30;  // Допустимое отклонение для определения фона

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = source.getRGB(x, y);
                Color color = new Color(rgb, true);

                if (isBackground(color, backgroundColor, tolerance)) {
                    outputImage.setRGB(x, y, 0x00FFFFFF);  // Установим прозрачный пиксель
                } else {
                    outputImage.setRGB(x, y, rgb);  // Скопируем пиксель как есть
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

    private byte[] bufferedImageToByteArray(BufferedImage image, String format) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ImageIO.write(image, format, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    public byte[] convertImage(MultipartFile file, String format) throws IOException {
        BufferedImage inputImage = ImageIO.read(file.getInputStream());
        if (inputImage == null) {
            throw new IOException("Could not open or find the image");
        }

        BufferedImage result = removeBackground(inputImage);

        return bufferedImageToByteArray(result, format);
    }
}