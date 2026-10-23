package com.example.smartpantrymanager.util;

import java.util.Locale;

/** Small number/date formatting helpers shared by the screens and adapters. */
public final class FormatUtils {

    private FormatUtils() {
    }

    /** 200.0 -> "200", 0.5 -> "0.5", 1.33333 -> "1.33". */
    public static String number(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.005) {
            return String.valueOf((long) Math.rint(value));
        }
        return String.format(Locale.getDefault(), "%.2f", value);
    }

    /** "Tomatoes" -> "Tomatoes", "tomatoes" -> "Tomatoes". */
    public static String capitalise(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return text.substring(0, 1).toUpperCase(Locale.getDefault()) + text.substring(1);
    }

    /** Days -> "Use in 2 day(s)"-style wording used by the expiry badge. */
    public static String daysLabel(int days) {
        if (days < 0) {
            return "Expired";
        }
        if (days == 0) {
            return "Expires today";
        }
        if (days == 1) {
            return "Use within 1 day";
        }
        return "Use within " + days + " days";
    }
}
