package com.example.smartpantrymanager.util;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Understands the units used in the pantry so that quantities can be compared
 * across simple real-world differences: "500 g" vs "0.5 kg", "1 tbsp" vs "15 ml",
 * "tin" vs "can".
 *
 * Units are grouped. Comparisons are only made inside a group, so grams are never
 * silently treated as millilitres. Count units ("piece", "slice", "clove"...) each
 * form their own group because they do not convert into one another.
 */
public final class UnitConverter {

    private static final double EPSILON = 0.0001;

    private static final class Unit {
        final String group;
        final double factor;

        Unit(String group, double factor) {
            this.group = group;
            this.factor = factor;
        }
    }

    private static final Map<String, Unit> UNITS = new HashMap<>();

    static {
        // Mass, base unit = gram
        register("mg", "mass", 0.001);
        register("g", "mass", 1);
        register("gram", "mass", 1);
        register("grams", "mass", 1);
        register("kg", "mass", 1000);
        register("kilogram", "mass", 1000);
        register("kilograms", "mass", 1000);
        register("oz", "mass", 28.3495);
        register("ounce", "mass", 28.3495);
        register("ounces", "mass", 28.3495);
        register("lb", "mass", 453.592);
        register("lbs", "mass", 453.592);
        register("pound", "mass", 453.592);
        register("pounds", "mass", 453.592);

        // Volume, base unit = millilitre
        register("ml", "volume", 1);
        register("millilitre", "volume", 1);
        register("milliliter", "volume", 1);
        register("l", "volume", 1000);
        register("litre", "volume", 1000);
        register("liter", "volume", 1000);
        register("tsp", "volume", 4.929);
        register("teaspoon", "volume", 4.929);
        register("teaspoons", "volume", 4.929);
        register("tbsp", "volume", 14.787);
        register("tablespoon", "volume", 14.787);
        register("tablespoons", "volume", 14.787);
        register("cup", "volume", 236.588);
        register("cups", "volume", 236.588);
        register("pint", "volume", 473.176);

        // Counts: each family is independent
        register("piece", "count.piece", 1);
        register("pieces", "count.piece", 1);
        register("pc", "count.piece", 1);
        register("pcs", "count.piece", 1);
        register("unit", "count.piece", 1);
        register("units", "count.piece", 1);
        register("whole", "count.piece", 1);
        register("slice", "count.slice", 1);
        register("slices", "count.slice", 1);
        register("clove", "count.clove", 1);
        register("cloves", "count.clove", 1);
        register("bunch", "count.bunch", 1);
        register("bunches", "count.bunch", 1);
        register("tin", "count.tin", 1);
        register("tins", "count.tin", 1);
        register("can", "count.tin", 1);
        register("cans", "count.tin", 1);
    }

    private UnitConverter() {
    }

    private static void register(String unit, String group, double factor) {
        UNITS.put(unit, new Unit(group, factor));
    }

    /** "Piece(s)" -> "piece", "KG" -> "kg", "  tbsp " -> "tbsp". */
    public static String canonical(String rawUnit) {
        if (rawUnit == null) {
            return "";
        }
        String unit = rawUnit.toLowerCase(Locale.getDefault()).trim();
        int bracketStart = unit.indexOf('(');
        if (bracketStart >= 0) {
            unit = unit.substring(0, bracketStart).trim();
        }
        int slashStart = unit.indexOf('/');
        if (slashStart >= 0) {
            unit = unit.substring(0, slashStart).trim();
        }
        while (unit.endsWith(".")) {
            unit = unit.substring(0, unit.length() - 1).trim();
        }
        return unit;
    }

    public static boolean isKnown(String rawUnit) {
        return UNITS.containsKey(canonical(rawUnit));
    }

    /** The comparison family of a unit, or "" when the unit is not recognised. */
    public static String group(String rawUnit) {
        String unit = canonical(rawUnit);
        Unit definition = UNITS.get(unit);
        return definition == null ? "" : definition.group;
    }

    /** Converts a quantity into its base unit, or returns the quantity unchanged if unknown. */
    public static double toBase(String rawUnit, double quantity) {
        String unit = canonical(rawUnit);
        Unit definition = UNITS.get(unit);
        if (definition == null) {
            return quantity;
        }
        return quantity * definition.factor;
    }

    /**
     * The single decision used by the matcher: does the pantry holding
     * (haveUnit/haveQuantity) cover the recipe requirement (needUnit/needQuantity)?
     * Unknown units fall back to a like-for-like comparison of the cleaned-up unit text.
     */
    public static boolean covers(String haveUnit, double haveQuantity,
                                 String needUnit, double needQuantity) {
        String haveCanonical = canonical(haveUnit);
        String needCanonical = canonical(needUnit);
        boolean haveKnown = UNITS.containsKey(haveCanonical);
        boolean needKnown = UNITS.containsKey(needCanonical);

        if (!haveKnown || !needKnown) {
            return haveCanonical.equals(needCanonical) && haveQuantity + EPSILON >= needQuantity;
        }
        if (!group(haveCanonical).equals(group(needCanonical))) {
            return false;
        }
        return toBase(haveCanonical, haveQuantity) + EPSILON >= toBase(needCanonical, needQuantity);
    }

    /** Display-only conversion for the imperial units preference. */
    public static String format(double quantity, String rawUnit, boolean imperial) {
        String unit = canonical(rawUnit);
        if (imperial && UNITS.containsKey(unit)) {
            double base = toBase(unit, quantity);
            if ("mass".equals(group(unit))) {
                if (base >= 453.592) {
                    return FormatUtils.number(base / 453.592) + " lb";
                }
                return FormatUtils.number(base / 28.3495) + " oz";
            }
            if ("volume".equals(group(unit))) {
                if (base >= 236.588) {
                    return FormatUtils.number(base / 236.588) + " cup(s)";
                }
                if (base >= 14.787) {
                    return FormatUtils.number(base / 14.787) + " tbsp";
                }
                return FormatUtils.number(base / 4.929) + " tsp";
            }
        }
        return FormatUtils.number(quantity) + " " + unit;
    }
}
