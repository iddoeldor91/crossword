package com.ie.service;


import com.ie.dao.ClueDao;
import com.ie.model.Word;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class CsvWordBankService implements WordBankService {

    private static final Logger LOG = LoggerFactory.getLogger(CsvWordBankService.class);

    @Autowired
    private ClueDao clueDao;
    private List<String> wordBankList;

    private static final String srcFilePath = "/static/zip_words.txt"; // TODO extract
    private static final int WORD_COLUMN = 0, OCCURRENCES_COLUMN = 1, numberOfRequiredWords = 12;
    private static final String COLUMN_DELIMITER = ",";
    private static final String ROW_DELIMITER = "\\|";

    @PostConstruct
    private void init() {
        wordBankList = new LinkedList<>();
        String content;
        try {
            content = IOUtils.toString(getClass().getResourceAsStream(srcFilePath), "UTF-8");
            LOG.debug("words loaded from resource file successfully");
            parseLines(content);
            wordBankList = wordBankList.subList(0, 25);
//            initClues(); // TODO doesn't seem to work on start.. need to investigate why
        } catch (IOException e) {
            LOG.error("words failed, getting fallback words" + e.getMessage());
        }
    }

    private void parseLines(String str) {
        String[] lines = str.split(ROW_DELIMITER);
        Stream<String> stream = Stream.of(lines);
        stream.forEach(l -> {
            String[] split = l.split(COLUMN_DELIMITER);
            Integer occurrences = Integer.valueOf(split[OCCURRENCES_COLUMN].trim());
            String word = split[WORD_COLUMN];
            int length = word.length();
            if (length > 3 && length < 8) {
                boolean wordRules = occurrences < 1_000 ||
                        word.contains("'") ||
                        word.contains("-") ||
                        word.contains("\"") ||
                        word.startsWith("ש") ||
                        word.startsWith("ב") ||
                        word.startsWith("כ") ||
                        word.startsWith("ה") ||
                        word.startsWith("ל") ||
                        word.endsWith("ים");
                if (!wordRules) {
                    wordBankList.add(word);
                }
            }
        });
        stream.close();
    }

    @Override
    public void initClues() {
        clueDao.evictAll();

        int before = wordBankList.size();
        List<Word> wordList = wordBankList.stream().map(w -> new Word(w, null)).collect(Collectors.toList());

        getCluesInParallel(wordList);

        // remove words without clues
        wordList.stream().filter(w -> w.getClue() == null).forEach(w -> wordBankList.remove(w.getWord()));

        LOG.info("before [{}] after [{}]", before, wordBankList.size());
    }

    @Override
    public List<Word> getRandomWords(int level) {
        List<Word> wordList = new ArrayList<>();
        Set<Integer> numbersAlreadyPicked = new HashSet<>();
        int wordBankSize = wordBankList.size();
        for (int i = 0; i < numberOfRequiredWords; i++) {
            int randomNumber;
            do {
                randomNumber = ThreadLocalRandom.current().nextInt(wordBankSize);
            } while (numbersAlreadyPicked.contains(randomNumber));
            numbersAlreadyPicked.add(randomNumber);
            String word = wordBankList.get(randomNumber);
            wordList.add(new Word(word, null));
        }
        return wordList;
    }


    @Override
    public void getCluesInParallel(List<Word> wordList) {
        ExecutorService es = Executors.newFixedThreadPool(wordList.size());
        wordList.forEach(word -> es.execute(() -> word.setClue(clueDao.getClue(word.getWord()))));
        es.shutdown();
        try {
            es.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOG.error(e.getMessage());
        }
    }

}