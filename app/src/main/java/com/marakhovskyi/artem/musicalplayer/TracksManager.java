package com.marakhovskyi.artem.musicalplayer;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class TracksManager {
    private final DBHelper db;

    public TracksManager(DBHelper db) {
        this.db = db;
    }

    public List<Track> getItems() {
        Cursor cursor = db.getReadableDatabase()
                .query(
                        "tracks",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null);
//        if (cursor.getCount() == 0)
//            return new ArrayList<Track>();

        ArrayList<Track> result = new ArrayList<Track>();

        int idIdx = cursor.getColumnIndex("id");
        int pathIdx = cursor.getColumnIndex("uri");
        int durationIdx = cursor.getColumnIndex("duration");
        int titleIdx = cursor.getColumnIndex("title");
        int displayNameIdx = cursor.getColumnIndex("display_name");

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(idIdx);
                String uri = cursor.getString(pathIdx);
                String duration = cursor.getString(durationIdx);
                String title = cursor.getString(titleIdx);
                String displayName = cursor.getString(displayNameIdx);
                Track item = new Track();
                item.id = id;
                item.uri = uri;
                item.duration = duration;
                item.title = title;
                item.displayName = displayName;

                result.add(item);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();

        return result;
    }

    public void upsert(Track track) {

        ContentValues cv = new ContentValues();

        cv.put("uri", track.uri);
        cv.put("duration", track.duration);
        cv.put("title", track.title);
        cv.put("display_name", track.displayName);

        if (track.id == 0) {
            db.getWritableDatabase().insert("tracks", null, cv);
        } else {
            cv.put("id", track.id );
            db.getWritableDatabase().update(
                    "tracks",
                    cv,
                    "id = "+String.valueOf(track.id),
                    null);
        }
    }

    public Track getItem(int id) {
        Cursor cursor = db.getReadableDatabase().rawQuery(
                "select id, uri, duration, title, display_name" +
                        " from tracks where id="+String.valueOf(id), null);

        int pathIdx = cursor.getColumnIndex("uri");
        int durationIdx = cursor.getColumnIndex("duration");
        int titleIdx = cursor.getColumnIndex("title");
        int displayNameIdx = cursor.getColumnIndex("display_name");

        if (cursor.moveToFirst()) {

            Track track = new Track();
            track.uri = cursor.getString(pathIdx);
            track.duration = cursor.getString(durationIdx);
            track.title = cursor.getString(titleIdx);
            track.displayName = cursor.getString(displayNameIdx);
            track.id = id;

            return track;
        }

        return null;
    }

}
