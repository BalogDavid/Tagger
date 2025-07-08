package com.example.tagger;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;



/**
 * ACTIVITATEA DE BAZA CARE GESTIONEAZA TOATE ACTIVITATIILE
 * SI CONFIGURAREA LIMBII
 */
public abstract class BaseActivity extends AppCompatActivity {





    @Override
    protected void attachBaseContext(Context newBase) {
        // OBTINEM PREFERINTELE
        SharedPreferences preferences = newBase.getSharedPreferences("app_preferences", Context.MODE_PRIVATE);
        String languageCode = preferences.getString(SettingsActivity.PREF_LANGUAGE, SettingsActivity.LANGUAGE_ROMANIAN);
        
        // APLICA LIMBA
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        
        Configuration config = new Configuration(newBase.getResources().getConfiguration());
        config.setLocale(locale);
        
        Context context = newBase.createConfigurationContext(config);
        super.attachBaseContext(context);
    }




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // CONFIGURATII COMUNE
    }



} 