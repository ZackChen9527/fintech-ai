package com.codinghappy.fintechai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FinancialLeadApplication {
    public static void main(String[] args) {
        SpringApplication.run(FinancialLeadApplication.class, args);
    }
}
