package ru.VirtaMarketAnalyzer.parser;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.VirtaMarketAnalyzer.data.*;
import ru.VirtaMarketAnalyzer.main.Utils;
import ru.VirtaMarketAnalyzer.main.Wizard;
import ru.VirtaMarketAnalyzer.scrapper.Downloader;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

/**
 * Created by cobr123 on 16.01.16.
 */
public final class TopRetailParser {
    private static final Logger logger = LoggerFactory.getLogger(TopRetailParser.class);

    public static void main(String[] args) throws IOException {
        BasicConfigurator.configure(new ConsoleAppender(new PatternLayout("%r %d{ISO8601} [%t] %p %c %x - %m%n")));
        final List<Shop> list = getShopList(Wizard.host, "olga", new ArrayList<>(), new ArrayList<>());
        System.out.println("list.size() = " + list.size());
    }

    public static List<Shop> getShopList(final String realm, final List<TradeAtCity> stats, final List<Product> products) throws IOException {
        final Map<String, List<Product>> productsByImgSrc = products.stream().collect(Collectors.groupingBy(Product::getImgUrl));
        return stats.parallelStream()
                .map(TradeAtCity::getMajorSellInCityList)
                .flatMap(Collection::stream)
                .map(msic -> {
                            Shop shop = null;
                            try {
                                shop = ShopParser.parse(realm, msic.getProductId(), msic.getCountryId(), msic.getRegionId(), msic.getTownId(), msic.getUnitUrl(), productsByImgSrc);
                            } catch (final Exception e) {
                                logger.error(e.getLocalizedMessage(), e);
                            }
                            return shop;
                        }
                )
                .filter(Objects::nonNull)
                .filter(s -> s.getShopProducts().size() > 0)
                .filter(s -> !"Не известен".equals(s.getTownDistrict()))
                .filter(s -> !"Не известен".equals(s.getServiceLevel()))
                .collect(Collectors.toList());
    }

    public static List<Shop> getShopList(final String baseUrl, final String realm, final List<City> cities, final List<Product> products) throws IOException {
        final List<Shop> shops = new ArrayList<>();

        final String newRef = baseUrl + realm + "/main/company/toplist/retail";
        String nextPageUrl = newRef;
        String ref = "";
        for (int page = 1; page <= 10; ++page) {
            try {
                final Document doc = Downloader.getDoc(nextPageUrl, ref);
                final Elements companyLinks = doc.select("table > tbody > tr > td:nth-child(2) > span > a");
                logger.info("companyLinks.size() = {}", companyLinks.size());
                final List<Shop> tmp = companyLinks.parallelStream()
                        .map(link -> {
                            try {
                                final String companyId = Utils.getLastFromUrl(link.attr("href"));
                                return UnitListParser.getShopList(baseUrl, realm, companyId, cities, products);
                            } catch (final Exception e) {
                                logger.error(e.getLocalizedMessage(), e);
                                return null;
                            }
                        })
                        .flatMap(Collection::parallelStream)
                        .collect(toList());
                shops.addAll(tmp);

                nextPageUrl = Utils.getNextPageHref(doc);
                ref = newRef;

                logger.info("shops.size(): {}", shops.size());
                logger.info("shops.size() diff: {}", tmp.size());
                if (nextPageUrl.isEmpty() || tmp.size() < 500) {
                    break;
                }
                logger.info("nextPageUrl: {}", Utils.getLastBySep(nextPageUrl, "/"));
            } catch (final Exception e) {
                logger.error(e.getLocalizedMessage(), e);
            }
        }

        return shops;
    }
}
