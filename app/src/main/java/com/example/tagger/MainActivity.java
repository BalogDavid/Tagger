package com.example.tagger;

import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.GridView;
import android.widget.ImageButton;
import androidx.core.os.HandlerCompat;
import android.os.Handler;
import androidx.appcompat.app.AlertDialog;

public class MainActivity extends BaseActivity {

    private String[] brandNames = {
            "Nike", "Adidas", "Ralph Lauren", "Tommy Hilfiger", "Lacoste", "Levi's",
            "Carhartt", "Chanel", "Gucci", "Jordan",
            "Louis Vuitton", "The North Face", "Off-White", "Balenciaga",
            "Stone Island", "Stussy", "Supreme", "Bape"
    };

    private int[] brandLogos = {
            R.drawable.nike, R.drawable.adidas, R.drawable.ralphlauren, R.drawable.tommyhilfiger, R.drawable.lacoste, R.drawable.levis,
            R.drawable.carhartt, R.drawable.chanel, R.drawable.gucci, R.drawable.jordan,
            R.drawable.louisvuitton, R.drawable.north, R.drawable.offwhite, R.drawable.balenciaga,
            R.drawable.stoneisland, R.drawable.stussy, R.drawable.supreme, R.drawable.bape
    };

    private BrandAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // CONFIGURAM BUTOANELE DE ACTIUNE
        ImageButton historyButton = findViewById(R.id.historyButton);
        ImageButton settingsButton = findViewById(R.id.settingsButton);
        
        // SETAM CLICK LISTENERS PENTRU BUTOANE
        historyButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
            startActivity(intent);
        });
        
        settingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        // CONFIGURAM GRIDVIEW
        final GridView gridView = findViewById(R.id.brandGrid);
        
        // OPTIMIZARI PENTRU SCROLL
        gridView.setFastScrollEnabled(true);
        gridView.setScrollingCacheEnabled(false);
        
        // FOLOSIM HANDLERCOMPAT PENTRU A INCARCA ADAPTORUL DUPA CE UI-UL ESTE PREGATIT
        // HANDLERCOMPAT OFERA COMPATIBILITATE IMBUNATATITA PENTRU VERSIUNI MAI NOI
        Handler mainHandler = HandlerCompat.createAsync(Looper.getMainLooper());
        mainHandler.post(() -> {
            adapter = new BrandAdapter(this, brandNames, brandLogos);
            gridView.setAdapter(adapter);

            gridView.setOnItemClickListener((parent, view, position, id) -> {
                // VERIFICAM DACA BRANDUL ESTE DISPONIBIL
                if (adapter.isBrandAvailable(position)) {
                    // BRAND DISPONIBIL - NAVIGHEAZA LA SCANACTIVITY
                    Intent intent = new Intent(MainActivity.this, ScanActivity.class);
                    intent.putExtra("BRAND_NAME", brandNames[position]);
                    startActivity(intent);
                } else {
                    // BRAND INDISPONIBIL - AFISEAZA POPUP
                    showBrandUnavailableDialog(brandNames[position]);
                }
            });
            
            // ASCUNDEM LOADING INDICATOR DACA EXISTA
            View loadingIndicator = findViewById(R.id.loadingIndicator);
            if (loadingIndicator != null) {
                loadingIndicator.setVisibility(View.GONE);
            }
        });
    }
    
    /**
     * AFISEAZA UN DIALOG CARE INFORMEAZA UTILIZATORUL CA BRANDUL NU ESTE DISPONIBIL
     */
    private void showBrandUnavailableDialog(String brandName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(brandName)
               .setMessage(R.string.brand_unavailable_message)
               .setPositiveButton(R.string.got_it, (dialog, which) -> dialog.dismiss())
               .setIcon(android.R.drawable.ic_dialog_info)
               .show();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // FORTAM REDRAW-UL GRIDVIEW PENTRU A REZOLVA PROBLEME DE SCROLLING
        GridView gridView = findViewById(R.id.brandGrid);
        if (gridView != null && gridView.getAdapter() != null) {
            gridView.invalidateViews();
        }
    }
}