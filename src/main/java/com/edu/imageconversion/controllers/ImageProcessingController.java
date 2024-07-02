package com.edu.imageconversion.controllers;

import com.edu.imageconversion.services.ImageProcessingService;
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

    public ImageProcessingController(ImageProcessingService imageProcessingService) {
        this.imageProcessingService = imageProcessingService;
    }

    @Operation(summary = "Convert image",
            description = "Convert an uploaded image to PNG or SVG and remove background")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Image converted successfully",
                    content = @Content(mediaType = "application/octet-stream")),
            @ApiResponse(responseCode = "400",
                    description = "Invalid input data",
                    content = @Content),
            @ApiResponse(responseCode = "500",
                    description = "Internal server error",
                    content = @Content)
    })
    @PostMapping(value = "/convert", consumes = {"multipart/form-data"})
    public ResponseEntity<byte[]> convertImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("format") String format) {
        try {
            // Проверяем чтобы формат был либо PNG, либо SVG
            if (!format.equalsIgnoreCase("png") && !format.equalsIgnoreCase("svg")) {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }

            // Конвертируем изображение
            byte[] result = imageProcessingService.convertImage(file, format);

            // Устанавливаем правильное расширение файла в заголовке Content-Disposition
            String filename = "converted_image." + format.toLowerCase();
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(result);
        } catch (IOException e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}