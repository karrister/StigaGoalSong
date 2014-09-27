package fi.karrikivela.stigagoalsong;

import android.app.Activity;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import fi.karrikivela.stigagoalsong.R;


public class GoalSongActivity extends Activity {

    private Button homeGoalSongButton;
    private Button awayGoalSongButton;
    private Button stopGoalSongButton;
    private Button settingsButton;

    private MediaPlayer mediaPlayer;

    private Boolean isHomeGoalSongPlaying;
    private Boolean isAwayGoalSongPlaying;
    private Boolean isGoalSongMuted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_goal_song);

        //Call the init function for creating media player
        myInit();
    }

    private void myInit(){
        homeGoalSongButton = (Button) findViewById(R.id.homegoalbutton);
        awayGoalSongButton = (Button) findViewById(R.id.awaygoalbutton);
        stopGoalSongButton = (Button) findViewById(R.id.stopbutton);
        settingsButton     = (Button) findViewById(R.id.settingsbutton);


        //TODO create dynamic mediaplayer without a static song!
        mediaPlayer = MediaPlayer.create(this, R.raw.shaibu_shaibu_traktor);


        isHomeGoalSongPlaying = Boolean.FALSE;
        isAwayGoalSongPlaying = Boolean.FALSE;

        isGoalSongMuted       = Boolean.FALSE;
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
                mediaPlayer.start();
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

    }


    /* Function: stopGoalSongOnClick
    *  Summary: This method is called whenever the stop goal song button is being clicked
    * */
    public void stopGoalSongOnClick(View v){
        mediaPlayer.stop();
        isHomeGoalSongPlaying = Boolean.FALSE;
        isAwayGoalSongPlaying = Boolean.FALSE;
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
