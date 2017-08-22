package com.ie;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportResource;

/**
 * https://spring.io/guides/gs/actuator-service/
 * https://devcenter.heroku.com/articles/deploying-spring-boot-apps-to-heroku#preparing-a-spring-boot-app-for-heroku
 * /home/iddo/.ssh/id_rsa.pub
 * https://cryptic-hollows-99859.herokuapp.com/ | https://git.heroku.com/cryptic-hollows-99859.git
 * git add . && git commit -m "init 5" && git push heroku master
 * heroku logs --tail
 */
@SpringBootApplication
@ImportResource("classpath:config.xml")
public class CrosswordApplication {

	public static void main(String[] args) {
		SpringApplication.run(CrosswordApplication.class, args);
	}
}
