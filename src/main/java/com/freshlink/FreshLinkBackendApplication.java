package com.freshlink;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication()
@EnableCaching 
public class FreshLinkBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(FreshLinkBackendApplication.class, args);
	}

}
