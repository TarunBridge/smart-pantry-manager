package com.example.smartpantrymanager.util;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Turns messy, human-typed ingredient names into one comparable key so that
 * "Tomatoes", "tomato" and "TOMATO " all resolve to the same pantry item.
 *
 * This is deliberately a rule-based normaliser, not an NLP solution: it handles
 * the real-world messiness the brief names (case, spacing, plurals, spellings)
 * and leaves everything else alone.
 */
public final class IngredientNormalizer {

    /** Alternative names mapped onto one canonical spelling. */
    private static final Map<String, String> ALIASES = new HashMap<>();

    static {
        ALIASES.put("capsicum", "bell pepper");
        ALIASES.put("sweet pepper", "bell pepper");
        ALIASES.put("cilantro", "coriander");
        ALIASES.put("aubergine", "eggplant");
        ALIASES.put("brinjal", "eggplant");
        ALIASES.put("courgette", "zucchini");
        ALIASES.put("shrimp", "prawn");
        ALIASES.put("scallion", "spring onion");
        ALIASES.put("green onion", "spring onion");
        ALIASES.put("rocket", "arugula");
        ALIASES.put("garbanzo bean", "chickpea");
        ALIASES.put("minced beef", "ground beef");
        ALIASES.put("beef mince", "ground beef");
        ALIASES.put("chopped tomato", "tomato");
        ALIASES.put("tinned tomato", "tomato");
    }

    private IngredientNormalizer() {
    }

    /** The comparable key for an ingredient name. */
    public static String canonical(String rawName) {
        if (rawName == null) {
            return "";
        }
        String text = rawName.toLowerCase(Locale.getDefault()).trim();
        text = text.replaceAll("[^a-z0-9\\s-]", " ").replaceAll("\\s+", " ").trim();
        if (text.isEmpty()) {
            return "";
        }
        text = singularise(text);
        String alias = ALIASES.get(text);
        return alias == null ? text : alias;
    }

    /** Applies simple English plural rules to the last word of the phrase. */
    private static String singularise(String phrase) {
        String[] words = phrase.split(" ");
        String last = words[words.length - 1];
        words[words.length - 1] = singulariseWord(last);
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (result.length() > 0) {
                result.append(' ');
            }
            result.append(word);
        }
        return result.toString();
    }

    static String singulariseWord(String word) {
        if (word.length() <= 3 || !word.endsWith("s") || word.endsWith("ss") || word.endsWith("us")
                || word.endsWith("is")) {
            return word;
        }
        if (word.endsWith("ies") && word.length() > 4) {
            return word.substring(0, word.length() - 3) + "y";
        }
        if (word.endsWith("oes") || word.endsWith("ses") || word.endsWith("xes")
                || word.endsWith("zes") || word.endsWith("ches") || word.endsWith("shes")) {
            return word.substring(0, word.length() - 2);
        }
        return word.substring(0, word.length() - 1);
    }
}
