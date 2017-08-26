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

import java.net.URL;
import java.util.Base64;

@Repository
public class ClueDao {

    private static final Logger LOG = LoggerFactory.getLogger(ClueDao.class);

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/53.0.2785.116 Safari/537.36";
    private static final String IMAGE_PROVIDER_URL = "https://www.google.co.il/search?site=imghp&tbm=isch&source=hp&gws_rd=cr&tbs=isz:m&q=";
    private static final String REFERRER = "https://www.google.co.il/";
    private static final String IMAGE_QUERY_SELECTOR = "div.rg_meta";
    private static final String URL_MAP_KEY = "ou";
    private static final int TIMEOUT_MS = 3_000;
    private static final String CACHE_KEY = "clues";

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
        try {
            LOG.debug("trying to fetch clue for word [{}]", word);
            Document doc = Jsoup.connect(IMAGE_PROVIDER_URL + word)
                    .userAgent(USER_AGENT).referrer(REFERRER).timeout(TIMEOUT_MS)
                .get();
            Elements elements = doc.select(IMAGE_QUERY_SELECTOR);
            String firstElement = elements.first().childNodes().get(0).toString();
            String imgUrl = JsonParserFactory.getJsonParser().parseMap(firstElement).get(URL_MAP_KEY).toString();
            URL url = new URL(imgUrl);
            byte[] imageBytes = IOUtils.toByteArray(url);
            result = Base64.getEncoder().encodeToString(imageBytes);
            LOG.info("fetched clue [{}] url [{}] length [{}]", word, imgUrl, result.length());
        } catch (Exception e) {
            LOG.error("Error fetching clue [{}] [{}]", word, e.getMessage());
        }
        return result;
    }

    @CacheEvict(CACHE_KEY)
    public void evictFromCache(final String word) {
        LOG.debug("Evict from cache [{}]", word);
    }

    @CacheEvict(value = CACHE_KEY, allEntries = true)
    public void evictAll() {
        LOG.debug("evict all from cache");
    }
}
