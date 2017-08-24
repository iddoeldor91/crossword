package com.ie.services;


import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.ie.models.Word;
import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.json.JsonParserFactory;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CsvWordBankService implements WordBankService {

    private static final Logger LOG = LoggerFactory.getLogger(CsvWordBankService.class);

    private static final String COLUMN_DELIMITER = ",";
    private static final String ROW_DELIMITER = "\\|";

    private List<String> wordBankList;
    private String srcFilePath;
    private final int WORD_COLUMN = 0, OCCURRENCES_COLUMN = 1;

    private LoadingCache<String, String> cache;

    private void init() {
        cache = CacheBuilder.newBuilder()
                .recordStats()
                .maximumSize(100)
                .recordStats()
                .build(new CacheLoader<String, String>() {
                    @Override
                    public String load(String key) throws Exception {
                        return getClue(key);
                    }
                });

        wordBankList = new LinkedList<>();
        String content;
        try {
            content = IOUtils.toString(getClass().getResourceAsStream(srcFilePath), "UTF-8");
            LOG.info("words loaded from resource file successfully");
            parseLines(content);
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
        wordBankList = wordBankList.subList(0, 25);
        List<Word> wordList = wordBankList.stream().map(w -> new Word(w, null)).collect(Collectors.toList());
        try {
            initClues(wordList);
        } catch (InterruptedException e) {
            LOG.error("error init clues: " + e.getMessage());
        }
        int before = wordBankList.size();
        wordList.forEach(word -> {
            if (word.getClue() == null) {
                String curWord = word.getWord();
                System.out.println("did not found clue for word [" + curWord + "]");
                wordBankList.remove(curWord);
            }
        });
        LOG.info("before: " + before + "\tafter: " + wordBankList.size());
    }

    private void initClues(List<Word> wordList) throws InterruptedException {
        ExecutorService es = Executors.newFixedThreadPool(wordList.size());

        wordList.forEach(word -> es.execute(() -> {
            String _word = word.getWord();
            String clue = this.getClue(_word);
            if (clue != null) {
                cache.put(_word, clue);
            }
            word.setClue(clue);
        }));

        es.shutdown();
        es.awaitTermination(60, TimeUnit.SECONDS);
    }

    @Override
    public List<Word> getRandomWords(int level) {
        List<Word> wordList = new ArrayList<>();
        Set<Integer> numbersAlreadyPicked = new HashSet<>();
        int numberOfRequiredWords = 12;
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
    public void getClues(List<Word> wordList) {
        ExecutorService es = Executors.newFixedThreadPool(wordList.size());
        wordList.forEach(word -> {
            LOG.debug("going for getClue " + word.getWord() + "\t" + word.toString());
            es.execute(() -> {
                try {
                    word.setClue(cache.get(word.getWord()));
                } catch (ExecutionException e) {
                    LOG.error("error getting from cache: " + e.getMessage());
                }
            });
        });
        try {
            es.shutdown();
            es.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOG.error(e.getMessage());
            e.printStackTrace();
        }
    }

    private String getClue(String query) {
        // TODO extract to config
        final String userAgent = "Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/53.0.2785.116 Safari/537.36";
        final String url = "https://www.google.co.il/search?site=imghp&tbm=isch&source=hp&gws_rd=cr&tbs=isz:m&q=";
        final String referrer = "https://www.google.co.il/";
        final String querySelect1 = "div.rg_meta";
        final String jsonUrlKey = "ou";

        String imageBase64 = null;
        try {
            Document doc = Jsoup.connect(url + query).userAgent(userAgent).referrer(referrer).get();
            Elements elements = doc.select(querySelect1);
            String firstElement = elements.first().childNodes().get(0).toString();
            String ou = JsonParserFactory.getJsonParser().parseMap(firstElement).get(jsonUrlKey).toString();
            byte[] imageBytes = IOUtils.toByteArray(new URL(ou));
            imageBase64 = Base64.getEncoder().encodeToString(imageBytes);
            LOG.debug("fetching clue \t" + query + "\n" + ou + "\n" + imageBase64.length());
        } catch (Exception e) {
            LOG.error("#get clue: " + e.getMessage());
            // TODO handle exception.. either return text with the clue and "sorry, here is the solution" or try again
        }
        return imageBase64;
    }

    public void setCsvSourceFilePath(String csvSourceFilePath) {
        this.srcFilePath = csvSourceFilePath;
    }

}