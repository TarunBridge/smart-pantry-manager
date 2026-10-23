package com.example.smartpantrymanager.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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
    private String sortOrder = DatabaseHelper.SORT_NAME;

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
    public void onDelete(final Ingredient ingredient) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_delete_title)
                .setMessage(getString(R.string.dialog_delete_message, ingredient.getName()))
                .setPositiveButton(R.string.dialog_yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int rows = databaseHelper.deleteIngredient(ingredient.getId());
                        if (rows > 0) {
                            Toast.makeText(MainActivity.this,
                                    getString(R.string.msg_ingredient_deleted, ingredient.getName()),
                                    Toast.LENGTH_SHORT).show();
                            loadPantry();
                        }
                    }
                })
                .setNegativeButton(R.string.dialog_no, null)
                .show();
    }

    // ---- Toolbar menu: change the sort order ----

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_sort_name) {
            sortOrder = DatabaseHelper.SORT_NAME;
            loadPantry();
            return true;
        } else if (id == R.id.action_sort_expiry) {
            sortOrder = DatabaseHelper.SORT_EXPIRY;
            loadPantry();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
