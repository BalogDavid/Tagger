package com.example.tagger;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;

import com.example.tagger.data.ScanHistoryItem;
import com.example.tagger.data.ScanHistoryRepository;

import java.util.List;

public class HistoryActivity extends BaseActivity {
    private ScanHistoryRepository historyRepository;
    private ListView historyListView;
    private TextView emptyHistoryText;
    private Button clearHistoryButton;
    private Button backButton;
    private HistoryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        // INITIALIZAM REPOSITORY-UL
        historyRepository = new ScanHistoryRepository(this);

        // INITIALIZAM VIEW-URILE
        historyListView = findViewById(R.id.historyListView);
        emptyHistoryText = findViewById(R.id.emptyHistoryText);
        clearHistoryButton = findViewById(R.id.clearHistoryButton);
        backButton = findViewById(R.id.historyBackButton);

        // CONFIGURAM BUTONUL PENTRU STERGEREA ISTORICULUI
        clearHistoryButton.setOnClickListener(v -> {
            showDeleteConfirmationDialog();
        });

        // CONFIGURAM BUTONUL DE NAVIGARE INAPOI
        backButton.setOnClickListener(v -> finish());

        // INCARCAM DATELE SI CONFIGURAM ADAPTORUL
        loadHistory();
    }

    /**
     * AFISEAZA UN DIALOG DE CONFIRMARE INAINTE DE STERGEREA ISTORICULUI
     */
    private void showDeleteConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.delete_confirmation_title);
        builder.setMessage(R.string.delete_confirmation_message);
        builder.setPositiveButton(R.string.yes, (dialog, which) -> {
            historyRepository.clearHistory();
            loadHistory();
        });
        builder.setNegativeButton(R.string.no, (dialog, which) -> {
            dialog.dismiss();
        });
        builder.create().show();
    }

    /**
     * INCARCA ISTORICUL SCANARILOR SI ACTUALIZEAZA UI-UL
     */
    private void loadHistory() {
        List<ScanHistoryItem> historyItems = historyRepository.getAllScans();

        // ACTUALIZAM VIZIBILITATEA
        if (historyItems.isEmpty()) {
            historyListView.setVisibility(View.GONE);
            emptyHistoryText.setVisibility(View.VISIBLE);
        } else {
            historyListView.setVisibility(View.VISIBLE);
            emptyHistoryText.setVisibility(View.GONE);

            // CONFIGURAM ADAPTORUL
            if (adapter == null) {
                adapter = new HistoryAdapter(this, historyItems);
                historyListView.setAdapter(adapter);
            } else {
                adapter.updateData(historyItems);
            }
        }
    }
} 