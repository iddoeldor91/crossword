package com.ie;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportResource;

/**
 * https://spring.io/guides/gs/actuator-service/
 * https://devcenter.heroku.com/articles/deploying-spring-boot-apps-to-heroku#preparing-a-spring-boot-app-for-heroku
 * https://cryptic-hollows-99859.herokuapp.com/ | https://git.heroku.com/cryptic-hollows-99859.git
 * git add . && git commit -m "init 5" && git push heroku master && heroku logs --tail
 * http://memorynotfound.com/spring-boot-create-executable-using-maven-parent-pom/
 * http://memorynotfound.com/selenium-record-video-junit-java/
 *
 */
@ImportResource("classpath:config.xml")
@SpringBootApplication
public class CrosswordApplication {
	public static void main(String[] args) {
		SpringApplication.run(CrosswordApplication.class, args);
	}
}