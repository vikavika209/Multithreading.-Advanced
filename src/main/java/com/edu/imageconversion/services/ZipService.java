package com.edu.imageconversion.services;

import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ZipService {

    public byte[] zipFiles(byte[][] files, String format) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {
            for (int i = 0; i < files.length; i++) {
                ZipEntry zipEntry = new ZipEntry("image" + (i + 1) + "." + format);
                zipOutputStream.putNextEntry(zipEntry);
                zipOutputStream.write(files[i]);
                zipOutputStream.closeEntry();
            }
        }

        return byteArrayOutputStream.toByteArray();
    }
}