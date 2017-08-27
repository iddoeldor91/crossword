package com.ie.controller;

import com.ie.CrosswordApplication;
import com.ie.dao.ClueDao;
import com.ie.model.Crossword;
import com.ie.model.Word;
import com.ie.service.WordBankService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.stream.Collectors;

@Controller
public class CrosswordController {

    private static final Logger LOG = LoggerFactory.getLogger(CrosswordApplication.class);

    @Autowired
    private WordBankService wordBankService;

    @Autowired
    private ClueDao clueDao;

    private static final String TEMPLATE_CLUE = "clue";

    @GetMapping("/get")
    public @ResponseBody Crossword generateCrossword(@RequestParam(value="level", defaultValue= "1") int level) {
        Crossword crossword = new Crossword(wordBankService.getRandomWords(level));
        crossword.computeCrossword();

        LOG.debug("word bank [{}]", crossword.getAvailableWords().stream().map(Word::getWord).collect(Collectors.toList()));

//        Stream.of(crossword.getGrid()).forEach(row -> System.out.println(Arrays.toString(row)));

        wordBankService.getCluesInParallel(crossword.getCurrentWordList());

        LOG.debug("used {} of of {} words", crossword.getCurrentWordList().size(), crossword.getAvailableWords().size());

        return crossword;
    }

    @GetMapping("/clue/{word}")
    public String getClue(Model model, @PathVariable final String word) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        String clue = clueDao.getClue(word);
        stopWatch.stop();
        LOG.debug("Execution time [{}] [{}] [{}]", stopWatch, word, clue.getBytes().length);
        model.addAttribute("word", word);
        model.addAttribute("clue", clue);
        return TEMPLATE_CLUE;
    }

    @GetMapping("cache/evict/{word}")
    public @ResponseBody String evictClueFromCache(@PathVariable final String word) {
        clueDao.evictFromCache(word);
        return word + " evicted";
    }

    @GetMapping("cache/init")
    public @ResponseBody String initClues() {
        StopWatch sw = new StopWatch();
        sw.start();
        wordBankService.initClues();
        sw.stop();
        return "init clues finished " + sw.toString();
    }

}
