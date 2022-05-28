package cs1302.gallery;

import java.net.http.HttpClient;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.Scene;
import javafx.stage.Stage;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.TilePane;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;
import javafx.geometry.Pos;
import javafx.geometry.Orientation;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Separator;
import javafx.scene.control.ToolBar;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

/**
 * Represents an iTunes Gallery App.
 */
public class GalleryApp extends Application {

    /** HTTP client. */
    public static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_2)           // uses HTTP protocol version 2 where possible
        .followRedirects(HttpClient.Redirect.NORMAL)  // always redirects, except from HTTPS to HTTP
        .build();                                     // builds and returns a HttpClient object

    /** Google {@code Gson} object for parsing JSON-formatted strings. */
    public static Gson GSON = new GsonBuilder()
        .setPrettyPrinting()                          // enable nice output when printing
        .create();                                    // builds and returns a Gson object

    private Stage stage;
    private Scene scene;
    private VBox root;
    private HBox toolBar;
    private ToolBar tools;
    private HBox imageArray;
    private HBox bottomBar;
    private Button play;
    private Label label;
    private Label search;
    private Label bottomLabel;
    private TextField tf;
    private ComboBox<String> dropDown;
    private Button getImages;
    private TilePane tile;
    private ProgressBar progressBar;
    private double progress = 0.0;
    private Separator separator;
    private ImageView[] iv = new ImageView[20];
    private Image defImg;
    private String[] urls;
    private Image tempImg;
    private Image[] tempImgArr;
    private ArrayList<String> distinctUrls;
    private double playCnt = -1.0;
    private boolean playValue = true;
    private Timeline timeline;
    private Thread t;
    private String uri = "";

    /**
     * Constructs a {@code GalleryApp} object}.
     */
    public GalleryApp() {
        this.stage = null;
        this.scene = null;
        this.root = new VBox(8);
        this.toolBar = new HBox();
        this.imageArray = new HBox();
        this.bottomBar = new HBox(25);
        this.play = new Button("Play");
        this.label = new Label("Type in a term, select a media type, then click the button.");
        this.search = new Label("Search:");
        this.bottomLabel = new Label("Images provided by iTunes Search API.");
        this.tf = new TextField("travis scott");
        this.getImages = new Button("Get Images");
        this.separator = new Separator(Orientation.VERTICAL);
        this.defImg = new Image("file:resources/default.png");
    } // GalleryApp

    /** {@inheritDoc} */
    @Override
    public void init() {
        // feel free to modify this method
        System.out.println("init() called");
        root.getChildren().addAll(toolBar, label, imageArray, bottomBar);
        toolBar.getChildren().addAll(addToolBar());
        imageArray.getChildren().addAll(addTilePane());
        imageArray.setAlignment(Pos.BASELINE_CENTER);
        bottomBar.getChildren().addAll(addProgressBar());
        bottomBar.setAlignment(Pos.BASELINE_CENTER);
        progressBar.setPrefWidth(250);
        toolBar.setAlignment(Pos.BASELINE_CENTER);
        play.setDisable(true);
    } // init

    /** {@inheritDoc} */
    @Override
    public void start(Stage stage) {
        this.stage = stage;
        this.scene = new Scene(this.root, 540, 500);
        this.stage.setOnCloseRequest(event -> Platform.exit());
        this.stage.setTitle("GalleryApp!");
        this.stage.setScene(this.scene);
        this.stage.sizeToScene();
        this.stage.show();
        Platform.runLater(() -> this.stage.setResizable(false));
    } // start

    /** {@inheritDoc} */
    @Override
    public void stop() {
        // feel free to modify this method
        System.out.println("stop() called");
        System.exit(0);
    } // stop

    /**
     * Adds the toolbar wiht the required elements.
     *
     * @return ToolBar the toolbar with the required elements.
     *
     */
    private ToolBar addToolBar() {
        // adds the tools to the toolbar themselves
        this.tools = new ToolBar(
            play,
            separator,
            search,
            tf,
            addDropDown(),
            getImages
            );
        // sets the action for the play button to the checkplay method
        play.setOnAction(e -> {
            checkPlay();
        });
        // creates a thread and whenever the getimages button is clicked it will
        // create a new thread which runs the getimages method.
        getImages.setOnAction(e -> {
            t = new Thread(() -> {
                getImages();
            });
            t.setDaemon(true);
            t.start();
        });
        // returns the toolbar.
        return tools;
    }

    /**
     * Adds a combo box when called.
     *
     * @return ComboBox the combo box to be created.
     */
    private ComboBox addDropDown() {
        // adds the required values to the combobox.
        this.dropDown = new ComboBox<>();
        // sets the initial value to music.
        dropDown.setValue("music");
        // adds the elements.
        dropDown.getItems().addAll(
            "movie",
            "podcast",
            "music",
            "musicVideo",
            "audiobook",
            "shortFilm",
            "tvShow",
            "software",
            "ebook",
            "all");
        // returns the combobox.
        return dropDown;
    } // addDropDown

    /**
     * Adds the tile pane with the default image.
     *
     * @return TilePane the tilepane with the 5x4 tiles with the default image.
     */
    private TilePane addTilePane() {
        // sets the rows and columns to the required values.
        this.tile = new TilePane();
        tile.setPrefRows(4);
        tile.setPrefColumns(5);
        for (int i = 0; i < iv.length; i++) {
            // sets the imageview to the default image.
            iv[i] = new ImageView(defImg);
            // sets the width and height to 100.
            iv[i].setFitWidth(100.0);
            iv[i].setFitHeight(100.0);
            // adds the imageview to the tile.
            tile.getChildren().addAll(iv[i]);
        }
        // returns the tilepane with the default elements.
        return tile;
    } // addTilePane

    /**
     *
     * Adds the progress bar and the bottom label.
     *
     * @return HBox the hbox with both elements.
     */
    private HBox addProgressBar() {
        // creates the progress bar.
        HBox bar = new HBox();
        this.progressBar = new ProgressBar();
        // sets the progress bar to the initial value 0.
        progressBar.setProgress(progress);
        // adds the progress bar along with the bottom label to the bottom.
        bar.getChildren().addAll(progressBar, bottomLabel);
        // creates the bottom bar.
        return bar;
    } // addProgressBar

    /**
     * Increments the progress when called by 0.05.
     */
    private void increaseProgress() {
        // increases the progress value by 0.05.
        progress += 0.05;
        progressBar.setProgress(progress);
    } // increaseProgress


    /**
     * gets the images based off the query.
     *
     */
    private void getImages() {
        //creating itunes search and setting the label change and button changes.
        String itunes = "https://itunes.apple.com/search";
        Platform.runLater(() -> {
            label.setText("Getting Images...");
            play.setText("Play");
            getImages.setDisable(true);
            play.setDisable(true);
            timeline.pause();
        });
        // resets progress.
        progress = 0.0;
        progressBar.setProgress(progress);
        try {
            // getting the response and sending the request. code from class.
            String term = URLEncoder.encode(inputParse(tf), StandardCharsets.UTF_8);
            String media = URLEncoder.encode(getComboValue(), StandardCharsets.UTF_8);
            String limit = URLEncoder.encode("200", StandardCharsets.UTF_8);
            String query = String.format("?term=%s&media=%s&limit=%s", term, media, limit);
            this.uri = itunes + query;
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .build();
            HttpResponse<String> response = HTTP_CLIENT
                .send(request, BodyHandlers.ofString());
            // ensure the request is okay
            if (response.statusCode() != 200) {
                throw new IOException(response.toString());
            } // if
            String jsonString = response.body();
            jsonString.trim();
            ItunesResponse itunesResponse = GSON.fromJson(jsonString, ItunesResponse.class);
            // creating urls array and prints the urls used and how many there are.
            urls = parseItunesResponse(itunesResponse);
            checkUrls();
            // this throws IAE which creates the alert box.
            lessThan21Error(checkUrls());
            System.out.println(uri);
            System.out.println("# of total urls found " + urls.length);
            System.out.println("# of distinct urls found " + distinctUrls.size());
            updateProgress();
            updateTiles(uri);
            // sets buttons to normal state to continue searches/play.
            getImages.setDisable(false);
            play.setDisable(false);
        } catch (IOException | InterruptedException | IllegalArgumentException e) {
            getImages.setDisable(false);
            play.setDisable(false);
            checkDefault();
            // creates and displays the exception box when called/
            Platform.runLater(() -> {
                label.setText("Last attempt to get images failed...");
                Alert alert = new Alert(Alert.AlertType.ERROR,
                    "URI: " + uri +  " \nException: " + e.toString(),
                    ButtonType.OK);
                alert.showAndWait();
                play.setText("Play");
            });
        } // try
    } // getImages

    /**
     * Checks if the default image is up. If it is then after displaying
     * a failed response the play button will still be disabled.
     *
     */
    private void checkDefault() {
        // checking if the iv value is equal to the default image.
        if (iv[0].getImage().equals(defImg)) {
            // sets the play button to not be clickable.
            play.setDisable(true);
        } // if
    } // checkDefault

    /**
     * checks and adds all distinct urls to an arraylist.
     *
     *
     * @return int the number of distinct urls.
     */
    private int checkUrls() {
        // initializes the arraylist.
        distinctUrls = new ArrayList<>();
        for (String s : urls) {
            // iterates through and checks if repeated urls are in the arraylist.
            if (!distinctUrls.contains(s)) {
                // adds distinct ones if they exist.
                distinctUrls.add(s);
            } // if
        } // for
        // return the number of distinct urls.
        return distinctUrls.size();
    } // checkUrls

    /**
     *
     * Adds all the distinct urls to a new imagearray of image objects.
     *
     */
    private void updateProgress() {
        // creates a new image array with length 20.
        tempImgArr = new Image[iv.length];
        for (int i = 0; i < iv.length; i++) {
            // iterates through and adds the first 20 distinct urls to new image objects.
            tempImgArr[i] = new Image(distinctUrls.get(i));
            // increases progress bar progress
            increaseProgress();
            // prints to console
            System.out.println(distinctUrls.get(i));
        } // for
    } // updateProgress

    /**
     * updates the tiles in the app with the new image objects.
     *
     *
     * @param uri the uri to set text to.
     */
    private void updateTiles(String uri) {
        //iterating through
        for (int i = 0; i < iv.length; i++) {
            // setting the imageview to the image in the image array at I
            iv[i].setImage(tempImgArr[i]);
            // modifiying width and height.
            iv[i].setFitWidth(100.0);
            iv[i].setFitHeight(100.0);
        } // for

        // changing the label at the top to the uri.
        Platform.runLater(() -> {
            label.setText(uri);
        });
        // allows the label to be used.
        play.setDisable(false);
    } // updateTiles

    /**
     * Parses the itunesResponse to return the artwork url.
     *
     * @param itunesResponse the response to parse.
     * @return String[] a string array with the artworkurls.
     */
    private String[] parseItunesResponse(ItunesResponse itunesResponse) {
        GSON.toJson(itunesResponse);
        String[] uris = new String[getNumberResponse(itunesResponse)];
        // # of itunes response for the given url.
        for (int i = 0; i < getNumberResponse(itunesResponse); i++) {
            ItunesResult result = itunesResponse.results[i];
            uris[i] = result.artworkUrl100;
        } // for
        //return the string array.
        return uris;
    } // parseItunesResponse

    /**
     * gets the number of responses in the itunes response.
     *
     * @param itunesResponse the response to parse.
     * @return int the number of responses.
     */
    private int getNumberResponse(ItunesResponse itunesResponse) {
        GSON.toJson(itunesResponse);
        int resultCount = itunesResponse.resultCount;
        return resultCount;
    } // getNumberResponse

    /**
     * checks if the number of responses is less than 21.
     *
     * @param response the number to check.
     * @throws IllegalArgumentException
     */
    private void lessThan21Error(int response) throws IllegalArgumentException {
        if (response < 21) {
            throw new IllegalArgumentException(checkUrls() +
            " distinct results found, but 21 or more are needed.");
        } // if
    } // lessThan21Error


    /**
     * Returns the currently selected combobox value.
     *
     * @return String the string value of the selection.
     */
    private String getComboValue() {
        String value = dropDown.getValue();
        return value;
    } // getComboValue

    /**
     *
     * parses the input based off the textfield response.
     *
     * @param textfield the textfield to use.
     * @return String the parsed user Input.
     */
    private String inputParse(TextField textfield) {
        // sets a string to the input in the textfield.
        String input = textfield.getText();
        // splits the words
        String[] userWords = input.split(" ");
        input = "";
        String userInput = "";
        for (int i = 0; i < userWords.length; i++) {
            if (i == 0) {
                // if its the first word the userInput will be the first word
                userInput = userWords[i];
            } else {
                // any index after will be the 2 words seperated by +.
                userInput = userInput + "+" + userWords[i];
            } // if - else
        } // for
        // returns the text encoded.
        return userInput;
    } // inputParse

    /**
     * randomizes the image in a random tile.
     *
     *
     */
    private void imageRandomizer() {
        EventHandler<ActionEvent> play = (e -> {
            Random random = new Random();
            // picks a random url from 21, to the end of the # of distinct urls.
            int randomUrl = random.nextInt(21, distinctUrls.size());
            // picks a random tile to change.
            int randomTile = random.nextInt(20);
            int tileDisplay = randomTile + 1;
            // setting the tile at the index to the new tile.
            iv[randomTile].setImage(new Image(distinctUrls.get(randomUrl)));
            System.out.println("url: " + distinctUrls.get(randomUrl));
            System.out.println("edited tile: " + tileDisplay);
        });
        // playing it.
        setTimeline(play);
    } // imageRandomizer

    /**
     * checks if it should be playing(randomizing).
     *
     *
     *
     */
    private void checkPlay() {
        // turns playCnt to 0.
        playCnt++;
        if (playCnt % 2 == 0.0) {
            playValue = true;
        } // if
        if (playCnt % 2 != 0.0) {
            playValue = false;
        } // if
        // changes play button and executes randomizer.
        Platform.runLater(() -> {
            if (playValue) {
                play.setText("Pause");
                timeline.play();
            } // if
            if (!playValue) {
                play.setText("Play");
                timeline.pause();
            } // if
        });
        // calls randomizer.
        if (playValue) {
            imageRandomizer();
        } // if
    } // checkPlay

    /**
     * sets the timeline and how long between randomizing the images.
     *
     * @param handler the eventhandler of type actionevent to use.
     *
     */
    private void setTimeline(EventHandler<ActionEvent> handler) {
        // creating a keyframe with the length of each swap.
        KeyFrame keyFrame = new KeyFrame(Duration.seconds(2), handler);
        // creating a new timeline.
        timeline = new Timeline();
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.getKeyFrames().add(keyFrame);
        timeline.play(); // change to RUNNING
    } // setTimeline

} // GalleryApp
