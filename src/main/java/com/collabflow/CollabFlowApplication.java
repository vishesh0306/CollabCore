package com.collabflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan // finds the @ConfigurationProperties records (e.g. JwtProperties)
public class CollabFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(CollabFlowApplication.class, args);
    }
}
