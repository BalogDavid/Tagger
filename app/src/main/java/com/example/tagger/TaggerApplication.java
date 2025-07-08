package com.example.tagger;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;

import androidx.appcompat.app.AppCompatDelegate;

import java.util.Locale;

/**
 * CLASA APPLICATION PERSONALIZATA PENTRU TAGGER, RESPONSABILA CU GESTIONAREA
 * STARII GLOBALE A APLICATIEI, INCLUSIV SETAREA TEMEI.
 */
public class TaggerApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        applyTheme();
        applyLanguage();
    }

    /**
     * APLICA TEMA SALVATA IN PREFERINTE
     */
    private void applyTheme() {
        SharedPreferences sharedPreferences = getSharedPreferences("app_preferences", Context.MODE_PRIVATE);
        int themeMode = sharedPreferences.getInt(SettingsActivity.PREF_THEME, SettingsActivity.THEME_SYSTEM);
        
        switch (themeMode) {
            case SettingsActivity.THEME_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case SettingsActivity.THEME_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case SettingsActivity.THEME_SYSTEM:
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    /**
     * APLICA LIMBA SALVATA IN PREFERINTE
     */
    private void applyLanguage() {
        SharedPreferences sharedPreferences = getSharedPreferences("app_preferences", Context.MODE_PRIVATE);
        String languageCode = sharedPreferences.getString(SettingsActivity.PREF_LANGUAGE, SettingsActivity.LANGUAGE_ROMANIAN);
        
        setLocale(languageCode);
    }

    /**
     * SETEAZA LIMBA APLICATIEI
     */
    private void setLocale(String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        
        Resources resources = getResources();
        Configuration config = resources.getConfiguration();
        config.setLocale(locale);
        
        resources.updateConfiguration(config, resources.getDisplayMetrics());
    }
} 