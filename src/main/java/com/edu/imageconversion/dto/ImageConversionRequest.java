package com.edu.imageconversion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.multipart.MultipartFile;

public class ImageConversionRequest {

    @Schema(type = "array", format = "binary", description = "Array of images to be uploaded")
    private MultipartFile[] files;

    @Schema(description = "Target format for image conversion, either 'png' or 'svg'")
    private String format;

    @Schema(description = "Quality of the images after conversion, default is 0.8")
    private float quality = 0.8f;

    // Getters and setters
    public MultipartFile[] getFiles() {
        return files;
    }

    public void setFiles(MultipartFile[] files) {
        this.files = files;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public float getQuality() {
        return quality;
    }

    public void setQuality(float quality) {
        this.quality = quality;
    }
}