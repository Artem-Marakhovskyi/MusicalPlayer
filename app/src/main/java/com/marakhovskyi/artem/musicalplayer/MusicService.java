package com.marakhovskyi.artem.musicalplayer;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

import java.io.IOException;
import java.util.List;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class MusicService extends IntentService {
    private static final String ACTION_PLAY = "com.marakhovskyi.artem.musicalplayer.action.PLAY";
    private static final String ACTION_PLAY_NO_PARAM = "com.marakhovskyi.artem.musicalplayer.action.PLAY_NOPARAM";
    private static final String ACTION_STOP = "com.marakhovskyi.artem.musicalplayer.action.STOP";
    private static final String ACTION_NEXT = "com.marakhovskyi.artem.musicalplayer.action.NEXT";
    private static final String ACTION_PREV = "com.marakhovskyi.artem.musicalplayer.action.PREV";
    private static final String ACTION_MODE = "com.marakhovskyi.artem.musicalplayer.action.MODE";
    private static final String ACTION_SEEK = "com.marakhovskyi.artem.musicalplayer.action.SEEK";
    private static final String ACTION_GET_POSITION = "com.marakhovskyi.artem.musicalplayer.action.GET_POSITION";
    private static final String ACTION_GET_PLAYING = "com.marakhovskyi.artem.musicalplayer.action.GET_PLAYING";

    private static final String PLAY_PARAM = "com.marakhovskyi.artem.musicalplayer.extra.URI";
    private static final String PLAY_PARAM_ID = "com.marakhovskyi.artem.musicalplayer.extra.ID";
    private static final String SEEK_PARAM = "com.marakhovskyi.artem.musicalplayer.extra.SEEK";

    private static MediaPlayer player = null;
    private static int trackId = -1;
    private static int mMode = -1;
    private Context me = this;

    public MusicService() {
        super("MusicService");

        if (player == null) {
            player = new MediaPlayer();
        }
        player.setOnCompletionListener(onCompletion);
        player.setOnErrorListener(onError);
    }

    private MediaPlayer.OnCompletionListener onCompletion = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp) {
            MusicService.startNext(me);
        }
    };
    private MediaPlayer.OnErrorListener onError = new MediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            return false;
        }
    };

    public static void startPlay(Context context) {
        Intent intent = new Intent(context, MusicService.class);
        intent.setAction(ACTION_PLAY_NO_PARAM);
        context.startService(intent);
    }


    public static void startPlay(Context context, String uri, int id) {
        Intent intent = new Intent(context, MusicService.class);
        intent.setAction(ACTION_PLAY);
        intent.putExtra(PLAY_PARAM, uri);
        intent.putExtra(PLAY_PARAM_ID, id);
        context.startService(intent);
    }

    public static void startStop(Context context) {
        Intent intent = new Intent(context, MusicService.class);
        intent.setAction(ACTION_STOP);
        context.startService(intent);
    }

    public static void startPrev(Context context) {
        Intent intent = new Intent(context, MusicService.class);
        intent.setAction(ACTION_PREV);
        context.startService(intent);
    }

    public static void startNext(Context context) {
        Intent intent = new Intent(context, MusicService.class);
        intent.setAction(ACTION_NEXT);
        context.startService(intent);
    }

    public static void startSeek(Context context, int millis) {
        Intent intent = new Intent(context, MusicService.class);
        intent.setAction(ACTION_SEEK);
        intent.putExtra(SEEK_PARAM, millis);
        context.startService(intent);
    }

    public static void startMode(Context context) {
        Intent intent = new Intent(context, MusicService.class);
        intent.setAction(ACTION_MODE);
        context.startService(intent);
    }

    public static void startPosition(Context context) {
        Intent intent = new Intent(context, MusicService.class);
        intent.setAction(ACTION_GET_POSITION);
        context.startService(intent);
    }


    public static void startGetPlayingState(Context context) {
        Intent intent = new Intent(context, MusicService.class);
        intent.setAction(ACTION_GET_PLAYING);
        context.startService(intent);
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_PLAY.equals(action)) {
                final Uri uri = Uri.parse(intent.getStringExtra(PLAY_PARAM));
                final int id = intent.getIntExtra(PLAY_PARAM_ID, 0);
                try {
                    handlePlay(uri, id);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (ACTION_STOP.equals(action)) {
                handleStop();
            } else if (ACTION_PREV.equals(action)) {
                handlePrev();
            } else if (ACTION_NEXT.equals(action)) {
                handleNext();
            } else if (ACTION_MODE.equals(action)) {
                handleMode();
            } else if (ACTION_SEEK.equals(action)) {
                final int seek = intent.getIntExtra(SEEK_PARAM, 0);
                handleSeek(seek);
            } else if (ACTION_PLAY_NO_PARAM.equals(action)) {
                handlePlay();
            } else if (ACTION_GET_POSITION.equals(action)) {
                sendPosition();
            } else if (ACTION_GET_PLAYING.equals(action)) {
                sendIsPlaying();
            }
        }
    }

    private void handlePlay() {
        player.reset();

        TracksManager tracksManager = new TracksManager(new DBHelper(this));
        if (trackId >= 0) {
            Track track = tracksManager.getItem(trackId);
            try {
                player.setDataSource(this, Uri.parse(track.uri));
                player.prepare();
                player.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private void handleSeek(int seek) {
        player.stop();
        player.seekTo(seek);
        player.start();

        sendPosition();
    }

    private void handleMode() {
        mMode = (mMode + 1) % 3;
        sendMode();
    }

    private void handleNext() {
        player.stop();
        player.reset();
        TracksManager tracksManager = new TracksManager(new DBHelper(this));

        List<Track> tracks = tracksManager.getItems();
        if (trackId >= 0) {
            if (mMode == 0) {
                trackId = (trackId + 1) % tracks.size();
            } else if (mMode == 2) {
                trackId += 214 % tracks.size();
            }

            try {
                player.setDataSource(this, Uri.parse(tracksManager.getItem(trackId).uri));
                player.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }
            player.start();
        }

        sendNextTrack();
    }

    private void handlePrev() {
        player.stop();
        player.reset();
        TracksManager tracksManager = new TracksManager(new DBHelper(this));

        List<Track> tracks = tracksManager.getItems();
        if (trackId >= 0) {
            if (mMode == 0) {
                if (trackId == 0) {
                    trackId = tracks.size() - 1;
                } else {
                    trackId--;
                }
            } else if (mMode == 2) {
                trackId += 214 % tracks.size();
            }

            try {
                player.setDataSource(this, Uri.parse(tracksManager.getItem(trackId).uri));
                player.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }
            player.start();
        }

        sendPreviousTrack();
    }

    private void handleStop() {
        player.stop();
    }

    private void handlePlay(Uri uri, int id) throws IOException {
        trackId = id;
        player.reset();
        player.setDataSource(this, uri);
        player.prepare();
        player.start();
    }

    private void sendPosition() {
        Intent intent = new Intent(ScrollingActivity.CURRENT_POSITION);
        intent.putExtra(ScrollingActivity.CURRENT_POSITION_PARAM, player.getCurrentPosition());
        Bundle b = new Bundle();
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void sendIsPlaying() {
        Intent intent = new Intent(ScrollingActivity.PLAYING_STATE);
        intent.putExtra(ScrollingActivity.PLAYING_STATE_PARAM, player.isPlaying());
        Bundle b = new Bundle();
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void sendNextTrack() {
        Intent intent = new Intent(ScrollingActivity.PREVIOUS_TRACK);

        TracksManager tm = new TracksManager(new DBHelper(this));

        intent.putExtra(ScrollingActivity.NEXT_TRACK_NAME, tm.getItem(trackId).displayName);
        intent.putExtra(ScrollingActivity.NEXT_TRACK_URI, tm.getItem(trackId).uri);

        Bundle b = new Bundle();
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void sendPreviousTrack() {
        Intent intent = new Intent(ScrollingActivity.PREVIOUS_TRACK);

        TracksManager tm = new TracksManager(new DBHelper(this));

        intent.putExtra(ScrollingActivity.PREVIOUS_TRACK_NAME, tm.getItem(trackId).displayName);
        intent.putExtra(ScrollingActivity.PREVIOUS_TRACK_URI, tm.getItem(trackId).uri);

        Bundle b = new Bundle();
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void sendMode() {
        Intent intent = new Intent(ScrollingActivity.MODE_STATE);
        intent.putExtra(ScrollingActivity.MODE_STATE_PARAM, mMode);
        Bundle b = new Bundle();
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}
