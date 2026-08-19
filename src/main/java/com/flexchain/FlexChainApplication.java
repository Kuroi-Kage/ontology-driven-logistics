package com.flexchain;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FlexChainApplication {

    public static void main(String[] args) {

        SpringApplication.run(FlexChainApplication.class,args);

    }

}