package com.edu.imageconversion.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(title = "WebScrape API", version = "1.0", description = "API for web scraping"))
public class OpenApiConfig {
}
