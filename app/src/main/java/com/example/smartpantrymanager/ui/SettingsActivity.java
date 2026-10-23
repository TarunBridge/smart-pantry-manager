package com.example.smartpantrymanager.ui;

import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.example.smartpantrymanager.R;
import com.example.smartpantrymanager.data.DatabaseHelper;
import com.example.smartpantrymanager.util.Prefs;
import com.google.android.material.switchmaterial.SwitchMaterial;

/**
 * SCREEN 5 - Settings.
 *
 * Each control writes straight to SharedPreferences, so the choice survives a restart and
 * takes effect the next time the pantry or recipe screen resumes.
 */
public class SettingsActivity extends BaseNavActivity {

    private DatabaseHelper databaseHelper;
    private SwitchMaterial switchAlerts;
    private SwitchMaterial switchExcludeExpired;
    private SwitchMaterial switchAlmostThere;
    private RadioGroup radioUnits;
    private TextView tvDbInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        databaseHelper = new DatabaseHelper(this);

        switchAlerts = findViewById(R.id.switchAlerts);
        switchExcludeExpired = findViewById(R.id.switchExcludeExpired);
        switchAlmostThere = findViewById(R.id.switchAlmostThere);
        radioUnits = findViewById(R.id.radioUnits);
        tvDbInfo = findViewById(R.id.tvDbInfo);

        switchAlerts.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Prefs.setBoolean(SettingsActivity.this, Prefs.KEY_EXPIRY_ALERTS, isChecked);
            }
        });

        switchExcludeExpired.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Prefs.setBoolean(SettingsActivity.this, Prefs.KEY_EXCLUDE_EXPIRED, isChecked);
            }
        });

        switchAlmostThere.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Prefs.setBoolean(SettingsActivity.this, Prefs.KEY_SHOW_ALMOST_THERE, isChecked);
            }
        });

        radioUnits.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                Prefs.setUnits(SettingsActivity.this, checkedId == R.id.radioImperial);
            }
        });

        findViewById(R.id.btnLoadDemo).setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                databaseHelper.loadDemoPantry();
                Toast.makeText(SettingsActivity.this, R.string.msg_demo_loaded, Toast.LENGTH_SHORT).show();
                refreshInfo();
            }
        });

        findViewById(R.id.btnClearPantry).setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                new AlertDialog.Builder(SettingsActivity.this)
                        .setTitle(R.string.action_clear_pantry)
                        .setMessage(R.string.dialog_clear_pantry_message)
                        .setPositiveButton(R.string.dialog_yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                databaseHelper.deleteAllIngredients();
                                Toast.makeText(SettingsActivity.this, R.string.msg_pantry_cleared,
                                        Toast.LENGTH_SHORT).show();
                                refreshInfo();
                            }
                        })
                        .setNegativeButton(R.string.dialog_no, null)
                        .show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupBottomNavigation(R.id.nav_settings);

        // Reflect the stored values in the widgets.
        switchAlerts.setChecked(Prefs.expiryAlertsEnabled(this));
        switchExcludeExpired.setChecked(Prefs.excludeExpiredEnabled(this));
        switchAlmostThere.setChecked(Prefs.showAlmostThereEnabled(this));
        radioUnits.check(Prefs.useImperialUnits(this) ? R.id.radioImperial : R.id.radioMetric);

        refreshInfo();
    }

    private void refreshInfo() {
        tvDbInfo.setText(getString(R.string.msg_recipes_seeded, databaseHelper.countRecipes())
                + "  |  " + getString(R.string.label_pantry_count, databaseHelper.countIngredients()));
    }
}
