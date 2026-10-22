package com.zaitul.servicedesk;

import org.springframework.boot.SpringApplication;

public class TestServiceDeskApplication {

	public static void main(String[] args) {
		SpringApplication.from(ServiceDeskApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
