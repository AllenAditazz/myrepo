package allen;

import java.awt.Desktop;
import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Slider;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.PopupWindow;
import javafx.stage.Stage;

public class App extends Application {
	static final String VERSION = "1.7";
	static String[] commandArgs;
	// static String configFilename = "configTest.yaml";
	public static String configFilename = "config.txt";
	int numberOfClipsInDatabase;
	List<Video> playList;
	VideoPreference vp;
	List<CheckBox> tagCheckBoxes = new ArrayList<CheckBox>();
	List<CheckBox> dirCheckBoxes = new ArrayList<CheckBox>();
	Stage primary;
	String fileSubstitutionPattern;

	TextArea infoPane;
	String textToShow = "version " + VERSION + "\n";

	public static void main(String[] args) {
		commandArgs = args;
		launch(args);
	}

	public void start(Stage primaryStage) throws MalformedURLException {
		primary = primaryStage;
		if (commandArgs.length > 0)
			configFilename = commandArgs[0];
		if (commandArgs.length > 1)
			Config.fileSubstitutionPrefix  = commandArgs[1];


		final Config config = Config.parseDB(configFilename);

		playList = config.videos;
		numberOfClipsInDatabase = playList.size();

		primaryStage.setOnCloseRequest(ev -> {
			Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
			alert.setTitle("Save config file?");
			alert.setHeaderText("Save config file?");
			Optional<ButtonType> option = alert.showAndWait();
			if (option.get() == ButtonType.OK) {
				config.serializeDB(configFilename, infoPane);
				System.out.println("Config Saved");
			}
		}

		);

		// UI elements
		TabPane tabPane = new TabPane();

		FlowPane topLevelPaneChooser = new FlowPane(); // tagPane, dirPane, control pane

		// Analysis Pane
		VBox analysisPane = new VBox(5);
		HBox commandsPane = new HBox(15);

		Tab tab1 = new Tab("Chooser", topLevelPaneChooser);
		tabPane.getTabs().add(tab1);
		Tab tab2 = new Tab("Config", analysisPane);
		tabPane.getTabs().add(tab2);

		Button analyzeFiles = new Button("Analyze Files");
		Button scanDirs = new Button("Scan Directories");
		Button updateConfig = new Button("Update config File");
		Button trashDeleted = new Button("Trash Files Marked Deleted");
		Button newFiles = new Button("New Files");


		commandsPane.getChildren().add(analyzeFiles);
		commandsPane.getChildren().add(scanDirs);
		commandsPane.getChildren().add(updateConfig);
		commandsPane.getChildren().add(trashDeleted);
		commandsPane.getChildren().add(newFiles);
		analysisPane.getChildren().add(commandsPane);

		TextArea analysisText = new TextArea("Config file: " + configFilename + "\n");
		analysisText.setPrefRowCount(40);
		analysisText.setWrapText(true);
		analysisPane.getChildren().add(analysisText);

		topLevelPaneChooser.setVgap(20);
		topLevelPaneChooser.setHgap(40);
		topLevelPaneChooser.setStyle("-fx-border-color: black");
		topLevelPaneChooser.setPadding(new Insets(10, 10, 10, 10));

		// tag pane
		VBox tagPane = new VBox(10); // check box for each tag plus an "and" box
		topLevelPaneChooser.getChildren().add(tagPane);
		tagPane.setStyle("-fx-border-color: black");
		tagPane.setPadding(new Insets(5, 15, 5, 5));
		CheckBox andTag = new CheckBox("And");
		tagPane.getChildren().add(andTag);
		tagCheckBoxes.add(andTag);
		for (String s : config.tags) {
			CheckBox tagCheckBox = new CheckBox(s);
			tagPane.getChildren().add(tagCheckBox);
			tagCheckBoxes.add(tagCheckBox);
		}

		// dir pane
		VBox dirPane = new VBox(10); // checkBox for each directory
		topLevelPaneChooser.getChildren().add(dirPane);
		for (String s : config.directories) {
			CheckBox dButton = new CheckBox(s);
			dirPane.getChildren().add(dButton);
			dirCheckBoxes.add(dButton);
		}

		// control pane

		VBox controlPane = new VBox(25); // action pane, rating, etc...
		HBox actionPane = new HBox(15);
		topLevelPaneChooser.getChildren().add(controlPane);
		controlPane.getChildren().add(actionPane);

		Button playButton = new Button("Play");
		actionPane.getChildren().add(playButton);
		Button convertButton = new Button("Convert");
		actionPane.getChildren().add(convertButton);

		// rating
		HBox ratingPane = new HBox(15);
		controlPane.getChildren().add(ratingPane);
		CheckBox unratedCheckBox = new CheckBox("Unrated");
		ratingPane.getChildren().add(unratedCheckBox);
		Label ratingLabel = new Label("Rating");
		ratingPane.getChildren().add(ratingLabel);
		String[] ratingLevels = { "all", "1", "2", "3" };
		ComboBox<String> ratingLevelsBox = new ComboBox<String>(FXCollections.observableArrayList(ratingLevels));
		ratingLevelsBox.setEditable(false);
		ratingPane.getChildren().add(ratingLevelsBox);
		CheckBox exactRating = new CheckBox("Exact Rating");
		ratingPane.getChildren().add(exactRating);

		// number of clips
		HBox numberOfClipsPane = new HBox(15);
		Label numberOfClipsLabel = new Label("Number of Clips");
		numberOfClipsPane.getChildren().add(numberOfClipsLabel);
		String[] numberOfClipsSelection = { "1", "3", "15", "30", "45" };
		ComboBox<String> numClipsComboBox = new ComboBox<String>(
				FXCollections.observableArrayList(numberOfClipsSelection));
		numClipsComboBox.setEditable(true);
		numberOfClipsPane.getChildren().add(numClipsComboBox);
		controlPane.getChildren().add(numberOfClipsPane);

		// number of plays
		HBox playFrequencyPane = new HBox(15);
		controlPane.getChildren().add(playFrequencyPane);
		RadioButton unPlayedRadio = new RadioButton("Unplayed");
		playFrequencyPane.getChildren().add(unPlayedRadio);
		RadioButton lessThan3Radio = new RadioButton("less than 3");
		playFrequencyPane.getChildren().add(lessThan3Radio);
		RadioButton lessThan5Radio = new RadioButton("less than 5");
		playFrequencyPane.getChildren().add(lessThan5Radio);
		RadioButton allRadio = new RadioButton("All");
		allRadio.setSelected(true);

		playFrequencyPane.getChildren().add(allRadio);
		ToggleGroup playGroup = new ToggleGroup();
		unPlayedRadio.setToggleGroup(playGroup);
		lessThan3Radio.setToggleGroup(playGroup);
		lessThan5Radio.setToggleGroup(playGroup);
		allRadio.setToggleGroup(playGroup);

		HBox searchPane = new HBox(15);
		controlPane.getChildren().add(searchPane);
		Label serchLabel = new Label("search:");
		TextField searchTextField = new TextField();
		searchTextField.setPromptText("search regular expression");
		searchPane.getChildren().add(serchLabel);
		searchPane.getChildren().add(searchTextField);

		// date this orders the clips
		HBox datePane = new HBox(15);
		controlPane.getChildren().add(datePane);

		RadioButton mostRecentRadioButton = new RadioButton("Most Recent");
		datePane.getChildren().add(mostRecentRadioButton);

		RadioButton oldestRadioButton = new RadioButton("Oldest");
		datePane.getChildren().add(oldestRadioButton);

		RadioButton alphabeticalRadioButton = new RadioButton("alphabetical");
		datePane.getChildren().add(alphabeticalRadioButton);

		RadioButton randomRadioButton = new RadioButton("Random");
		randomRadioButton.setSelected(true);
		datePane.getChildren().add(randomRadioButton);

		ToggleGroup dateGroup = new ToggleGroup();
		mostRecentRadioButton.setToggleGroup(dateGroup);
		oldestRadioButton.setToggleGroup(dateGroup);
		randomRadioButton.setToggleGroup(dateGroup);
		alphabeticalRadioButton.setToggleGroup(dateGroup);

		GridPane dateRangeGrid = new GridPane();
		controlPane.getChildren().add(dateRangeGrid);

		Label dateFocus = new Label("date focus");
		Label dateRange = new Label("date range");
		Slider timeSelector = new Slider(10, 100, 50);
		Slider timeVariance = new Slider(5, 100, 100);

		dateRangeGrid.add(dateFocus, 0, 0, 1, 1);
		dateRangeGrid.add(dateRange, 1, 0, 1, 1);
		dateRangeGrid.add(timeSelector, 0, 1, 1, 1);
		dateRangeGrid.add(timeVariance, 1, 1, 1, 1);

		infoPane = new TextArea();
		infoPane.setPrefRowCount(15);
		controlPane.getChildren().add(infoPane);
		infoPane.setText(textToShow);

		// actions
		playButton.setOnAction(new EventHandler<ActionEvent>() {
			private int numberToInitiallyPlay;

			@Override
			public void handle(ActionEvent event) {
				// config.filterUnplayableVideos();
				vp = new VideoPreference();
				vp.andTags = false;
				vp.tags = new ArrayList<String>();
				vp.directories = new ArrayList<String>();
				// tags
				for (CheckBox tag : tagCheckBoxes) {
					if (tag.isSelected())
						if (tag.getText().equalsIgnoreCase("and")) {
							vp.andTags = true;
						}
						else
							vp.tags.add(tag.getText());
				}
				// dirs
				for (CheckBox tag : dirCheckBoxes) {
					if (tag.isSelected())
						vp.directories.add(tag.getText());
				}
				vp.equalRating = exactRating.isSelected();
				vp.onlyUnrated = unratedCheckBox.isSelected();
				if (ratingLevelsBox.getValue() != null) {
					if( ratingLevelsBox.getValue().equals("all"))
						vp.rating = null;
					else
						vp.rating = Integer.parseInt(ratingLevelsBox.getValue());
				}
				// number of clips
				String numClipsS = numClipsComboBox.getValue();
				numberToInitiallyPlay = 20;
				if (!(numClipsS == null))
					try {
					numberToInitiallyPlay = Integer.parseInt(numClipsS);
					}
					catch(NumberFormatException nfe) {
						numberToInitiallyPlay = 20;
					}

				// plays
				vp.plays = null;
				if (unPlayedRadio.isSelected())
					vp.plays = 0;
				else if (lessThan3Radio.isSelected())
					vp.plays = 3;
				else if (lessThan5Radio.isSelected())
					vp.plays = 5;

				vp.mostRecent = mostRecentRadioButton.isSelected();
				vp.oldest = oldestRadioButton.isSelected();
				vp.random = randomRadioButton.isSelected();
				vp.alphabetical = alphabeticalRadioButton.isSelected();
				vp.focusDate = timeSelector.getValue();
				vp.rangeDate = timeVariance.getValue();
				vp.searchString = searchTextField.getText().toLowerCase();

				try {
					Pattern p = Pattern.compile(vp.searchString);
				} catch (PatternSyntaxException e) {
					infoPane.appendText("Invalid search pattern \n");
					return;
				}
				List<Video>  vidsToSearch = playList.stream().filter( vid -> config.directories.contains(vid.getDirectory())).collect(Collectors.toList()); ;
				List<Video> matchingClips = vp.getAllPlayableClips(playList, infoPane);
				String msg = matchingClips.size() + " Matching videos \n";

				Utility.msg( infoPane, msg);

				List<Video> orderedMatchingClips = vp.orderClips(matchingClips);

				if (matchingClips.size() > 0) {
					primaryStage.hide();

					new Player(orderedMatchingClips, config, numberToInitiallyPlay, infoPane, primaryStage);
				}
			}
		});

		convertButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {


				vp = new VideoPreference();
				vp.andTags = false;
				vp.tags = new ArrayList<String>();
				vp.directories = new ArrayList<String>();
				for (CheckBox tag : dirCheckBoxes) {
					if (tag.isSelected())
						vp.directories.add(tag.getText());
				}
				vp.mostRecent = mostRecentRadioButton.isSelected();
				vp.oldest = oldestRadioButton.isSelected();
				vp.random = randomRadioButton.isSelected();
				vp.alphabetical = alphabeticalRadioButton.isSelected();
				vp.focusDate = timeSelector.getValue();
				vp.rangeDate = timeVariance.getValue();
				vp.searchString = searchTextField.getText().toLowerCase();
				List<Video> matchingClips = vp.getAllUnPlayableClips(playList, infoPane);

				String msg = matchingClips.size() + " Matching videos for conversion \n";
				Utility.msg(analysisText, msg);
				String numClipsS = numClipsComboBox.getValue();
				int numberToConvert = 5;
				if (!(numClipsS == null))
					numberToConvert = Integer.parseInt(numClipsS);
				List<Video> orderedMatchingClips = vp.orderClips(matchingClips);
				orderedMatchingClips = orderedMatchingClips.subList(0,
						Integer.min(numberToConvert, orderedMatchingClips.size()));
				
				
				for (Video v : orderedMatchingClips) {
					
					 MediaConverter.convert(v, infoPane);
					 config.serializeDB(config.configFileName, infoPane);

				}

			}
		});

		analyzeFiles.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				config.analyze(analysisText);

			}
		});

		scanDirs.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				config.scanDirectories(analysisText);

			}
		});

		updateConfig.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				config.serializeDB(configFilename, analysisText);
			}
		});

		trashDeleted.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				config.trashFilesMarkedForDeletion(analysisText);
			}
		});
		newFiles.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				new FileMover(config, analysisText);
			}
		});



		StackPane root = new StackPane();
		root.getChildren().add(tabPane);

		Scene scene = new Scene(root, 1000, 1000);

		primaryStage.setTitle("Video Player");
		primaryStage.setScene(scene);
		primaryStage.show();

	}

	public void stop() {
		System.out.println("Stop called");
	}

}
