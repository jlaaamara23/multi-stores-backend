package com.example.multi_stores.util;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses pack labels like {@code 250G}, {@code 1KG} to kilograms for price-per-kg products.
 */
public final class WeightPriceUtil {

    private static final Pattern KG = Pattern.compile("^(\\d+(?:\\.\\d+)?)\\s*KG$", Pattern.CASE_INSENSITIVE);
    private static final Pattern G = Pattern.compile("^(\\d+(?:\\.\\d+)?)\\s*G$", Pattern.CASE_INSENSITIVE);

    private WeightPriceUtil() {
    }

    /** Pack weight in kg, e.g. 250G → 0.25, 2KG → 2. */
    public static Optional<BigDecimal> parsePackWeightKg(String size) {
        if (size == null || size.isBlank()) {
            return Optional.empty();
        }
        String s = size.trim();
        Matcher mKg = KG.matcher(s);
        if (mKg.matches()) {
            return Optional.of(new BigDecimal(mKg.group(1)));
        }
        Matcher mG = G.matcher(s);
        if (mG.matches()) {
            BigDecimal grams = new BigDecimal(mG.group(1));
            return Optional.of(grams.movePointLeft(3));
        }
        return Optional.empty();
    }
}
