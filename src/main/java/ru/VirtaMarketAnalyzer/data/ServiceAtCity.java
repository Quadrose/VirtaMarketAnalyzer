package ru.VirtaMarketAnalyzer.data;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

/**
 * Created by cobr123 on 25.02.16.
 */
public final class ServiceAtCity {
    @SerializedName("ci")
    final private String countryId;
    @SerializedName("ri")
    final private String regionId;
    @SerializedName("ti")
    final private String townId;
    @SerializedName("v")
    final private long volume;
    @SerializedName("p")
    final private double price;
    @SerializedName("sc")
    final private int subdivisionsCnt;
    @SerializedName("cc")
    final private long companiesCnt;
    @SerializedName("mdi")
    final private double marketDevelopmentIndex;
    @SerializedName("pbs")
    final private Map<String, Double> percentBySpec;


    public ServiceAtCity(final String countryId, final String regionId, final String townId,
                         final long volume, final double price, final int subdivisionsCnt, final long companiesCnt,
                         final double marketDevelopmentIndex, final Map<String, Double> percentBySpec) {
        this.countryId = countryId;
        this.regionId = regionId;
        this.townId = townId;
        this.volume = volume;
        this.price = price;
        this.subdivisionsCnt = subdivisionsCnt;
        this.companiesCnt = companiesCnt;
        this.marketDevelopmentIndex = marketDevelopmentIndex;
        this.percentBySpec = percentBySpec;
    }

    public String getCountryId() {
        return countryId;
    }

    public String getRegionId() {
        return regionId;
    }

    public String getTownId() {
        return townId;
    }

    public long getVolume() {
        return volume;
    }

    public double getPrice() {
        return price;
    }

    public int getSubdivisionsCnt() {
        return subdivisionsCnt;
    }

    public long getCompaniesCnt() {
        return companiesCnt;
    }

    public double getMarketDevelopmentIndex() {
        return marketDevelopmentIndex;
    }

    public Map<String, Double> getPercentBySpec() {
        return percentBySpec;
    }
}