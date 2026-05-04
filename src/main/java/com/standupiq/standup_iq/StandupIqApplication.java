package com.standupiq.standup_iq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class StandupIqApplication {

	public static void main(String[] args) {
		SpringApplication.run(StandupIqApplication.class, args);
	}

}
