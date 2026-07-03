package com.baas.flashsale;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FlashSaleBaasApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlashSaleBaasApplication.class, args);
    }

}
