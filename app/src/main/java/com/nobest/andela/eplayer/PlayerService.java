package com.nobest.andela.eplayer;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;

import static android.app.PendingIntent.*;

public class PlayerService extends Service implements MediaPlayer.OnCompletionListener,
        MediaPlayer.OnSeekCompleteListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener {
    MediaPlayer mediaPlayer;
    private ArrayList songsList;
    private int position;
    private static final int NOTIFICATION_ID = 0606;
    private NotificationManager notificationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnSeekCompleteListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.reset();

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //NotificationBar notification
        showNotification();

        mediaPlayer.reset();
        Bundle bundle = intent.getExtras();
        songsList = (ArrayList) bundle.getParcelableArrayList("play_list");
        position = bundle.getInt("position");

        if (!mediaPlayer.isPlaying()) {
            loadMediaPlayer(position);
        }

        return START_STICKY;
    }

    private void loadMediaPlayer(int position) {
        try {
            mediaPlayer.reset();
            Uri uri = Uri.parse(songsList.get(position).toString());
            mediaPlayer.setDataSource(getApplicationContext(), uri);
            mediaPlayer.prepare();

        } catch (IllegalStateException ise) {
            ise.printStackTrace();
        } catch (IllegalArgumentException iae) {
            iae.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showNotification() {
        Intent musicList = new Intent(getApplicationContext(), MediaPlayback.class);
        PendingIntent pIntent = getActivity(getApplicationContext(), 0, musicList, FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder nBuilder = new NotificationCompat.Builder(getApplicationContext());
        nBuilder.setSmallIcon(R.drawable.notification_icon);
        nBuilder.setContentTitle("ePlayer");
        nBuilder.setContentText("Music Service Running");
        nBuilder.setContentIntent(pIntent);
        nBuilder.setDefaults(NotificationCompat.DEFAULT_ALL);
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
        Log.d("semiu", "Playing next song @ position: " + position);
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
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
