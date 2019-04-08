package com.marakhovskyi.artem.musicalplayer;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
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
    private static final int UPDATE_FREQUENCY = 500;
    private static final int STEP_VALUE = 4000;
    private static final int READ_EXTERNAL_STORAGE_CODE = 1251;
    private TextView selectedfile = null;
    private SeekBar seekBar = null;
    private MediaPlayer player = null;
    private ImageButton prev = null;
    private ImageButton play = null;
    private ImageButton next = null;
    private ImageView mode = null;
    private MusicalListAdapter adapter = null;
    private RecyclerView listView = null;
    private boolean isStarted = true;
    private int currentTrackId = 0;
    private boolean isMovingSeekBar = false;
    private final Handler handler = new Handler();
    private FloatingActionButton fab;
    private TracksManager tracksManager;

    private final Runnable updatePositinRunnable = new Runnable() {
        @Override
        public void run() {
            updatePosition();
        }
    };
    private Toolbar toolbar;

    private View.OnClickListener mTrackClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            currentTrackId = (int) v.getTag();
            startPlay(currentTrackId);
        }
    };
    private List<Track> tracks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
        player = new MediaPlayer();
        player.setOnCompletionListener(onCompletion);
        player.setOnErrorListener(onError);
        seekBar.setOnSeekBarChangeListener(seekBarChanged);
        tracksManager = new TracksManager(new DBHelper(this));

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

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchForTracks();
            }
        });


        mode.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                int modeNumber = (int) mode.getTag();

                if (modeNumber == 1) {
                    mode.setImageResource(R.drawable.repeat_one);
                    modeNumber = 2;
                } else if (modeNumber == 2) {
                    mode.setImageResource(R.drawable.shuffle);
                    modeNumber = 3;
                } else if(modeNumber == 3) {
                    mode.setImageResource(R.drawable.repeat);
                    modeNumber = 1;
                }

                mode.setTag(modeNumber);
            }
        });
    }

    private void next() {
        stopPlay();
        int position = -1;
        List<Track> tracks = tracksManager.getItems();
        for(int i = 0; i < tracks.size(); i++) {
            if (tracks.get(i).id == currentTrackId) {
                position = i;
                break;
            }
        }
        if (position != -1) {
            int modeNum = (int) mode.getTag();
            if (modeNum == 1) {
                currentTrackId = (currentTrackId + 1) % tracks.size();
            } else if (modeNum == 3) {
                currentTrackId = (currentTrackId + 241) % tracks.size();
            }

            startPlay(currentTrackId);
        }

    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == Activity.RESULT_OK)
            switch (requestCode) {
                case FileUtils.READ_REQUEST_CODE:
                    Track track = new Track();
                    String[] proj = {MediaStore.Audio.Media._ID,
                            MediaStore.MediaColumns.DATA,
                            MediaStore.Audio.Media.TITLE,
                            MediaStore.Audio.Media.DISPLAY_NAME};

                    Cursor tempCursor = getContentResolver().query(
                            data.getData(),
                            proj,
                            null,
                            null,
                            null);

                    tempCursor.moveToFirst();
                    int col_index = -1;
                    track.uri = data.getData().toString();
                    col_index = tempCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
                    track.title = tempCursor.getString(col_index);
                    col_index = tempCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME);
                    track.displayName = tempCursor.getString(col_index);

                    tempCursor.close();

                    MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                    mmr.setDataSource(this, data.getData());
                    String duration =
                            String.valueOf(Integer.parseInt(
                                    mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)) / 1000 / 60)
                            + ":" + String.valueOf(Integer.parseInt(
                                    mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)) / 1000 % 60);

                    track.duration = duration;

                    tracksManager.upsert(track);
                    adapter.refresh();

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

        prev.setOnClickListener(OnButtonClick);
        play.setOnClickListener(OnButtonClick);
        next.setOnClickListener(OnButtonClick);
    }

    private void startPlay(int trackId) {

        Track track = tracksManager.getItem(trackId);

        selectedfile.setText(track.displayName);
        seekBar.setProgress(0);
        player.stop();
        player.reset();

        try {
            player.setDataSource(this, Uri.parse(track.uri));
            player.prepare();
            player.start();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        seekBar.setMax(player.getDuration());
        play.setImageResource(android.R.drawable.ic_media_pause);
        updatePosition();
        isStarted = true;
    }

    private void stopPlay() {
        player.stop();
        player.reset();
        play.setImageResource(android.R.drawable.ic_media_play);
        handler.removeCallbacks(updatePositinRunnable);
        seekBar.setProgress(0);
        isStarted = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updatePositinRunnable);
        player.stop();
        player.reset();
        player.release();
        player = null;
    }

    private void updatePosition() {
        handler.removeCallbacks(updatePositinRunnable);
        seekBar.setProgress(player.getCurrentPosition());
        handler.postDelayed(updatePositinRunnable, UPDATE_FREQUENCY);
    }

    private View.OnClickListener OnButtonClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.play: {
                    if (player.isPlaying()) {
                        handler.removeCallbacks(updatePositinRunnable);
                        player.pause();
                        play.setImageResource(android.R.drawable.ic_media_play);
                    } else {
                        if (isStarted) {
                            player.start();
                            play.setImageResource(android.R.drawable.ic_media_pause);
                            updatePosition();
                        } else {
                            startPlay(currentTrackId);
                        }
                    }
                    break;
                }

                case R.id.next: {
                    stopPlay();
                    next();
                    break;
                }

                case R.id.previous: {
                    int seekto = player.getCurrentPosition() - STEP_VALUE;
                    if (seekto < 0)
                        seekto = 0;
                    player.pause();
                    player.seekTo(seekto);
                    player.start();
                    break;
                }
            }
        }
    };
    private MediaPlayer.OnCompletionListener onCompletion = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp) {
            next();
        }
    };
    private MediaPlayer.OnErrorListener onError = new MediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            return false;
        }
    };

    private SeekBar.OnSeekBarChangeListener seekBarChanged =
            new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (isMovingSeekBar) {
                        player.seekTo(progress);
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
}
