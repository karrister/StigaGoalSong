package fi.karrikivela.stigagoalsong;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import fi.karrikivela.stigagoalsong.R;


public class GoalSongActivity extends Activity {

    private MediaPlayer mediaPlayer;

    private Boolean isHomeGoalSongPlaying;
    private Boolean isAwayGoalSongPlaying;
    private Boolean isGoalSongMuted;

    private String homeTeamGoalSongFileName;
    private String awayTeamGoalSongFileName;

    private String homeTeamName = "Home team";
    private String awayTeamName = "Away team";

    private TextView homeTeamTextView;
    private TextView awayTeamTextView;

    private String absolutePathToGoalSongsDirectory;
    private String absolutePathToFaceoffSongsDirectory;

    private final String goalSongsFolderName = "goalsongs";
    private final String faceoffSongsFolderName = "faceoffsongs";

    private List<String> faceOffSongsFileList;

    private Random randomFaceOffSongIndexGenerator;



    private void getSettings(){
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        //Get team name
        homeTeamName = sharedPref.getString(SettingsActivity.HOME_TEAM_NAME_KEY_NAME, "");
        awayTeamName = sharedPref.getString(SettingsActivity.AWAY_TEAM_NAME_KEY_NAME, "");

        homeTeamTextView.setText(homeTeamName);
        awayTeamTextView.setText(awayTeamName);

        //Get goal song
        homeTeamGoalSongFileName = sharedPref.getString(SettingsActivity.HOME_GOAL_SONG_KEY_NAME, "");
        awayTeamGoalSongFileName = sharedPref.getString(SettingsActivity.AWAY_GOAL_SONG_KEY_NAME, "");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_goal_song);

        //Call the init function for creating media player
        myInit();
    }

    private void myInit(){

        homeTeamTextView = (TextView)findViewById(R.id.home_name);
        awayTeamTextView = (TextView)findViewById(R.id.away_name);

        absolutePathToGoalSongsDirectory = String.valueOf(getExternalFilesDir(null)) + "/" + goalSongsFolderName;

        absolutePathToFaceoffSongsDirectory = String.valueOf(getExternalFilesDir(null)) + "/" + faceoffSongsFolderName;

        //Init face-off songs list
        faceOffSongsFileList = new ArrayList<String>();

        //Init the random generator for generating randomly the face-off song index in the array
        randomFaceOffSongIndexGenerator = new Random();

        //Create MediaPlayer for playing the music
        mediaPlayer = new MediaPlayer();

        isHomeGoalSongPlaying = Boolean.FALSE;
        isAwayGoalSongPlaying = Boolean.FALSE;

        isGoalSongMuted       = Boolean.FALSE;
    }


    private void prepareAndStartPlayingSong(String filepath){

        if(!mediaPlayer.isPlaying()) {
            try {
                //Get back to IDLE state to be able to setDataSource
                mediaPlayer.reset();

                mediaPlayer.setDataSource(filepath);

                mediaPlayer.prepare();
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                mediaPlayer.start();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }
    }


    @Override
    public void onPause() {
        super.onPause();  // Always call the superclass method first
    }

    @Override
    public void onResume() {
        super.onResume();  // Always call the superclass method first

        //Get settings
        getSettings();


        //Get list of face-off songs
        File f = new File(absolutePathToFaceoffSongsDirectory);
        File file_listing[] = f.listFiles();

        //Always clear whatever entries there were before
        faceOffSongsFileList.clear();

        for(int x = 0 ; x < file_listing.length; x++) {
            if(file_listing[x].isFile()) {
                faceOffSongsFileList.add(file_listing[x].getName());
            }
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.goal_song, menu);
        return true;
    }

    /* Function: homeGoalButtonOnClick
    *  Summary: This method is called whenever the home goal button is being clicked
    * */
    public void homeGoalButtonOnClick(View v){
        if (isAwayGoalSongPlaying == Boolean.FALSE) {
            if (isHomeGoalSongPlaying == Boolean.FALSE) {
                prepareAndStartPlayingSong(absolutePathToGoalSongsDirectory + "/" + homeTeamGoalSongFileName);
                isHomeGoalSongPlaying = Boolean.TRUE;
            } else {
                mediaPlayer.pause();
                isHomeGoalSongPlaying = Boolean.FALSE;
            }
        }
    }

    /* Function: awayGoalButtonOnClick
    *  Summary: This method is called whenever the away goal button is being clicked
    * */
    public void awayGoalButtonOnClick(View v){
        //Start playing away goal song (IF home goal song is not already playing)

        //Put the running time clock to pause

        //If already playing, put to pause

        //If paused, put back to play

        if (isHomeGoalSongPlaying == Boolean.FALSE) {
            if (isAwayGoalSongPlaying == Boolean.FALSE) {
                prepareAndStartPlayingSong(absolutePathToGoalSongsDirectory + "/" + awayTeamGoalSongFileName);
                isAwayGoalSongPlaying = Boolean.TRUE;
            } else {
                mediaPlayer.pause();
                isAwayGoalSongPlaying = Boolean.FALSE;
            }
        }
    }


    /* Function: faceoffSongButtonOnClick
    *  Summary: This method is called whenever the faceoff button is being clicked
    * */
    public void faceoffSongButtonOnClick(View v){
        //On first click start playing face-off song

        //On second click stop song and start timer

        Integer randomIndexInFaceOffSongArrayToPlayNext = randomFaceOffSongIndexGenerator.nextInt(faceOffSongsFileList.size());

        String randomlyChosenFaceOffSongFileName = faceOffSongsFileList.get(randomIndexInFaceOffSongArrayToPlayNext);

        prepareAndStartPlayingSong(absolutePathToFaceoffSongsDirectory + "/" + randomlyChosenFaceOffSongFileName);
    }


    /* Function: stopGoalSongOnClick
    *  Summary: This method is called whenever the stop goal song button is being clicked
    * */
    public void stopGoalSongOnClick(View v){
        try {
            mediaPlayer.stop();

        } catch (IllegalStateException e) {
            System.err.println("FAIL2!"); //TODO add logging
        }

        isHomeGoalSongPlaying = Boolean.FALSE;
        isAwayGoalSongPlaying = Boolean.FALSE;

        //Prepare the mediaplayer already for the play
        try {
            mediaPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //It seems that stop doesn't do it's job afterall - seek to the beginning!
        mediaPlayer.seekTo(0);
    }


    /* Function: muteGoalSongOnClick
    *  Summary: This method is called whenever the mute goal song button is being clicked
    * */
    public void muteGoalSongOnClick(View v){
        if(isGoalSongMuted == Boolean.TRUE) {
            //Puts full volume back on
            mediaPlayer.setVolume( (float) 1.0 , (float) 1.0);
            isGoalSongMuted = Boolean.FALSE;
        }
        else {
            //Mutes volume
            mediaPlayer.setVolume( (float) 0.0 , (float) 0.0);
            isGoalSongMuted = Boolean.TRUE;
        }
    }


    public void settingsOnClick(View v) {
        Intent intent = new Intent(this, SettingsActivity.class);

        File f = new File(absolutePathToGoalSongsDirectory);
        File file_listing[] = f.listFiles();

        List<String> file_list_string = new ArrayList<String>();

        for(int x = 0 ; x < file_listing.length; x++) {
            if(file_listing[x].isFile()) {
                file_list_string.add(file_listing[x].getName());
            }
        }

        //Attach the list files found in the goal songs directory, for user to choose in the settings Activity/screen
        intent.putStringArrayListExtra("file_list_string", (ArrayList<String>) file_list_string);

        startActivity(intent);
    }


    public void timerStartOnClick(View v) {

        new CountDownTimer(30000, 1000) {

            public void onTick(long millisUntilFinished) {
                timeLeftTextField.setText((float) millisUntilFinished / 1000);
            }

            public void onFinish() {
                mTextField.setText("done!");
            }
        }.start();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
