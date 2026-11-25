package com.vone.simrs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching 
public class SimrsModernApplication {

    public static void main(String[] args) {
        SpringApplication.run(SimrsModernApplication.class, args);
    }
}
