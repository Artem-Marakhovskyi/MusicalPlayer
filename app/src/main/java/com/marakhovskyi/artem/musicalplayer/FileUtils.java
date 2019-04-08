package com.marakhovskyi.artem.musicalplayer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

public final class FileUtils {
    public static final int READ_REQUEST_CODE = 1528;

    public static void getFile(Activity activity) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        intent.setType("audio/*");

        activity.startActivityForResult(intent, READ_REQUEST_CODE);
    }
}
