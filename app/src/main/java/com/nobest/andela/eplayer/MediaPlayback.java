package com.nobest.andela.eplayer;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

public class MediaPlayback extends AppCompatActivity {
    private ListView songs;
    private File files;
    private ArrayList<File> songList;
    private ArrayList<String> fileList;
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
        for (int i = 0; i < songList.size(); i++){
            songFiles[i] = i+1 + "  " + songList.get(i).getName();
        }

        //Toast.makeText(this, "The number of musics is: " + songList.size(), Toast.LENGTH_LONG).show();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getApplicationContext(), R.layout.song_layout_model,R.id.song, songFiles );
        songs.setAdapter(adapter);
        songs.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Intent player = new Intent(getApplicationContext(), Player.class)
                        .putExtra("position", position)
                        .putExtra("play_list", songList);
                startActivity(player);
            }
        });

    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void requestAccessToStorage(){
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        startActivityForResult(intent, 4232);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 4232){

        }
    }

    public ArrayList<File> loadSongs(File path) {
        File[] files = path.listFiles();
        ArrayList<File> filesFound = new ArrayList<>();

        for(File singleFile : files){
            if (singleFile.getName().endsWith(".mp3")){
                filesFound.add(singleFile);
            }
            else {
                if(singleFile.isDirectory() && !(singleFile.isHidden())){
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
