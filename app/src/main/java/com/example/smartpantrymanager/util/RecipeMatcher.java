package com.example.smartpantrymanager.util;

import com.example.smartpantrymanager.model.Ingredient;
import com.example.smartpantrymanager.model.Recipe;
import com.example.smartpantrymanager.model.RecipeIngredient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * THE CORE BUSINESS LOGIC.
 *
 * Strict rule (assignment section 2.3): a recipe may only be suggested when every
 * single ingredient it requires is present in the pantry in at least the required
 * quantity. One missing ingredient, or an insufficient quantity, excludes the recipe.
 *
 * The matcher works in three steps:
 *   1. index the pantry by normalised ingredient name,
 *   2. test every recipe requirement against that index,
 *   3. split the results into "ready now" (missing nothing) and "almost there"
 *      (missing exactly one requirement).
 */
public final class RecipeMatcher {

    private RecipeMatcher() {
    }

    /** Pantry lookup table: canonical name -> unit group -> total quantity in base units. */
    public static final class PantryIndex {

        private final Map<String, Map<String, Double>> totals = new HashMap<>();

        void add(String canonicalName, String unit, double quantity) {
            String group = UnitConverter.isKnown(unit) ? UnitConverter.group(unit) : "raw:" + UnitConverter.canonical(unit);
            double base = UnitConverter.toBase(unit, quantity);
            Map<String, Double> byGroup = totals.get(canonicalName);
            if (byGroup == null) {
                byGroup = new HashMap<>();
                totals.put(canonicalName, byGroup);
            }
            Double current = byGroup.get(group);
            byGroup.put(group, current == null ? base : current + base);
        }

        boolean contains(String canonicalName) {
            return totals.containsKey(canonicalName);
        }

        /** True when the pantry holds at least "neededBase" of this group for this ingredient. */
        public boolean covers(String canonicalName, String unit, double neededBase) {
            Map<String, Double> byGroup = totals.get(canonicalName);
            if (byGroup == null) {
                return false;
            }
            String group = UnitConverter.isKnown(unit) ? UnitConverter.group(unit) : "raw:" + UnitConverter.canonical(unit);
            Double available = byGroup.get(group);
            return available != null && available + 0.0001 >= neededBase;
        }
    }

    /** The outcome of testing one recipe against the pantry. */
    public static final class Match {
        public final Recipe recipe;
        public final List<RecipeIngredient> missing;

        Match(Recipe recipe, List<RecipeIngredient> missing) {
            this.recipe = recipe;
            this.missing = missing;
        }

        /** A recipe "can be cooked" only when nothing at all is missing. */
        public boolean canCook() {
            return missing.isEmpty();
        }

        public int missingCount() {
            return missing.size();
        }

        /** "Garlic, Parsley" - the ingredient names still needed. */
        public String missingNames() {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < missing.size(); i++) {
                if (i > 0) {
                    builder.append(", ");
                }
                builder.append(FormatUtils.capitalise(missing.get(i).getName()));
            }
            return builder.toString();
        }
    }

    /** Builds the pantry lookup table, optionally ignoring expired items. */
    public static PantryIndex index(List<Ingredient> pantry, boolean excludeExpired) {
        PantryIndex index = new PantryIndex();
        if (pantry == null) {
            return index;
        }
        for (Ingredient item : pantry) {
            if (excludeExpired && item.isExpired()) {
                continue;
            }
            index.add(IngredientNormalizer.canonical(item.getName()), item.getUnit(), item.getQuantity());
        }
        return index;
    }

    /** Tests one recipe and records every requirement the pantry cannot cover. */
    public static Match evaluate(Recipe recipe, PantryIndex index) {
        List<RecipeIngredient> missing = new ArrayList<>();
        for (RecipeIngredient required : recipe.getIngredients()) {
            String canonicalName = IngredientNormalizer.canonical(required.getName());
            double neededBase = UnitConverter.toBase(required.getUnit(), required.getQuantity());
            if (!index.covers(canonicalName, required.getUnit(), neededBase)) {
                missing.add(required);
            }
        }
        return new Match(recipe, missing);
    }

    public static List<Match> evaluateAll(List<Recipe> recipes, PantryIndex index) {
        List<Match> results = new ArrayList<>();
        if (recipes == null) {
            return results;
        }
        for (Recipe recipe : recipes) {
            results.add(evaluate(recipe, index));
        }
        return results;
    }

    /** Recipes with zero missing requirements - the main Suggested Recipes list. */
    public static List<Match> readyToCook(List<Match> matches) {
        List<Match> ready = new ArrayList<>();
        for (Match match : matches) {
            if (match.canCook()) {
                ready.add(match);
            }
        }
        return ready;
    }

    /** Optional bonus list: recipes missing exactly one requirement, kept clearly separate. */
    public static List<Match> almostThere(List<Match> matches, int missingCount) {
        List<Match> almost = new ArrayList<>();
        for (Match match : matches) {
            if (!match.canCook() && match.missingCount() == missingCount) {
                almost.add(match);
            }
        }
        return almost;
    }
}
