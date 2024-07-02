package com.edu.imageconversion.services;

import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.svg2svg.SVGTranscoder;
import org.apache.batik.util.XMLResourceDescriptor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
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

    private byte[] optimizeSvg(byte[] svgBytes) throws IOException, TranscoderException {
        String parser = XMLResourceDescriptor.getXMLParserClassName();
        SAXSVGDocumentFactory factory = new SAXSVGDocumentFactory(parser);
        String uri = "http://www.w3.org/2000/svg";
        org.w3c.dom.Document svgDocument = factory.createDocument(uri, new ByteArrayInputStream(svgBytes));

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(byteArrayOutputStream, "UTF-8");

        TranscoderInput input = new TranscoderInput(svgDocument);
        TranscoderOutput output = new TranscoderOutput(outputStreamWriter);

        SVGTranscoder transcoder = new SVGTranscoder();
        transcoder.transcode(input, output);

        outputStreamWriter.flush();
        outputStreamWriter.close();
        return byteArrayOutputStream.toByteArray();
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

    public byte[] convertImage(MultipartFile file, String format, float quality) throws IOException, TranscoderException {
        BufferedImage inputImage = ImageIO.read(file.getInputStream());
        if (inputImage == null) {
            throw new IOException("Could not open or find the image");
        }

        BufferedImage result = removeBackground(inputImage);

        if ("svg".equalsIgnoreCase(format)) {
            byte[] svgBytes = bufferedImageToSvg(result);
            return optimizeSvg(svgBytes);
        }

        return bufferedImageToByteArray(result, format, quality);
    }
}