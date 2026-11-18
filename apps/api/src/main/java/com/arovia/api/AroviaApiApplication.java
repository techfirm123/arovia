package com.arovia.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan(basePackages = "com.arovia.api")
public class AroviaApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(AroviaApiApplication.class, args);
    }
}