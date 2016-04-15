package com.nobest.andela.eplayer;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class Player extends AppCompatActivity implements View.OnClickListener,
        SeekBar.OnSeekBarChangeListener, MediaPlayer.OnCompletionListener {

    static MediaPlayer mediaPlayer;
    private ArrayList<File> songsList;
    private int position;
    private SeekBar seekBar;
    ImageButton previous, backward, play, forward, next, stop;
    ImageView albumArt;
    private TextView time_duration, elapsed_time, now_playing;
    private Runnable runnable;
    private Handler handler;
    //private Thread seekBarThread;
    private ProgressBar progressBar1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        songsList = (ArrayList) bundle.getParcelableArrayList("play_list");
        position = bundle.getInt("position");

        time_duration = (TextView) findViewById(R.id.time_duration);
        elapsed_time = (TextView) findViewById(R.id.elapsed_time);
        now_playing = (TextView) findViewById(R.id.now_playing);

        seekBar = (SeekBar) findViewById(R.id.seekBar);
        previous = (ImageButton) findViewById(R.id.button_prev);
        backward = (ImageButton) findViewById(R.id.button_backward);
        play = (ImageButton) findViewById(R.id.button_play);
        forward = (ImageButton) findViewById(R.id.button_forward);
        next = (ImageButton) findViewById(R.id.button_next);
        stop = (ImageButton) findViewById(R.id.imageButton_stop);
        albumArt = (ImageView) findViewById(R.id.album_art);
        progressBar1 = (ProgressBar) findViewById(R.id.progressBar1);

        //play.setBackgroundResource(R.drawable.pause);
        //play.setImageResource(R.drawable.pause);

        previous.setOnClickListener(this);
        backward.setOnClickListener(this);
        play.setOnClickListener(this);
        forward.setOnClickListener(this);
        next.setOnClickListener(this);
        stop.setOnClickListener(this);

        stopMediaPlayer();
        createMediaPlayer(position).start();

        seekBar.setOnClickListener(this);
        seekBar.setOnSeekBarChangeListener(this);

        startSeekBar().run();
    }

    private Runnable startSeekBar() {
        handler = new android.os.Handler();
        //if (runnable == null) {
        runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    mediaPlayer.setOnCompletionListener(Player.this);
                    int currentPosition = mediaPlayer.getCurrentPosition();
                    seekBar.setProgress(currentPosition);
                    progressBar1.setProgress(currentPosition);
                    elapsed_time.setText(timeFormatter(currentPosition));
                    int duration = mediaPlayer.getDuration();
                    time_duration.setText(timeFormatter(duration));
                    seekBar.setMax(duration);
                    progressBar1.setMax(duration);
                    handler.postDelayed(runnable, 0);
                }catch (IllegalStateException ise){
                    ise.printStackTrace();
                }
            }
        };
        // }
        return runnable;
    }

    private MediaPlayer createMediaPlayer(int position) {
        Uri uri = Uri.parse(songsList.get(position).toString());
        String title = songsList.get(position).toString().substring(songsList.get(position).toString().lastIndexOf("/") + 1);
        now_playing.setText(title);
//        startSeekBar().run();
        return mediaPlayer = MediaPlayer.create(getApplicationContext(), uri);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.imageButton_stop:
                stopMediaPlayer();
                createMediaPlayer(position);
                handler.removeCallbacks(runnable);
                break;
            case R.id.button_play:
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    handler.removeCallbacks(runnable);
                } else {
                    mediaPlayer.start();
                    startSeekBar().run();
                }
                break;
            case R.id.button_backward:
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.seekTo(mediaPlayer.getCurrentPosition() - 5000);
                }
                break;
            case R.id.button_forward:
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.seekTo(mediaPlayer.getCurrentPosition() + 5000);
                }
                break;
            case R.id.button_next:
                playNext();
                break;
            case R.id.button_prev:
                playPrevious();
                break;
            default:
                break;
        }
    }

    private void playPrevious() {
        stopMediaPlayer();
        position = (position == 0) ? songsList.size() - 1 : position - 1;
        createMediaPlayer(position).start();
        Log.d("semiu", position + "");
        startSeekBar().run();
    }

    public void playNext() {
        stopMediaPlayer();
        position = ((position + 1) % songsList.size());
        createMediaPlayer(position).start();
        Log.d("semiu", position + "");
        startSeekBar().run();
    }

    private void stopMediaPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            seekBar.setProgress(0);
            mediaPlayer.release();
            handler.removeCallbacks(runnable);
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            mediaPlayer.seekTo(seekBar.getProgress());
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        mediaPlayer.seekTo(seekBar.getProgress());
    }

    private String timeFormatter(long milliseconds) {
        String result = String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(milliseconds),
                TimeUnit.MILLISECONDS.toMinutes(milliseconds) % TimeUnit.HOURS.toMinutes(1),
                TimeUnit.MILLISECONDS.toSeconds(milliseconds) % TimeUnit.MINUTES.toSeconds(1));

        if (result.startsWith("00:")) {
            return result.substring(3);
        }
        return result;
    }

    @Override
    protected void onDestroy() {
        stopMediaPlayer();
        mediaPlayer.release();
        super.onDestroy();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (position != songsList.size() - 1) {
            playNext();
        }
    }
}



 /*seekBarThread = new Thread(){
            @Override
            public void run() {
                int currentPosition = mediaPlayer.getCurrentPosition();
                int duration = mediaPlayer.getDuration();
                while (currentPosition <= duration){
                    try {
                        sleep(0);
                        currentPosition = mediaPlayer.getCurrentPosition();
                        seekBar.setProgress(currentPosition);
                        if (currentPosition == duration){
                            playNext();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                super.run();
            }
        };*/

//seekBarThread.start();