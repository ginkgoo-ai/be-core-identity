package com.benwk.ginkgoocoreidentity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class GinkgooCoreIdentityApplication {

	public static void main(String[] args) {
		SpringApplication.run(GinkgooCoreIdentityApplication.class, args);
	}

}
