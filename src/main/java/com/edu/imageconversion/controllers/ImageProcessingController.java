package com.edu.imageconversion.controllers;

import com.edu.imageconversion.services.ImageProcessingService;
import com.edu.imageconversion.services.ZipService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/image")
public class ImageProcessingController {
    private final ImageProcessingService imageProcessingService;
    private final ZipService zipService;

    public ImageProcessingController(ImageProcessingService imageProcessingService, ZipService zipService) {
        this.imageProcessingService = imageProcessingService;
        this.zipService = zipService;
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
            byte[] zipData = zipService.zipFiles(convertedImages, format);

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

    @Operation(summary = "Convert images in parallel", description = "Convert uploaded images to PNG or SVG and remove background using parallel processing")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Images converted successfully", content = @Content(mediaType = "application/zip")),
            @ApiResponse(responseCode = "400", description = "Invalid input data", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @PostMapping(value = "/convert-parallel", consumes = {"multipart/form-data"})
    public ResponseEntity<byte[]> convertAndZipImagesParallel(
            @RequestPart(value = "files", required = true) MultipartFile[] files,
            @RequestParam("format") String format,
            @RequestParam(value = "quality", defaultValue = "0.8") float quality) {
        try {
            if (!format.equalsIgnoreCase("png") && !format.equalsIgnoreCase("svg")) {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }

            byte[][] convertedImages = imageProcessingService.convertImagesParallel(files, format, quality);
            byte[] zipData = zipService.zipFiles(convertedImages, format);

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=converted_images_parallel.zip");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(zipData);
        } catch (IOException e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}