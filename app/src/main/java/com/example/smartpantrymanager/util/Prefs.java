package com.example.smartpantrymanager.util;

import android.content.Context;
import android.content.SharedPreferences;

/** Typed access to the SharedPreferences backing the Settings screen. */
public final class Prefs {

    public static final String FILE_NAME = "smart_pantry_prefs";
    public static final String KEY_EXPIRY_ALERTS = "pref_expiry_alerts";
    public static final String KEY_EXCLUDE_EXPIRED = "pref_exclude_expired";
    public static final String KEY_SHOW_ALMOST_THERE = "pref_show_almost_there";
    public static final String KEY_UNITS = "pref_units";

    public static final String UNIT_METRIC = "metric";
    public static final String UNIT_IMPERIAL = "imperial";

    private Prefs() {
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
    }

    public static boolean expiryAlertsEnabled(Context context) {
        return preferences(context).getBoolean(KEY_EXPIRY_ALERTS, true);
    }

    public static boolean excludeExpiredEnabled(Context context) {
        return preferences(context).getBoolean(KEY_EXCLUDE_EXPIRED, true);
    }

    public static boolean showAlmostThereEnabled(Context context) {
        return preferences(context).getBoolean(KEY_SHOW_ALMOST_THERE, true);
    }

    public static boolean useImperialUnits(Context context) {
        return UNIT_IMPERIAL.equals(preferences(context).getString(KEY_UNITS, UNIT_METRIC));
    }

    public static void setBoolean(Context context, String key, boolean value) {
        preferences(context).edit().putBoolean(key, value).apply();
    }

    public static void setUnits(Context context, boolean imperial) {
        preferences(context).edit()
                .putString(KEY_UNITS, imperial ? UNIT_IMPERIAL : UNIT_METRIC)
                .apply();
    }
}
