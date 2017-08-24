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

import java.util.stream.Collectors;

@Controller
@RequestMapping("/get")
public class CrosswordGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(CrosswordApplication.class);

    @Autowired
    private WordBankService wordBankService;

    @RequestMapping(method= RequestMethod.GET)
    public @ResponseBody Crossword generateCrossword(@RequestParam(value="level", defaultValue= "1") int level) {
        Crossword crossword = new Crossword(wordBankService.getRandomWords(level));
        crossword.computeCrossword();

        LOG.debug("word bank " + crossword.getAvailableWords().stream().map(Word::getWord).collect(Collectors.toList()));

        LOG.debug("Crossword grid");
//        Stream.of(crossword.getGrid()).forEach(row -> System.out.println(Arrays.toString(row)));

        LOG.debug("legend");

        wordBankService.getClues(crossword.getCurrentWordList());

        LOG.debug("used " + crossword.getCurrentWordList().size() + " out of " + crossword.getAvailableWords().size() + " words");

        return crossword;
    }

}
