package com.example.smartpantrymanager.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartpantrymanager.R;
import com.example.smartpantrymanager.adapter.RecipeAdapter;
import com.example.smartpantrymanager.data.DatabaseHelper;
import com.example.smartpantrymanager.model.Ingredient;
import com.example.smartpantrymanager.model.Recipe;
import com.example.smartpantrymanager.util.Prefs;
import com.example.smartpantrymanager.util.RecipeMatcher;

import java.util.ArrayList;
import java.util.List;

/**
 * SCREEN 3 - Suggested Recipes.
 *
 * Every time this screen resumes it re-reads the pantry, rebuilds the pantry index and
 * re-runs the strict matcher. Only recipes with zero missing requirements are listed
 * under "Ready to cook now". Recipes missing exactly one requirement go to the clearly
 * separated "Almost there" section, which the user can switch off in Settings.
 */
public class SuggestedRecipesActivity extends BaseNavActivity implements RecipeAdapter.OnRecipeClickListener {

    public static final String EXTRA_RECIPE_ID = "extra_recipe_id";

    private DatabaseHelper databaseHelper;
    private RecyclerView recyclerRecipes;
    private RecipeAdapter adapter;
    private TextView tvNoMatches;
    private TextView tvMatchCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_suggested_recipes);

        databaseHelper = new DatabaseHelper(this);
        recyclerRecipes = findViewById(R.id.recyclerRecipes);
        tvNoMatches = findViewById(R.id.tvNoMatches);
        tvMatchCount = findViewById(R.id.tvMatchCount);

        recyclerRecipes.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RecipeAdapter(this);
        recyclerRecipes.setAdapter(adapter);

        setupBottomNavigation(R.id.nav_recipes);
        evaluateRecipes();
    }

    private void evaluateRecipes() {
        // 1. Everything the user currently has (expired items excluded when the setting says so).
        List<Ingredient> pantry = databaseHelper.getAllIngredients(DatabaseHelper.SORT_NAME);
        RecipeMatcher.PantryIndex index = RecipeMatcher.index(pantry, Prefs.excludeExpiredEnabled(this));

        // 2. Every seeded recipe, tested against that index.
        List<Recipe> recipes = databaseHelper.getAllRecipes();
        List<RecipeMatcher.Match> matches = RecipeMatcher.evaluateAll(recipes, index);

        // 3. Split into the strict list and the optional bonus list.
        List<RecipeMatcher.Match> ready = RecipeMatcher.readyToCook(matches);

        List<Object> rows = new ArrayList<>();
        rows.add(getString(R.string.header_ready, ready.size()));
        rows.addAll(ready);

        if (ready.isEmpty()) {
            rows.add(getString(R.string.hint_no_matches_inline));
        }

        if (Prefs.showAlmostThereEnabled(this)) {
            List<RecipeMatcher.Match> almost = RecipeMatcher.almostThere(matches, 1);
            if (!almost.isEmpty()) {
                rows.add(getString(R.string.header_almost, almost.size()));
                rows.addAll(almost);
            }
        }

        adapter.setRows(rows);

        boolean nothingToList = ready.isEmpty() && rows.size() <= 2;
        tvNoMatches.setVisibility(nothingToList ? View.VISIBLE : View.GONE);
        recyclerRecipes.setVisibility(nothingToList ? View.GONE : View.VISIBLE);
        tvMatchCount.setText(getString(R.string.label_recipes_used, ready.size(), recipes.size()));
    }

    /** Opens the detail screen, passing the recipe id as an Intent extra. */
    @Override
    public void onRecipeClick(long recipeId) {
        Intent intent = new Intent(this, RecipeDetailActivity.class);
        intent.putExtra(EXTRA_RECIPE_ID, recipeId);
        startActivity(intent);
    }
}
