package com.tourswitch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
public class TourswitchApplication {

    public static void main(String[] args) {
        SpringApplication.run(TourswitchApplication.class, args);
    }

}
