package com.example.smartpantrymanager.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartpantrymanager.R;
import com.example.smartpantrymanager.adapter.IngredientAdapter;
import com.example.smartpantrymanager.data.DatabaseHelper;
import com.example.smartpantrymanager.model.Ingredient;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

/**
 * SCREEN 1 - Pantry List.
 *
 * Reads every pantry row from SQLite into a RecyclerView and opens the Add/Edit
 * screen with an explicit Intent.
 */
public class MainActivity extends BaseNavActivity implements IngredientAdapter.OnIngredientListener {

    private DatabaseHelper databaseHelper;
    private RecyclerView recyclerPantry;
    private TextView tvEmptyPantry;
    private TextView tvPantryCount;
    private IngredientAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        databaseHelper = new DatabaseHelper(this);

        recyclerPantry = findViewById(R.id.recyclerPantry);
        tvEmptyPantry = findViewById(R.id.tvEmptyPantry);
        tvPantryCount = findViewById(R.id.tvPantryCount);

        recyclerPantry.setLayoutManager(new LinearLayoutManager(this));
        recyclerPantry.setHasFixedSize(true);
        adapter = new IngredientAdapter(this, true, false);
        recyclerPantry.setAdapter(adapter);

        FloatingActionButton fabAdd = findViewById(R.id.fabAddIngredient);
        fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, AddEditIngredientActivity.class));
            }
        });

        setupBottomNavigation(R.id.nav_pantry);
        loadPantry();
    }

    private void loadPantry() {
        List<Ingredient> ingredients = databaseHelper.getAllIngredients(DatabaseHelper.SORT_NAME);
        adapter.setItems(ingredients);

        boolean empty = ingredients.isEmpty();
        tvEmptyPantry.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerPantry.setVisibility(empty ? View.GONE : View.VISIBLE);
        tvPantryCount.setText(getString(R.string.label_pantry_count, ingredients.size()));
    }

    @Override
    public void onEdit(Ingredient ingredient) {
        Intent intent = new Intent(this, AddEditIngredientActivity.class);
        intent.putExtra(AddEditIngredientActivity.EXTRA_INGREDIENT_ID, ingredient.getId());
        startActivity(intent);
    }

    @Override
    public void onDelete(Ingredient ingredient) {
        databaseHelper.deleteIngredient(ingredient.getId());
        loadPantry();
    }
}
