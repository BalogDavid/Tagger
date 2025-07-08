package com.example.tagger;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.tagger.data.ScanHistoryItem;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * ADAPTOR PENTRU AFISAREA ELEMENTELOR DIN ISTORICUL SCANARILOR
 */
public class HistoryAdapter extends BaseAdapter {
    private Context context;
    private List<ScanHistoryItem> historyItems;
    private SimpleDateFormat dateFormat;

    public HistoryAdapter(Context context, List<ScanHistoryItem> historyItems) {
        this.context = context;
        this.historyItems = historyItems;
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
    }

    public void updateData(List<ScanHistoryItem> newItems) {
        this.historyItems = newItems;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return historyItems != null ? historyItems.size() : 0;
    }

    @Override
    public Object getItem(int position) {
        return historyItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return historyItems.get(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.history_item, parent, false);
            holder = new ViewHolder();
            holder.dateTextView = convertView.findViewById(R.id.historyItemDate);
            holder.brandTextView = convertView.findViewById(R.id.historyItemBrand);
            holder.resultTextView = convertView.findViewById(R.id.historyItemResult);
            holder.thumbnailImageView = convertView.findViewById(R.id.historyItemImage);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        // OBTINEM ELEMENTUL CURENT
        ScanHistoryItem item = historyItems.get(position);

        // SETAM DATELE
        String formattedDate = dateFormat.format(new Date(item.getDate()));
        holder.dateTextView.setText(context.getString(R.string.history_item_date, formattedDate));
        holder.brandTextView.setText(context.getString(R.string.history_item_brand, item.getBrandName()));
        holder.resultTextView.setText(context.getString(R.string.history_item_result, item.getResult()));

        // INCARCAM IMAGINEA DACA EXISTA
        String imagePath = item.getImagePath();
        if (imagePath != null && !imagePath.isEmpty()) {
            // INCARCAM IMAGINEA DIN CALEA SALVATA
            File imageFile = new File(imagePath);
            if (imageFile.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
                if (bitmap != null) {
                    holder.thumbnailImageView.setImageBitmap(bitmap);
                } else {
                    holder.thumbnailImageView.setImageResource(R.drawable.ic_launcher_background);
                }
            } else {
                holder.thumbnailImageView.setImageResource(R.drawable.ic_launcher_background);
            }
        } else {
            holder.thumbnailImageView.setImageResource(R.drawable.ic_launcher_background);
        }

        // COLORAM REZULTATUL IN FUNCTIE DE VALOARE
        String resultText = item.getResult().toLowerCase();
        if (resultText.contains("autentic")) {
            holder.resultTextView.setTextColor(context.getResources().getColor(android.R.color.holo_green_dark, null));
        } else if (resultText.contains("fals")) {
            holder.resultTextView.setTextColor(context.getResources().getColor(android.R.color.holo_red_dark, null));
        } else {
            holder.resultTextView.setTextColor(context.getResources().getColor(android.R.color.darker_gray, null));
        }

        return convertView;
    }

    static class ViewHolder {
        TextView dateTextView;
        TextView brandTextView;
        TextView resultTextView;
        ImageView thumbnailImageView;
    }
} 