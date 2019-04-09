package com.marakhovskyi.artem.musicalplayer;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;

public class ScrollingActivity extends AppCompatActivity {
    private static final int READ_EXTERNAL_STORAGE_CODE = 1251;
    public static final String CURRENT_POSITION = "CURRENT_POSITION";
    public static final String CURRENT_POSITION_PARAM = "CURRENT_POSITION_PARAM";
    public static final String NEXT_TRACK = "NEXT_TRACK";
    public static final String NEXT_TRACK_URI = "NEXT_TRACK_URI";
    public static final String NEXT_TRACK_NAME = "NEXT_TRACK_NAME";
    public static final String PREVIOUS_TRACK = "PREVIOUS_TRACK";
    public static final String PREVIOUS_TRACK_URI = "PREVIOUS_TRACK_PARAM";
    public static final String PREVIOUS_TRACK_NAME = "PREVIOUS_TRACK_NAME";
    public static final String PLAYING_STATE = "PLAYING_STATE";
    public static final String PLAYING_STATE_PARAM = "PLAYING_STATE_PARAM";
    public static final String MODE_STATE = "MODE_STATE";
    public static final String MODE_STATE_PARAM = "MODE_STATE_PARAM";

    private TextView selectedfile = null;
    private SeekBar seekBar = null;
    private ImageButton prev = null;
    private ImageButton play = null;
    private ImageButton next = null;
    private ImageView mode = null;
    private MusicalListAdapter adapter = null;
    private RecyclerView listView = null;
    private int currentTrackId = 0;
    private boolean isMovingSeekBar = false;
    private final Handler handler = new Handler();
    private FloatingActionButton fab;
    private TracksManager tracksManager;
    private ScrollingActivity me;
    private final Runnable updatePositinRunnable = new Runnable() {
        @Override
        public void run() {
            updatePosition();
        }
    };
    private Toolbar toolbar;
    private String currentTrackName;
    private String currentTrackUri;

    private View.OnClickListener mTrackClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int id = (int)v.getTag();
            Track t = tracksManager.getItem(id);
            MusicService.startPlay(me, t.uri, t.id);
        }
    };
    private List<Track> tracks;
    private int currentProgress;
    private boolean isPlaying;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        me = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scrolling);
        selectedfile = (TextView) findViewById(R.id.selecteditem);
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        prev = (ImageButton) findViewById(R.id.previous);
        play = (ImageButton) findViewById(R.id.play);
        next = (ImageButton) findViewById(R.id.next);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        listView = (RecyclerView) findViewById(R.id.item_list);
        fab = (FloatingActionButton) findViewById(R.id.fab);
        mode = (ImageView) findViewById(R.id.mode);
        mode.setTag(1);

        seekBar.setOnSeekBarChangeListener(seekBarChanged);
        tracksManager = new TracksManager(new DBHelper(this));

        checkPermissions();

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchForTracks();
            }
        });

        setListenerOnMode();

        subscribeToBroadcastReceiving();
    }

    private void subscribeToBroadcastReceiving() {
        LocalBroadcastManager.getInstance(this).registerReceiver(
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        final int position = intent.getIntExtra(CURRENT_POSITION_PARAM, 0);
                        currentProgress = position;
                        updatePosition();
                    }
                }
                , new IntentFilter(CURRENT_POSITION));

        LocalBroadcastManager.getInstance(this).registerReceiver(
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        isPlaying = intent.getBooleanExtra(PLAYING_STATE_PARAM, false);
                        if (!isPlaying) {
                            handler.removeCallbacks(updatePositinRunnable);
                            play.setImageResource(android.R.drawable.ic_media_play);
                            seekBar.setProgress(0);
                            updatePosition();
                        } else {
                            play.setImageResource(android.R.drawable.ic_media_pause);
                            updatePosition();
                        }
                    }
                }
                , new IntentFilter(PLAYING_STATE));

        LocalBroadcastManager.getInstance(this).registerReceiver(
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        final int modeNumber = intent.getIntExtra(MODE_STATE_PARAM, -1);
                        if (modeNumber == 0) {
                            mode.setImageResource(R.drawable.shuffle);
                        } else if (modeNumber == 1) {
                            mode.setImageResource(R.drawable.repeat_one);
                        } else if(modeNumber == 3) {
                            mode.setImageResource(R.drawable.repeat);
                        }
                    }
                }
                , new IntentFilter(MODE_STATE));

        LocalBroadcastManager.getInstance(this).registerReceiver(
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        currentTrackName = intent.getStringExtra(NEXT_TRACK_NAME);
                        selectedfile.setText(currentTrackName);
                        seekBar.setProgress(0);
                        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                        mmr.setDataSource(me, Uri.parse(intent.getStringExtra(NEXT_TRACK_URI)));
                        seekBar.setMax(Integer.parseInt(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)));
                        play.setImageResource(android.R.drawable.ic_media_pause);
                        updatePosition();
                    }
                }
                , new IntentFilter(NEXT_TRACK));

        LocalBroadcastManager.getInstance(this).registerReceiver(
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        currentTrackName = intent.getStringExtra(NEXT_TRACK_NAME);
                        selectedfile.setText(currentTrackName);
                        seekBar.setProgress(0);
                        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                        mmr.setDataSource(me, Uri.parse(intent.getStringExtra(NEXT_TRACK_URI)));
                        seekBar.setMax(Integer.parseInt(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)));
                        play.setImageResource(android.R.drawable.ic_media_pause);
                        updatePosition();
                    }
                }
                , new IntentFilter(PREVIOUS_TRACK));
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == Activity.RESULT_OK)
            switch (requestCode) {
                case FileUtils.READ_REQUEST_CODE:
                    upsertData(data.getData());
                    break;
            }
    }
    public void searchForTracks() {
        FileUtils.getFile(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        fillListViewByData();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case READ_EXTERNAL_STORAGE_CODE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    fillListViewByData();
                } else {
                    Toast.makeText(this, R.string.what_a_pity, Toast.LENGTH_LONG);
                }
                return;
            }
        }
    }

    private void fillListViewByData() {

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) return;

        tracks = tracksManager.getItems();

        adapter = new MusicalListAdapter(
                this,
                tracksManager,
                mTrackClickListener);
        listView.setAdapter(adapter);

        View.OnClickListener OnButtonClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onButtonClick(v);
            }
        };

        prev.setOnClickListener(OnButtonClick);
        play.setOnClickListener(OnButtonClick);
        next.setOnClickListener(OnButtonClick);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updatePositinRunnable);
    }

    private void updatePosition() {
        MusicService.startPosition(this);
        handler.removeCallbacks(updatePositinRunnable);
        seekBar.setProgress(currentProgress);
        handler.postDelayed(updatePositinRunnable, 500);
    }

    public void onButtonClick(View v) {
        switch (v.getId()) {
            case R.id.play: {
                MusicService.startPlay(this);
                updatePosition();
                break;
            }

            case R.id.next: {
                MusicService.startNext(this);
                updatePosition();
                break;
            }

            case R.id.previous: {
                MusicService.startPrev(this);
                updatePosition();
                break;
            }
        }
    }

    private SeekBar.OnSeekBarChangeListener seekBarChanged =
            new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (isMovingSeekBar) {
                        MusicService.startSeek(me, progress);
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    isMovingSeekBar = true;
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    isMovingSeekBar = false;
                }
            };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_scrolling, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
                Toast.makeText(this, R.string.allow_please, Toast.LENGTH_LONG);
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        READ_EXTERNAL_STORAGE_CODE);
            }
        } else {
            fillListViewByData();
        }
    }

    private void upsertData(Uri data) {
        Track track = new Track();
        String[] proj = {MediaStore.Audio.Media._ID,
                MediaStore.MediaColumns.DATA,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DISPLAY_NAME};

        Cursor tempCursor = getContentResolver().query(
                data,
                proj,
                null,
                null,
                null);

        tempCursor.moveToFirst();
        int col_index = -1;
        track.uri = data.toString();
        col_index = tempCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
        track.title = tempCursor.getString(col_index);
        col_index = tempCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME);
        track.displayName = tempCursor.getString(col_index);

        tempCursor.close();

        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(this, data);
        String duration =
                String.valueOf(Integer.parseInt(
                        mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)) / 1000 / 60)
                        + ":" + String.valueOf(Integer.parseInt(
                        mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)) / 1000 % 60);

        track.duration = duration;

        tracksManager.upsert(track);
        adapter.refresh();
    }

    private void setListenerOnMode() {
        mode.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                MusicService.startMode(me);
            }
        });
    }
}
