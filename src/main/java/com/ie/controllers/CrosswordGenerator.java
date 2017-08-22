package com.ie.controllers;

import com.ie.CrosswordApplication;
import com.ie.models.Crossword;
import com.ie.models.Word;
import com.ie.services.WordBankService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Controller
@RequestMapping("/get")
public class CrosswordGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(CrosswordApplication.class);

    @Autowired
    private WordBankService wordBankService;

    @RequestMapping(method= RequestMethod.GET)
    public @ResponseBody Crossword generateCrossword (@RequestParam(value="level", defaultValue= "1") int level) {
        Crossword crossword = new Crossword(wordBankService.getRandomWords(level));
        crossword.computeCrossword();

        LOG.info("word bank " + crossword.getAvailableWords().stream().map(Word::getWord).collect(Collectors.toList()));

        LOG.debug("Crossword grid");
        Stream.of(crossword.getGrid()).forEach(row -> System.out.println(Arrays.toString(row)));
        LOG.debug("legend");

        // getting clues
        ExecutorService es = Executors.newFixedThreadPool(crossword.getCurrentWordList().size());
        crossword.getCurrentWordList().forEach(word -> {
            LOG.debug(word.toString());
            es.execute(() -> word.setClue(wordBankService.getClue(word.getWord())));
        });
        try {
            es.shutdown();
            es.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        LOG.debug("used " + crossword.getCurrentWordList().size() + " out of " + crossword.getAvailableWords().size() + " words");

        return crossword;
    }

}
