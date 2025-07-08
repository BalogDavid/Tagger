package com.example.tagger;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.util.Log;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.CompatibilityList;
import org.tensorflow.lite.gpu.GpuDelegate;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class LabelClassifier {
    private static final String TAG = "LabelClassifier";

    // MODEL PARAMETERS
    private static final int IMAGE_SIZE = 224;
    private GpuDelegate gpuDelegate = null;
    private Interpreter interpreter;
    private List<String> labels;
    private final Context context;
    private int numClasses = 0;
    private String modelPath;
    private String labelPath;

    // CONSTRUCTOR THAT TAKES BRAND NAME AND BUILDS MODEL AND LABEL PATHS
    public LabelClassifier(Context context, String brandName) throws IOException {
        this.context = context;
        
        // ELIMINAM COMPLET SPATIILE DIN NUMELE BRANDULUI PENTRU CAILE DE FISIERE
        String brandNameForPath = brandName.toLowerCase().replace(" ", "");
        
        // SETAM CAILE PENTRU FISIERE FOLOSIND CONVENTIA SIMPLIFICATA
        this.modelPath = brandNameForPath + "/" + brandNameForPath + "_model.tflite";
        this.labelPath = brandNameForPath + "/" + brandNameForPath + "_labels.txt";
        
        Log.d(TAG, "Using model path: " + modelPath + " and labels path: " + labelPath);
        setupInterpreter();
        loadLabels();
    }

    private void setupInterpreter() throws IOException {
        Log.d(TAG, "Setting up interpreter for model: " + modelPath);
        
        // DISPLAY ASSET DIRECTORY CONTENTS FOR DEBUGGING
        String[] assets = context.getAssets().list("");
        if (assets != null) {
            Log.d(TAG, "Asset directory contents:");
            for (String asset : assets) {
                Log.d(TAG, "- " + asset);
            }
        }
        
        // GET BRAND DIRECTORY NAME FROM MODEL PATH
        String brandDir = modelPath.substring(0, modelPath.lastIndexOf('/'));
        String[] brandAssets = context.getAssets().list(brandDir);
        if (brandAssets != null) {
            Log.d(TAG, "Brand directory contents for " + brandDir + ":");
            for (String asset : brandAssets) {
                Log.d(TAG, "- " + asset);
            }
        } else {
            Log.e(TAG, "Brand directory not found: " + brandDir);
        }

        // SETUP INTERPRETER OPTIONS
        Interpreter.Options options = new Interpreter.Options();
        
        // SKIP GPU FOR COMPATIBILITY AND USE CPU WITH XNNPACK
        Log.d(TAG, "Using CPU with XNNPACK acceleration");
        options.setUseXNNPACK(true);
        options.setNumThreads(4);

        try {
            MappedByteBuffer modelData = loadModelFile(context.getAssets(), modelPath);
            Log.d(TAG, "Model data loaded, size: " + modelData.capacity() + " bytes");
            interpreter = new Interpreter(modelData, options);
            
            // LOG TENSOR INFO FOR DEBUGGING
            int[] inputShape = interpreter.getInputTensor(0).shape();
            int[] outputShape = interpreter.getOutputTensor(0).shape();
            Log.d(TAG, "Input tensor shape: [" + inputShape[0] + ", " + inputShape[1] + ", " 
                  + inputShape[2] + ", " + inputShape[3] + "]");
            Log.d(TAG, "Output tensor shape: [" + outputShape[0] + ", " + outputShape[1] + "]");
            
            numClasses = outputShape[1]; // GET NUMBER OF CLASSES FROM OUTPUT TENSOR
            Log.d(TAG, "Number of classes detected: " + numClasses);
            
        } catch (IOException e) {
            Log.e(TAG, "Error loading model: " + e.getMessage(), e);
            throw e;
        }
    }

    private MappedByteBuffer loadModelFile(AssetManager assetManager, String modelPath) throws IOException {
        try {
            AssetFileDescriptor fileDescriptor = assetManager.openFd(modelPath);
            FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
            FileChannel fileChannel = inputStream.getChannel();
            long startOffset = fileDescriptor.getStartOffset();
            long declaredLength = fileDescriptor.getDeclaredLength();
            Log.d(TAG, "Loading model from " + modelPath + ", offset: " + startOffset + ", length: " + declaredLength);
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
        } catch (IOException e) {
            Log.e(TAG, "Failed to load model file: " + modelPath, e);
            throw e;
        }
    }

    private void loadLabels() throws IOException {
        Log.d(TAG, "Loading labels from: " + labelPath);
        labels = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open(labelPath)));
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.length() > 0) {
                labels.add(line);
                Log.d(TAG, "Loaded label: " + line);
            }
        }
        reader.close();
        
        // ENSURE LABELS COUNT MATCHES MODEL OUTPUT
        if (numClasses > 0 && labels.size() != numClasses) {
            Log.w(TAG, "Warning: Number of labels (" + labels.size() + 
                 ") doesn't match model output classes (" + numClasses + ")");
        }
    }
    
    // METHOD TO CLASSIFY AN IMAGE FROM FILE PATH
    public String classifyImage(String imagePath) throws IOException {
        Log.d(TAG, "Loading image from: " + imagePath);
        File imgFile = new File(imagePath);
        if (!imgFile.exists()) {
            throw new IOException("Image file does not exist: " + imagePath);
        }
        
        // LOAD AND ROTATE THE IMAGE ACCORDING TO EXIF
        Bitmap bitmap = loadAndRotateImage(imagePath);
        if (bitmap == null) {
            throw new IOException("Failed to decode image: " + imagePath);
        }
        
        // CALL THE EXISTING CLASSIFY METHOD WITH THE BITMAP
        String result = classify(bitmap);
        
        // CLEAN UP BITMAP
        bitmap.recycle();
        
        return result;
    }
    
    // HELPER METHOD TO LOAD AND ROTATE IMAGE BASED ON EXIF DATA
    private Bitmap loadAndRotateImage(String imagePath) {
        try {
            // FIRST CHECK EXIF ORIENTATION
            ExifInterface exif = new ExifInterface(imagePath);
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
                    
            // GET ROTATION ANGLE
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
            
            // FIRST DECODE BOUNDS TO CHECK DIMENSIONS
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(imagePath, options);
            
            // CALCULATE INSAMPLESIZE TO AVOID LOADING TOO LARGE IMAGES
            options.inSampleSize = calculateInSampleSize(options, 1024, 1024);
            
            // DECODE BITMAP WITH INSAMPLESIZE SET
            options.inJustDecodeBounds = false;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath, options);
            
            if (bitmap == null) {
                Log.e(TAG, "Failed to decode bitmap from file: " + imagePath);
                return null;
            }
            
            // IF WE NEED TO ROTATE THE IMAGE
            if (rotationAngle != 0) {
                Matrix matrix = new Matrix();
                matrix.postRotate(rotationAngle);
                
                Bitmap rotatedBitmap = Bitmap.createBitmap(
                    bitmap, 
                    0, 0, 
                    bitmap.getWidth(), 
                    bitmap.getHeight(), 
                    matrix, 
                    true
                );
                
                // RECYCLE ORIGINAL BITMAP TO FREE MEMORY
                if (bitmap != rotatedBitmap) {
                    bitmap.recycle();
                }
                
                return rotatedBitmap;
            }
            
            return bitmap;
        } catch (Exception e) {
            Log.e(TAG, "Error loading/rotating image: " + e.getMessage(), e);
            return null;
        }
    }
    
    // CALCULATE SAMPLE SIZE FOR EFFICIENT BITMAP LOADING
    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // RAW HEIGHT AND WIDTH OF IMAGE
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        
        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            
            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight 
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        
        return inSampleSize;
    }

    public String classify(Bitmap bitmap) {
        try {
            if (bitmap == null) {
                Log.e(TAG, "Bitmap is null");
                return "Error: No image provided";
            }
            
            if (interpreter == null) {
                Log.e(TAG, "Interpreter is null");
                return "Error: Model not loaded";
            }

            // Resize bitmap to match model input while preserving aspect ratio
            Bitmap resizedBitmap = preserveAspectRatio(bitmap, IMAGE_SIZE);
            
            // Get input shape from the model to ensure correct buffer size
            int[] inputShape = interpreter.getInputTensor(0).shape();
            int batchSize = inputShape[0];
            int inputHeight = inputShape[1];
            int inputWidth = inputShape[2];
            int channels = inputShape[3];
            
            Log.d(TAG, "Model expects input shape: [" + batchSize + ", " + inputHeight + ", " + 
                  inputWidth + ", " + channels + "]");
            
            // Calculate buffer size based on model input shape (4 bytes per float)
            int bufferSize = batchSize * inputHeight * inputWidth * channels * 4;
            Log.d(TAG, "Creating buffer with size: " + bufferSize + " bytes");
            
            // Prepare input data with correct size
            ByteBuffer inputBuffer = ByteBuffer.allocateDirect(bufferSize);
            inputBuffer.order(ByteOrder.nativeOrder());
            
            int[] pixels = new int[inputHeight * inputWidth];
            resizedBitmap.getPixels(pixels, 0, inputWidth, 0, 0, inputWidth, inputHeight);
            
            // Calculate image statistics for normalization
            float totalR = 0, totalG = 0, totalB = 0;
            for (int pixel : pixels) {
                totalR += ((pixel >> 16) & 0xFF);
                totalG += ((pixel >> 8) & 0xFF);
                totalB += (pixel & 0xFF);
            }
            
            int pixelCount = pixels.length;
            float avgR = totalR / pixelCount;
            float avgG = totalG / pixelCount;
            float avgB = totalB / pixelCount;
            
            // Calculate standard deviation for better normalization
            float varR = 0, varG = 0, varB = 0;
            for (int pixel : pixels) {
                float r = ((pixel >> 16) & 0xFF);
                float g = ((pixel >> 8) & 0xFF);
                float b = (pixel & 0xFF);
                
                varR += (r - avgR) * (r - avgR);
                varG += (g - avgG) * (g - avgG);
                varB += (b - avgB) * (b - avgB);
            }
            
            float stdR = (float) Math.sqrt(varR / pixelCount);
            float stdG = (float) Math.sqrt(varG / pixelCount);
            float stdB = (float) Math.sqrt(varB / pixelCount);
            
            // Fine-tuned parameters for better normalization
            float scaleFactor = 0.2f;  // Reduced from 0.25 for less contrast
            float centralValue = 0.5f; // Keep centered around 0.5
            
            // Normalize pixel values with improved standardization
            for (int pixel : pixels) {
                // Extract RGB values
                float r = ((pixel >> 16) & 0xFF);
                float g = ((pixel >> 8) & 0xFF);
                float b = (pixel & 0xFF);
                
                // Apply standardization: (pixel - mean) / (std + epsilon)
                // Then rescale to [0,1] range for the model with fine-tuned parameters
                float epsilon = 1.0f; // Prevent division by zero
                float normalizedR = Math.min(Math.max((r - avgR) / (stdR + epsilon) * scaleFactor + centralValue, 0.0f), 1.0f);
                float normalizedG = Math.min(Math.max((g - avgG) / (stdG + epsilon) * scaleFactor + centralValue, 0.0f), 1.0f);
                float normalizedB = Math.min(Math.max((b - avgB) / (stdB + epsilon) * scaleFactor + centralValue, 0.0f), 1.0f);
                
                // TensorFlow model expects RGB inputs
                inputBuffer.putFloat(normalizedR);
                inputBuffer.putFloat(normalizedG);
                inputBuffer.putFloat(normalizedB);
            }
            
            // Recycle the resized bitmap to free memory
            if (resizedBitmap != bitmap) {
                resizedBitmap.recycle();
            }
            
            // Reset position to start
            inputBuffer.rewind();
            
            // Prepare output array
            float[][] outputBuffer = new float[1][numClasses > 0 ? numClasses : labels.size()];
            
            // Run inference
            interpreter.run(inputBuffer, outputBuffer);
            
            // Get prediction result
            int maxIndex = 0;
            float maxConfidence = 0;
            
            Log.d(TAG, "Classification results:");
            for (int i = 0; i < outputBuffer[0].length; i++) {
                float confidence = outputBuffer[0][i];
                if (i < labels.size()) {
                    Log.d(TAG, labels.get(i) + ": " + confidence);
                }
                
                if (confidence > maxConfidence) {
                    maxConfidence = confidence;
                    maxIndex = i;
                }
            }
            
            if (maxIndex >= labels.size()) {
                Log.e(TAG, "Max index out of bounds: " + maxIndex + ", labels size: " + labels.size());
                return "Error: Invalid classification result";
            }
            
            // Format result with label and confidence score
            String label = labels.get(maxIndex);
            String result = label + " (" + maxConfidence + ")";
            Log.d(TAG, "Best match: " + result);
            return result;
            
        } catch (Exception e) {
            Log.e(TAG, "Error classifying image: " + e.getMessage(), e);
            return "Error: " + e.getMessage();
        }
    }
    
    // Resize while maintaining aspect ratio
    private Bitmap preserveAspectRatio(Bitmap original, int targetSize) {
        if (original == null) return null;
        
        int width = original.getWidth();
        int height = original.getHeight();
        
        // If the image is already square with desired dimensions, return as is
        if (width == targetSize && height == targetSize) {
            return original;
        }
        
        // Create a blank square bitmap with black background
        Bitmap result = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888);
        result.eraseColor(android.graphics.Color.BLACK);
        
        // Calculate scaling factors
        float scaleFactor;
        if (width > height) {
            scaleFactor = (float) targetSize / width;
        } else {
            scaleFactor = (float) targetSize / height;
        }
        
        // Calculate new dimensions
        int scaledWidth = Math.round(width * scaleFactor);
        int scaledHeight = Math.round(height * scaleFactor);
        
        // Scale the bitmap
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(original, scaledWidth, scaledHeight, true);
        
        // Calculate offsets to center image
        int xOffset = (targetSize - scaledWidth) / 2;
        int yOffset = (targetSize - scaledHeight) / 2;
        
        // Draw the scaled bitmap onto the center of result bitmap
        Canvas canvas = new Canvas(result);
        canvas.drawBitmap(scaledBitmap, xOffset, yOffset, null);
        
        // Recycle the scaled bitmap if not the original
        if (scaledBitmap != original) {
            scaledBitmap.recycle();
        }
        
        return result;
    }

    public void close() {
        if (interpreter != null) {
            interpreter.close();
        }
        if (gpuDelegate != null) {
            gpuDelegate.close();
        }
    }
}
