// FilePathUtil.java

package com.example.splashscreen.utils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

public class FilePathUtil {
    public static String getRealPathFromURI(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
            if (cursor == null) return null;
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } catch (Exception e) {
            // Log the error if path retrieval fails
            android.util.Log.e("FilePathUtil", "Error retrieving real path from URI", e);
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}