package com.example.admin.guessthecelebrity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A class for the "Guess The Celebrity game"
 */
public class MainActivity extends AppCompatActivity {

    /**
     * An object of this class downloads an HTML
     */
    public class DownLoadHtmlTask extends AsyncTask<String, Void, String>{
        @Override
        /**
         * Gets a String which is expected to be a URL, downloads it's HTML, parses it and returns
         * a String object that holds the whole HTML
         */
        protected String doInBackground(String... params) {
            String result = "";
            try {
                URL url = new URL(params[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                InputStream in = connection.getInputStream();
                InputStreamReader reader = new InputStreamReader(in);
                int data = reader.read();
                char cur;
                while (data != -1){
                    cur = (char) data;
                    result += cur;
                    data = reader.read();
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return result;

        }
    }

    /**
     * An object of this class downloads an image from a given URL
     */
    public class DownLoadImageTask extends AsyncTask<String, Void, Bitmap>{
        @Override
        /**
         * Gets a String which is expected to be a link for an image, downloads it and returns the
         * image as a "Bitmap" object
         */
        protected Bitmap doInBackground(String... params) {
            Bitmap img = null;
            try {
                URL imageUrl = new URL(params[0]);
                HttpURLConnection connection = (HttpURLConnection) imageUrl.openConnection();
                connection.connect();
                InputStream in = connection.getInputStream();
                img = BitmapFactory.decodeStream(in);
            }
            catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return img;
        }
    }

    // The website's HTML
    public String html;
    // Holds the urls for each celebrity on the website
    public ArrayList<String> urlsList = new ArrayList<>();
    // Holds the name of each celebrity on the website
    // Note: the i'th element on "urlsList" holds the url for an image of the celebrity with the
    // i'th name in "namesList", respectively
    public ArrayList<String> namesList = new ArrayList<>();
    // Tells us if we can choose the i'th name. We can choose the i'th name iff availableList[i] == true
    public ArrayList<Boolean> availableList = new ArrayList<>();
    // The current wrong celebrity names on the screen
    public int[] currentNames = {-1, -1, -1};
    public Random rand;
    // Downloads the requested image
    public DownLoadImageTask imgDownLoader;
    // The current right celebrity name on the screen
    public String currentCelebName;
    // The celebrity's index
    public int currentCelebIndex;
    // Player's score
    int score;
    // Number of rounds
    int totalRounds;

    /**
     * Gets the HTML of the website, and than calls the function "extractUrlsAndNamesFromHtml"
     * @throws ExecutionException
     * @throws InterruptedException
     */
    private void getHtml() throws ExecutionException, InterruptedException {
        DownLoadHtmlTask downLoadHtmlTask = new DownLoadHtmlTask();
        html = downLoadHtmlTask.execute("http://www.posh24.se/kandisar").get();
        extractUrlsAndNamesFromHtml();
    }

    /**
     * Parses the HTML and extracts the celebs' names and URLs for their images and puts those
     * in the ArrayLists "namesList" and "urlLists"
     */
    private void extractUrlsAndNamesFromHtml() {
        Pattern urlPattern = Pattern.compile("src=(.*?) alt=");
        Matcher urlMatcher = urlPattern.matcher(html);
        String cur;
        int counter = 0;
        // Extracts the urls
        while (urlMatcher.find()){
            cur = urlMatcher.group(1);
            cur = cur.substring(1, cur.length() - 1);
            urlsList.add(cur);
        }
        // Updates the HTML so it'll be easier to extract the names
        html = html.replaceAll("\\s+", "");
        ArrayList<String> namesWithoutSpaces = new ArrayList<>();
        Pattern namePattern = Pattern.compile("name\">(.*?)</div>");
        Matcher NameMatcher = namePattern.matcher(html);
        while (NameMatcher.find()){
            cur = NameMatcher.group(1);
            namesWithoutSpaces.add(cur);
            availableList.add(true);
        }
        // Puts a whitespace between the first name and the second name of each celeb
        namesList = fixNames(namesWithoutSpaces);
    }

    /**
     * Puts a whitespace between the first name and the second name of each celeb
     * @param namesWithoutSpaces An ArrayList object that holds the names we'd like to split to
     *                           first name and last name
     * @return The splitted celebs' names
     */
    private ArrayList<String> fixNames(ArrayList<String> namesWithoutSpaces){
        ArrayList<String> fixedList = new ArrayList<>();
        String[] nameArray;
        String firstName;
        String lastName;
        int counter = 0;
        for (String name : namesWithoutSpaces){
            nameArray = name.split("(?=\\p{Upper})");
            if (nameArray.length < 3){
                urlsList.remove(counter);
                continue;
            }
            firstName = nameArray[1];
            lastName = nameArray[2];
            fixedList.add(firstName + " " + lastName);
            counter += 1;
        }
        return fixedList;
    }

    /**
     * Randomly picks a new available celebrity, downloads it's iamge, update the relevant fields
     * and returns the celeb's index
     * @return The celeb's index
     * @throws ExecutionException
     * @throws InterruptedException
     */
    private int pickNewCelebrity() throws ExecutionException, InterruptedException {
        int celebIndex = rand.nextInt(namesList.size());
        try{
            System.out.println(namesList.get(celebIndex));
            System.out.println(urlsList.get(celebIndex));
            imgDownLoader = new DownLoadImageTask();
            Bitmap celebImg = imgDownLoader.execute(urlsList.get(celebIndex)).get();
            ImageView celebImgView = (ImageView) findViewById(R.id.celebImgView);
            celebImgView.setImageBitmap(celebImg);
        } catch (Exception e){
            return -1;
        }
        return celebIndex;
    }

    /**
     * Starts the game
     * @param view The "GO!" button
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public void startGame(View view) throws ExecutionException, InterruptedException {
        view.setVisibility(View.INVISIBLE);
        GridLayout gameGrid = (GridLayout) findViewById(R.id.answersGrid);
        gameGrid.setVisibility(View.VISIBLE);
        // Displays the four names on the screen
        for (int i = 0; i < 4; i ++){
            Button curButton = (Button) gameGrid.getChildAt(i);
            curButton.setVisibility(View.VISIBLE);
        }
        ImageView celebImg = (ImageView) findViewById(R.id.celebImgView);
        celebImg.setVisibility(View.VISIBLE);
        runGame();
    }

    /**
     * An "onClick" function for each one of the names buttons. If the clicked button holds the
     * name of the celebrity on the screen, an informaitve message appears and the score is updated.
     * Else, another message appears.
     * Anyway, after a button is clicked, another round begins
     * @param view The clicked button
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public void buttonClicked(View view) throws ExecutionException, InterruptedException {
        Button button = (Button) view;
        if (button.getText().equals(currentCelebName)){
            score += 1;
            totalRounds += 1;
            Toast.makeText(MainActivity.this, "CORRECT! Your score is: " + Integer.toString(score) + "/" + Integer.toString(totalRounds), Toast.LENGTH_LONG).show();
        }
        else{
            Toast.makeText(MainActivity.this, "WRONG! This is: " + currentCelebName, Toast.LENGTH_LONG).show();
            totalRounds += 1;
        }
        runGame();
    }

    /**
     * Marks all the celebs from the former round as available to be chosen once again
     */
    private void setOldNamesAsAvailable() {
        for (int curCelebIndex : currentNames){
            availableList.set(curCelebIndex, true);
        }
    }

    /**
     * Picks a new trio of wrong names
     */
    public void pickNewWrongAnswers(){
        int rightAnswerIndex = rand.nextInt(4);
        currentCelebName = namesList.get(currentCelebIndex);
        GridLayout answersGrid = (GridLayout) findViewById(R.id.answersGrid);
        Button rightAnswerButton = (Button) answersGrid.getChildAt(rightAnswerIndex);
        rightAnswerButton.setText(currentCelebName);
        Button curButton;
        int curNameIndex;
        int j = 0;
        for (int i = 0; i < 4; i++) {
            curNameIndex = -1;
            // We'd like to choose a new which isn't the right answer:
            if (i != rightAnswerIndex) {
                curButton = (Button) answersGrid.getChildAt(i);
                curNameIndex = rand.nextInt(namesList.size());
                while (!availableList.get(curNameIndex) && curNameIndex != currentCelebIndex) { // We'll pick celebs randomlly until finding an available one
                    curNameIndex = rand.nextInt(namesList.size());
                }
                curButton.setText(namesList.get(curNameIndex));
                availableList.set(curNameIndex, false);
                currentNames[j] = curNameIndex;
                j += 1;
            }
        }
    }

    /**
     * Runs a single round of the game
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public void runGame() throws ExecutionException, InterruptedException {
        if (totalRounds > 0) {
            setOldNamesAsAvailable();
        }
        currentCelebIndex = pickNewCelebrity();
        while (currentCelebIndex == -1){
            currentCelebIndex = pickNewCelebrity();
        }
        pickNewWrongAnswers();
    }


    @Override
    /**
     * Gets the HTML
     */
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        rand = new Random();
        try {
            getHtml();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
