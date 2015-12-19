package fi.karrikivela.stigagoalsong;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

class EndResults {
    public String homeTeamName;
    public String awayTeamName;
    public Integer homeTeamScore;
    public Integer awayTeamScore;
    public boolean isWinnerHomeTeam;
    public boolean isGameEndedInTie = Boolean.FALSE;
    public boolean isWonInOvertime = Boolean.FALSE;
    public long regulationLengthSeconds = 300;
}

public class GoalSongActivity extends Activity {

    public Integer TIMER_RESOLUTION = 1000;
    public Integer LAST_MINUTE_IN_SECONDS = 60;
    public Integer AMOUNT_OF_CSV_SCORETABLE_COLUMNS = 8;

    private MediaPlayer mediaPlayer;

    private Boolean isHomeGoalSongPlaying;
    private Boolean isAwayGoalSongPlaying;
    private Boolean isfaceOffSongPlaying;
    private Boolean isGoalSongMuted;
    private Boolean isGoalSongDirExist;
    private Boolean isLastMinuteAnnounced;
    private Boolean isLastMinuteAnnouncementPresent;
    private Boolean isGameOnGoing = Boolean.FALSE;

    private String scoreTableFileName = "scoreboard.csv";
    private String lastMinuteAnnouncementFileName;
    private String homeTeamGoalSongFileName;
    private String awayTeamGoalSongFileName;

    private String homeTeamName = "Home team";
    private String awayTeamName = "Away team";

    private Integer homeTeamScore;
    private Integer awayTeamScore;

    private TextView homeTeamTextView;
    private TextView awayTeamTextView;

    private TextView homeTeamScoreTextView;
    private TextView awayTeamScoreTextView;

    private String absolutePathToScoreDataDirectory;
    private String absolutePathToLastMinuteAnnouncementDirectory;
    private String absolutePathToGoalSongsDirectory;
    private String absolutePathToFaceoffSongsDirectory;

    private final String goalSongsFolderName = "goalsongs";
    private final String faceoffSongsFolderName = "faceoffsongs";
    private final String appSoundsFolderName = "appsounds";
    private final String scoresDatabaseFolderName = "scoretable";

    private List<String> faceOffSongsFileList;

    private Random randomFaceOffSongIndexGenerator;

    private Boolean regulationTimeHasEnded;
    private Boolean countDownTimerIsRunning;
    private Boolean isTimerEnabledByUser;
    private long userSettingForGamePeriod = 300 * TIMER_RESOLUTION;
    private long currentUserSettingForGamePeriod = userSettingForGamePeriod; //Used for writing down the results
    public TextView statusTextView;
    public long timeLeftInTimerMillis;
    public CountDownTimer countDownTimer;


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

        //Not supported currently as didn't work
        //isTimerEnabledByUser = sharedPref.getBoolean(SettingsActivity.TIMER_ENABLED_KEY_NAME, "");

        userSettingForGamePeriod = (Integer.parseInt(sharedPref.getString(SettingsActivity.GAME_LENGTH_KEY_NAME, "")) * SettingsActivity.USER_SETTING_BASIC_UNIT_IN_SECONDS * TIMER_RESOLUTION);

        if(isGameOnGoing == Boolean.FALSE) {
            timeLeftInTimerMillis = userSettingForGamePeriod;
        }

        Long minsLeft = ((timeLeftInTimerMillis / TIMER_RESOLUTION) / 60);
        //Set text on screen every time this CB is called
        statusTextView.setText( Long.toString( minsLeft )
                                + ":"
                                + Float.toString( ((float) timeLeftInTimerMillis / TIMER_RESOLUTION) - (float) (minsLeft * 60) )
                                );
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
        statusTextView = (TextView)findViewById(R.id.statusTextView);

        homeTeamScoreTextView = (TextView)findViewById(R.id.home_team_score);
        awayTeamScoreTextView = (TextView)findViewById(R.id.away_team_score);

        absolutePathToGoalSongsDirectory = String.valueOf(getExternalFilesDir(null)) + "/" + goalSongsFolderName;

        absolutePathToFaceoffSongsDirectory = String.valueOf(getExternalFilesDir(null)) + "/" + faceoffSongsFolderName;

        absolutePathToLastMinuteAnnouncementDirectory = String.valueOf(getExternalFilesDir(null)) + "/" + appSoundsFolderName;

        absolutePathToScoreDataDirectory = String.valueOf(getExternalFilesDir(null)) + "/" + scoresDatabaseFolderName;

        //Check if folders exist, create if needed!
        File goalSongsDir = new File(absolutePathToGoalSongsDirectory);
        File faceOffSongsDir = new File(absolutePathToFaceoffSongsDirectory);
        File lastMinAnnouncementDir = new File(absolutePathToLastMinuteAnnouncementDirectory);
        File scoreTableDir = new File(absolutePathToScoreDataDirectory);


        if(!goalSongsDir.exists()) {
            if(!goalSongsDir.mkdir()) {
                System.out.print("Warning! Could not create goal songs directory!");
            }
        }
        if(!faceOffSongsDir.exists()) {
            if(!faceOffSongsDir.mkdir()) {
                System.out.print("Warning! Could not create face off songs directory!");
            }
        }
        if(!lastMinAnnouncementDir.exists()) {
            if(!lastMinAnnouncementDir.mkdir()) {
                System.out.print("Warning! Could not create last minute announcement directory!");
            }
        }
        if(!scoreTableDir.exists()) {
            if(!scoreTableDir.mkdir()) {
                System.out.print("Warning! Could not create the score tables directory!");
            }
        }

        //Try to also create the score table file
        File scoreTableFile = new File(absolutePathToScoreDataDirectory, scoreTableFileName);

        if(!scoreTableFile.exists()) {
            try {
                if (!scoreTableFile.createNewFile()) {
                    System.out.print("Warning! Could not create the score tables file!");
                }

                writeScoreTableLine(scoreTableFile);

            } catch (Exception e) {
                System.out.print("Warning! Could not create the score tables file!");
            }
        }

        //Init face-off songs list
        faceOffSongsFileList = new ArrayList<String>();

        //Init the random generator for generating randomly the face-off song index in the array
        randomFaceOffSongIndexGenerator = new Random();

        //Create MediaPlayer for playing the music
        mediaPlayer = new MediaPlayer();

        isHomeGoalSongPlaying = Boolean.FALSE;
        isAwayGoalSongPlaying = Boolean.FALSE;
        isfaceOffSongPlaying = Boolean.FALSE;

        isGoalSongMuted       = Boolean.FALSE;

        //Timer:
        isTimerEnabledByUser = Boolean.TRUE;

        timeLeftInTimerMillis = userSettingForGamePeriod;

        Long minsLeft = ((timeLeftInTimerMillis / TIMER_RESOLUTION) / 60);
        //Set text on screen every time this CB is called
        statusTextView.setText( Long.toString( minsLeft )
                                + ":"
                                + Float.toString( ((float) timeLeftInTimerMillis / TIMER_RESOLUTION) - (float) (minsLeft * 60) )
                                );

        regulationTimeHasEnded = Boolean.FALSE;
        countDownTimerIsRunning = Boolean.FALSE;

        homeTeamScore = 0;
        homeTeamScoreTextView.setText(Integer.toString(homeTeamScore));
        awayTeamScore = 0;
        awayTeamScoreTextView.setText(Integer.toString(awayTeamScore));

        prepareFaceOffSongs();

        prepareLastMinuteAnnouncement();

        isLastMinuteAnnounced = Boolean.FALSE;
    }

    private void prepareLastMinuteAnnouncement(){

        isLastMinuteAnnouncementPresent = Boolean.FALSE;

        try {
            //Get list of face-off songs
            File f = new File(absolutePathToLastMinuteAnnouncementDirectory);
            File file_listing[] = f.listFiles();

            //Just take the first valid file there is
            for(int i = 0; i < file_listing.length; i++) {
                if (file_listing[i].isFile()) {
                    lastMinuteAnnouncementFileName = file_listing[i].getName();
                    isLastMinuteAnnouncementPresent = Boolean.TRUE;
                    break;
                }
            }

        } catch(Exception e) {
            e.toString();
            statusTextView.setText("No Goal song directory exists! Please create one under:");

            isLastMinuteAnnouncementPresent = Boolean.FALSE;
        }
    }

    private void prepareFaceOffSongs(){

        try {
            //Get list of face-off songs
            File f = new File(absolutePathToFaceoffSongsDirectory);
            File file_listing[] = f.listFiles();

            //Always clear whatever entries there were before
            faceOffSongsFileList.clear();

            for (int x = 0; x < file_listing.length; x++) {
                if (file_listing[x].isFile()) {
                    faceOffSongsFileList.add(file_listing[x].getName());
                }
            }

            isGoalSongDirExist = Boolean.TRUE;

        } catch(Exception e) {
            e.toString();
            statusTextView.setText("No Goal song directory exists! Please create one under:");
            isGoalSongDirExist = Boolean.FALSE;
        }
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

        try {
            //Get settings
            getSettings();
        }
        catch (Exception e){

        }

        if(!isTimerEnabledByUser){
            statusTextView.setText("GAME ON");
        }

        prepareFaceOffSongs();

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

        stopRegulationTimer();

        homeTeamScore++;
        homeTeamScoreTextView.setText(Integer.toString(homeTeamScore));

        if(regulationTimeHasEnded == Boolean.TRUE){
            regulationTimeHasEnded = Boolean.FALSE;//Reset flag
            statusTextView.setText(homeTeamName + " WINS!");

            EndResults result = new EndResults();

            result.isWonInOvertime = Boolean.TRUE;
            result.regulationLengthSeconds = currentUserSettingForGamePeriod / TIMER_RESOLUTION / 60;
            result.homeTeamScore = homeTeamScore;
            result.awayTeamScore = awayTeamScore;
            result.homeTeamName = homeTeamName;
            result.awayTeamName = awayTeamName;
            result.isWinnerHomeTeam = Boolean.TRUE;

            registerGameEndScore(result);

            isLastMinuteAnnounced = Boolean.FALSE;
        }

        if (isAwayGoalSongPlaying == Boolean.TRUE || isHomeGoalSongPlaying == Boolean.TRUE) {
            stopMusicFromPlaying();
        }

        statusTextView.setText(homeTeamName + " SCORES!");

        prepareAndStartPlayingSong(absolutePathToGoalSongsDirectory + "/" + homeTeamGoalSongFileName);
        isHomeGoalSongPlaying = Boolean.TRUE;


    }

    /* Function: awayGoalButtonOnClick
    *  Summary: This method is called whenever the away goal button is being clicked
    * */
    public void awayGoalButtonOnClick(View v){
        //Start playing away goal song (IF home goal song is not already playing)

        //Put the running time clock to pause

        //If already playing, put to pause

        //If paused, put back to play

        stopRegulationTimer();

        awayTeamScore++;
        awayTeamScoreTextView.setText(Integer.toString(awayTeamScore));

        if(regulationTimeHasEnded == Boolean.TRUE){
            regulationTimeHasEnded = Boolean.FALSE;//Reset flag
            statusTextView.setText(awayTeamName + " WINS!");

            EndResults result = new EndResults();

            result.isWonInOvertime = Boolean.TRUE;
            result.regulationLengthSeconds = currentUserSettingForGamePeriod / TIMER_RESOLUTION / 60;
            result.homeTeamScore = homeTeamScore;
            result.awayTeamScore = awayTeamScore;
            result.homeTeamName = homeTeamName;
            result.awayTeamName = awayTeamName;
            result.isWinnerHomeTeam = Boolean.FALSE;

            registerGameEndScore(result);

            isLastMinuteAnnounced = Boolean.FALSE;
        }

        if (isAwayGoalSongPlaying == Boolean.TRUE || isHomeGoalSongPlaying == Boolean.TRUE) {
            stopMusicFromPlaying();
        }

        statusTextView.setText(awayTeamName + " SCORES!");

        prepareAndStartPlayingSong(absolutePathToGoalSongsDirectory + "/" + awayTeamGoalSongFileName);
        isAwayGoalSongPlaying = Boolean.TRUE;

    }


    /* Function: faceoffSongButtonOnClick
    *  Summary: This method is called whenever the faceoff button is being clicked
    * */
    public void faceoffSongButtonOnClick(View v){

        if(isGameOnGoing == Boolean.FALSE) {
            //Set for correct saving of period length at the end of the game
            currentUserSettingForGamePeriod = userSettingForGamePeriod;
            //Set status that game has started
            isGameOnGoing = Boolean.TRUE;
        }

        //On first click start playing face-off song
        if(!isfaceOffSongPlaying){

            if(isHomeGoalSongPlaying || isAwayGoalSongPlaying) {
                stopMusicFromPlaying();
            }

            isfaceOffSongPlaying = Boolean.TRUE;

            statusTextView.setText("FACE-OFF TIME!");

            Integer randomIndexInFaceOffSongArrayToPlayNext = randomFaceOffSongIndexGenerator.nextInt(faceOffSongsFileList.size());

            String randomlyChosenFaceOffSongFileName = faceOffSongsFileList.get(randomIndexInFaceOffSongArrayToPlayNext);

            prepareAndStartPlayingSong(absolutePathToFaceoffSongsDirectory + "/" + randomlyChosenFaceOffSongFileName);
        }

        //On second click stop song and start timer
        else{
            isfaceOffSongPlaying = Boolean.FALSE;

            statusTextView.setText("GAME ON!");
            stopMusicFromPlaying();
            startRegulationTimer();
        }
    }


    private void stopMusicFromPlaying(){
        try {
            mediaPlayer.stop();

        } catch (IllegalStateException e) {
            System.err.println("FAIL2!"); //TODO add logging
        }

        isHomeGoalSongPlaying = Boolean.FALSE;
        isAwayGoalSongPlaying = Boolean.FALSE;
        isfaceOffSongPlaying = Boolean.FALSE;

        //Prepare the mediaplayer already for the play
        try {
            mediaPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //It seems that stop doesn't do it's job afterall - seek to the beginning!
        mediaPlayer.seekTo(0);
    }


    /* Function: stopGoalSongOnClick
    *  Summary: This method is called whenever the stop goal song button is being clicked
    * */
    public void stopGoalSongOnClick(View v){
        stopMusicFromPlaying();
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

    public void writeScoreTableLine(File scoreTableFile) {
        try {
            FileWriter scoreTableWriter = new FileWriter(scoreTableFile.getAbsoluteFile(), true);
            BufferedWriter scoreTableBufferedWriter = new BufferedWriter(scoreTableWriter);

            scoreTableBufferedWriter.append("WINNER");
            scoreTableBufferedWriter.append(",");
            scoreTableBufferedWriter.append("LOSER");
            scoreTableBufferedWriter.append(",");
            scoreTableBufferedWriter.append("WINNER GOALS");
            scoreTableBufferedWriter.append(",");
            scoreTableBufferedWriter.append("LOSER GOALS");
            scoreTableBufferedWriter.append(",");
            scoreTableBufferedWriter.append("WINNER POINTS");
            scoreTableBufferedWriter.append(",");
            scoreTableBufferedWriter.append("LOSER POINTS");
            scoreTableBufferedWriter.append(",");
            scoreTableBufferedWriter.append("GAME LENGTH MINUTES");
            scoreTableBufferedWriter.append(",");
            scoreTableBufferedWriter.append("HOME TEAM NAME");

            scoreTableBufferedWriter.newLine();

            scoreTableBufferedWriter.close();
        } catch (Exception e) {

        }
    }

    public void writeScoreTableLine(File scoreTableFile,
                                    String winningTeam,
                                    String losingTeam,
                                    String winningScore,
                                    String losingScore,
                                    String winnerPoints,
                                    String loserPoints,
                                    String gameLength) {

        try {
            FileWriter scoreTableWriter = new FileWriter(scoreTableFile.getAbsoluteFile(), true);
            BufferedWriter scoreTableBufferedWriter = new BufferedWriter(scoreTableWriter);

            scoreTableBufferedWriter.append(winningTeam);
            scoreTableBufferedWriter.append(",");
            scoreTableBufferedWriter.append(losingTeam);
            scoreTableBufferedWriter.append(",");
            scoreTableBufferedWriter.append(winningScore);
            scoreTableBufferedWriter.append(",");
            scoreTableBufferedWriter.append(losingScore);
            scoreTableBufferedWriter.append(",");
            scoreTableBufferedWriter.append(winnerPoints);
            scoreTableBufferedWriter.append(",");
            scoreTableBufferedWriter.append(loserPoints);
            scoreTableBufferedWriter.append(",");
            scoreTableBufferedWriter.append(gameLength);
            scoreTableBufferedWriter.append(",");
            scoreTableBufferedWriter.append(homeTeamName);

            scoreTableBufferedWriter.newLine();

            scoreTableBufferedWriter.close();
        } catch (Exception e) {

        }
    }

    public void registerGameEndScore(EndResults results) {

        /* This function seems to be always called when game ends (sensing some sarcasm
         * towards my OWN code!!), thus it is a good place to set game end status */
        isGameOnGoing = Boolean.FALSE;

        //CSV header:
        //
        //winning team;losing team;winning score;losing score;winner points;loser points;game length minutes;home team name
        String winningTeam;
        String losingTeam;
        String winningScore;
        String losingScore;
        String winnerPoints;
        String loserPoints;
        String gameLength;
        String homeTeamName;

        if(results.isWinnerHomeTeam == Boolean.TRUE) {
            winningTeam = results.homeTeamName;
            losingTeam = results.awayTeamName;
            winningScore = results.homeTeamScore.toString();
            losingScore = results.awayTeamScore.toString();
        } else { //else winner is away team
            winningTeam = results.awayTeamName;
            losingTeam = results.homeTeamName;
            winningScore = results.awayTeamScore.toString();
            losingScore = results.homeTeamScore.toString();
        }

        if(results.isWonInOvertime == Boolean.TRUE) {
            winnerPoints = "2";
            loserPoints = "1";
        } else {
            winnerPoints = "3";
            loserPoints = "0";
        }

        gameLength = Long.toString(results.regulationLengthSeconds);
        homeTeamName = results.homeTeamName;

        File scoreTableFile = new File(absolutePathToScoreDataDirectory, scoreTableFileName);

        //Sanity check for file existence
        if(scoreTableFile.exists()) {
            writeScoreTableLine(scoreTableFile, winningTeam, losingTeam, winningScore, losingScore, winnerPoints, loserPoints, gameLength);
        }

    }


    public void settingsOnClick(View v) {

        stopRegulationTimer();


        Intent intent = new Intent(this, SettingsActivity.class);

        try {
            File f = new File(absolutePathToGoalSongsDirectory);
            File file_listing[] = f.listFiles();

            List<String> file_list_string = new ArrayList<String>();

            for (int x = 0; x < file_listing.length; x++) {
                if (file_listing[x].isFile()) {
                    file_list_string.add(file_listing[x].getName());
                }
            }

            isGoalSongDirExist = Boolean.TRUE;

            //Attach the list files found in the goal songs directory, for user to choose in the settings Activity/screen
            intent.putStringArrayListExtra("file_list_string", (ArrayList<String>) file_list_string);

            startActivity(intent);
        } catch(Exception e) {
            e.toString();

            isGoalSongDirExist = Boolean.FALSE;
        }

    }

    public void gameRegulationTimeHasEndedCB(){
        regulationTimeHasEnded = Boolean.TRUE;

        if(homeTeamScore == awayTeamScore) {
            statusTextView.setText("OVERTIME");
        }
        else {
            EndResults result = new EndResults();

            if(homeTeamScore > awayTeamScore){
                statusTextView.setText(homeTeamName + " WINS!");
                prepareAndStartPlayingSong(absolutePathToGoalSongsDirectory + "/" + homeTeamGoalSongFileName);

                result.isWinnerHomeTeam = Boolean.TRUE;
            }
            else if(awayTeamScore > homeTeamScore){
                statusTextView.setText(awayTeamName + " WINS!");
                prepareAndStartPlayingSong(absolutePathToGoalSongsDirectory + "/" + awayTeamGoalSongFileName);

                result.isWinnerHomeTeam = Boolean.FALSE;
            }
            else{
                /* This should never happen, since we always have overtime in case of a tie game. */
                statusTextView.setText("A tie game!");

                result.isGameEndedInTie = Boolean.TRUE;
            }

            result.isWonInOvertime = Boolean.FALSE;
            result.regulationLengthSeconds = currentUserSettingForGamePeriod / TIMER_RESOLUTION / 60;
            result.homeTeamScore = homeTeamScore;
            result.awayTeamScore = awayTeamScore;
            result.homeTeamName = homeTeamName;
            result.awayTeamName = awayTeamName;

            registerGameEndScore(result);

            isLastMinuteAnnounced = Boolean.FALSE;
        }
    }

    public void resetButtonOnClick(View v) {

        //Set game status to not be ongoing
        isGameOnGoing = Boolean.FALSE;

        //Stop timer
        stopRegulationTimer();

        //Stop music
        if(isHomeGoalSongPlaying || isAwayGoalSongPlaying || isfaceOffSongPlaying) {
            stopMusicFromPlaying();
        }

        //Reset score
        homeTeamScore = 0;
        homeTeamScoreTextView.setText(Integer.toString(homeTeamScore));
        awayTeamScore = 0;
        awayTeamScoreTextView.setText(Integer.toString(awayTeamScore));

        //Reset time
        timeLeftInTimerMillis = userSettingForGamePeriod;

        //Reset text field for timer
        Long minsLeft = ((timeLeftInTimerMillis / TIMER_RESOLUTION) / 60);
        //Set text on screen every time this CB is called
        statusTextView.setText( Long.toString( minsLeft )
                                + ":"
                                + Float.toString( ((float) timeLeftInTimerMillis / TIMER_RESOLUTION) - (float) (minsLeft * 60) )
                                );

        isLastMinuteAnnounced = Boolean.FALSE;

        regulationTimeHasEnded = Boolean.FALSE;
    }

    public void startRegulationTimer(){

        if(isTimerEnabledByUser) {

            if (!countDownTimerIsRunning) {
                countDownTimerIsRunning = Boolean.TRUE;

                //Create timer updating each 10 milliseconds
                countDownTimer = new CountDownTimer(timeLeftInTimerMillis, 10) {

                    public void onTick(long millisUntilFinished) {

                        timeLeftInTimerMillis = millisUntilFinished;
                        Long minsLeft = ((timeLeftInTimerMillis / TIMER_RESOLUTION) / 60);
                        //Set text on screen every time this CB is called
                        statusTextView.setText( Long.toString( minsLeft )
                                                + ":"
                                                + Float.toString( ((float) timeLeftInTimerMillis / TIMER_RESOLUTION) - (float) (minsLeft * 60) )
                                                );

                        if( (isLastMinuteAnnouncementPresent == Boolean.TRUE) &&
                            (isLastMinuteAnnounced == Boolean.FALSE)          &&
                            ((timeLeftInTimerMillis / TIMER_RESOLUTION) <= LAST_MINUTE_IN_SECONDS))
                        {

                            prepareAndStartPlayingSong(absolutePathToLastMinuteAnnouncementDirectory + "/" + lastMinuteAnnouncementFileName);

                            isLastMinuteAnnounced = Boolean.TRUE;
                        }
                    }

                    public void onFinish() {
                        gameRegulationTimeHasEndedCB();
                    }
                };

                countDownTimer.start();
            }
        }
    }

    public void stopRegulationTimer(){

        if(isTimerEnabledByUser) {

            if (countDownTimerIsRunning) {
                countDownTimerIsRunning = Boolean.FALSE;
                countDownTimer.cancel();
            }
        }
    }

    public void timerStartStopOnClick(View v) {

        if(isGameOnGoing == Boolean.FALSE) {
            //Set for correct saving of period length at the end of the game
            currentUserSettingForGamePeriod = userSettingForGamePeriod;
            //Set status that game has started
            isGameOnGoing = Boolean.TRUE;
        }

        if (countDownTimerIsRunning) {
            stopRegulationTimer();
        } else {
            startRegulationTimer();
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
