package org.simpleneo.datasource;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.web.SpringBootServletInitializer;

@SpringBootApplication
public class DatasourcesApplication extends SpringBootServletInitializer {

	public static void main(String[] args) {
		System.getProperties().put( "server.port", 8090);
		SpringApplication.run(DatasourcesApplication.class, args);
	}

}