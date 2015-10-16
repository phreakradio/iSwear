package iswear.hack.com.iswear;


import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.Firebase;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;

import static edu.cmu.pocketsphinx.SpeechRecognizerSetup.defaultSetup;



public class MainActivity extends Activity implements RecognitionListener{
    public static final String LOG_TAG = "Main Activity";


    private static final String KWS_SEARCH = "wakeup";
    private static final String ANIMAL_SEARCH = "animals";

    // Keyword we are looking for to activate menu
//    private static final String KEYPHRASE = "hello";
    private static final String KEYPHRASE = " ";

    private SpeechRecognizer recognizer;
    private HashMap<String, Integer> captions;
    private HashMap<String, Integer> wordCount;
    private Firebase firebase_words;
    private static final int RESULT_SETTINGS = 1;
    private BasePreferences settings;
    private SharedPreferences sharedPrefs;

    /**
     * Default creator
     * Prepares data for UI and creates AsyntTask
     * @param state
     */
    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        settings = new BasePreferences(this);
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        Firebase.setAndroidContext(this);
        firebase_words = new Firebase("https://iswear.firebaseio.com/").child("Count");

        captions = new HashMap<String, Integer>();
        wordCount = new HashMap<String, Integer>();

        captions.put(KWS_SEARCH, R.string.kws_caption);
        captions.put(ANIMAL_SEARCH, R.string.animals_caption);

        wordCount.put("cat",0);
        wordCount.put("dog",0);
        wordCount.put("rat", 0);

        setContentView(R.layout.main);
        ((TextView) findViewById(R.id.caption_text)).setText("Preparing the recognizer");
        ((TextView) findViewById(R.id.result_text)).setText("cat: " + wordCount.get("cat") + "\n" +
                "dog: " + wordCount.get("dog") +"\n" +
                "rat: " + wordCount.get("rat"));


        // Recognizer initialization is a time-consuming and it involves IO, so we execute it in async task
        new AsyncTask<Void, Void, Exception>() {
            @Override
            protected Exception doInBackground(Void... params) {
                try {
                    Assets assets = new Assets(MainActivity.this);
                    File assetDir = assets.syncAssets();
                    setupRecognizer(assetDir);
                } catch (IOException e) {
                    return e;
                }
                return null;
            }

            @Override
            protected void onPostExecute(Exception result) {
                if (result != null) {
                    Toast.makeText(MainActivity.this,"Failed to init recognizer " + result,Toast.LENGTH_SHORT).show();
                } else {
                    switchSearch(ANIMAL_SEARCH);
                }
            }
        }.execute();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.settings, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case R.id.menu_settings:
                Intent i = new Intent(this, UserSettingActivity.class);
                startActivityForResult(i, RESULT_SETTINGS);
                break;
        }
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);

        switch(requestCode){
            case RESULT_SETTINGS:
                break;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        recognizer.cancel();
        recognizer.shutdown();
    }

    /**
     * In partial result we get quick updates about current hypothesis. In
     * keyword spotting mode we can react here, in other modes we need to wait
     * for final result in onResult.
     *
     * THIS IS ALSO WHERE THE UI CAN BE UPDATED!!
     * @param hypothesis
     */
    @Override
    public void onPartialResult(Hypothesis hypothesis) {
        if (hypothesis != null) {
            String text = hypothesis.getHypstr().replaceAll("\\s+", "");
            if (text.equals("cat") | text.equals("dog") | text.equals("rat")) {
                //UPDATE ADAPTER HERE
//                ListAdapter adapter = new ListAdapter(this,Constants.curses_demo);
//                setListAdapter(adapter);
//                ((TextView) findViewById(R.id.result_text)).setText("cat: " + wordCount.get("cat") + "\n" +
//                        "dog: " + wordCount.get("dog") + "\n" +
//                        "rat: " + wordCount.get("rat"));

            Log.i(LOG_TAG, "I thought I heard: " + hypothesis.getHypstr());
            }
            switchSearch(ANIMAL_SEARCH);
        }
    }

    /**
     * This callback is called when we stop the recognizer.
     * @param hypothesis
     */
    @Override
    public void onResult(Hypothesis hypothesis) {
        if (hypothesis != null) {
            String text = hypothesis.getHypstr().replaceAll("\\s+", "");
            if (text.equals("cat") | text.equals("dog") | text.equals("rat")) {
//                settings.setTotalWordCount(text, settings.getTotalWordCount().get(text) + 1);
                wordCount.put(text, wordCount.get(text) + 1);
                if(sharedPrefs.getBoolean("prefSendReport",false)){
//                    firebase_words.child(text).push().setValue(settings.getTotalWordCount().get(text));
                    firebase_words.child(text).setValue(wordCount.get(text));
                }

                Log.i(LOG_TAG, "I know I heard: " + text);

                //UPDATE ADAPTER HERE
                ((TextView) findViewById(R.id.result_text)).setText("cat: " + wordCount.get("cat") + "\n" +
                        "dog: " + wordCount.get("dog") + "\n" +
                        "rat: " + wordCount.get("rat"));
            }

        }
    }


    /**
     * Function determines what to do when speech is inputted
     */
    @Override
    public void onBeginningOfSpeech() {
    }

    /**
     * We stop recognizer here to get a final result
     */
    @Override
    public void onEndOfSpeech() {
        Log.i(LOG_TAG, "Updating count");
        ((TextView) findViewById(R.id.result_text)).setText("cat: " + wordCount.get("cat") + "\n" +
                "dog: " + wordCount.get("dog") + "\n" +
                "rat: " + wordCount.get("rat"));

//        String temp_text = recognizer.getSearchName();
//
        if (!recognizer.getSearchName().equals(KWS_SEARCH))
            switchSearch(ANIMAL_SEARCH);

    }


    /**
     * Function stops recognizer and switches textview UI
     * @param searchName
     */
    private void switchSearch(String searchName) {
        recognizer.stop();

        // If we are not spotting, start listening with timeout (10000 ms or 10 seconds).
        if(searchName.equals(KWS_SEARCH))
            recognizer.startListening(searchName);
        else
            recognizer.startListening(searchName, 10000);

        String caption = getResources().getString(captions.get(searchName));
        ((TextView) findViewById(R.id.caption_text)).setText(caption);
    }

    /**
     * The recognizer can be configured to perform multiple searches
     * of different kind and switch between them.
     *
     * THE HEART OF THE APP!
     * @param assetsDir
     * @throws IOException
     */
    private void setupRecognizer(File assetsDir) throws IOException {
        recognizer = defaultSetup()
                .setAcousticModel(new File(assetsDir, "en-us-ptm"))         //Acoustic model
                .setDictionary(new File(assetsDir, "cmudict-en-us.dict"))   //English dictionary of most common words
                .setRawLogDir(assetsDir)                                    // To disable logging of raw audio comment out this call (takes a lot of space on the device)
                .setKeywordThreshold(1e-15f)                                // Threshold to tune for keyphrase to balance between false alarms and misses
//                .setBoolean("-allphone_ci", true)                           // Use context-independent phonetic search, context-dependent is too slow for mobile
                .getRecognizer();
        recognizer.addListener(this);

        // Create keyword-activation search.
        recognizer.addKeyphraseSearch(KWS_SEARCH, KEYPHRASE);

        File animalGrammar = new File(assetsDir, "animals.gram");
        recognizer.addKeywordSearch("animals", animalGrammar);

    }

    @Override
    public void onError(Exception error) {
        ((TextView) findViewById(R.id.caption_text)).setText(error.getMessage());
//        Toast.makeText(MainActivity.this, error.getMessage(),Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onTimeout() {
        switchSearch(ANIMAL_SEARCH);
        Log.i(LOG_TAG,"Updating count because of timeout");
        ((TextView) findViewById(R.id.result_text)).setText("cat: " + wordCount.get("cat") + "\n" +
                "dog: " + wordCount.get("dog") + "\n" +
                "rat: " + wordCount.get("rat"));

    }



}
