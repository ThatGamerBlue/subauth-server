package com.thatgamerblue.subauth.server;

import com.thatgamerblue.subauth.server.util.Env;
import java.util.Properties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
public class SubAuthServer {
	private static ApplicationContext appContext;

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(SubAuthServer.class);

		Properties properties = new Properties();
		properties.put("spring.datasource.url", Env.Database.URL.get());
		properties.put("spring.datasource.username", Env.Database.USER.get());
		properties.put("spring.datasource.password", Env.Database.PASSWORD.get());

		app.setDefaultProperties(properties);

		appContext = app.run(args);
	}
}
