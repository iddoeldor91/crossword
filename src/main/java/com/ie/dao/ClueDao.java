package com.ie.dao;

import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.Base64;

@Repository
public class ClueDao {

    private static final Logger LOG = LoggerFactory.getLogger(ClueDao.class);

    private static final String CACHE_KEY = "clues";

    private static final String IMAGE_PROVIDER_API = "https://www.google.co.il/search?site=imghp&tbm=isch&source=hp&gws_rd=cr&tbs=isz:m&q=";
    // faking user agent
    private static final String UA = "Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/53.0.2785.116 Safari/537.36";
    // fake referee
    private static final String REF = "https://www.google.co.il/";
    private static final String IMAGE_QUERY_SELECTOR = "div.rg_meta";
    private static final String URL_MAP_KEY = "ou";
    // request timeout in milliseconds
    private static final int TO = 3_000;
    private static final int ATTEMPT_COUNTER = 3;

    /**
     *
     * @param word the word to get clue for
     * @return searching the word in google images and return the first image in base64
     * TODO if http status != 200, should continue to second image
     * in the future we should return Clue object which might be video/text/audio/two images
     */
    @Cacheable(CACHE_KEY)
    public String getClue(String word) {
        String result = null;
        int counter = 0;
        LOG.debug("trying to fetch clue for word [{}]", word);
        Elements elements = null;
        try {
            Document doc = Jsoup.connect(IMAGE_PROVIDER_API + word).userAgent(UA).referrer(REF).timeout(TO).get();
            elements = doc.select(IMAGE_QUERY_SELECTOR);
        } catch (IOException e) {
            LOG.error("failed fetching source for [{}]", word);
        }
        // source code exit, && haven't fetched clue url yet && not reached attempts limit
        while (elements != null && result == null && counter < ATTEMPT_COUNTER) {
            try {
                String firstElement = elements.get(counter).childNodes().get(0).toString();
                String imgUrl = JsonParserFactory.getJsonParser().parseMap(firstElement).get(URL_MAP_KEY).toString();
                URL url = new URL(imgUrl);
                byte[] imageBytes = IOUtils.toByteArray(url);
                result = Base64.getEncoder().encodeToString(imageBytes);
                LOG.info("fetched clue [{}] url [{}] length [{}]", word, imgUrl, result.length());
            } catch (Exception e) {
                LOG.error("Error fetching clue [{}] [{}]", word, e.getMessage());
                counter++;
            }
        }
        return result;
    }

    @CacheEvict(CACHE_KEY)
    public void evictFromCache(final String word) {
        LOG.debug("Evict from cache [{}]", word);
    }

    @CacheEvict(value = CACHE_KEY, allEntries = true)
    public void evictAll() {
        LOG.debug("evicted all from cache");
    }
}
