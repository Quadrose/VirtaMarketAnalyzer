package ru.VirtaMarketAnalyzer.scrapper;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.VirtaMarketAnalyzer.main.Utils;
import ru.VirtaMarketAnalyzer.main.Wizard;

import java.io.File;
import java.io.IOException;
import java.util.Date;

/**
 * Created by cobr123 on 24.04.2015.
 */
public final class Downloader {
    private static final Logger logger = LoggerFactory.getLogger(Downloader.class);

    public static String getCrearedUrl(final String url, final String referrer) {
        String clearedUrl;
        if (referrer != null && !referrer.isEmpty()) {
            final String[] parts = url.split("/");
            final String page = File.separator + parts[parts.length - 2] + File.separator + parts[parts.length - 1];
            clearedUrl = referrer.replace("http://", "").replace("https://", "").replace("/", File.separator) + page;
        } else {
            clearedUrl = url.replace("http://", "").replace("https://", "").replace("/", File.separator);
        }
        return clearedUrl;
    }

    public static void invalidateCache(final String url) throws IOException {
        invalidateCache(url, "");
    }

    public static void invalidateCache(final String url, final String referrer) throws IOException {
        final String clearedUrl = getCrearedUrl(url, referrer);
        final String fileToSave = Utils.getDir() + clearedUrl + ".html";
        final File file = new File(fileToSave);
        if (file.exists()) {
            file.delete();
        }
    }

    public static Document getDoc(final String url) throws IOException {
        return getDoc(url, null);
    }

    public static Document getDoc(final String url, final int maxTriesCnt) throws IOException {
        return getDoc(url, null, maxTriesCnt);
    }

    public static Document getDoc(final String url, final String referrer) throws IOException {
        return getDoc(url, referrer, 99);
    }

    public static Document getDoc(final String url, final String referrer, final int maxTriesCnt) throws IOException {
        final String clearedUrl = getCrearedUrl(url, referrer);
        final String fileToSave = Utils.getDir() + clearedUrl + ".html";
        final File file = new File(fileToSave);
        if (file.exists() && Utils.equalsWoTime(new Date(file.lastModified()), new Date())) {
            logger.trace("Взят из кэша: {}", file.getAbsolutePath());
            return Jsoup.parse(file, "UTF-8", Wizard.host);
        } else {
            logger.trace("Запрошен адрес: {}", url);

            for (int tries = 1; tries <= maxTriesCnt; ++tries) {
                try {
                    final Connection conn = Jsoup.connect(url);
                    if (referrer != null && !referrer.isEmpty()) {
                        logger.trace("referrer: {}", referrer);
                        conn.referrer(referrer);
                    }
                    conn.header("Accept-Language", "ru");
                    conn.header("Accept-Encoding", "gzip, deflate");
                    conn.userAgent("Mozilla/5.0 (Windows NT 6.1; WOW64; rv:23.0) Gecko/20100101 Firefox/23.0");
                    conn.maxBodySize(0);
                    conn.timeout(600000);
                    final Document doc = conn.get();
                    Utils.writeFile(fileToSave, doc.outerHtml());
                    return doc;
                } catch (final IOException e) {
                    logger.error("Ошибка при запросе, попытка #{} из {}: {}", tries, maxTriesCnt, url);
                    logger.trace("Ошибка: ", e);
                    if (maxTriesCnt == tries) {
                        throw new IOException(e);
                    } else {
                        waitSecond(3 * tries);
                    }
                }
            }
        }
        return null;
    }

    public static void waitSecond(final long seconds) {
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    public static void main(final String[] args) throws IOException {
        final Document doc = Jsoup.connect(Wizard.host + "olga/main/geo/citylist/331858").get();
        Utils.writeFile(Utils.getDir() + "citylist.html", doc.outerHtml());
    }
}