package com.lulajax.instagraph;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class InstaGraphApplication {

    public static void main(String[] args) {
        SpringApplication.run(InstaGraphApplication.class, args);
    }

}
