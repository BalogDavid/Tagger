package com.example.tagger;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import androidx.core.content.res.ResourcesCompat;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.WeakHashMap;


/**
 * ACTIVITATEA DE AFISARE A GRILEI CU BRANDURI PT. MAINACTIVITY
 * GESTIONEAZA ACTIVITATEA
 */

public class BrandAdapter extends BaseAdapter {

    private Context context;
    private String[] brandNames;
    private int[] brandLogos;
    private final WeakHashMap<Integer, Bitmap> drawableCache = new WeakHashMap<>();



    // LIMITA PENTRU LOGOURI LA 300p
    private static final int MAX_IMAGE_SIZE = 300;



    // BRANDURI PENTRU CARE AM MODEL
    private static final Set<String> AVAILABLE_BRANDS = new HashSet<>(Arrays.asList(
        "Nike", "Adidas", "Ralph Lauren", "Tommy Hilfiger"
    ));

    public BrandAdapter(Context context, String[] brandNames, int[] brandLogos) {
        this.context = context;
        this.brandNames = brandNames;
        this.brandLogos = brandLogos;
    }

    @Override
    public int getCount() {
        return brandNames.length;
    }

    @Override
    public Object getItem(int position) {
        return brandNames[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }
    
    @Override
    public boolean hasStableIds() {
        return true;
    }



     //VERIFICARE DACA UN BRAND ESTE DISPONIBIL
    public boolean isBrandAvailable(int position) {
        if (position >= 0 && position < brandNames.length) {
            return AVAILABLE_BRANDS.contains(brandNames[position]);
        }
        return false;
    }

    static class ViewHolder {
        ImageView brandLogo;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.grid_item, parent, false);
            holder = new ViewHolder();
            holder.brandLogo = convertView.findViewById(R.id.brandLogo);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        //OBTINE RESURSA LOGO
        final int logoResourceId = brandLogos[position];
        
        //VERIFICAM CACHE-UL
        Bitmap cachedBitmap = drawableCache.get(position);
        if (cachedBitmap != null) {
            holder.brandLogo.setImageBitmap(cachedBitmap);
        } else {
            //REDIMENSIONEAZA IMAGINEA DE LA LOGO
            try {
                Bitmap bitmap = decodeSampledBitmapFromResource(context, logoResourceId, MAX_IMAGE_SIZE, MAX_IMAGE_SIZE);
                if (bitmap != null) {
                    //ADAUGA IN CACHE
                    drawableCache.put(position, bitmap);
                    holder.brandLogo.setImageBitmap(bitmap);
                } else {
                    //FALLBACK DACA DECODAREA ESUEASA
                    holder.brandLogo.setImageResource(logoResourceId);
                }
            } catch (Exception e) {
                //FALLBACK
                holder.brandLogo.setImageResource(logoResourceId);
            }
        }
        
        //APLICA UN FILTRU DACA BRANDUL NU ARE MODEL
        if (!isBrandAvailable(position)) {
            ColorMatrix matrix = new ColorMatrix();
            matrix.setSaturation(0f);
            ColorFilter grayFilter = new ColorMatrixColorFilter(matrix);
            holder.brandLogo.setColorFilter(grayFilter);
            holder.brandLogo.setAlpha(0.5f);
        } else {
            //BRANDURILE CU ML DISPONIBIL RAMAN NORMALE
            holder.brandLogo.setColorFilter(null);
            holder.brandLogo.setAlpha(1.0f);
        }
        
        return convertView;
    }
    
    /**
     * METODA PENTRU DECODAREA EFICIENTA A RESURSELOR DE IMAGINI,
     * LIMITAND DIMENSIUNEA LA VALOAREA SPECIFICATA
     */
    private static Bitmap decodeSampledBitmapFromResource(Context context, int resId, int reqWidth, int reqHeight) {
        // PRIMA DATA, DECODIFICAM DOAR DIMENSIUNEA IMAGINII
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(context.getResources(), resId, options);
        
        // CALCULAM FACTORUL DE SCALARE
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        
        // DECODIFICAM BITMAP-UL CU FACTORUL DE SCALARE STABILIT
        options.inJustDecodeBounds = false;
        // FOLOSIM O CONFIGURATIE MAI EFICIENTA PENTRU MEMORIE
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        
        return BitmapFactory.decodeResource(context.getResources(), resId, options);
    }
    
    /**
     * CALCULEAZA FACTORUL DE ESANTIONARE PENTRU REDIMENSIONARE
     */
    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // DIMENSIUNEA ORIGINALA A IMAGINII
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        
        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            
            // CALCULAM CEL MAI MARE FACTOR INSAMPLESIZE CARE ESTE PUTERE DE 2
            // SI MENTINE AMBELE DIMENSIUNI MAI MARI SAU EGALE CU DIMENSIUNEA CERUTA

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        
        return inSampleSize;
    }
}
