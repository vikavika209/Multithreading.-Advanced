package com.edu.imageconversion;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Scanner;

@SpringBootApplication(scanBasePackages = "com.edu.imageconversion")
public class WebScrapeApplication {

	public static void main(String[] args) {
		SpringApplication.run(WebScrapeApplication.class, args);
	}
}