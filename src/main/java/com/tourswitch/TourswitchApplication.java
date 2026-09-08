package com.tourswitch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class TourswitchApplication {

    public static void main(String[] args) {
        SpringApplication.run(TourswitchApplication.class, args);
    }

}
