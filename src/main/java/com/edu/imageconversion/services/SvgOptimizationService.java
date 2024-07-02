package com.edu.imageconversion.services;

import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.svg2svg.SVGTranscoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class SvgOptimizationService {

    private static final Logger logger = Logger.getLogger(SvgOptimizationService.class.getName());

    public byte[] optimizeSvg(byte[] svgData) throws IOException {
        logger.info("Starting SVG optimization");

        if (svgData == null || svgData.length == 0) {
            throw new IllegalArgumentException("SVG data is invalid or empty");
        }

        try (StringWriter stringWriter = new StringWriter()) {

            String svgContent = new String(svgData, StandardCharsets.UTF_8);
            TranscoderInput input = new TranscoderInput(new StringReader(svgContent));
            TranscoderOutput output = new TranscoderOutput(stringWriter);

            SVGTranscoder transcoder = new SVGTranscoder();

            logger.info("Transcoding SVG...");
            transcoder.transcode(input, output);
            logger.info("Transcoding completed.");

            // Convert the optimized SVG content back to bytes
            return stringWriter.toString().getBytes(StandardCharsets.UTF_8);
        } catch (TranscoderException e) {
            logger.log(Level.SEVERE, "TranscoderException during SVG optimization: ", e);
            throw new IOException("SVG optimization failed", e);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Unexpected exception during SVG optimization: ", ex);
            throw new IOException("Unexpected error during SVG optimization", ex);
        }
    }
}