package ru.VirtaMarketAnalyzer.data;

import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Created by cobr123 on 06.04.2016.
 */
public final class RawMaterial {
    @SerializedName("pc")
    final private String productCategory;
    @SerializedName("s")
    final private String imgUrl;
    @SerializedName("i")
    final private String id;
    @SerializedName("c")
    final private String caption;
    @SerializedName("q")
    final private int quantity;

    public RawMaterial(final String productCategory, final String imgUrl, final String id, final String caption, final int quantity) {
        this.productCategory = productCategory;
        this.imgUrl = imgUrl;
        this.id = id;
        this.caption = caption;
        this.quantity = quantity;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(productCategory)
                .append(imgUrl)
                .append(id)
                .append(caption)
                .toHashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof RawMaterial) {
            final RawMaterial other = (RawMaterial) obj;
            return new EqualsBuilder()
                    .append(productCategory, other.productCategory)
                    .append(imgUrl, other.imgUrl)
                    .append(id, other.id)
                    .append(caption, other.caption)
                    .isEquals();
        } else {
            return false;
        }
    }

    public String getImgUrl() {
        return imgUrl;
    }

    public String getId() {
        return id;
    }

    public String getCaption() {
        return caption;
    }

    public String getProductCategory() {
        return productCategory;
    }

    public int getQuantity() {
        return quantity;
    }
}
