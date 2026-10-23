package com.example.smartpantrymanager.ui;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.smartpantrymanager.R;
import com.example.smartpantrymanager.data.DatabaseHelper;
import com.example.smartpantrymanager.model.Ingredient;
import com.example.smartpantrymanager.model.Recipe;
import com.example.smartpantrymanager.model.RecipeIngredient;
import com.example.smartpantrymanager.util.FormatUtils;
import com.example.smartpantrymanager.util.Prefs;
import com.example.smartpantrymanager.util.RecipeMatcher;
import com.example.smartpantrymanager.util.UnitConverter;

import java.util.List;

/**
 * SCREEN 4 - Recipe Detail.
 *
 * Opened from the Suggested Recipes list with an explicit Intent carrying the recipe id.
 * Each required ingredient is re-checked against the live pantry so the user can see
 * exactly what they have and what is still missing.
 */
public class RecipeDetailActivity extends AppCompatActivity {

    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_detail);

        databaseHelper = new DatabaseHelper(this);

        long recipeId = getIntent().getLongExtra(SuggestedRecipesActivity.EXTRA_RECIPE_ID, -1L);
        Recipe recipe = databaseHelper.getRecipe(recipeId);
        if (recipe == null) {
            Toast.makeText(this, R.string.hint_no_matches, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setTitle(recipe.getName());

        TextView tvTitle = findViewById(R.id.tvRecipeTitle);
        TextView tvMeta = findViewById(R.id.tvRecipeMeta);
        TextView tvSteps = findViewById(R.id.tvSteps);
        LinearLayout container = findViewById(R.id.containerIngredients);

        tvTitle.setText(recipe.getName());
        tvMeta.setText(getString(R.string.label_prep_time, recipe.getPrepMinutes())
                + "  |  " + getString(R.string.label_serves, recipe.getServings()));

        // Re-check the pantry so the ingredient list can be annotated.
        List<Ingredient> pantry = databaseHelper.getAllIngredients(DatabaseHelper.SORT_NAME);
        RecipeMatcher.PantryIndex index = RecipeMatcher.index(pantry, Prefs.excludeExpiredEnabled(this));
        boolean imperial = Prefs.useImperialUnits(this);

        for (RecipeIngredient required : recipe.getIngredients()) {
            boolean haveIt = index.covers(
                    com.example.smartpantrymanager.util.IngredientNormalizer.canonical(required.getName()),
                    required.getUnit(),
                    UnitConverter.toBase(required.getUnit(), required.getQuantity()));
            addIngredientRow(container, required, haveIt, imperial);
        }

        tvSteps.setText(numberSteps(recipe.getSteps()));
    }

    private void addIngredientRow(LinearLayout container, RecipeIngredient required,
                                  boolean haveIt, boolean imperial) {
        TextView row = new TextView(this);
        String quantity = UnitConverter.format(required.getQuantity(), required.getUnit(), imperial);
        row.setText((haveIt ? "  [OK]  " : "  [MISSING]  ")
                + quantity + " " + FormatUtils.capitalise(required.getName()));
        row.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        row.setTextColor(ContextCompat.getColor(this, haveIt ? R.color.black : R.color.red_700));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = 8;
        row.setLayoutParams(params);
        container.addView(row);
    }

    /** Turns the stored step text into a numbered method. */
    private String numberSteps(String steps) {
        String[] lines = steps.split("\n");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            builder.append(i + 1).append(". ").append(lines[i].trim());
            if (i < lines.length - 1) {
                builder.append("\n\n");
            }
        }
        return builder.toString();
    }
}
