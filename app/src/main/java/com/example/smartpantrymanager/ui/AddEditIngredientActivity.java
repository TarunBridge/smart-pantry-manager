package com.example.smartpantrymanager.ui;

import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.smartpantrymanager.R;
import com.example.smartpantrymanager.data.DatabaseHelper;
import com.example.smartpantrymanager.model.Ingredient;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * SCREEN 2 - Add / Edit Ingredient.
 *
 * The same Activity handles both modes. When the Intent carries EXTRA_INGREDIENT_ID the
 * screen pre-fills the record and enables Delete; otherwise it behaves as "add new".
 * No record reaches the database until validateInput() has passed.
 */
public class AddEditIngredientActivity extends AppCompatActivity {

    public static final String EXTRA_INGREDIENT_ID = "extra_ingredient_id";
    public static final long NO_ID = -1L;

    private DatabaseHelper databaseHelper;
    private long ingredientId = NO_ID;

    private TextInputLayout tilName;
    private TextInputLayout tilQuantity;
    private TextInputLayout tilExpiry;
    private TextInputEditText etName;
    private TextInputEditText etQuantity;
    private TextInputEditText etExpiry;
    private Spinner spinnerUnit;
    private Button btnSave;
    private Button btnDelete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_ingredient);

        databaseHelper = new DatabaseHelper(this);

        tilName = findViewById(R.id.tilName);
        tilQuantity = findViewById(R.id.tilQuantity);
        tilExpiry = findViewById(R.id.tilExpiry);
        etName = findViewById(R.id.etName);
        etQuantity = findViewById(R.id.etQuantity);
        etExpiry = findViewById(R.id.etExpiry);
        spinnerUnit = findViewById(R.id.spinnerUnit);
        btnSave = findViewById(R.id.btnSave);
        btnDelete = findViewById(R.id.btnDelete);

        // Units come from res/values/arrays.xml so the spinner and the database
        // always use exactly the same strings.
        ArrayAdapter<CharSequence> unitAdapter = ArrayAdapter.createFromResource(this,
                R.array.units, android.R.layout.simple_spinner_item);
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerUnit.setAdapter(unitAdapter);

        ingredientId = getIntent().getLongExtra(EXTRA_INGREDIENT_ID, NO_ID);
        boolean editMode = ingredientId != NO_ID;
        setTitle(editMode ? R.string.title_edit_ingredient : R.string.title_add_ingredient);
        btnDelete.setVisibility(editMode ? View.VISIBLE : View.GONE);

        if (editMode) {
            populateFields(databaseHelper.getIngredient(ingredientId));
        }

        // The expiry field is read-only: typing is replaced by a DatePicker so the
        // stored value is always a valid "yyyy-MM-dd" string.
        View.OnClickListener openPicker = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker();
            }
        };
        etExpiry.setOnClickListener(openPicker);
        etExpiry.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    showDatePicker();
                }
            }
        });

        clearErrorOnTyping(etName, tilName);
        clearErrorOnTyping(etQuantity, tilQuantity);

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveIngredient();
            }
        });

        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmDelete();
            }
        });

        spinnerUnit.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                tilExpiry.setError(null);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // A unit is always pre-selected from the array.
            }
        });
    }

    private void populateFields(Ingredient ingredient) {
        if (ingredient == null) {
            Toast.makeText(this, R.string.err_record_missing, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        etName.setText(ingredient.getName());
        etQuantity.setText(String.valueOf(ingredient.getQuantity()));
        etExpiry.setText(ingredient.getExpiryDate());

        ArrayAdapter<CharSequence> units = (ArrayAdapter<CharSequence>) spinnerUnit.getAdapter();
        for (int i = 0; i < units.getCount(); i++) {
            if (units.getItem(i).toString().equalsIgnoreCase(ingredient.getUnit())) {
                spinnerUnit.setSelection(i);
                break;
            }
        }
    }

    /** Input validation: nothing is written to the database until this passes. */
    private boolean validateInput() {
        boolean valid = true;

        String name = textOf(etName).trim();
        if (name.isEmpty()) {
            tilName.setError(getString(R.string.err_name_required));
            valid = false;
        } else if (!name.matches("[A-Za-z0-9 \\-']+")) {
            tilName.setError(getString(R.string.err_name_invalid));
            valid = false;
        } else {
            tilName.setError(null);
        }

        String quantityText = textOf(etQuantity).trim();
        if (quantityText.isEmpty()) {
            tilQuantity.setError(getString(R.string.err_quantity_required));
            valid = false;
        } else {
            try {
                double quantity = Double.parseDouble(quantityText);
                if (quantity <= 0) {
                    tilQuantity.setError(getString(R.string.err_quantity_invalid));
                    valid = false;
                } else {
                    tilQuantity.setError(null);
                }
            } catch (NumberFormatException e) {
                tilQuantity.setError(getString(R.string.err_quantity_invalid));
                valid = false;
            }
        }

        if (spinnerUnit.getSelectedItem() == null) {
            Toast.makeText(this, R.string.err_unit_required, Toast.LENGTH_SHORT).show();
            valid = false;
        }
        return valid;
    }

    private void saveIngredient() {
        if (!validateInput()) {
            return;
        }

        Ingredient ingredient = new Ingredient();
        ingredient.setId(ingredientId);
        ingredient.setName(textOf(etName).trim());
        ingredient.setQuantity(Double.parseDouble(textOf(etQuantity).trim()));
        ingredient.setUnit(spinnerUnit.getSelectedItem().toString());
        ingredient.setExpiryDate(textOf(etExpiry).trim());

        if (ingredientId == NO_ID) {
            databaseHelper.insertIngredient(ingredient);
        } else {
            databaseHelper.updateIngredient(ingredient);
        }

        Toast.makeText(this, getString(R.string.msg_ingredient_saved, ingredient.getName()),
                Toast.LENGTH_SHORT).show();
        finish();
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_delete_title)
                .setMessage(getString(R.string.dialog_delete_message, textOf(etName)))
                .setPositiveButton(R.string.dialog_yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        databaseHelper.deleteIngredient(ingredientId);
                        Toast.makeText(AddEditIngredientActivity.this,
                                getString(R.string.msg_ingredient_deleted, textOf(etName)),
                                Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .setNegativeButton(R.string.dialog_no, null)
                .show();
    }

    private void showDatePicker() {
        final Calendar calendar = Calendar.getInstance();
        String current = textOf(etExpiry).trim();
        if (!current.isEmpty()) {
            try {
                Date parsed = new SimpleDateFormat(Ingredient.DATE_FORMAT, Locale.getDefault()).parse(current);
                if (parsed != null) {
                    calendar.setTime(parsed);
                }
            } catch (Exception ignored) {
                // Fall back to today.
            }
        }

        new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                calendar.set(year, month, dayOfMonth);
                String formatted = new SimpleDateFormat(Ingredient.DATE_FORMAT, Locale.getDefault())
                        .format(calendar.getTime());
                etExpiry.setText(formatted);
            }
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void clearErrorOnTyping(TextInputEditText field, final TextInputLayout layout) {
        field.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                layout.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private String textOf(TextInputEditText field) {
        return field.getText() == null ? "" : field.getText().toString();
    }
}
