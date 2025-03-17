package allen;

import java.io.File;
//import javafx.embed.swing.SwingFXUtils;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaException;
import javafx.scene.media.MediaException.Type;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaPlayer.Status;
import javafx.scene.media.MediaView;
import javafx.scene.robot.Robot;
import javafx.stage.Stage;
import javafx.util.Duration;

public class Player {
	
	Stage primaryStage;
	Config config;
	
	Media nowPlayingMedia;
	MediaPlayer nowPlayingPlayer;
	MediaView mediaView;
	

	//videos requested to play, some may not be valid, list may be too large to allocate
	// media players for each
	List<Video> playList;
	/// media players created for Videos. List may be expanded 
	List<MediaPlayer> playerList = new ArrayList<MediaPlayer>();
	//recover the Video from its player
	private Map<MediaPlayer, Video>  playerToVideo = new HashMap<MediaPlayer, Video>();

	//UI elements that may be updated
	private Slider timeSlider;
	private Label clipRez;
	private Label playTimeLabel;
	private Label muteSpeedLabel;
	
	static final Double HSIZE = 3360.0; 
	//static final Double HSIZE = 2560.0;
	//static final Double HSIZE = 1660.0;
	//static final Double HSIZE = 1920.0;
	
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
	private Duration nowPlayingDuration;
	private int nowPlayingIndex;
	private int nextPlayingIndex;
	private String nowPlayingName;
	
	
	private int numberOfClipsOnPlayerList;
	
	private int numberOfClipsRequestedToPlay; 
	private int playListIndex = 0;  //Index of the next vid on playL:ist to create a player

	//private Map<MediaPlayer, Duration> resumeTime = new HashMap<MediaPlayer, Duration>();
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
		createOrAugmentPlayerList();
		
		
		//first clip set up here, others set up by end of media
		System.out.println("number of videos to play: " + numberOfClipsOnPlayerList);
		if(numberOfClipsOnPlayerList == 0) {
			 stage.hide();
             primaryStage.show();
             return;
		}
			

		//Set up the UI

		mediaView = new MediaView();	
		mediaView.setFitWidth(HSIZE);
		mediaView.setFitHeight(VSIZE);
		mediaView.setSmooth(true);
		mediaView.setPreserveRatio(true);
		
//		Rectangle rect = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
//		
//
//		Toolkit.getDefaultToolkit().getScreenSize().width;
//		Toolkit.getDefaultToolkit().getScreenSize().height;



		// Create the Buttons labels and sliders that go in the controlBox (an Hbox)
		
		
		
		Button playPauseButton = new Button("Play/Pause");
		playPauseButton.setOnAction(new EventHandler<ActionEvent>() {			  
            @Override
            public void handle(ActionEvent event) {
            	event.consume();
            	System.out.println("play pause handler player status is "  + nowPlayingPlayer.getStatus());
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
// no op java media dies            	
//            	nowPlayingPlayer.pause();
//            	nextPlayingIndex = playerList.size() - 1;    //when resumed we play newly added videos
//            	createOrAugmentPlayerList();
//        		playNextClip();
                
            }
        });
		
		Button snapButton = new Button("Snap");
		snapButton.setOnAction(new EventHandler<ActionEvent>() {			  
            @Override
            public void handle(ActionEvent event) {
            	Path pic = Paths.get(config.picDirectory);
            	WritableImage wim = new WritableImage(HSIZE.intValue() , VSIZE.intValue());   	
            	mediaView.snapshot(null, wim);
//            	 try {
//            	   ImageIO.write(SwingFXUtils.fromFXImage(wim, null), "png", new File("/test.png"));
//            	 } catch (Exception s) {
//            	   System.out.println(s);
//            	 }
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
                	// System.out.println("time slider listener invoked");  // for testing
                    // multiply duration by percentage calculated by slider position
                	nowPlayingPlayer.seek(nowPlayingDuration.multiply(timeSlider.getValue() / 100.0));
                }
            }
        });
        
        // Add Play label
        playTimeLabel = new Label();
//        playTimeLabel.setPrefWidth(130);
//        playTimeLabel.setMinWidth(50);
        

        
        // Add deleted label
        deletedLabel = new Label();
        
        playListSizeLabel = new Label("Mataching Videos:" + playList.size());
 


		// Create the HBox
		HBox controlBox = new HBox(5, playListSizeLabel, playPauseButton, closeButton, 
				closeUpdateButton, moreButton,  clipRez,  playsLabel, muteSpeedLabel, deletedLabel, ratingLabel, timeSlider, playTimeLabel);
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
//				else {
//					System.out.println("tag check box handler"  + e.getText());
//				}

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
		
		nowPlayingPlayer = playerList.get(0);
		nowPlayingIndex = 0;
		nextPlayingIndex =  1 % numberOfClipsOnPlayerList;  //if playerList size is 1 stay at 0
		mediaView.setMediaPlayer(nowPlayingPlayer);
		setMediaProperties();
		
		nowPlayingPlayer.play();
    	Video video = playerToVideo.get(nowPlayingPlayer);
		SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy");
    	stage.setTitle(video.getDirectory() + " " + video.fileName  + " " + df.format(new Date(video.date)));

		
		stage.show();
		
		Robot robot = new Robot();
		double x = robot.getMouseX();
		double y = robot.getMouseY();
		robot.mouseMove(x , VSIZE + 200 );
		
	}
	
	private void createOrAugmentPlayerList() {
		int newSize = playerList.size() + numberOfClipsRequestedToPlay;
		while(playerList.size() < newSize  && playListIndex <  playList.size()) {
			createMediaPlayer(playList.get(playListIndex));
			playListIndex++;		
		}
		
		numberOfClipsOnPlayerList  =  playerList.size(); 
		
	}

	private boolean createMediaPlayer(Video vid) {
		String fullpathFileName = vid.getDirectory()  + "/" + vid.fileName;
		String modifiedFullpathFileName = Utility.getModifiedPath(fullpathFileName);
		File mediaFile = new File(modifiedFullpathFileName);
		if (! mediaFile.exists()) {
			Utility.msg(ta,  mediaFile.toString()  + " does not exist");
			//return false;
		}
		
		try {
			String uri = mediaFile.toURI().toURL().toString();
			Media media = null;
			try {
				media = new Media(uri);
			} catch (MediaException e) {
				if(e.getType() == Type.MEDIA_UNSUPPORTED) {
					vid.addTag(Config.BAD_CODEC);
					Utility.msg(ta,  mediaFile.toString()  + "unsupported: tagged " + Config.BAD_CODEC);	
				}
				else {
					Utility.msg(ta,  mediaFile.toString() +  " Unplayable: " +  e.getType()  );	
					
				}
				return false;
			}
			
			MediaPlayer player =  new MediaPlayer(media); 
			player.setAutoPlay(false);  //or they all start playing with only one visible
			player.setOnEndOfMedia(new Runnable() {   
		        @Override public void run() {
		        	player.seek(player.getStartTime());
		        	player.pause();
		        	updateClipInfo();
		        	nextClip();		        	
		        }					
		      });
			
			
			player.currentTimeProperty().addListener(new InvalidationListener() {
		        public void invalidated(Observable ov) {
		            updateUIMediaStatus();
		        }
		    });
				
			playerList.add(player);
			playerToVideo.put(player,vid);
			
		} catch (MalformedURLException e1) {
			Utility.msg(ta, "Malformed file name "  + fullpathFileName  );
			return false;
		}
		return true;
	}
	
	private void closeAction() {
	    stage.hide();
		nowPlayingPlayer.stop();
    	updateClipInfo();
   
        primaryStage.show();
        playerList.stream().forEach(mp -> mp.dispose());
	}
	
	private void setMediaProperties() {
		nowPlayingMedia = nowPlayingPlayer.getMedia();
		Duration currentTime = nowPlayingPlayer.getCurrentTime();
		nowPlayingDuration = nowPlayingPlayer.getStopTime();
		String ct = formatTime(currentTime, nowPlayingDuration);
		playTimeLabel.setText(ct);
		Video video = playerToVideo.get(nowPlayingPlayer);
		nowPlayingName = video.fileName;
		List<String> tags = video.tags;
		clipRez.setText(Integer.toString(nowPlayingMedia.getHeight()));
		ratingLabel.setText(video.rating == null ? "unrated" : Integer.toString(video.rating));
		playsLabel.setText(Integer.toString(video.plays));
		deletedLabel.setText(video.markedForDeletion ? "marked deleted" : "");
		deletedLabel.setStyle("-fx-text-fill: red");
		nowPlayingPlayer.setMute(isMuted);
		nowPlayingPlayer.setVolume(volume);
		for (CheckBox cb : tagButtons) {
			cb.setSelected(tags.contains(cb.getText()));
		}
	}
	
	private void nextClip() {
    	nowPlayingIndex = nextPlayingIndex;
    	nowPlayingPlayer = playerList.get(nowPlayingIndex);
    	nextPlayingIndex = (nextPlayingIndex + 1 ) % numberOfClipsOnPlayerList; 
    	mediaView.setMediaPlayer(nowPlayingPlayer);	
    	setMediaProperties();  	
    	nowPlayingPlayer.play();
    	Video video = playerToVideo.get(nowPlayingPlayer);
    	
		SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy");
    	stage.setTitle(video.getDirectory() + " " + video.fileName  + " " + df.format(new Date(video.date)));

    	
		
	}
	
	private void updateClipInfo() {
		Video v = playerToVideo.get(nowPlayingPlayer);
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
        Status status = nowPlayingPlayer.getStatus();
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
//        else if (key.isDigitKey()) {
//        	int rating = Integer.parseInt(key.getChar());
//        	playerToVideo.get(nowPlayingPlayer).rating = rating;
//        	ratingLabel.setText(key.getChar());
//        	
//        }
        int pressStatus = statusCommand + statusAlt + statusShift;
//        System.out.println("pressStatus: " + pressStatus);  // for testing
	}
	
	private void keyReleased(KeyEvent evt) {

        KeyCode key = evt.getCode();  // keyboard code for the pressed key
//        System.out.println("Key Released: " + key);  // for testing
        
        Status status = nowPlayingPlayer.getStatus();
        //System.out.println("player status: " + status);  // for testing

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

//        System.out.println("pressStatus: " + pressStatus);  // for testing
        
        if (key == KeyCode.LEFT) { 
        	
			Duration current = nowPlayingPlayer.getCurrentTime();
			Duration next = current.subtract(new Duration(offset * 500));
//			System.out.println("Key released left key current: " + current.toSeconds() + "next: " + next.toSeconds()); 
//			System.out.println("Key released start time: " +  nowPlayingPlayer.getStartTime().toSeconds()); 
			if(next.lessThan(nowPlayingPlayer.getStartTime()))
				next = nowPlayingPlayer.getStartTime();
//			System.out.println("Key released left key current: " + current.toSeconds() + "next: " + next.toSeconds()); 
			nowPlayingPlayer.seek(next);
        	
        	
        	
        }// left arrow key
     
        else if (key == KeyCode.RIGHT) {  // right arrow key
           

			Duration current = nowPlayingPlayer.getCurrentTime();
			Duration next = current.add(new Duration(offset * 1000));
			Duration nearEnd = nowPlayingPlayer.getStopTime().subtract(CLIP_END_BUFFER);
			Duration end = nowPlayingPlayer.getStopTime();
			if(next.greaterThan(end))
				next = end;
			
//			System.out.println("Key released Right key current: " + current.toSeconds() + "next: " + next.toSeconds()); 
//			System.out.println("Key released stop time: " +  nowPlayingPlayer.getStopTime().toSeconds()); 
			
			if(current.greaterThan(nearEnd))  
				nowPlayingPlayer.seek(end);
			else if (next.greaterThan(nearEnd))
				nowPlayingPlayer.seek(nearEnd);
			else
				nowPlayingPlayer.seek(next);

        }
        else if (key == KeyCode.UP) {  // up arrow key
        	//System.out.println("up Pressed: "); 
        	nowPlayingPlayer.pause();
        	playNextClip();
        	
        }
        else if (key == KeyCode.DOWN) {  // down arrow key
        	
        	nowPlayingPlayer.pause();
        	nextPlayingIndex = nowPlayingIndex == 0 ? numberOfClipsOnPlayerList - 1 : nowPlayingIndex - 1;
//        	System.out.println("down Pressed: "); 
        	playNextClip();

        }
	}

	private void playNextClip() {
		//resumeTime.put(nowPlayingPlayer, nowPlayingPlayer.getCurrentTime());
		updateClipInfo();
//		System.out.println("up Pressed: resumeTime: "  + nowPlayingPlayer.getCurrentTime().toSeconds());
		nextClip();
	}

	private void keyTyped(KeyEvent evt) {

		// if (evt.getEventType() != KeyEvent.KEY_TYPED ) return;

		Status status = nowPlayingPlayer.getStatus();

		String ch = evt.getCharacter();
//		System.out.println("Char Typed: " + ch);
		evt.consume();

		if (ch.equals(" ")) // space binds to default button press
			togglePlayPause();
			
		else if (ch.equals("m")) {
			isMuted = !nowPlayingPlayer.isMute();
			nowPlayingPlayer.setMute(isMuted);
			muteSpeedLabel.setText(setMutedSpeedLabel());
		}
			
		else if (ch.equals(",") || ch.equals(".") ){
			if(ch.equals(",") ) {
				volume = volume * 0.8;
			}
			else {
				volume = Double.min(1.0, volume * 1.2);
			}
				
			nowPlayingPlayer.setVolume(volume);
			isMuted = false;
			nowPlayingPlayer.setMute(isMuted);
			muteSpeedLabel.setText(setMutedSpeedLabel());
		}
		else if (ch.equals("d")) {
			boolean del =  ! playerToVideo.get(nowPlayingPlayer).markedForDeletion;
			playerToVideo.get(nowPlayingPlayer).markedForDeletion =  del;
			deletedLabel.setText( del ? "marked deleted" : "");
		}
		else if (ch.equals("l")) {
        	playerToVideo.get(nowPlayingPlayer).rating = 1;
        	ratingLabel.setText("1");
			
		}
		else if (ch.equals(";")) {
        	playerToVideo.get(nowPlayingPlayer).rating = 2;
        	ratingLabel.setText("2");
			
		}
		else if (ch.equals("'")) {
        	playerToVideo.get(nowPlayingPlayer).rating = 3;
        	ratingLabel.setText("3");
			
		}
		else if (ch.equals("/")) {
        	if(rate == 1.0) {
        		rate = 0.5;
        		wasMutedBeforeSpeedChange = isMuted;
        		
        		isMuted = true;
				nowPlayingPlayer.setMute(isMuted);
        	}
        		
        		
        	else if (rate == 0.5) {
        		rate = 0.25;
        	}
        	else {
        			rate = 1.0;
        			isMuted = wasMutedBeforeSpeedChange;
        			nowPlayingPlayer.setMute(isMuted);
        		}
        	nowPlayingPlayer.setRate(rate);
        	muteSpeedLabel.setText(setMutedSpeedLabel());
			
		}

	}

	private void togglePlayPause() {
		Status status = nowPlayingPlayer.getStatus();
		if (status == Status.PLAYING) {
			nowPlayingPlayer.pause();
			
			
		}
		else
			nowPlayingPlayer.play();

		
	}
	
	
	String setMutedSpeedLabel() {
		
		String retVal = "";
		if (isMuted) retVal += "Muted";
		if(rate == 0.25) retVal += "  25";
		if(rate == 0.50) retVal += "  50";
		return retVal;

	}
		


	
	protected void updateUIMediaStatus() {


		Platform.runLater(new Runnable() {
			public void run() {
				Duration currentTime = nowPlayingPlayer.getCurrentTime();
				String ct = formatTime(currentTime, nowPlayingDuration);
				//System.out.println("time display: " + ct); // for testing
				playTimeLabel.setText(ct);

				timeSlider.setDisable(nowPlayingDuration.isUnknown());
				if (!timeSlider.isDisabled() && nowPlayingDuration.greaterThan(Duration.ZERO)
						&& !timeSlider.isValueChanging()) {
					timeSlider.setValue(currentTime.divide(nowPlayingDuration).toMillis() * 100.0);

				}
			}
		});
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


