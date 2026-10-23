package com.example.smartpantrymanager.ui;

import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smartpantrymanager.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Shared bottom-navigation behaviour for the three main screens.
 * Every screen keeps its own BottomNavigationView (id = bottomNav) and this helper
 * highlights the current tab and launches the others with an explicit Intent.
 */
public abstract class BaseNavActivity extends AppCompatActivity {

    /**
     * @param selectedItemId the menu item representing this screen, e.g. R.id.nav_pantry
     */
    protected void setupBottomNavigation(final int selectedItemId) {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav == null) {
            return;
        }
        bottomNav.setSelectedItemId(selectedItemId);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == selectedItemId) {
                return true;
            }
            Intent intent = null;
            if (itemId == R.id.nav_pantry) {
                intent = new Intent(this, MainActivity.class);
            } else if (itemId == R.id.nav_recipes) {
                intent = new Intent(this, SuggestedRecipesActivity.class);
            } else if (itemId == R.id.nav_settings) {
                intent = new Intent(this, SettingsActivity.class);
            }
            if (intent != null) {
                // Reuse the existing screen instead of stacking copies of it.
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            }
            return true;
        });
    }
}
