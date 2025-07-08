package com.example.tagger;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatDelegate;

import java.util.Locale;

public class SettingsActivity extends BaseActivity {
    private SharedPreferences sharedPreferences;
    private RadioGroup themeRadioGroup;
    private RadioGroup languageRadioGroup;
    private TextView userGuideTextView;
    private Button backButton;

    // CONSTANTE PENTRU CHEI PREFERINTE
    public static final String PREF_THEME = "app_theme";
    public static final String PREF_LANGUAGE = "app_language";
    public static final int THEME_LIGHT = 0;
    public static final int THEME_DARK = 1;
    public static final int THEME_SYSTEM = 2;
    public static final String LANGUAGE_ROMANIAN = "ro";
    public static final String LANGUAGE_ENGLISH = "en";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // CONFIGURAM BARA DE ACTIUNI CU BUTONUL DE INAPOI
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.settings);
        }

        // INITIALIZAM PREFERINTELE DIRECT FOLOSIND CONTEXT
        sharedPreferences = getSharedPreferences("app_preferences", Context.MODE_PRIVATE);

        // INITIALIZAM VIEW-URILE
        themeRadioGroup = findViewById(R.id.themeRadioGroup);
        languageRadioGroup = findViewById(R.id.languageRadioGroup);
        userGuideTextView = findViewById(R.id.userGuideText);
        backButton = findViewById(R.id.settingsBackButton);

        // Setăm listener pentru butonul de înapoi
        backButton.setOnClickListener(v -> finish());

        // Setăm valoarea selectată pentru tema aplicației
        int currentTheme = sharedPreferences.getInt(PREF_THEME, THEME_SYSTEM);
        selectThemeRadioButton(currentTheme);

        // Setăm valoarea selectată pentru limba aplicației
        String currentLanguage = sharedPreferences.getString(PREF_LANGUAGE, LANGUAGE_ROMANIAN);
        selectLanguageRadioButton(currentLanguage);

        // Listener pentru modificarea temei
        themeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            int selectedTheme;
            if (checkedId == R.id.radioLight) {
                selectedTheme = THEME_LIGHT;
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            } else if (checkedId == R.id.radioDark) {
                selectedTheme = THEME_DARK;
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                selectedTheme = THEME_SYSTEM;
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            }
            
            // Salvăm setarea
            sharedPreferences.edit().putInt(PREF_THEME, selectedTheme).apply();
        });

        // Listener pentru modificarea limbii
        languageRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            String selectedLanguage;
            if (checkedId == R.id.radioRomanian) {
                selectedLanguage = LANGUAGE_ROMANIAN;
            } else {
                selectedLanguage = LANGUAGE_ENGLISH;
            }
            
            // Dacă se schimbă limba, salvăm setarea și restartăm activitatea
            if (!currentLanguage.equals(selectedLanguage)) {
                // Salvăm limba selectată
                sharedPreferences.edit().putString(PREF_LANGUAGE, selectedLanguage).apply();
                
                // Restartăm activitățile pentru a aplica noua limbă
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Gestionăm click-ul pe butonul de înapoi
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    /**
     * SELECTEAZA BUTONUL RADIO CORESPUNZATOR TEMEI SALVATE
     */
    private void selectThemeRadioButton(int theme) {
        int radioButtonId;
        switch (theme) {
            case THEME_LIGHT:
                radioButtonId = R.id.radioLight;
                break;
            case THEME_DARK:
                radioButtonId = R.id.radioDark;
                break;
            case THEME_SYSTEM:
            default:
                radioButtonId = R.id.radioSystem;
                break;
        }
        themeRadioGroup.check(radioButtonId);
    }
    
    /**
     * Selectează butonul radio corespunzător limbii salvate
     */
    private void selectLanguageRadioButton(String language) {
        int radioButtonId;
        if (language.equals(LANGUAGE_ENGLISH)) {
            radioButtonId = R.id.radioEnglish;
        } else {
            radioButtonId = R.id.radioRomanian;
        }
        languageRadioGroup.check(radioButtonId);
    }
} 