package com.example.smartpantrymanager.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.smartpantrymanager.model.Ingredient;
import com.example.smartpantrymanager.model.Recipe;
import com.example.smartpantrymanager.model.RecipeIngredient;

import java.util.ArrayList;
import java.util.List;

/**
 * All persistence for the app. SQLite is used through SQLiteOpenHelper:
 *
 *   ingredients         - the user's pantry (created, read, updated, deleted by the user)
 *   recipes             - the 20 seeded recipes
 *   recipe_ingredients  - the ingredients each recipe requires (one row per requirement)
 *
 * The database file lives in the app's private storage, so pantry data survives
 * closing and reopening the app, and survives a device restart.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "smart_pantry.db";
    private static final int DATABASE_VERSION = 1;

    // ----- ingredients table (the user's pantry) -----
    public static final String TABLE_INGREDIENTS = "ingredients";
    public static final String COL_ID = "_id";
    public static final String COL_NAME = "name";
    public static final String COL_QUANTITY = "quantity";
    public static final String COL_UNIT = "unit";
    public static final String COL_EXPIRY = "expiry_date";

    // ----- recipes table (seeded on first run) -----
    public static final String TABLE_RECIPES = "recipes";
    public static final String COL_RECIPE_NAME = "name";
    public static final String COL_PREP_MINUTES = "prep_minutes";
    public static final String COL_SERVINGS = "servings";
    public static final String COL_STEPS = "steps";

    // ----- recipe_ingredients table -----
    public static final String TABLE_RECIPE_INGREDIENTS = "recipe_ingredients";
    public static final String COL_RI_RECIPE_ID = "recipe_id";
    public static final String COL_RI_NAME = "ingredient_name";
    public static final String COL_RI_QUANTITY = "quantity";
    public static final String COL_RI_UNIT = "unit";

    public static final String SORT_NAME = COL_NAME + " COLLATE NOCASE ASC";
    public static final String SORT_EXPIRY = "CASE WHEN " + COL_EXPIRY + " IS NULL OR " + COL_EXPIRY
            + " = '' THEN 1 ELSE 0 END ASC, " + COL_EXPIRY + " ASC, " + COL_NAME + " COLLATE NOCASE ASC";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // ------------------------------------------------------------------
    // Schema creation
    // ------------------------------------------------------------------
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_INGREDIENTS + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_NAME + " TEXT NOT NULL, "
                + COL_QUANTITY + " REAL NOT NULL, "
                + COL_UNIT + " TEXT NOT NULL, "
                + COL_EXPIRY + " TEXT DEFAULT '');");

        db.execSQL("CREATE TABLE " + TABLE_RECIPES + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_RECIPE_NAME + " TEXT NOT NULL, "
                + COL_PREP_MINUTES + " INTEGER NOT NULL, "
                + COL_SERVINGS + " INTEGER NOT NULL, "
                + COL_STEPS + " TEXT NOT NULL);");

        db.execSQL("CREATE TABLE " + TABLE_RECIPE_INGREDIENTS + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_RI_RECIPE_ID + " INTEGER NOT NULL, "
                + COL_RI_NAME + " TEXT NOT NULL, "
                + COL_RI_QUANTITY + " REAL NOT NULL, "
                + COL_RI_UNIT + " TEXT NOT NULL, "
                + "FOREIGN KEY (" + COL_RI_RECIPE_ID + ") REFERENCES "
                + TABLE_RECIPES + "(" + COL_ID + ") ON DELETE CASCADE);");

        seedRecipes(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RECIPE_INGREDIENTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RECIPES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_INGREDIENTS);
        onCreate(db);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    // ------------------------------------------------------------------
    // RECIPES: read only (they are seeded once)
    // ------------------------------------------------------------------

    public List<Recipe> getAllRecipes() {
        List<Recipe> recipes = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_RECIPES, null, null, null, null, null,
                COL_RECIPE_NAME + " COLLATE NOCASE ASC");
        while (cursor.moveToNext()) {
            Recipe recipe = new Recipe();
            recipe.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)));
            recipe.setName(cursor.getString(cursor.getColumnIndexOrThrow(COL_RECIPE_NAME)));
            recipe.setPrepMinutes(cursor.getInt(cursor.getColumnIndexOrThrow(COL_PREP_MINUTES)));
            recipe.setServings(cursor.getInt(cursor.getColumnIndexOrThrow(COL_SERVINGS)));
            recipe.setSteps(cursor.getString(cursor.getColumnIndexOrThrow(COL_STEPS)));
            recipes.add(recipe);
        }
        cursor.close();

        // Attach the ingredient requirements to each recipe.
        for (Recipe recipe : recipes) {
            for (RecipeIngredient required : getIngredientsForRecipe(recipe.getId())) {
                recipe.addIngredient(required);
            }
        }
        return recipes;
    }

    public Recipe getRecipe(long id) {
        List<Recipe> recipes = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_RECIPES, null, COL_ID + " = ?",
                new String[]{String.valueOf(id)}, null, null, null);
        if (cursor.moveToFirst()) {
            Recipe recipe = new Recipe();
            recipe.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)));
            recipe.setName(cursor.getString(cursor.getColumnIndexOrThrow(COL_RECIPE_NAME)));
            recipe.setPrepMinutes(cursor.getInt(cursor.getColumnIndexOrThrow(COL_PREP_MINUTES)));
            recipe.setServings(cursor.getInt(cursor.getColumnIndexOrThrow(COL_SERVINGS)));
            recipe.setSteps(cursor.getString(cursor.getColumnIndexOrThrow(COL_STEPS)));
            recipes.add(recipe);
        }
        cursor.close();
        if (recipes.isEmpty()) {
            return null;
        }
        Recipe recipe = recipes.get(0);
        for (RecipeIngredient required : getIngredientsForRecipe(recipe.getId())) {
            recipe.addIngredient(required);
        }
        return recipe;
    }

    public List<RecipeIngredient> getIngredientsForRecipe(long recipeId) {
        List<RecipeIngredient> requirements = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_RECIPE_INGREDIENTS, null, COL_RI_RECIPE_ID + " = ?",
                new String[]{String.valueOf(recipeId)}, null, null, COL_ID + " ASC");
        while (cursor.moveToNext()) {
            RecipeIngredient item = new RecipeIngredient();
            item.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)));
            item.setRecipeId(cursor.getLong(cursor.getColumnIndexOrThrow(COL_RI_RECIPE_ID)));
            item.setName(cursor.getString(cursor.getColumnIndexOrThrow(COL_RI_NAME)));
            item.setQuantity(cursor.getDouble(cursor.getColumnIndexOrThrow(COL_RI_QUANTITY)));
            item.setUnit(cursor.getString(cursor.getColumnIndexOrThrow(COL_RI_UNIT)));
            requirements.add(item);
        }
        cursor.close();
        return requirements;
    }

    public int countRecipes() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_RECIPES, null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    // ------------------------------------------------------------------
    // Seed data: 20 recipes, loaded on first run
    // ------------------------------------------------------------------
    private void seedRecipes(SQLiteDatabase db) {
        insertRecipe(db, "Tomato & Basil Pasta", 20, 2,
                "Boil the penne in salted water until al dente.\n"
                        + "Fry the garlic in olive oil for one minute.\n"
                        + "Add the chopped tomatoes and cook for five minutes.\n"
                        + "Toss the pasta through the sauce, then finish with basil and parmesan.",
                new String[][]{
                        {"penne", "200", "g"},
                        {"olive oil", "2", "tbsp"},
                        {"garlic", "2", "clove"},
                        {"tomato", "4", "piece"},
                        {"basil", "1", "bunch"},
                        {"parmesan", "30", "g"}});

        insertRecipe(db, "Cheese Omelette", 10, 1,
                "Beat the eggs with the milk.\n"
                        + "Melt the butter in a pan over medium heat.\n"
                        + "Pour in the egg mixture and scatter over the cheese.\n"
                        + "Fold once cooked through and serve immediately.",
                new String[][]{
                        {"egg", "3", "piece"},
                        {"milk", "60", "ml"},
                        {"butter", "10", "g"},
                        {"cheddar cheese", "40", "g"}});

        insertRecipe(db, "Garlic Bread", 15, 4,
                "Heat the oven to 200 degrees Celsius.\n"
                        + "Mash the butter with the crushed garlic and chopped parsley.\n"
                        + "Spread over the bread slices.\n"
                        + "Bake for ten minutes until crisp and golden.",
                new String[][]{
                        {"bread", "4", "slice"},
                        {"butter", "50", "g"},
                        {"garlic", "3", "clove"},
                        {"parsley", "1", "bunch"}});

        insertRecipe(db, "Fried Rice", 20, 2,
                "Cook the rice and allow it to cool completely.\n"
                        + "Scramble the eggs in hot oil and set aside.\n"
                        + "Fry the onion until soft, then add the peas.\n"
                        + "Add the rice and soy sauce, stir through the egg and serve.",
                new String[][]{
                        {"rice", "250", "g"},
                        {"egg", "2", "piece"},
                        {"soy sauce", "2", "tbsp"},
                        {"onion", "1", "piece"},
                        {"peas", "100", "g"},
                        {"cooking oil", "2", "tbsp"}});

        insertRecipe(db, "Chicken Stir Fry", 25, 2,
                "Slice the chicken into thin strips.\n"
                        + "Sear the chicken in hot oil until browned.\n"
                        + "Add the onion, bell pepper and garlic and stir fry for five minutes.\n"
                        + "Splash in the soy sauce and toss to coat.",
                new String[][]{
                        {"chicken breast", "300", "g"},
                        {"bell pepper", "2", "piece"},
                        {"onion", "1", "piece"},
                        {"soy sauce", "3", "tbsp"},
                        {"cooking oil", "2", "tbsp"},
                        {"garlic", "2", "clove"}});

        insertRecipe(db, "Vegetable Soup", 35, 4,
                "Dice the onion, carrot and potato.\n"
                        + "Soften the vegetables in olive oil for five minutes.\n"
                        + "Pour in the stock and simmer for twenty five minutes.\n"
                        + "Season and serve hot.",
                new String[][]{
                        {"onion", "1", "piece"},
                        {"carrot", "2", "piece"},
                        {"potato", "3", "piece"},
                        {"vegetable stock", "1", "l"},
                        {"olive oil", "1", "tbsp"}});

        insertRecipe(db, "Beef Tacos", 25, 4,
                "Brown the ground beef in oil with the diced onion.\n"
                        + "Warm the tortillas in a dry pan.\n"
                        + "Fill with the beef, diced tomato and grated cheese.\n"
                        + "Fold and serve.",
                new String[][]{
                        {"ground beef", "300", "g"},
                        {"tortilla", "4", "piece"},
                        {"onion", "1", "piece"},
                        {"tomato", "2", "piece"},
                        {"cheddar cheese", "50", "g"},
                        {"cooking oil", "1", "tbsp"}});

        insertRecipe(db, "Greek Salad", 10, 2,
                "Chop the cucumber, tomato and onion into chunks.\n"
                        + "Crumble the feta over the top.\n"
                        + "Add the olives.\n"
                        + "Drizzle with olive oil and toss lightly.",
                new String[][]{
                        {"cucumber", "1", "piece"},
                        {"tomato", "3", "piece"},
                        {"feta cheese", "100", "g"},
                        {"olive oil", "2", "tbsp"},
                        {"onion", "1", "piece"},
                        {"olives", "50", "g"}});

        insertRecipe(db, "Banana Pancakes", 20, 2,
                "Mash the bananas in a bowl.\n"
                        + "Whisk in the flour, milk, egg and baking powder.\n"
                        + "Cook spoonfuls of batter in butter until bubbling.\n"
                        + "Flip and cook for one more minute.",
                new String[][]{
                        {"banana", "2", "piece"},
                        {"flour", "150", "g"},
                        {"milk", "200", "ml"},
                        {"egg", "1", "piece"},
                        {"butter", "20", "g"},
                        {"baking powder", "1", "tsp"}});

        insertRecipe(db, "Chicken Curry", 40, 4,
                "Dice the chicken and brown it in oil.\n"
                        + "Add the onion and curry powder and cook for two minutes.\n"
                        + "Pour in the coconut milk and simmer for twenty minutes.\n"
                        + "Serve over boiled rice.",
                new String[][]{
                        {"chicken breast", "400", "g"},
                        {"curry powder", "2", "tbsp"},
                        {"coconut milk", "400", "ml"},
                        {"onion", "1", "piece"},
                        {"rice", "200", "g"},
                        {"cooking oil", "2", "tbsp"}});

        insertRecipe(db, "Spaghetti Bolognese", 35, 4,
                "Boil the spaghetti until al dente.\n"
                        + "Fry the onion and garlic in olive oil.\n"
                        + "Add the ground beef and brown it, then stir in the tomato and tomato paste.\n"
                        + "Simmer for twenty minutes and spoon over the pasta.",
                new String[][]{
                        {"spaghetti", "250", "g"},
                        {"ground beef", "300", "g"},
                        {"tomato", "4", "piece"},
                        {"onion", "1", "piece"},
                        {"garlic", "2", "clove"},
                        {"olive oil", "2", "tbsp"},
                        {"tomato paste", "2", "tbsp"}});

        insertRecipe(db, "Mushroom Risotto", 35, 3,
                "Fry the onion and sliced mushrooms in butter.\n"
                        + "Stir in the rice and cook for one minute.\n"
                        + "Add the hot stock one ladle at a time, stirring, for twenty minutes.\n"
                        + "Finish with parmesan.",
                new String[][]{
                        {"rice", "250", "g"},
                        {"mushroom", "250", "g"},
                        {"onion", "1", "piece"},
                        {"vegetable stock", "750", "ml"},
                        {"parmesan", "50", "g"},
                        {"butter", "30", "g"}});

        insertRecipe(db, "Potato Wedges", 40, 3,
                "Heat the oven to 220 degrees Celsius.\n"
                        + "Cut the potatoes into thick wedges.\n"
                        + "Toss with olive oil and paprika.\n"
                        + "Roast for thirty minutes, turning once.",
                new String[][]{
                        {"potato", "4", "piece"},
                        {"olive oil", "3", "tbsp"},
                        {"paprika", "1", "tsp"}});

        insertRecipe(db, "Tuna Sandwich", 8, 2,
                "Drain the tuna and mix with the mayonnaise.\n"
                        + "Layer the lettuce and sliced tomato on two slices of bread.\n"
                        + "Spoon over the tuna mixture.\n"
                        + "Close with the remaining bread and cut in half.",
                new String[][]{
                        {"bread", "4", "slice"},
                        {"tuna", "1", "tin"},
                        {"mayonnaise", "2", "tbsp"},
                        {"lettuce", "2", "piece"},
                        {"tomato", "1", "piece"}});

        insertRecipe(db, "Shakshuka", 25, 2,
                "Fry the onion, garlic and paprika in olive oil.\n"
                        + "Add the chopped tomatoes and simmer for ten minutes.\n"
                        + "Make four wells and crack an egg into each.\n"
                        + "Cover and cook until the eggs are set.",
                new String[][]{
                        {"egg", "4", "piece"},
                        {"tomato", "5", "piece"},
                        {"onion", "1", "piece"},
                        {"olive oil", "2", "tbsp"},
                        {"paprika", "1", "tsp"},
                        {"garlic", "2", "clove"}});

        insertRecipe(db, "Chicken Salad", 15, 2,
                "Cook and slice the chicken breast.\n"
                        + "Shred the lettuce and slice the cucumber and tomato.\n"
                        + "Whisk the olive oil and mayonnaise into a dressing.\n"
                        + "Toss everything together and serve.",
                new String[][]{
                        {"chicken breast", "250", "g"},
                        {"lettuce", "1", "piece"},
                        {"tomato", "2", "piece"},
                        {"cucumber", "1", "piece"},
                        {"olive oil", "2", "tbsp"},
                        {"mayonnaise", "2", "tbsp"}});

        insertRecipe(db, "French Toast", 12, 2,
                "Whisk the egg, milk and cinnamon together.\n"
                        + "Dip each slice of bread in the mixture.\n"
                        + "Fry in butter until golden on both sides.\n"
                        + "Serve warm.",
                new String[][]{
                        {"bread", "4", "slice"},
                        {"egg", "2", "piece"},
                        {"milk", "150", "ml"},
                        {"butter", "20", "g"},
                        {"cinnamon", "1", "tsp"}});

        insertRecipe(db, "Chickpea Curry", 30, 3,
                "Fry the onion in oil until soft.\n"
                        + "Stir in the curry powder and cook for one minute.\n"
                        + "Add the chickpeas and coconut milk and simmer for fifteen minutes.\n"
                        + "Serve over rice.",
                new String[][]{
                        {"chickpea", "400", "g"},
                        {"onion", "1", "piece"},
                        {"curry powder", "2", "tbsp"},
                        {"coconut milk", "400", "ml"},
                        {"rice", "200", "g"},
                        {"cooking oil", "2", "tbsp"}});

        insertRecipe(db, "Beef Noodle Stir Fry", 25, 3,
                "Cook the noodles and drain.\n"
                        + "Brown the ground beef in hot oil.\n"
                        + "Add the julienned carrot and garlic and fry for three minutes.\n"
                        + "Add the noodles and soy sauce and toss together.",
                new String[][]{
                        {"noodle", "200", "g"},
                        {"ground beef", "250", "g"},
                        {"soy sauce", "3", "tbsp"},
                        {"carrot", "1", "piece"},
                        {"cooking oil", "2", "tbsp"},
                        {"garlic", "2", "clove"}});

        insertRecipe(db, "Garlic Butter Prawns", 15, 2,
                "Melt the butter in a pan.\n"
                        + "Add the crushed garlic and cook for thirty seconds.\n"
                        + "Add the prawns and cook for three minutes until pink.\n"
                        + "Finish with chopped parsley and a squeeze of lemon.",
                new String[][]{
                        {"prawn", "300", "g"},
                        {"butter", "40", "g"},
                        {"garlic", "4", "clove"},
                        {"parsley", "1", "bunch"},
                        {"lemon", "1", "piece"}});
    }

    /** Writes one recipe and all of its required ingredients in a single pass. */
    private void insertRecipe(SQLiteDatabase db, String name, int prepMinutes, int servings,
                              String steps, String[][] ingredients) {
        ContentValues recipeValues = new ContentValues();
        recipeValues.put(COL_RECIPE_NAME, name);
        recipeValues.put(COL_PREP_MINUTES, prepMinutes);
        recipeValues.put(COL_SERVINGS, servings);
        recipeValues.put(COL_STEPS, steps);
        long recipeId = db.insert(TABLE_RECIPES, null, recipeValues);

        for (String[] row : ingredients) {
            ContentValues values = new ContentValues();
            values.put(COL_RI_RECIPE_ID, recipeId);
            values.put(COL_RI_NAME, row[0]);
            values.put(COL_RI_QUANTITY, Double.parseDouble(row[1]));
            values.put(COL_RI_UNIT, row[2]);
            db.insert(TABLE_RECIPE_INGREDIENTS, null, values);
        }
    }
}
