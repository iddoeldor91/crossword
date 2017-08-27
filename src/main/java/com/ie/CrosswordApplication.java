package com.ie;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * https://devcenter.heroku.com/articles/deploying-spring-boot-apps-to-heroku#preparing-a-spring-boot-app-for-heroku
 * https://cryptic-hollows-99859.herokuapp.com/ | https://git.heroku.com/cryptic-hollows-99859.git
 * git add . && git commit -m "refactoring" && git push heroku master && heroku logs --tail
 * http://memorynotfound.com/spring-boot-create-executable-using-maven-parent-pom/
 * http://memorynotfound.com/selenium-record-video-junit-java/
 *
 * TODO CRUD Clue
 * 1. Create new clue: auto complete from GoogleAutoSuggestionsAPI + pick image from google grid
 */
@EnableCaching
@SpringBootApplication
public class CrosswordApplication {
	public static void main(String[] args) {
		SpringApplication.run(CrosswordApplication.class, args);
	}
}