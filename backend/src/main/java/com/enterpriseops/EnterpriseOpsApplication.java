package com.enterpriseops;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class EnterpriseOpsApplication {
    public static void main(String[] args) {
        SpringApplication.run(EnterpriseOpsApplication.class, args);
    }
}
