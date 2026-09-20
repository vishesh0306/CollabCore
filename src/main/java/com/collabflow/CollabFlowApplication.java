package com.collabflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling // for the overdue reminder job // finds the @ConfigurationProperties records (e.g. JwtProperties)
public class CollabFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(CollabFlowApplication.class, args);
    }
}
