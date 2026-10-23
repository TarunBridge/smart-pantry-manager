package com.example.smartpantrymanager.model;

import java.util.ArrayList;
import java.util.List;

/** A recipe row plus the list of ingredients it requires. */
public class Recipe {

    private long id;
    private String name;
    private int prepMinutes;
    private int servings;
    private String steps;
    private final List<RecipeIngredient> ingredients = new ArrayList<>();

    public Recipe() {
        this(-1L, "", 0, 0, "");
    }

    public Recipe(long id, String name, int prepMinutes, int servings, String steps) {
        this.id = id;
        this.name = name;
        this.prepMinutes = prepMinutes;
        this.servings = servings;
        this.steps = steps;
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

    public int getPrepMinutes() {
        return prepMinutes;
    }

    public void setPrepMinutes(int prepMinutes) {
        this.prepMinutes = prepMinutes;
    }

    public int getServings() {
        return servings;
    }

    public void setServings(int servings) {
        this.servings = servings;
    }

    public String getSteps() {
        return steps == null ? "" : steps;
    }

    public void setSteps(String steps) {
        this.steps = steps;
    }

    public List<RecipeIngredient> getIngredients() {
        return ingredients;
    }

    public void addIngredient(RecipeIngredient ingredient) {
        ingredients.add(ingredient);
    }
}
