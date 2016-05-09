package com.nobest.andela.eplayer;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v7.app.NotificationCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static android.app.PendingIntent.getActivity;

public class PlayerService extends Service implements MediaPlayer.OnCompletionListener,
        MediaPlayer.OnSeekCompleteListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener {
    public static final String PLAYER_SERVICE_BROADCAST = "com.nobest.ePlayer.music.service";
    private static final int NOTIFICATION_ID = 0606;
    MediaPlayer mediaPlayer;
    Handler handler;
    Intent mucicServiceIntent;
    int musicLength;
    int currentPosition;
    private ArrayList songsList;
    private int position;
    private NotificationManager notificationManager;
    private TelephonyManager telephonyManager;
    private PhoneStateListener phoneStateListener;
    private boolean isPausedInCall;
    private int headsetPlugedIn = 1;
    private Runnable runnable;
    private BroadcastReceiver receiver;
    private boolean musicIsStopped;
    String title;

    private BroadcastReceiver headsetMonitor = new BroadcastReceiver() {
        private boolean headsetConnected = false;

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra("state")){
                if (headsetConnected && intent.getIntExtra("state", 0) == 0){
                    headsetConnected = false;
                    headsetPlugedIn = 0;
                }else if (!headsetConnected && intent.getIntExtra("state", 0) == 1){
                    headsetConnected = true;
                    headsetPlugedIn = 1;
                }
            }
            switch (headsetPlugedIn){
                case 0:
                    headsetDisconnected();
                    break;
                case 1:
                    break;
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        mucicServiceIntent =new Intent(PLAYER_SERVICE_BROADCAST);
        handler = new Handler();

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnSeekCompleteListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.reset();

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        registerReceiver(headsetMonitor,new IntentFilter(Intent.ACTION_HEADSET_PLUG));
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateMediaPlayer(intent);
            }
        };

        registerReceiver(receiver, new IntentFilter(Player.CONTROL_BUTTONS));
    }

    private void updateMediaPlayer(Intent intent) {
        int value = intent.getIntExtra("value", 0);
        switch (value){
            case 10:
                stopSong();
                savePreference("stopped", "yes");
                savePreference("playing", title);
                musicIsStopped = true;
                break;
            case 0:
                if (musicIsStopped){
                    loadMediaPlayer(position);
                }else {

                    if (mediaPlayer.isPlaying()) {
                        pauseMedia();
                    } else {
                        playSong();
                    }
                }
                break;
            case -1:
                if (mediaPlayer.isPlaying()){
                    mediaPlayer.seekTo(mediaPlayer.getCurrentPosition() - 5000);
                }
                break;
            case 1:
                if (mediaPlayer.isPlaying()){
                    mediaPlayer.seekTo(mediaPlayer.getCurrentPosition() + 5000);
                }
                break;
            case 2:
                position = (position == songsList.size() - 1) ? 0 : position + 1;
                loadMediaPlayer(position);
                break;
            case -2:
                position = (position == 0) ? songsList.size() - 1 : position - 1;
                loadMediaPlayer(position);
                break;
            case 100:
                if (mediaPlayer.isPlaying()){
                    mediaPlayer.seekTo(intent.getIntExtra("pos",0));
                }
            default:
                break;

        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                switch (state){
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                    case TelephonyManager.CALL_STATE_RINGING:
                        if (mediaPlayer != null){
                            pauseMedia();
                            isPausedInCall = true;
                        }
                        break;
                    case TelephonyManager.CALL_STATE_IDLE:
                        if (mediaPlayer != null){
                            if (isPausedInCall){
                                isPausedInCall = false;
                                playSong();
                            }
                        }
                        break;
                }
            }
        };

        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        showNotification();

        mediaPlayer.reset();
        Bundle bundle = intent.getExtras();
        songsList = (ArrayList) bundle.getParcelableArrayList("play_list");
        position = bundle.getInt("position");

        if (!mediaPlayer.isPlaying()) {
            loadMediaPlayer(position);
        }
        musicIsStopped = false;
        initializeHandler();
        startSendingData();
        return START_STICKY;
    }

    private void initializeHandler() {
        handler.removeCallbacks(runnable);

    }

    private void pauseMedia() {
        if (mediaPlayer.isPlaying()){
            mediaPlayer.pause();
        }
    }

    private void loadMediaPlayer(int position) {
        try {
            mediaPlayer.reset();
            Uri uri = Uri.parse(songsList.get(position).toString());
            mediaPlayer.setDataSource(getApplicationContext(), uri);
            mediaPlayer.prepare();

        } catch (IllegalStateException | IllegalArgumentException | IOException e) {
            e.printStackTrace();
        }
        musicIsStopped = false;
    }

    private void showNotification() {
        Intent musicList = new Intent(getApplicationContext(), Player.class);
        PendingIntent pIntent = getActivity(getApplicationContext(), 0, musicList, FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder nBuilder = new NotificationCompat.Builder(getApplicationContext());
        nBuilder.setSmallIcon(R.drawable.notification_icon);
        nBuilder.setContentTitle("ePlayer");
        nBuilder.setContentText("Music Service Running");
        nBuilder.setContentIntent(pIntent);
        nBuilder.setDefaults(NotificationCompat.DEFAULT_VIBRATE);
        nBuilder.setOngoing(true);
        nBuilder.setAutoCancel(false);
        notificationManager.notify(NOTIFICATION_ID, nBuilder.build());
    }

    private void cancelNotification() {
        notificationManager.cancel(NOTIFICATION_ID);
    }

    public void playNext() {
        position = ((position + 1) % songsList.size());
        loadMediaPlayer(position);
        playSong();
        //Log.d("semiu", "Playing next song @ position: " + position);
    }

    private void headsetDisconnected() {
        stopSong();
        stopSelf();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (position < songsList.size() - 1) {
            playNext();
        } else {
            stopSong();
            stopSelf();
        }

    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {

    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        String message = "Error: ";
        switch (what) {
            case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                message += "Media Error Not Valid For Progressive Playback: " + extra;
                break;
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                message += "media Error Server Died: " + extra;
                break;
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                message += "Unknown Media Error: " + extra;
                break;
            default:
                message += "An error occur while trying to play the media. ";
                break;
        }
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        playSong();
    }

    private void playSong() {
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
    }

    private void stopSong() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
    }

    @Override
    public void onDestroy() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
        }
        cancelNotification();

        if (phoneStateListener != null){
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        }
        unregisterReceiver(headsetMonitor);
        unregisterReceiver(receiver);
        handler.removeCallbacks(runnable);
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private void startSendingData() {
        runnable = new Runnable() {
            @Override
            public void run() {
                sendMediaData();
                handler.postDelayed(runnable, 1000);

                if (musicIsStopped){
                    handler.postDelayed(runnable, 30000);
                    stopSelf();
                }
            }

        };

        runnable.run();
    }

    private void sendMediaData() {
        if (mediaPlayer.isPlaying()){
            musicLength = mediaPlayer.getDuration();
            currentPosition = mediaPlayer.getCurrentPosition();

            mucicServiceIntent.putExtra("duration", musicLength);
            mucicServiceIntent.putExtra("current_position", currentPosition);
            title = songsList.get(position).toString().substring(songsList.get(position).toString().lastIndexOf("/") + 1);
            mucicServiceIntent.putExtra("now_playing", title);

            sendBroadcast(mucicServiceIntent);
        }
    }

    private void savePreference(String key, String value){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value).commit();
    }
}
