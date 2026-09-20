package allen;

import java.awt.Dimension;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.robot.Robot;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.embed.swing.SwingFXUtils;

import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.javafx.videosurface.ImageViewVideoSurface;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;

/**
 * Playback engine note: video is decoded and rendered by libvlc via vlcj
 * (VideoLAN), not javafx.scene.media. One EmbeddedMediaPlayer is created and
 * reused for the whole session; advancing to the next clip loads a new MRL
 * into the same player rather than creating a new player per clip. This is
 * both simpler and lighter-weight than the previous JavaFX MediaPlayer
 * pre-allocation scheme, and libvlc supports a far broader range of
 * containers/codecs than the bundled JavaFX media engine did.
 */
public class Player {

	Stage primaryStage;
	Config config;

	// vlcj playback engine
	private MediaPlayerFactory mediaPlayerFactory;
	private EmbeddedMediaPlayer mediaPlayer;
	private ImageView mediaView;

	// videos requested to play; the batch below is a rotating window into this list
	List<Video> playList;

	// clips currently in rotation are playList.get(0 .. numberOfClipsOnPlayerList)
	private int numberOfClipsOnPlayerList;
	private int numberOfClipsRequestedToPlay;

	private Video currentVideo;

	// remembers, per video, the playback position (ms) we were at when we
	// last switched away from it, so cycling back to it later resumes instead
	// of restarting from the beginning. Session-only, like the old per-video
	// MediaPlayer objects were - not persisted to the config file.
	private final Map<Video, Long> resumePositions = new HashMap<>();

	//UI elements that may be updated
	private Slider timeSlider;
	private Label clipRez;
	private Label playTimeLabel;
	private Label muteSpeedLabel;

	static final Double HSIZE = 3360.0;
//	static final Double HSIZE = 2560.0;
//	static final Double HSIZE = 1450.0;
//	static final Double HSIZE = 1920.0;

	static final Double VSIZE = (HSIZE * 0.5626); // 16:9
	static final Duration CLIP_END_BUFFER = Duration.seconds(10.0);


	//these fields hold the state of the player
	private int statusCommand = 0;
	private int statusAlt = 0;
	private int statusShift = 0;
	private boolean isMuted = false;
	private boolean wasMutedBeforeSpeedChange = false;
	//when going from paused to play suppress on play actions

	private Double volume = 0.1;
	private double rate = 1.0;
	private Duration nowPlayingDuration = Duration.UNKNOWN;
	private int nowPlayingIndex;
	private int nextPlayingIndex;
	private String nowPlayingName;

	private List<CheckBox> tagButtons = new ArrayList<CheckBox>();
	private Label ratingLabel;
	private Label playsLabel;
	private Label deletedLabel;
	private Label playListSizeLabel;
	private TextArea ta;
	Stage stage;


	public Player(List<Video> playListArg, Config configArg, int numberOfClipsArg, TextArea ma, Stage primaryStageArg) {
		ta = ma;
		playList = playListArg;
		config = configArg;
		numberOfClipsRequestedToPlay = numberOfClipsArg;
		primaryStage = primaryStageArg;
		stage = new Stage();
		start(stage);
	}

	public void start(Stage stage){
		augmentBatch();

		//first clip set up here, others loaded into the same player as we advance
		System.out.println("number of videos to play: " + numberOfClipsOnPlayerList);
		if(numberOfClipsOnPlayerList == 0) {
			 stage.hide();
			 primaryStage.show();
			 return;
		}

		// One libvlc-backed player for the whole session; video renders into mediaView.
		mediaPlayerFactory = new MediaPlayerFactory();
		mediaPlayer = mediaPlayerFactory.mediaPlayers().newEmbeddedMediaPlayer();
		mediaPlayer.events().addMediaPlayerEventListener(new PlayerEvents());

		//Set up the UI

		mediaView = new ImageView();
		mediaView.setFitWidth(HSIZE);
		mediaView.setFitHeight(VSIZE);
		mediaView.setSmooth(true);
		mediaView.setPreserveRatio(true);
		mediaPlayer.videoSurface().set(new ImageViewVideoSurface(mediaView));



		// Create the Buttons labels and sliders that go in the controlBox (an Hbox)



		Button playPauseButton = new Button("Play/Pause");
		playPauseButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
            	event.consume();
            	togglePlayPause();
            }
        });


		Button closeButton = new Button("Close");
		closeButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
            	closeAction();
            }
        });
		Button closeUpdateButton = new Button("Close/Update");
		closeUpdateButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
            	closeAction();
            	config.serializeDB(App.configFilename, null);
            }
        });
		Button moreButton = new Button("More");
		moreButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
            	mediaPlayer.controls().setPause(true);
            	int oldBatchSize = numberOfClipsOnPlayerList;
            	augmentBatch();
            	updateClipInfo();
            	//when resumed we play a newly added video if one was added, otherwise carry on as normal
            	int target = oldBatchSize < numberOfClipsOnPlayerList ? oldBatchSize : nextPlayingIndex;
            	playClip(target);
            }
        });

		Button snapButton = new Button("Snap");
		snapButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                // Create a snapshot of the mediaView
                WritableImage wim = new WritableImage(HSIZE.intValue(), VSIZE.intValue());
                mediaView.snapshot(null, wim);
                // Save the image to the configured picDirectory with a timestamped filename
                try {
                    String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                    String filename = config.picDirectory + File.separator + nowPlayingName + timestamp + ".png";
                    File outputFile = new File(filename);
                    ImageIO.write(
                        SwingFXUtils.fromFXImage(wim, null),
                        "png",
                        outputFile
                    );
                    Utility.msg(ta, "Snapshot saved: " + filename);
                } catch (Exception ex) {
                    Utility.msg(ta, "Error saving snapshot: " + ex.getMessage());
                }
            }
        });



        // Add isMuted label
        muteSpeedLabel = new Label();
        muteSpeedLabel.setStyle("-fx-text-fill: red");

        // Add clip name label
        clipRez = new Label();

        ratingLabel = new Label();

        playsLabel = new Label();

        // Add time slider
        timeSlider = new Slider();

        HBox.setHgrow(timeSlider, Priority.ALWAYS);

        timeSlider.setMinWidth(50);
        timeSlider.setMaxWidth(350);
        timeSlider.valueProperty().addListener(new InvalidationListener() {
            public void invalidated(Observable ov) {
                if (timeSlider.isValueChanging()) {
                    // multiply duration by percentage calculated by slider position
                	seekToFraction(timeSlider.getValue() / 100.0);
                }
            }
        });

        // Add Play label
        playTimeLabel = new Label();


        // Add deleted label
        deletedLabel = new Label();

        playListSizeLabel = new Label("Mataching Videos:" + playList.size());



		// Create the HBox
		HBox controlBox = new HBox(5, playListSizeLabel, playPauseButton, closeButton,
				closeUpdateButton, moreButton,  snapButton, clipRez,  playsLabel, muteSpeedLabel, deletedLabel, ratingLabel, timeSlider, playTimeLabel);
		controlBox.setAlignment(Pos.CENTER);


		HBox tagBox = new HBox(5);
		tagBox.setAlignment(Pos.CENTER);
		for (String s : config.tags) {
			CheckBox tCheckBox = new CheckBox(s);
			tCheckBox.setOnKeyTyped(e -> {
				if (e.getCharacter().equals(" ")){
					e.consume();
					togglePlayPause();
					tCheckBox.setSelected(!tCheckBox.isSelected());
				}
			});

			tagBox.getChildren().add(tCheckBox);
			tagButtons.add(tCheckBox);

		}


		// Create the VBox
		VBox root = new VBox(15, mediaView, controlBox, tagBox );
		root.setStyle("-fx-font-size: 13pt");

		// Create the Scene
		Scene scene = new Scene(root);
        scene.setOnKeyPressed( e -> keyPressed(e));
        scene.setOnKeyTyped( e -> keyTyped(e));
        scene.setOnKeyReleased( e -> keyReleased(e));


		stage.setOnCloseRequest(
				ev -> {
					Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
					alert.setTitle("Save config file?");
					alert.setHeaderText("Save config file?");
					Optional<ButtonType> option = alert.showAndWait();
					if (option.get() == ButtonType.OK) {
						config.serializeDB(config.configFileName, null);
						System.out.println("Config Saved");
					}
				}
		);



		// Add the scene to the Stage
		stage.setScene(scene);
		// Set the title of the Stage
		stage.setTitle("Player");
		// Display the Stage

		nowPlayingIndex = 0;
		nextPlayingIndex = 1 % numberOfClipsOnPlayerList;  //if batch size is 1 stay at 0
		playClip(nowPlayingIndex);

		stage.show();

		Robot robot = new Robot();
		double x = robot.getMouseX();
		double y = robot.getMouseY();
		robot.mouseMove(x , VSIZE + 200 );

	}

	/** Grows the rotation by up to numberOfClipsRequestedToPlay more videos from playList. */
	private void augmentBatch() {
		numberOfClipsOnPlayerList = Math.min(playList.size(), numberOfClipsOnPlayerList + numberOfClipsRequestedToPlay);
	}

	private int advance(int batchIndex) {
		return (batchIndex + 1) % numberOfClipsOnPlayerList;
	}

	/**
	 * Loads and plays the video at the given index in the current batch, skipping
	 * over any clips whose file is missing. Unsupported/unplayable files are
	 * caught asynchronously by PlayerEvents.error once libvlc tries them.
	 */
	private void playClip(int batchIndex) {
		if (currentVideo != null) {
			resumePositions.put(currentVideo, mediaPlayer.status().time());
		}
		int attempts = 0;
		while (attempts < numberOfClipsOnPlayerList) {
			Video vid = playList.get(batchIndex);
			String fullpathFileName = vid.getDirectory() + "/" + vid.fileName;
			String modifiedFullpathFileName = Utility.getModifiedPath(fullpathFileName);
			File mediaFile = new File(modifiedFullpathFileName);

			if (!mediaFile.exists()) {
				Utility.msg(ta, mediaFile.toString() + " does not exist");
				batchIndex = advance(batchIndex);
				attempts++;
				continue;
			}

			nowPlayingIndex = batchIndex;
			nextPlayingIndex = advance(batchIndex);
			currentVideo = vid;
			nowPlayingName = vid.fileName;
			nowPlayingDuration = Duration.UNKNOWN;

			Long resumeMs = resumePositions.get(vid);
			if (resumeMs != null && resumeMs > 0) {
				mediaPlayer.media().play(mediaFile.getAbsolutePath(),
						String.format(java.util.Locale.US, ":start-time=%.3f", resumeMs / 1000.0));
			} else {
				mediaPlayer.media().play(mediaFile.getAbsolutePath());
			}
			mediaPlayer.audio().setMute(isMuted);
			mediaPlayer.audio().setVolume((int) Math.round(volume * 100));
			mediaPlayer.controls().setRate((float) rate);

			updateStaticClipLabels();

			SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy");
			stage.setTitle(vid.getDirectory() + " " + vid.fileName + " " + df.format(new Date(vid.date)));
			return;
		}
		Utility.msg(ta, "No playable clips found in this batch");
		closeAction();
	}

	private void closeAction() {
	    stage.hide();
	    if (mediaPlayer != null) {
	    	mediaPlayer.controls().setPause(true);
	    	updateClipInfo();
	    }
        primaryStage.show();
        if (mediaPlayer != null) {
        	mediaPlayer.release();
        }
        if (mediaPlayerFactory != null) {
        	mediaPlayerFactory.release();
        }
	}

	/** Labels that only depend on which video is loaded, not on playback progress. */
	private void updateStaticClipLabels() {
		ratingLabel.setText(currentVideo.rating == null ? "unrated" : Integer.toString(currentVideo.rating));
		playsLabel.setText(Integer.toString(currentVideo.plays));
		deletedLabel.setText(currentVideo.markedForDeletion ? "marked deleted" : "");
		deletedLabel.setStyle("-fx-text-fill: red");
		List<String> tags = currentVideo.tags;
		for (CheckBox cb : tagButtons) {
			cb.setSelected(tags.contains(cb.getText()));
		}
	}

	private void nextClip() {
		playClip(nextPlayingIndex);
	}

	private void updateClipInfo() {
		Video v = currentVideo;
    	List<String> tags = tagButtons.stream().
    			filter(tb -> tb.isSelected()).
    			filter(tb -> ! tb.getText().equals(Config.CONVERTED)).  //auto deselect converted
    			map( tb -> tb.getText()).
    			collect(Collectors.toList());
    	v.tags = tags;
    	//Give a default rating of 1 to any video that has a category tag
    	if(v.rating == null &&  v.tags.stream().anyMatch(st -> ! Config.SPECIAL_TAGS.contains(st)))
    		v.rating =1;
    	v.plays++;
	}

	private void keyPressed(KeyEvent evt) {

        KeyCode key = evt.getCode();  // keyboard code for the pressed key
        //System.out.println("Key Pressed: " + key);  // for testing
        evt.consume();

        if (key == KeyCode.COMMAND) {
        	statusCommand = 1;
        }
        else if (key == KeyCode.ALT) {
        	statusAlt = 2;
        }
        else if (key == KeyCode.SHIFT) {
        	statusShift = 4;
        }
	}

	private void keyReleased(KeyEvent evt) {

        KeyCode key = evt.getCode();  // keyboard code for the pressed key
//        System.out.println("Key Released: " + key);  // for testing

        if (key == KeyCode.COMMAND) {
        	statusCommand = 0;
        }
        else if (key == KeyCode.ALT) {
        	statusAlt = 0;
        }
        else if (key == KeyCode.SHIFT) {
        	statusShift = 0;
        }

        int pressStatus = statusCommand + statusAlt + statusShift;

    	int offset;

    	switch (pressStatus) {
    	case 0: offset = 15; break;
    	case 1: offset = 30; break;
    	case 2: offset = 60; break;
    	case 3: offset = 120; break;
    	case 4: offset = 240; break;
    	case 5: offset = 600; break;
    	case 6: offset = 1200; break;
    	case 7: offset = 2400; break;
    	default: offset = 0;
    	}

        if (key == KeyCode.LEFT) {

			long current = mediaPlayer.status().time();
			long next = current - (offset * 500L);
			if (next < 0)
				next = 0;
			mediaPlayer.controls().setTime(next);

        }// left arrow key

        else if (key == KeyCode.RIGHT) {  // right arrow key

			long current = mediaPlayer.status().time();
			long length = mediaPlayer.status().length();
			long next = current + (offset * 1000L);

			if (length <= 0) {
				// duration not known yet, just seek forward
				mediaPlayer.controls().setTime(next);
			} else {
				long nearEnd = length - (long) CLIP_END_BUFFER.toMillis();
				if (next > length)
					next = length;

				if (current > nearEnd) {
		        	mediaPlayer.controls().setPause(true);
		        	playNextClip();
				}
				else if (next > nearEnd)
					mediaPlayer.controls().setTime(nearEnd);
				else
					mediaPlayer.controls().setTime(next);
			}

        }
        else if (key == KeyCode.UP) {  // up arrow key
        	mediaPlayer.controls().setPause(true);
        	playNextClip();

        }
        else if (key == KeyCode.DOWN) {  // down arrow key

        	mediaPlayer.controls().setPause(true);
        	nextPlayingIndex = nowPlayingIndex == 0 ? numberOfClipsOnPlayerList - 1 : nowPlayingIndex - 1;
        	playNextClip();

        }

	}

	private void playNextClip() {
		updateClipInfo();
		nextClip();
	}

	private void keyTyped(KeyEvent evt) {

		String ch = evt.getCharacter();
		evt.consume();

		if (ch.equals(" ")) // space binds to default button press
			togglePlayPause();

		else if (ch.equals("m")) {
			isMuted = !mediaPlayer.audio().isMute();
			mediaPlayer.audio().setMute(isMuted);
			muteSpeedLabel.setText(setMutedSpeedLabel());
		}

		else if (ch.equals(",") || ch.equals(".") ){
			if(ch.equals(",") ) {
				volume = volume * 0.8;
			}
			else {
				volume = Double.min(1.0, volume * 1.2);
			}

			mediaPlayer.audio().setVolume((int) Math.round(volume * 100));
			isMuted = false;
			mediaPlayer.audio().setMute(isMuted);
			muteSpeedLabel.setText(setMutedSpeedLabel());
		}
		else if (ch.equals("d")) {
			boolean del =  ! currentVideo.markedForDeletion;
			currentVideo.markedForDeletion =  del;
			deletedLabel.setText( del ? "marked deleted" : "");
		}
		else if (ch.equals("l")) {
        	currentVideo.rating = 1;
        	ratingLabel.setText("1");

		}
		else if (ch.equals(";")) {
        	currentVideo.rating = 2;
        	ratingLabel.setText("2");

		}
		else if (ch.equals("'")) {
        	currentVideo.rating = 3;
        	ratingLabel.setText("3");

		}
		else if (ch.equals("q")) {
        	closeAction();

		}
		else if (ch.equals("/")) {
        	if(rate == 1.0) {
        		rate = 0.5;
        		wasMutedBeforeSpeedChange = isMuted;

        		isMuted = true;
				mediaPlayer.audio().setMute(isMuted);
        	}


        	else if (rate == 0.5) {
        		rate = 0.25;
        	}
        	else {
        			rate = 1.0;
        			isMuted = wasMutedBeforeSpeedChange;
        			mediaPlayer.audio().setMute(isMuted);
        		}
        	mediaPlayer.controls().setRate((float) rate);
        	muteSpeedLabel.setText(setMutedSpeedLabel());

		}

	}

	private void togglePlayPause() {
		if (mediaPlayer.status().isPlaying()) {
			mediaPlayer.controls().setPause(true);
		}
		else
			mediaPlayer.controls().setPause(false);

	}


	String setMutedSpeedLabel() {

		String retVal = "";
		if (isMuted) retVal += "Muted";
		if(rate == 0.25) retVal += "  25";
		if(rate == 0.50) retVal += "  50";
		return retVal;

	}

	/** vlcj/libvlc event callbacks. These fire on a native callback thread, so all
	 *  work (including any further calls back into the player) is deferred onto
	 *  the JavaFX Application Thread via Platform.runLater. */
	private class PlayerEvents extends MediaPlayerEventAdapter {

		@Override
		public void finished(uk.co.caprica.vlcj.player.base.MediaPlayer mp) {
			Platform.runLater(() -> {
				resumePositions.remove(currentVideo);
				updateClipInfo();
				nextClip();
			});
		}

		@Override
		public void error(uk.co.caprica.vlcj.player.base.MediaPlayer mp) {
			Platform.runLater(() -> {
				if (currentVideo != null) {
					currentVideo.addTag(Config.BAD_CODEC);
					Utility.msg(ta, nowPlayingName + " unsupported/unplayable: tagged " + Config.BAD_CODEC);
				}
				nextClip();
			});
		}

		@Override
		public void timeChanged(uk.co.caprica.vlcj.player.base.MediaPlayer mp, long newTime) {
			Platform.runLater(() -> updateUIMediaStatus());
		}
	}

	private void seekToFraction(double fraction) {
		long length = mediaPlayer.status().length();
		if (length > 0) {
			mediaPlayer.controls().setTime((long) (length * fraction));
		}
	}

	protected void updateUIMediaStatus() {

		long timeMs = mediaPlayer.status().time();
		long lengthMs = mediaPlayer.status().length();
		Duration currentTime = timeMs >= 0 ? Duration.millis(timeMs) : Duration.ZERO;
		nowPlayingDuration = lengthMs > 0 ? Duration.millis(lengthMs) : Duration.UNKNOWN;

		String ct = formatTime(currentTime, nowPlayingDuration);
		playTimeLabel.setText(ct);

		timeSlider.setDisable(nowPlayingDuration.isUnknown());
		if (!timeSlider.isDisabled() && nowPlayingDuration.greaterThan(Duration.ZERO)
				&& !timeSlider.isValueChanging()) {
			timeSlider.setValue(currentTime.divide(nowPlayingDuration).toMillis() * 100.0);
		}

		Dimension dim = mediaPlayer.video().videoDimension();
		if (dim != null) {
			clipRez.setText(Integer.toString(dim.height));
		}
	}



		private static String formatTime(Duration elapsed, Duration duration) {
			int intElapsed = (int) Math.floor(elapsed.toSeconds());
			int elapsedHours = intElapsed / (60 * 60);
			if (elapsedHours > 0) {
				intElapsed -= elapsedHours * 60 * 60;
			}
			int elapsedMinutes = intElapsed / 60;
			int elapsedSeconds = intElapsed - elapsedMinutes * 60;


			if (duration.greaterThan(Duration.ZERO)) {
				int intDuration = (int) Math.floor(duration.toSeconds());
				int durationHours = intDuration / (60 * 60);
				if (durationHours > 0) {
					intDuration -= durationHours * 60 * 60;
				}
				int durationMinutes = intDuration / 60;
				int durationSeconds = intDuration - durationMinutes * 60;
				if (durationHours > 0) {
					return String.format("%d:%02d:%02d/%d:%02d:%02d", elapsedHours, elapsedMinutes, elapsedSeconds,
							durationHours, durationMinutes, durationSeconds);
				} else {
					return String.format("%02d:%02d/%02d:%02d", elapsedMinutes, elapsedSeconds, durationMinutes,
							durationSeconds);
				}
			} else {
				if (elapsedHours > 0) {
					return String.format("%d:%02d:%02d", elapsedHours, elapsedMinutes, elapsedSeconds);
				} else {
					return String.format("%02d:%02d", elapsedMinutes, elapsedSeconds);
				}
			}
		}


}
