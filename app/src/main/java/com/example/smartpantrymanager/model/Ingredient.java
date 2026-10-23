package com.example.smartpantrymanager.model;

import android.text.TextUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * One row of the pantry table: an ingredient the user already owns.
 * The expiry date is stored as a plain "yyyy-MM-dd" string so that it can be
 * kept directly in a TEXT column and still be sorted with ORDER BY.
 */
public class Ingredient {

    public static final String DATE_FORMAT = "yyyy-MM-dd";

    /** Ingredients expiring within this many days are flagged as "expiring soon". */
    public static final int EXPIRING_SOON_DAYS = 3;

    private long id;
    private String name;
    private double quantity;
    private String unit;
    private String expiryDate;

    public Ingredient() {
        this(-1L, "", 0d, "", "");
    }

    public Ingredient(long id, String name, double quantity, String unit, String expiryDate) {
        this.id = id;
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.expiryDate = expiryDate == null ? "" : expiryDate;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate == null ? "" : expiryDate;
    }

    public boolean hasExpiryDate() {
        return !TextUtils.isEmpty(expiryDate);
    }

    /** Parses the stored expiry date, or returns null when there is none / it is unreadable. */
    private Date parseExpiryDate() {
        if (!hasExpiryDate()) {
            return null;
        }
        SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
        format.setLenient(false);
        try {
            return format.parse(expiryDate);
        } catch (ParseException e) {
            return null;
        }
    }

    /** Whole days from today until the expiry date; null when no date is stored. */
    public Integer daysUntilExpiry() {
        Date expiry = parseExpiryDate();
        if (expiry == null) {
            return null;
        }
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        long difference = expiry.getTime() - today.getTimeInMillis();
        return (int) Math.round(difference / 86400000d);
    }

    public boolean isExpired() {
        Integer days = daysUntilExpiry();
        return days != null && days < 0;
    }

    public boolean isExpiringSoon() {
        Integer days = daysUntilExpiry();
        return days != null && days >= 0 && days <= EXPIRING_SOON_DAYS;
    }

    /** Today's date in the storage format, used to pre-fill the date picker. */
    public static String today() {
        return new SimpleDateFormat(DATE_FORMAT, Locale.getDefault()).format(new Date());
    }
}
