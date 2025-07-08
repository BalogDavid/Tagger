package com.example.tagger;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.example.tagger.data.ScanHistoryItem;
import com.example.tagger.data.ScanHistoryRepository;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ResultActivity extends BaseActivity {
    private Bitmap resultBitmap;
    private Uri imageUri;
    private String brandName;
    private String classificationResult;
    private float scoreValue = 0.5f; // VALOARE IMPLICITA PENTRU SCOR
    private ScanHistoryRepository historyRepository;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        // INITIALIZAM REPOSITORY-UL PENTRU ISTORIC
        historyRepository = new ScanHistoryRepository(this);

        // INITIALIZAM VIEW-URILE
        ImageView imageView = findViewById(R.id.resultImage);
        TextView brandNameText = findViewById(R.id.brandNameText);
        TextView resultText = findViewById(R.id.resultText);
        TextView confidenceScore = findViewById(R.id.confidenceScore);
        Button homeButton = findViewById(R.id.homeButton);
        Button saveResultButton = findViewById(R.id.saveResultButton);

        // OBTINEM DATELE DIN INTENT
        Intent intent = getIntent();
        brandName = intent.getStringExtra("BRAND_NAME");
        classificationResult = intent.getStringExtra("RESULT");
        String imageUriString = intent.getStringExtra("IMAGE_URI");

        // SETAM NUMELE BRANDULUI
        if (!TextUtils.isEmpty(brandName)) {
            brandNameText.setText(brandName);
        }

        // Verificăm dacă avem o imagine validă și o afișează cu orientarea corectă
        if (imageUriString != null) {
            try {
                imageUri = Uri.parse(imageUriString);
                
                // Încărcăm imaginea și o rotim corect în funcție de metadatele EXIF
                resultBitmap = loadAndRotateImageCorrectly(imageUri);
                if (resultBitmap != null) {
                    imageView.setImageBitmap(resultBitmap);
                } else {
                    // Fallback la metoda simplă dacă rotația eșuează
                    imageView.setImageURI(imageUri);
                }
            } catch (Exception e) {
                Toast.makeText(this, "Eroare la încărcarea imaginii", Toast.LENGTH_SHORT).show();
            }
        }

        // Afișăm rezultatul parsând String-ul rezultatului
        if (!TextUtils.isEmpty(classificationResult)) {
            // Interpretăm formatul "X Label" cu valoare între 0-1
            boolean isAuthentic = false;
            scoreValue = 0.5f; // Valoare implicită
            
            // Verificăm dacă rezultatul conține cuvântul "Authentic"
            if (classificationResult.toLowerCase().contains("authentic")) {
                isAuthentic = true;
                // Extrage scorul din rezultat - formatul tipic este "1 Authentic Labels (0.xxxxx)"
                try {
                    String[] parts = classificationResult.split("\\(");
                    if (parts.length > 1) {
                        String scorePart = parts[1].replace(")", "").trim();
                        scoreValue = Float.parseFloat(scorePart);
                    }
                } catch (Exception e) {
                    // Folosim scorul implicit
                }
            } else if (classificationResult.toLowerCase().contains("fake")) {
                isAuthentic = false;
                // Extrage scorul din rezultat - formatul tipic este "0 Fake Labels (0.xxxxx)"
                try {
                    String[] parts = classificationResult.split("\\(");
                    if (parts.length > 1) {
                        String scorePart = parts[1].replace(")", "").trim();
                        scoreValue = Float.parseFloat(scorePart);
                        // Pentru etichete false, inversăm scorul pentru a arăta "cât de sigur este că e fals"
                        scoreValue = 1.0f - scoreValue;
                    }
                } catch (Exception e) {
                    // Folosim scorul implicit
                }
            }
            
            // Setăm culoarea textului în funcție de rezultat (verde pentru autentic, roșu pentru fals)
            if (isAuthentic) {
                resultText.setText(R.string.result_authentic);
                resultText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
            } else {
                resultText.setText(R.string.result_fake);
                resultText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
            }
            
            // Afișăm scorul de încredere ca procentaj
            confidenceScore.setText(getString(R.string.confidence_score, scoreValue * 100));
            
            // Salvăm rezultatul în istoricul scanărilor
            saveToHistory(resultText.getText().toString());
            
        } else {
            // Valori implicite
            resultText.setText(R.string.result_inconclusive);
            resultText.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
            confidenceScore.setText(getString(R.string.confidence_score, 50.0f));
        }

        // Buton pentru revenirea la ecranul principal
        homeButton.setOnClickListener(view -> {
            Intent homeIntent = new Intent(ResultActivity.this, MainActivity.class);
            homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(homeIntent);
            finish();
        });
        
        // Buton pentru salvarea rezultatului
        saveResultButton.setOnClickListener(view -> {
            showSaveOptionsDialog();
        });
    }
    
    // Metodă pentru afișarea dialogului de salvare
    private void showSaveOptionsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Salvare imagine");
        
        // Opțiunile pentru salvare
        String[] options = {
                getString(R.string.save_to_gallery),
                getString(R.string.save_to_files)
        };
        
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0: // Salvare în galerie
                    saveResultToGallery();
                    break;
                case 1: // Salvare în fișiere
                    saveResultToFiles();
                    break;
            }
        });
        
        builder.show();
    }
    
    // Salvează imaginea rezultat în galerie
    private void saveResultToGallery() {
        Bitmap resultImageWithText = createResultImage();
        if (resultImageWithText != null) {
            try {
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                String title = "Tagger_" + timeStamp;
                String description = "Tagger result for " + brandName + ": " + classificationResult;
                
                // Utilizăm MediaStore API modern pentru Android 10+ (Q)
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, title);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                values.put(MediaStore.Images.Media.DESCRIPTION, description);
                values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000);
                values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
                
                Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                if (uri != null) {
                    try (OutputStream outputStream = getContentResolver().openOutputStream(uri)) {
                        if (outputStream != null) {
                            resultImageWithText.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                        }
                    }
                    Toast.makeText(this, "Imagine salvată în galerie", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(this, "Eroare la salvarea imaginii: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    // Salvează imaginea rezultat în directorul de fișiere al aplicației
    private void saveResultToFiles() {
        Bitmap resultImageWithText = createResultImage();
        if (resultImageWithText != null) {
            try {
                File pictureFile = createImageFile();
                FileOutputStream fos = new FileOutputStream(pictureFile);
                resultImageWithText.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                fos.close();
                
                // Utilizăm API MediaScanner modern în loc de ACTION_MEDIA_SCANNER_SCAN_FILE deprecated
                MediaScannerConnection.scanFile(
                    this,
                    new String[] { pictureFile.getAbsolutePath() },
                    new String[] { "image/jpeg" },
                    null
                );
                
                Toast.makeText(this, "Imagine salvată în: " + pictureFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(this, "Eroare la salvarea imaginii: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    // Creează un fișier pentru salvarea imaginii
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "Tagger_" + timeStamp;
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }
    
    // Creează o imagine cu rezultatul suprapus
    private Bitmap createResultImage() {
        if (resultBitmap == null) return null;
        
        // Creăm o copie a imaginii originale
        Bitmap resultImage = resultBitmap.copy(resultBitmap.getConfig(), true);
        Canvas canvas = new Canvas(resultImage);
        
        // Calculăm factorul de scalare în funcție de rezoluția imaginii
        // Folosim 600 ca rezoluție de referință pentru width
        int imageWidth = resultImage.getWidth();
        int imageHeight = resultImage.getHeight();
        
        // Calculăm factorul de scalare bazat pe lățimea imaginii
        float scaleFactor = Math.max(1.0f, imageWidth / 600.0f);
        
        // Ajustăm factorul de scalare pentru dimensiuni foarte mari
        if (scaleFactor > 4.0f) {
            scaleFactor = 4.0f + (scaleFactor - 4.0f) * 0.5f; // Limitare pentru imagini foarte mari
        }
        
        // Logare pentru debugging
        Log.d("ResultActivity", "Image size: " + imageWidth + "x" + imageHeight + ", Scale factor: " + scaleFactor);
        
        // Configurăm textul pentru rezultat cu dimensiuni scalate
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setTextSize(50 * scaleFactor); // Scalare la dimensiunea imaginii
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setShadowLayer(5 * scaleFactor, 0, 0, Color.BLACK);
        
        // Determinăm culoarea fundalului în funcție de rezultat
        int backgroundColor;
        String result;
        
        if (classificationResult.toLowerCase().contains("authentic")) {
            backgroundColor = Color.argb(180, 0, 200, 0);
            result = getString(R.string.result_authentic);
        } else if (classificationResult.toLowerCase().contains("fake")) {
            backgroundColor = Color.argb(180, 200, 0, 0);
            result = getString(R.string.result_fake);
        } else {
            backgroundColor = Color.argb(180, 150, 150, 150);
            result = getString(R.string.result_inconclusive);
        }
        
        // Creăm un fundal pentru text
        Paint bgPaint = new Paint();
        bgPaint.setColor(backgroundColor);
        bgPaint.setStyle(Paint.Style.FILL);
        
        // Extragem scorul dacă există
        String scoreText = "";
        try {
            String[] parts = classificationResult.split("\\(");
            if (parts.length > 1) {
                String scorePart = parts[1].replace(")", "").trim();
                float scoreValue = Float.parseFloat(scorePart);
                if (result.equals(getString(R.string.result_fake))) {
                    scoreValue = 1.0f - scoreValue;
                }
                scoreText = "Scor: " + String.format(Locale.getDefault(), "%.2f%%", scoreValue * 100);
            }
        } catch (Exception e) {
            // Ignorăm eroarea, nu afișăm scorul
        }
        
        // Calculăm dimensiunile textului
        Rect bounds = new Rect();
        paint.getTextBounds(result, 0, result.length(), bounds);
        
        // Valorile de padding scalate
        int padding = (int)(15 * scaleFactor);
        int left = padding;
        
        // Poziționăm textul în stânga jos
        int textWidth = Math.max(bounds.width() + padding * 2, 
                (!scoreText.isEmpty()) ? (int) paint.measureText(scoreText) + padding * 2 : 0);
        
        // Eliminăm limitarea de 35% din lățimea imaginii pentru a permite textului să fie afișat complet
        int right = left + textWidth;
        
        int scoreTextHeight = (!scoreText.isEmpty()) ? (int)(40 * scaleFactor) : 0;
        int height = bounds.height() + padding * 2 + scoreTextHeight;
        
        // Calculăm poziția y pentru a plasa textul în partea de jos
        int bottom = resultImage.getHeight() - padding;
        int top = bottom - height;
        
        // Desenăm fundalul pentru text
        canvas.drawRect(left, top, right, bottom, bgPaint);
        
        // Desenăm textul rezultatului
        canvas.drawText(result, left + padding, top + bounds.height() + padding / 2, paint);
        
        // Desenăm textul scorului dacă există
        if (!scoreText.isEmpty()) {
            Paint scorePaint = new Paint();
            scorePaint.setColor(Color.WHITE);
            scorePaint.setTextSize(40 * scaleFactor);
            scorePaint.setShadowLayer(3 * scaleFactor, 0, 0, Color.BLACK);
            
            canvas.drawText(scoreText, left + padding, top + bounds.height() + padding + (25 * scaleFactor), scorePaint);
        }
        
        return resultImage;
    }
    
    // Metodă pentru determinarea orientării corecte a imaginii din metadatele EXIF
    private Bitmap loadAndRotateImageCorrectly(Uri imageUri) {
        try {
            // Încărcăm imaginea
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            Bitmap originalBitmap = BitmapFactory.decodeStream(inputStream);
            if (inputStream != null) {
                inputStream.close();
            }
            
            if (originalBitmap == null) {
                return null;
            }
            
            // Obținem orientarea din metadatele EXIF dacă există
            int rotation = 0;
            try {
                if (imageUri.getScheme().equals("content")) {
                    inputStream = getContentResolver().openInputStream(imageUri);
                    if (inputStream != null) {
                        ExifInterface exif = new ExifInterface(inputStream);
                        int orientation = exif.getAttributeInt(
                                ExifInterface.TAG_ORIENTATION,
                                ExifInterface.ORIENTATION_NORMAL);
                        
                        switch (orientation) {
                            case ExifInterface.ORIENTATION_ROTATE_90:
                                rotation = 90;
                                break;
                            case ExifInterface.ORIENTATION_ROTATE_180:
                                rotation = 180;
                                break;
                            case ExifInterface.ORIENTATION_ROTATE_270:
                                rotation = 270;
                                break;
                        }
                        inputStream.close();
                    }
                } else if (imageUri.getScheme().equals("file")) {
                    ExifInterface exif = new ExifInterface(imageUri.getPath());
                    int orientation = exif.getAttributeInt(
                            ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_NORMAL);
                    
                    switch (orientation) {
                        case ExifInterface.ORIENTATION_ROTATE_90:
                            rotation = 90;
                            break;
                        case ExifInterface.ORIENTATION_ROTATE_180:
                            rotation = 180;
                            break;
                        case ExifInterface.ORIENTATION_ROTATE_270:
                            rotation = 270;
                            break;
                    }
                }
            } catch (Exception e) {
                // Încearcă să rotească 90 de grade deoarece imaginile din galerie sunt de obicei rotite
                rotation = 90;
            }
            
            // Rotim imaginea cu unghiul necesar
            if (rotation != 0) {
                Matrix matrix = new Matrix();
                matrix.postRotate(rotation);
                
                return Bitmap.createBitmap(
                    originalBitmap, 
                    0, 0, 
                    originalBitmap.getWidth(), 
                    originalBitmap.getHeight(), 
                    matrix, 
                    true
                );
            }
            
            return originalBitmap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Salvează rezultatul scanării în baza de date locală
     */
    private void saveToHistory(String result) {
        try {
            // Salvăm imaginea pe disc și obținem calea
            String savedImagePath = saveImageForHistory();
            
            // Creăm un nou obiect ScanHistoryItem
            ScanHistoryItem historyItem = new ScanHistoryItem(
                    System.currentTimeMillis(), // timestamp curent
                    brandName,
                    result,
                    scoreValue,
                    savedImagePath
            );
            
            // Adăugăm în baza de date
            historyRepository.addScanToHistory(historyItem);
        } catch (Exception e) {
            // În caz de eroare, salvăm fără imagine
            ScanHistoryItem historyItem = new ScanHistoryItem(
                    System.currentTimeMillis(),
                    brandName,
                    result,
                    scoreValue,
                    null
            );
            historyRepository.addScanToHistory(historyItem);
        }
    }
    
    /**
     * Salvează imaginea în directorul aplicației pentru istoric
     */
    private String saveImageForHistory() {
        try {
            // Creem un bitmap de dimensiune mai mică pentru a economisi spațiu
            Bitmap thumbnailBitmap = createThumbnail(resultBitmap, 300, 300);
            
            // Creem un fișier în directorul intern al aplicației
            File storageDir = new File(getFilesDir(), "history_images");
            if (!storageDir.exists()) {
                storageDir.mkdirs();
            }
            
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            File imageFile = new File(storageDir, "history_" + timeStamp + ".jpg");
            
            FileOutputStream fos = new FileOutputStream(imageFile);
            thumbnailBitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos);
            fos.flush();
            fos.close();
            
            return imageFile.getAbsolutePath();
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Creează un thumbnail de dimensiuni reduse pentru a economisi spațiu
     */
    private Bitmap createThumbnail(Bitmap original, int maxWidth, int maxHeight) {
        if (original == null) return null;
        
        int width = original.getWidth();
        int height = original.getHeight();
        
        float ratio = Math.min((float) maxWidth / width, (float) maxHeight / height);
        
        int finalWidth = Math.round(width * ratio);
        int finalHeight = Math.round(height * ratio);
        
        return Bitmap.createScaledBitmap(original, finalWidth, finalHeight, true);
    }
}
