package com.nobest.andela.eplayer;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.File;
import java.util.ArrayList;

public class MediaPlayback extends AppCompatActivity {
    private ListView songs;
    private ArrayList<File> songList;
    private String[] songFiles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_playback);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        songs = (ListView) findViewById(R.id.song_list);

        songList = loadSongs(Environment.getExternalStorageDirectory());
        songFiles = new String[songList.size()];
        for (int i = 0; i < songList.size(); i++) {
            songFiles[i] = i + 1 + "  " + songList.get(i).getName();
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getApplicationContext(), R.layout.song_layout_model, R.id.song, songFiles);
        songs.setAdapter(adapter);
        songs.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Intent player = new Intent(getApplicationContext(), PlayerService.class)
                        .putExtra("position", position)
                        .putExtra("play_list", songList);
                startService(player);

                Intent player2 = new Intent(getApplicationContext(), Player.class);
                startActivity(player2);

            }
        });

    }

    public ArrayList<File> loadSongs(File path) {
        File[] files = path.listFiles();
        ArrayList<File> filesFound = new ArrayList<>();

        for (File singleFile : files) {
            if (!singleFile.getName().startsWith(".") && singleFile.getName().endsWith(".mp3")) {
                filesFound.add(singleFile);
            } else {
                if (singleFile.isDirectory() && !(singleFile.isHidden())) {
                    filesFound.addAll(loadSongs(singleFile));
                }
            }
        }
        return filesFound;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_media_playback, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
