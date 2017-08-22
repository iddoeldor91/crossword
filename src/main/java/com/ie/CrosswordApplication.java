package com.ie;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportResource;

/**
 * https://spring.io/guides/gs/actuator-service/
 * https://devcenter.heroku.com/articles/deploying-spring-boot-apps-to-heroku#preparing-a-spring-boot-app-for-heroku
 * /home/iddo/.ssh/id_rsa.pub
 */
@SpringBootApplication
@ImportResource("classpath:config.xml")
public class CrosswordApplication {

	public static void main(String[] args) {
		SpringApplication.run(CrosswordApplication.class, args);
	}
}
