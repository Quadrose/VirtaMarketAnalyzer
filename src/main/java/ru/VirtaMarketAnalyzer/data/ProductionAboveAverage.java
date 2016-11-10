package ru.VirtaMarketAnalyzer.data;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by r.tabulov on 16.10.2016.
 */
public final class ProductionAboveAverage {
    @SerializedName("mi")
    final private String manufactureID;
    @SerializedName("s")
    final private String specialization;
    @SerializedName("pi")
    final private String productID;
    @SerializedName("v")
    final private long volume;
    @SerializedName("q")
    final private double quality;
    @SerializedName("c")
    final private double cost;
    @SerializedName("tl")
    final private double techLvl;
    @SerializedName("ir")
    final private List<ProductRemain> ingredientsRemain;
    @SerializedName("o4t1")
    final private boolean isOptimalForTop1;

    public ProductionAboveAverage(final String manufactureID
            , final String specialization
            , final String productID
            , final long volume
            , final double quality
            , final double cost
            , final List<ProductRemain> ingredientsRemain
            , final double techLvl
            , final boolean isOptimalForTop1
    ) {
        this.manufactureID = manufactureID;
        this.specialization = specialization;
        this.productID = productID;
        this.volume = volume;
        this.quality = quality;
        this.cost = cost;
        this.ingredientsRemain = ingredientsRemain;
        this.techLvl = techLvl;
        this.isOptimalForTop1 = isOptimalForTop1;
    }

    public String getManufactureID() {
        return manufactureID;
    }

    public String getSpecialization() {
        return specialization;
    }

    public String getProductID() {
        return productID;
    }

    public long getVolume() {
        return volume;
    }

    public double getQuality() {
        return quality;
    }

    public double getCost() {
        return cost;
    }

    public double getTechLvl() {
        return techLvl;
    }

    public List<ProductRemain> getIngredientsRemain() {
        return ingredientsRemain;
    }
}
