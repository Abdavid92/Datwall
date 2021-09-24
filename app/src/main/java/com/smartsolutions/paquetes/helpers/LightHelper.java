package com.smartsolutions.paquetes.helpers;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

public class LightHelper {

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static Bitmap changeLight(@NonNull Bitmap img) {
        Bitmap bitmap = Bitmap.createBitmap(
                img.getWidth(),
                img.getHeight(),
                img.getConfig()
        );

        float light = 0.3f;

        for (int x = 0; x < img.getWidth(); x++) {
            for (int y = 0; y < img.getHeight(); y++) {

                int pixel = img.getPixel(x, y);

                Color color = Color.valueOf(pixel);
                float blue = color.blue() - light;
                float red = color.red() - light;
                float green = color.green() - light;

                bitmap.setPixel(x, y, Color.valueOf(red, green, blue, color.alpha()).toArgb());
            }
        }

        return bitmap;
    }
}
