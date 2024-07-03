package com.edu.imageconversion.controllers;

import com.edu.imageconversion.dto.ImageConversionRequest;
import com.edu.imageconversion.services.ImageProcessingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/api/image")
public class ImageProcessingController {
    private final ImageProcessingService imageProcessingService;

    public ImageProcessingController(ImageProcessingService imageProcessingService) {
        this.imageProcessingService = imageProcessingService;
    }

    @Operation(summary = "Convert images", description = "Convert uploaded images to PNG or SVG and remove background")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Images converted successfully", content = @Content(mediaType = "application/zip")),
            @ApiResponse(responseCode = "400", description = "Invalid input data", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @PostMapping(value = "/convert", consumes = {"multipart/form-data"})
    public ResponseEntity<byte[]> convertAndZipImages(
            @RequestPart(value = "files", required = true) MultipartFile[] files,
            @RequestParam("format") String format,
            @RequestParam(value = "quality", defaultValue = "0.8") float quality) {
        try {
            if (!format.equalsIgnoreCase("png") && !format.equalsIgnoreCase("svg")) {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }

            byte[][] convertedImages = imageProcessingService.convertImages(files, format, quality);

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            try (ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {
                for (int i = 0; i < convertedImages.length; i++) {
                    ZipEntry zipEntry = new ZipEntry("image" + (i + 1) + "." + format);
                    zipOutputStream.putNextEntry(zipEntry);
                    zipOutputStream.write(convertedImages[i]);
                    zipOutputStream.closeEntry();
                }
            }

            byte[] zipData = byteArrayOutputStream.toByteArray();

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=converted_images.zip");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(zipData);
        } catch (IOException e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}