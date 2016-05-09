package com.nobest.andela.eplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
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
        SeekBar.OnSeekBarChangeListener {

    // MediaPlayer mediaPlayer;
    private ArrayList<File> songsList;
    private int position;
    private SeekBar seekBar;
    ImageButton previous, backward, play, forward, next, stop;
    ImageView albumArt;
    private TextView time_duration, elapsed_time, now_playing;
    //private Thread seekBarThread;
    private ProgressBar progressBar1;
    private boolean receiverRegistered;
    private Runnable runnable;
    private BroadcastReceiver broadcastReceiver;
    public static final String CONTROL_BUTTONS = "com.nobest.eplayer.control.buttons";
    Intent controlButtonsIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        controlButtonsIntent = new Intent(CONTROL_BUTTONS);

        time_duration = (TextView) findViewById(R.id.time_duration);
        elapsed_time = (TextView) findViewById(R.id.elapsed_time);
        now_playing = (TextView) findViewById(R.id.now_playing);
        now_playing.setSelected(true);

        seekBar = (SeekBar) findViewById(R.id.seekBar);
        previous = (ImageButton) findViewById(R.id.button_prev);
        backward = (ImageButton) findViewById(R.id.button_backward);
        play = (ImageButton) findViewById(R.id.button_play);
        forward = (ImageButton) findViewById(R.id.button_forward);
        next = (ImageButton) findViewById(R.id.button_next);
        stop = (ImageButton) findViewById(R.id.imageButton_stop);
        albumArt = (ImageView) findViewById(R.id.album_art);
        progressBar1 = (ProgressBar) findViewById(R.id.progressBar1);

        previous.setOnClickListener(this);
        backward.setOnClickListener(this);
        play.setOnClickListener(this);
        forward.setOnClickListener(this);
        next.setOnClickListener(this);
        stop.setOnClickListener(this);

        seekBar.setOnClickListener(this);
        seekBar.setOnSeekBarChangeListener(this);

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                setUpUI(intent);
                //startSeekBar(intent);
            }
        };
        registerReceiver(broadcastReceiver, new IntentFilter(PlayerService.PLAYER_SERVICE_BROADCAST));
    }

    private void setUpUI(Intent intent) {
        int duration = intent.getIntExtra("duration",0);
        seekBar.setMax(duration);
        int progress = intent.getIntExtra("current_position",0);
        seekBar.setProgress(progress);
        receiverRegistered = true;
        now_playing.setText(intent.getStringExtra("now_playing"));
        time_duration.setText(timeFormatter(duration));
        elapsed_time.setText(timeFormatter(progress));
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.imageButton_stop:
                controlButtonsIntent.putExtra("value", 10);
                seekBar.setProgress(0);
                break;
            case R.id.button_play:
                controlButtonsIntent.putExtra("value", 0);
                break;
            case R.id.button_backward:
                controlButtonsIntent.putExtra("value", -1);
                break;
            case R.id.button_forward:
                controlButtonsIntent.putExtra("value", 1);
                break;
            case R.id.button_next:
                controlButtonsIntent.putExtra("value", 2);
                break;
            case R.id.button_prev:
                controlButtonsIntent.putExtra("value", -2);
                break;
            default:
                break;
        }
        sendBroadcast(controlButtonsIntent);
    }


    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {}

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {}

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        int pos = seekBar.getProgress();
        controlButtonsIntent.putExtra("value", 100);
        controlButtonsIntent.putExtra("pos", pos);
        sendBroadcast(controlButtonsIntent);
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

        if (receiverRegistered){
            try {
                unregisterReceiver(broadcastReceiver);
                receiverRegistered = false;
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        unregisterReceiver(broadcastReceiver);
        super.onPause();
    }

    @Override
    protected  void onResume(){
        super.onResume();
        registerReceiver(broadcastReceiver, new IntentFilter(PlayerService.PLAYER_SERVICE_BROADCAST));
    }
}