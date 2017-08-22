package com.ie.services;

import com.ie.CrosswordApplication;
import com.ie.models.Word;
import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.util.ResourceUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

public class CsvWordBankService implements WordBankService {

    private static final Logger LOG = LoggerFactory.getLogger(CsvWordBankService.class);

    private List<String> wordBankList;
    private String srcFilePath;

    private void init() throws URISyntaxException {
        wordBankList = new ArrayList<>();
        final int WORD_COLUMN = 0, OCCURRENCES_COLUMN = 1;
        try {
            Path path = Paths.get(ClassLoader.getSystemResource("words.txt").toURI());
            Stream<String> lines = Files.lines(path);
            lines.forEach(l -> {
                String[] split = l.split(",");
                Integer occurrences = Integer.valueOf(split[OCCURRENCES_COLUMN].trim());
                String word = split[WORD_COLUMN];
                int length = word.length();
                if (length > 4 && length < 7) {
                    if (
                            occurrences < 1_000 ||
                                    word.contains("'") ||
                                    word.contains("-") ||
                                    word.contains("\"") ||
                                    word.startsWith("ש") ||
                                    word.startsWith("ב") ||
                                    word.startsWith("כ") ||
                                    word.startsWith("ה") ||
                                    word.startsWith("ל") ||
                                    word.endsWith("ים")
                            ) {
                        // pass
                    } else {
                        wordBankList.add(word);
                    }
                }
            });
            lines.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
    public String getClue(String query) {
        final String userAgent = "Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/53.0.2785.116 Safari/537.36";
        final String url = "https://www.google.co.il/search?site=imghp&tbm=isch&source=hp&gws_rd=cr&tbs=isz:m&q=";
        final String referrer = "https://www.google.com/";
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
            LOG.debug("\n" + query + "\n" + ou + "\n" + imageBase64.length());
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }

        return imageBase64;
    }

    public void setCsvSourceFilePath(String csvSourceFilePath) {
        this.srcFilePath = csvSourceFilePath;
    }

    private String getCsvSourceFilePath() {
        return srcFilePath;
    }
}
