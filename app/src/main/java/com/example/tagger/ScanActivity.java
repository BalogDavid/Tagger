package com.example.tagger;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.core.ZoomState;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.lifecycle.LiveData;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.content.ActivityNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.widget.ImageButton;
import android.annotation.SuppressLint;
import android.widget.CheckBox;

public class ScanActivity extends BaseActivity {
    private static final String TAG = "ScanActivity";
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.CAMERA
    };
    
    // PERMISIUNE PENTRU ACCESUL LA IMAGINILE SELECTATE PENTRU ANDROID 15
    private static final String[] PHOTO_PERMISSIONS = {
        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
    };

    private PreviewView previewView;
    private Button captureButton;
    private Button galleryButton;
    private TextView brandText;
    private ProgressBar cameraProgress;
    private Button backButton;
    private String brandName;
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;
    private Camera camera;
    private float zoomRatio = 1.0f;
    private ScaleGestureDetector scaleGestureDetector;
    
    // LAUNCHER PENTRU PERMISIUNI DE IMAGINI
    private ActivityResultLauncher<String[]> requestImagePermissions;
    
    // LAUNCHER PENTRU SELECTAREA FOTOGRAFIILOR (PHOTO PICKER API)
    private ActivityResultLauncher<PickVisualMediaRequest> pickMedia;

    // LAUNCHER PENTRU SELECTAREA DIN DOCUMENTE (FILES)
    private ActivityResultLauncher<Intent> documentLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        // INITIALIZAM VIEW-URILE
        previewView = findViewById(R.id.previewView);
        captureButton = findViewById(R.id.captureButton);
        galleryButton = findViewById(R.id.galleryButton);
        brandText = findViewById(R.id.brandText);
        cameraProgress = findViewById(R.id.cameraProgress);
        backButton = findViewById(R.id.backButton);

        // OBTINEM NUMELE BRANDULUI DIN INTENT
        brandName = getIntent().getStringExtra("BRAND_NAME");
        brandText.setText(getString(R.string.scan_label));

        cameraExecutor = Executors.newSingleThreadExecutor();
        
        // INITIALIZAM DETECTOR PENTRU GESTURI DE ZOOM
        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                if (camera != null) {
                    float scale = detector.getScaleFactor();
                    zoomRatio *= scale;
                    
                    // ASIGURAM CA ZOOM-UL RAMANE IN LIMITE ACCEPTABILE
                    LiveData<ZoomState> zoomState = camera.getCameraInfo().getZoomState();
                    ZoomState currentZoomState = zoomState.getValue();
                    
                    if (currentZoomState != null) {
                        float minZoom = currentZoomState.getMinZoomRatio();
                        float maxZoom = currentZoomState.getMaxZoomRatio();
                        
                        // LIMITAM ZOOM-UL LA VALORILE MINIME SI MAXIME
                        zoomRatio = Math.max(minZoom, Math.min(zoomRatio, maxZoom));
                        
                        // APLICAM ZOOM-UL
                        camera.getCameraControl().setZoomRatio(zoomRatio);
                    }
                    
                    return true;
                }
                return false;
            }
        });
        
        // ADAUGAM LISTENER PENTRU GESTURI PE PREVIEWVIEW
        previewView.setOnTouchListener((v, event) -> {
            scaleGestureDetector.onTouchEvent(event);
            return true;
        });
        
        // INITIALIZAM LAUNCHER-UL PENTRU PERMISIUNI DE IMAGINI
        requestImagePermissions = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                boolean allGranted = true;
                for (Boolean granted : result.values()) {
                    allGranted = allGranted && granted;
                }
                
                if (allGranted) {
                    // Deschide dialogul de selectare a sursei
                    showImageSourceOptions();
                } else {
                    Toast.makeText(this, "Permisiuni necesare pentru accesul la galerie și fișiere", Toast.LENGTH_SHORT).show();
                }
            }
        );
        
        // Inițializăm launcher-ul pentru selectarea fotografiilor (Photo Picker API)
        pickMedia = registerForActivityResult(
            new ActivityResultContracts.PickVisualMedia(),
            uri -> {
                if (uri != null) {
                    processSelectedImage(uri);
                } else {
                    Log.d(TAG, "Nicio imagine selectată");
                }
            }
        );

        // Inițializăm launcher-ul pentru Document Picker
        documentLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        processSelectedImage(uri);
                    }
                }
            }
        );

        // Verificăm permisiunile și pornim camera
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        // Setăm listener pentru butonul de captură
        captureButton.setOnClickListener(view -> {
            if (imageCapture != null) {
                cameraProgress.setVisibility(View.VISIBLE);
                captureImage();
            } else {
                Toast.makeText(this, "Camera se inițializează, vă rugăm așteptați...", Toast.LENGTH_SHORT).show();
            }
        });
        
        // Setăm listener pentru butonul de galerie
        galleryButton.setOnClickListener(view -> {
            requestGalleryPermissions();
        });

        // Setăm listener pentru butonul de înapoi
        backButton.setOnClickListener(view -> {
            finish();
        });
    }
    
    private void requestGalleryPermissions() {
        // Verificăm dacă toate permisiunile sunt deja acordate
        boolean allGranted = true;
        for (String permission : PHOTO_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }
        
        if (allGranted) {
            showImageSourceOptions();
        } else {
            requestImagePermissions.launch(PHOTO_PERMISSIONS);
        }
    }
    
    private void showImageSourceOptions() {
        // Creăm un dialog pentru a alege sursa imaginii
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Selectează sursa");
        
        String[] options = {"Galerie", "Fișiere"};
        
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0: // Galerie
                    openPhotoSelector();
                    break;
                case 1: // Fișiere
                    openDocumentPicker();
                    break;
            }
        });
        
        builder.show();
    }
    
    private void openPhotoSelector() {
        // Folosim noua Photo Picker API pentru Android 15
        pickMedia.launch(new PickVisualMediaRequest.Builder()
            .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
            .build());
    }
    
    private void openDocumentPicker() {
        // Folosim Document Picker pentru a selecta imagini din fișiere
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        
        try {
            documentLauncher.launch(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Nu există aplicație pentru selectarea fișierelor", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void processSelectedImage(Uri imageUri) {
        try {
            // Creăm un fișier temporar pentru a stoca imaginea
            File photoFile = createImageFile();
            
            // Copiem imaginea selectată în fișierul temporar
            try (InputStream inputStream = getContentResolver().openInputStream(imageUri);
                FileOutputStream outputStream = new FileOutputStream(photoFile)) {
                 
                if (inputStream == null) {
                    throw new IOException("Nu s-a putut accesa imaginea selectată");
                }
                
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                
                // Obține URI pentru fișier
                Uri photoUri = FileProvider.getUriForFile(this,
                        getPackageName() + ".provider", photoFile);
                
                // Procesează imaginea și navighează la următorul ecran
                processImageAndNavigate(photoFile, photoUri);
            }
        } catch (Exception e) {
            Log.e(TAG, "Eroare la procesarea imaginii selectate: " + e.getMessage(), e);
            Toast.makeText(this, "Eroare la procesarea imaginii: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("Deprecation")
    private void startCamera() {
        cameraProgress.setVisibility(View.VISIBLE);
        
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = 
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // Configurăm preview-ul
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // Configurăm captura de imagine cu setări îmbunătățite
                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation())
                        .setJpegQuality(90)
                        .build();

                // Selectăm camera din spate
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                // Asociem camerele la lifecycle
                cameraProvider.unbindAll();
                camera = cameraProvider.bindToLifecycle(
                        this, cameraSelector, preview, imageCapture);

                cameraProgress.setVisibility(View.GONE);
                
                // Afișăm dialogul cu sfaturi pentru scanare
                showScanningTips();
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Eroare la pornirea camerei: ", e);
                Toast.makeText(this, "Eroare la inițializarea camerei. Încercați din nou.", 
                        Toast.LENGTH_SHORT).show();
                cameraProgress.setVisibility(View.GONE);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void captureImage() {
        if (imageCapture == null) return;

        // Creăm fișierul unde va fi salvată imaginea
        File photoFile;
        try {
            photoFile = createImageFile();
        } catch (IOException e) {
            Log.e(TAG, "Eroare la crearea fișierului: ", e);
            Toast.makeText(this, "Eroare la pregătirea stocării imaginii!", Toast.LENGTH_SHORT).show();
            cameraProgress.setVisibility(View.GONE);
            return;
        }

        // Opțiuni pentru salvarea imaginii
        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions
                .Builder(photoFile).build();

        // Captăm imaginea
        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Uri photoUri = FileProvider.getUriForFile(ScanActivity.this,
                                getPackageName() + ".provider", photoFile);
                        
                        // Procesăm imaginea și navigăm la următorul ecran
                        processImageAndNavigate(photoFile, photoUri);
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e(TAG, "Eroare la salvarea imaginii: ", exception);
                        Toast.makeText(ScanActivity.this, "Eroare la salvarea imaginii!", 
                                Toast.LENGTH_SHORT).show();
                        cameraProgress.setVisibility(View.GONE);
                    }
                });
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private void processImageAndNavigate(File photoFile, Uri photoUri) {
        // Pentru demonstrație, trecem direct la rezultate (autentice)
        String result = "Autentic (95%)";

        // Verificăm și corectăm rotația imaginii dacă e necesar
        try {
            // Obținem orientarea din EXIF
            ExifInterface exif = new ExifInterface(photoFile.getAbsolutePath());
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
            
            if (orientation != ExifInterface.ORIENTATION_NORMAL) {
                Log.d(TAG, "Imaginea necesită rotație, orientare EXIF: " + orientation);
                
                // Încărcăm bitmap
                Bitmap originalBitmap = BitmapFactory.decodeFile(photoFile.getAbsolutePath());
                
                // Calculăm unghiul de rotație
                int rotationAngle = 0;
                switch (orientation) {
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        rotationAngle = 90;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        rotationAngle = 180;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        rotationAngle = 270;
                        break;
                }
                
                if (rotationAngle != 0) {
                    Log.d(TAG, "Rotim imaginea cu " + rotationAngle + " grade");
                    Matrix matrix = new Matrix();
                    matrix.postRotate(rotationAngle);
                    
                    Bitmap rotatedBitmap = Bitmap.createBitmap(
                        originalBitmap, 
                        0, 0, 
                        originalBitmap.getWidth(), 
                        originalBitmap.getHeight(), 
                        matrix, 
                        true
                    );
                    
                    // Salvăm imaginea rotită
                    try (FileOutputStream out = new FileOutputStream(photoFile)) {
                        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
                        Log.d(TAG, "Imagine rotită salvată cu succes");
                    }
                    
                    // Curățăm memoria
                    originalBitmap.recycle();
                    rotatedBitmap.recycle();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Eroare la corectarea rotației imaginii: " + e.getMessage(), e);
        }

        // Încercăm să folosim clasificatorul specific pentru brand
        try {
            Log.d(TAG, "Inițializăm clasificatorul pentru brandul: " + brandName);
            LabelClassifier classifier = new LabelClassifier(this, brandName);
            Log.d(TAG, "Clasificăm imaginea de la calea: " + photoFile.getAbsolutePath());
            result = classifier.classifyImage(photoFile.getAbsolutePath());
            Log.d(TAG, "Rezultat clasificare: " + result);
            classifier.close();
        } catch (IOException e) {
            Log.e(TAG, "Eroare IO la clasificare: " + e.getMessage(), e);
            // Afișăm un mesaj explicit despre eroare
            String errorMessage = "Eroare la încărcarea modelului: " + e.getMessage();
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
            
            // Setăm un rezultat care indică eroarea
            result = "Eroare: " + e.getMessage();
        } catch (Exception e) {
            Log.e(TAG, "Eroare generală la clasificare: " + e.getMessage(), e);
            // Folosim rezultatul implicit dacă apare o eroare
            Toast.makeText(this, "Eroare la clasificare: " + e.getMessage(), Toast.LENGTH_LONG).show();
            
            // Setăm un rezultat care indică eroarea
            result = "Eroare clasificare: " + e.getMessage();
        }

        // Navigăm la ecranul de rezultate
        Intent intent = new Intent(ScanActivity.this, ResultActivity.class);
        intent.putExtra("BRAND_NAME", brandName);
        intent.putExtra("RESULT", result);
        intent.putExtra("IMAGE_URI", photoUri.toString());
        startActivity(intent);
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Permisiuni refuzate.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
    
    /**
     * Afișează un dialog cu sfaturi pentru scanare
     */
    private void showScanningTips() {
        // Verificăm mai întâi dacă utilizatorul a ales anterior să nu mai vadă sfaturile
        android.content.SharedPreferences prefs = getSharedPreferences("tagger_preferences", MODE_PRIVATE);
        boolean dontShowTips = prefs.getBoolean("dont_show_scanning_tips", false);
        
        // Dacă utilizatorul a ales să nu mai vadă sfaturile, nu mai afișăm dialogul
        if (dontShowTips) {
            return;
        }
        
        try {
            // Creăm un view personalizat cu checkbox
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_scanning_tips, null);
            CheckBox checkBoxDontShow = dialogView.findViewById(R.id.checkBoxDontShow);
            
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.scan_tips_title)
                   .setMessage(R.string.scan_tips_message)
                   .setView(dialogView)
                   .setPositiveButton(R.string.got_it, (dialog, which) -> {
                       // Salvăm preferința utilizatorului dacă checkbox-ul este bifat
                       if (checkBoxDontShow.isChecked()) {
                           android.content.SharedPreferences.Editor editor = prefs.edit();
                           editor.putBoolean("dont_show_scanning_tips", true);
                           editor.apply();
                       }
                       dialog.dismiss();
                   })
                   .setCancelable(true)
                   .create()
                   .show();
        } catch (Exception e) {
            // Fallback la dialog simplu dacă există probleme cu layout-ul personalizat
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.scan_tips_title)
                   .setMessage(R.string.scan_tips_message)
                   .setPositiveButton(R.string.got_it, (dialog, which) -> {
                       dialog.dismiss();
                   })
                   .setNeutralButton(R.string.dont_show_again_checkbox, (dialog, which) -> {
                       // Salvăm preferința utilizatorului
                       android.content.SharedPreferences.Editor editor = prefs.edit();
                       editor.putBoolean("dont_show_scanning_tips", true);
                       editor.apply();
                       dialog.dismiss();
                   })
                   .setCancelable(true)
                   .create()
                   .show();
        }
    }
}
