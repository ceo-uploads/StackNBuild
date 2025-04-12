package org.openjfx;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.media.*;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import javafx.collections.*;
import javafx.event.ActionEvent;

import java.io.File;
import java.util.Collections;
import java.util.Map;

public class MusicPlayerController {

    @FXML private ListView<File> musicList;
    @FXML private ImageView albumImage;
    @FXML private Label songTitle, songDetails, currentTime, totalTime;
    @FXML private Slider seekSlider, volumeSlider;
    @FXML private Button playPauseBtn, forwardBtn, backwardBtn, selectBtn, playPreviousBtn, playNextBtn, exitBtn;
    @FXML private CheckBox loopCheckbox, shuffle, loopSongCheckbox;

    private MediaPlayer mediaPlayer;
    private Media media;
    private ObservableList<File> songs = FXCollections.observableArrayList();
    private int currentIndex = 0;
    private RotateTransition rotate;

    @FXML
    public void initialize() {
        File downloadsDir = new File(System.getProperty("user.home"), "Downloads");
        System.out.println("Loading songs from: " + downloadsDir.getAbsolutePath());
        loadSongsFromDirectory(downloadsDir);

        setupAlbumAnimation();
        setupListViewCellFactory();

        volumeSlider.setValue(0.5);
        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (mediaPlayer != null) mediaPlayer.setVolume(newVal.doubleValue());
        });

        musicList.getSelectionModel().selectedIndexProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.intValue() >= 0 && newVal.intValue() < songs.size()) {
                currentIndex = newVal.intValue();
                playMusic(songs.get(currentIndex));
            }
        });

        selectBtn.setOnAction(e -> openFileOrFolderChooser());
        playPauseBtn.setOnAction(e -> togglePlayPause());
        //stopBtn.setOnAction(e -> stopMusic());
        forwardBtn.setOnAction(e -> seek(10));
        backwardBtn.setOnAction(e -> seek(-10));
        loopCheckbox.setOnAction(e -> setLooping());
        loopSongCheckbox.setOnAction(e -> toggleLooping());
        shuffle.setOnAction(e -> shuffleMusic());
        playPreviousBtn.setOnAction(e -> playPreviousSong());
        playNextBtn.setOnAction(e -> playNextSong());
        exitBtn.setOnAction(e -> handleExitButtonAction());

        // Auto-select and play the first song
        if (!songs.isEmpty()) {
            musicList.getSelectionModel().select(0);
        }    
    }

    private void openFileOrFolderChooser() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select MP3 File or Folder");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("MP3 Files", "*.mp3"));

        File defaultDir = new File(System.getProperty("user.home") + "/Downloads");
        if (defaultDir.exists()) {
            fileChooser.setInitialDirectory(defaultDir);
        }

        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            File selectedDir = selectedFile.getParentFile();
            loadSongsFromDirectory(selectedDir);
            if (!songs.isEmpty()) {
                musicList.getSelectionModel().select(0);
            }
        }
    }

    private void loadSongsFromDirectory(File dir) {
        if (dir != null && dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".mp3"));
            if (files != null && files.length > 0) {
                songs.setAll(files);
                musicList.setItems(songs);
            } else {
                songs.clear();
                musicList.getItems().clear();
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "No MP3 files found in the selected directory.");
                alert.showAndWait();
            }
        } else {
            System.err.println("Directory not found or invalid.");
        }
    }

    private void setupListViewCellFactory() {
        musicList.setCellFactory(listView -> new ListCell<>() {
            private final Label label = new Label();
            private final StackPane stackPane = new StackPane(label);
            private Timeline marquee;

            {
                label.setStyle("-fx-font-size: 14px; -fx-text-fill: black;");
                label.setMaxWidth(Double.MAX_VALUE);
                label.setMinWidth(Region.USE_PREF_SIZE);
                setGraphic(stackPane);
            }

            @Override
            protected void updateItem(File item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    label.setText(null);
                    setTooltip(null);
                    stopMarquee();
                    setStyle("-fx-text-fill: white");
                } else {
                    label.setText(item.getName());
                    setTooltip(new Tooltip(item.getName()));

                    if (isSelected()) {
                        playMarquee();
                        setStyle("-fx-background-color: #d0f0ff; -fx-text-fill: white");
                    } else {
                        stopMarquee();
                        label.setTranslateX(0);
                        setStyle("-fx-text-fill: white");
                    }
                }
            }

            @Override
            public void updateSelected(boolean selected) {
                super.updateSelected(selected);
                if (selected) {
                    playMarquee();
                    setStyle("-fx-background-color:rgba(47, 146, 191, 0.61); -fx-text-fill: white");
                } else {
                    stopMarquee();
                    label.setTranslateX(0);
                    setStyle("-fx-text-fill: white");
                }
            }

            private void playMarquee() {
                stopMarquee();
                double textWidth = label.getLayoutBounds().getWidth();
                double cellWidth = musicList.getWidth() - 20;

                if (textWidth > cellWidth) {
                    marquee = new Timeline(
                        new KeyFrame(Duration.ZERO, new KeyValue(label.translateXProperty(), 0)),
                        new KeyFrame(Duration.seconds(5), new KeyValue(label.translateXProperty(), -(textWidth - cellWidth)))
                    );
                    marquee.setCycleCount(Animation.INDEFINITE);
                    marquee.setAutoReverse(true);
                    marquee.play();
                }
            }

            private void stopMarquee() {
                if (marquee != null) {
                    marquee.stop();
                    marquee = null;
                }
            }
        });
    }

    private void playMusic(File file) {
        if (mediaPlayer != null) mediaPlayer.stop();
        if (file == null) return;

        media = new Media(file.toURI().toString());
        mediaPlayer = new MediaPlayer(media);
        mediaPlayer.setVolume(volumeSlider.getValue());
        bindUIControls();
        updateMetadata();
        playPauseBtn.setText("⏸");
        rotate.play();
        mediaPlayer.play();
        musicList.getSelectionModel().select(currentIndex);
    }

    private void bindUIControls() {
        mediaPlayer.currentTimeProperty().addListener((obs, old, current) -> {
            double progress = seekSlider.getValue() / seekSlider.getMax(); // Get the progress ratio (0 to 1)
            seekSlider.setValue(current.toSeconds());
            currentTime.setText(formatTime(current));
            seekSlider.setStyle("-fx-background-color: linear-gradient(to right, #00BFFF " 
                            + (progress * 100) + "%, rgb(1, 6, 7) " + (progress * 100) + "%);"
                + "-fx-background-radius: 10;");
        });

        mediaPlayer.setOnReady(() -> {
            double progress = seekSlider.getValue() / seekSlider.getMax(); // Get the progress ratio (0 to 1)
            Duration total = media.getDuration();
            seekSlider.setMax(total.toSeconds());
            totalTime.setText(formatTime(total));
            seekSlider.setStyle("-fx-background-color: linear-gradient(to right, #00BFFF " 
                            + (progress * 100) + "%, rgb(1, 6, 7) " + (progress * 100) + "%);"
                + "-fx-background-radius: 10;");
            updateMetadata();
        });

        seekSlider.setOnMousePressed(e -> {
            if (mediaPlayer != null) mediaPlayer.pause();
        });

        seekSlider.setOnMouseReleased(e -> {
            if (mediaPlayer != null) {
                double progress = seekSlider.getValue() / seekSlider.getMax(); // Get the progress ratio (0 to 1)
                mediaPlayer.seek(Duration.seconds(seekSlider.getValue()));
                seekSlider.setStyle("-fx-background-color: linear-gradient(to right, #00BFFF " 
                            + (progress * 100) + "%, rgb(1, 6, 7) " + (progress * 100) + "%);"
                + "-fx-background-radius: 10;");
                mediaPlayer.play();
            }
        });

        mediaPlayer.setOnEndOfMedia(() -> {
            rotate.stop();
            if (loopCheckbox.isSelected()) {
                mediaPlayer.seek(Duration.ZERO);
                mediaPlayer.play();
            } else if (loopSongCheckbox.isSelected()) {
                nextSong();
            } else {
                playPauseBtn.setText("▶");
            }
        });

        mediaPlayer.setOnError(() -> {
            System.err.println("Error occurred: " + mediaPlayer.getError().getMessage());
        });
    }

    private void togglePlayPause() {
        if (mediaPlayer == null) return;

        MediaPlayer.Status status = mediaPlayer.getStatus();
        if (status == MediaPlayer.Status.PLAYING) {
            mediaPlayer.pause();
            rotate.pause();
            playPauseBtn.setText("▶");
        } else {
            mediaPlayer.play();
            rotate.play();
            playPauseBtn.setText("⏸");
        }
    }

    /* 
    private void stopMusic() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            rotate.stop();
            playPauseBtn.setText("▶");
        }
    }
    */

    private void seek(int seconds) {
        if (mediaPlayer != null) {
            double newTime = Math.min(
                mediaPlayer.getTotalDuration().toSeconds(),
                Math.max(0, mediaPlayer.getCurrentTime().toSeconds() + seconds)
            );
            mediaPlayer.seek(Duration.seconds(newTime));
        }
    }

    private void nextSong() {
        if (songs.isEmpty()) return;
        currentIndex = (currentIndex + 1) % songs.size();
        musicList.getSelectionModel().select(currentIndex);
        playMusic(songs.get(currentIndex));
    }

    private void setLooping() {
        if (loopCheckbox.isSelected()) loopSongCheckbox.setSelected(false);
    }

    private void shuffleMusic() {
        if (loopSongCheckbox.isSelected()) {
        loopCheckbox.setSelected(false);
        loopSongCheckbox.setSelected(false); // Turn off normal loop

        // Shuffle the playlist
        if (songs != null && !songs.isEmpty()) {
            Collections.shuffle(songs); // Shuffle the list randomly

            currentIndex = 0; // Reset to the first track
            playMusic(songs.get(currentIndex)); // Start playing the first shuffled song
        }
    }
    }

    private String formatTime(Duration duration) {
        int minutes = (int) duration.toMinutes();
        int seconds = (int) duration.toSeconds() % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private void setupAlbumAnimation() {
        rotate = new RotateTransition(Duration.seconds(10), albumImage);
        rotate.setByAngle(360);
        rotate.setCycleCount(RotateTransition.INDEFINITE);
        rotate.setAutoReverse(false);
    }

    private void updateMetadata() {
        Map<String, Object> metadata = media.getMetadata();
        String title = (String) metadata.getOrDefault("title", songs.get(currentIndex).getName());
        String artist = (String) metadata.getOrDefault("artist", "Unknown Artist");
        String album = (String) metadata.getOrDefault("album", "Unknown Album");

        songTitle.setText(title);
        songDetails.setText(artist + " - " + album);

        Object img = metadata.get("image");
        if (img instanceof Image) {
            albumImage.setImage((Image) img);
        } else {
            try {
                albumImage.setImage(new Image(getClass().getResourceAsStream("/default_mp3.png")));
            } catch (Exception e) {
                System.err.println("Fallback album image not found.");
            }
        }
    }

    private void playPreviousSong() {
        currentIndex--;
        if (currentIndex < 0) {
            currentIndex = songs.size() - 1;  // or stop, depending on your requirement
        }
        mediaPlayer.stop();
        mediaPlayer = new MediaPlayer(new Media(songs.get(currentIndex).toURI().toString()));
        mediaPlayer.setVolume(volumeSlider.getValue());
        bindUIControls();
        updateMetadata();
        playPauseBtn.setText("⏸");
        rotate.play();
        mediaPlayer.play();
        musicList.getSelectionModel().select(currentIndex);
    }

    private void playNextSong() {
        currentIndex++;
        if (currentIndex >= songs.size()) {
            currentIndex = 0;
        }
        mediaPlayer.stop();
        mediaPlayer = new MediaPlayer(new Media(songs.get(currentIndex).toURI().toString()));
        mediaPlayer.setVolume(volumeSlider.getValue());
        bindUIControls();
        updateMetadata();
        playPauseBtn.setText("⏸");
        rotate.play();
        mediaPlayer.play();
        musicList.getSelectionModel().select(currentIndex);
    }

    private void toggleLooping() {
        if (loopSongCheckbox.isSelected()) {
            mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);  // Loop the current song indefinitely
        } else {
            mediaPlayer.setCycleCount(1);  // Stop looping, just play once
        }
    }

    @FXML
    private void handleExitButtonAction() {
        Platform.exit();
        System.exit(0);
    }

}
