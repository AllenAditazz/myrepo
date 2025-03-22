package allen;

import java.awt.Desktop;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import javafx.scene.control.TextArea;

public class Config {
	
//	public static String fileSubstitutionPrefix = null;


	public List<String> tags;
	public List<TagHierarchy> tagHierarchy;
	

	public List<String> directories;
	public List<Video> videos;
	public String picDirectory;
	public String importBaseDirectory;
	protected String copyDirectory;

	
	protected static final  String DUP = "dup";
	
	protected static final  String BAD_CODEC = "badCodec";
	
	protected static final  String CONVERTED = "converted";
	

	protected static final List<String> SPECIAL_TAGS = Arrays.asList(BAD_CODEC, DUP, CONVERTED);
	
	private Desktop  desktop = Desktop.getDesktop();

	private int unrated = 0;
	private int tagged = 0;
	private int badExtn;
	private int numVids;
	private boolean nullExtn = false;
	
	protected String configFileName;  //set when parsing

	
	private List<Video> unsupportedVideos;
	private List<Video> nonVideos;
	private List<Video> markedForDeletion;
	private List<Video> dupsInVideoList;
	private List<Video> deletedOrMoved;
	private List<Video> requireTranscoding;
	private List<String>  namesOfDupVideos;
	private  Map<String, Integer> videoCountByDir = new HashMap<String, Integer>();
	


	protected Map<String, List<Video>> vidIndex = new HashMap<String, List<Video>>();



	private Map<Integer, Integer> ratingToCount = new HashMap<Integer, Integer>();
	
	

//	public List<String> getModifiedDirectories() {
//		if(fileSubstitutionPrefix == null) {
//			return directories.stream().map(st -> subDir(st)).toList();
//		}
//		return directories;
//	}
	


	public void setDirectories(List<String> directories) {
		this.directories = directories;
	}

	public static Config parseDB(String filename) {
		
		FileReader input = null; 
		try {
			input = new FileReader(new File(filename)); 
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		Yaml yaml = new Yaml(new Constructor(Config.class));
		Config config = (Config) yaml.load(input);
		try {
			input.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (config.videos == null)
			config.videos = new ArrayList<Video>();
		config.configFileName = filename;
		return config;
	}

	void analyze(TextArea msgArea) {

		
		unsupportedVideos = new ArrayList<Video>();
		nonVideos = new ArrayList<Video>();
		markedForDeletion = new ArrayList<Video>();
		//these are duplicate entries that should be merged
		dupsInVideoList  = new ArrayList<Video>();
		//these are the names of duplicate videos (same name and size)
		namesOfDupVideos  = new ArrayList<String>();
		requireTranscoding = new ArrayList<Video>();

		
		vidIndex = new HashMap<String, List<Video>>();
		deletedOrMoved =  new ArrayList<Video>();
		ratingToCount = new HashMap<Integer, Integer>();

		unrated = 0;
		tagged = 0;
		badExtn = 0;
		

	

		for (int i = 1; i < 4; i++)
			ratingToCount.put(i, 0);
		
//		int removedDup = 0;
		for (Video vid : videos) {
			
			File f = new File(vid.getDirectory(), vid.fileName);
			File d = new File(vid.getDirectory());
			if (!f.exists()  && d.exists()) {  //sometimes the removable volume is not mounted
				deletedOrMoved.add(vid);
			}
			
			if(videoCountByDir.get(vid.getDirectory()) == null)
				videoCountByDir.put(vid.getDirectory(), 1);
			else
				videoCountByDir.put(vid.getDirectory(), videoCountByDir.get(vid.getDirectory()) + 1 );
			
			
			
	
//			if (vid.tags.contains(DUP)) {
//				vid.tags.remove(DUP);
//				removedDup++;
//				Utility.msg(msgArea, "removed dup tag" , vid.toString());
//				
//			}

			
//set codecInfo to null
			
			
			
			
			
			if (vid.fileExtn == null) {
				vid.fileExtn = Utility.getExtension(vid.fileName);
				nullExtn = false;
			}

			if (vidIndex.containsKey(vid.fileName)) {
				
				List<Video> vidsInIndex = vidIndex.get(vid.fileName);
				
				boolean isdupEntry = vidsInIndex.stream().anyMatch((v -> v.getDirectory().equals(vid.getDirectory())));
				if(isdupEntry) dupsInVideoList.add(vid);
				
				File newFile = new  File(vid.getDirectory(), vid.fileName);
				long newFileLength = newFile.length();
				
				
				for(Video v : vidsInIndex) {
					File oldFile = new  File(v.getDirectory(), v.fileName);
					long oldFileLength = oldFile.length();
					
					if(newFileLength == oldFileLength  && newFileLength != 0) {	
						namesOfDupVideos.add(vid.fileName);
					}				
				}
				vidIndex.get(vid.fileName).add(vid);
			}
				
	

			else {
					
			vidIndex.put(vid.fileName, new ArrayList<Video>());
			vidIndex.get(vid.fileName).add(vid);

			}


			if (vid.markedForDeletion)
				markedForDeletion.add(vid);
			
			if(vid.tags.stream().anyMatch( t -> t.equals(BAD_CODEC)))
					requireTranscoding.add(vid);

			if (vid.rating == null || vid.rating == 0)
				unrated++;
			else {
				ratingToCount.put(vid.rating, ratingToCount.get(vid.rating) + 1);
			}

			if (vid.tags == null || vid.tags.size() > 0)
				tagged++;

			if (! Utility.FX_SUPPORTED_MEDIA_EXTENSIONS.contains(vid.fileExtn.toLowerCase()))
				unsupportedVideos.add(vid);

			if (!  Utility.MEDIA_EXTENSIONS.contains(vid.fileExtn.toLowerCase())) {
				nonVideos.add(vid);
			}
		}

		
//		Utility.msg(msgArea, "removed dup tags from videos " +  removedDup);
		Utility.msg(msgArea,"number od deleted or moved files is  " + deletedOrMoved.size());

		
		
		
		for(Video v : deletedOrMoved)
			Utility.msg(msgArea, "deleted or moved" , v.toString());
		
		Utility.msg(msgArea, "Number of videos requiring transcoding: " + requireTranscoding.size());
//		for(Video v : requireTranscoding) {
//			Utility.msg(msgArea, "requireTranscoding" , v.toString());
//		}
		
		
		Utility.msg(msgArea,"number of duplicate entries  (same name and dir) in video list: " + dupsInVideoList.size());
		
		Utility.msg(msgArea,"Files marked for deletion: " + markedForDeletion.size());
		markedForDeletion.stream().forEach( v -> Utility.msg(msgArea, v.toString()));
		
		for (Entry<Integer, Integer> i : ratingToCount.entrySet())
			Utility.msg(msgArea, "rating: " + i.getKey() + " videos " + i.getValue());
		for(Video nv : nonVideos)
			Utility.msg(msgArea, "Found file with non-video extn: "  + nv.fileName + " in " + nv.getDirectory());
		for(Video v : dupsInVideoList)
			Utility.msg(msgArea, "Found multiple index entries for video : ", v.toString());
		for(String vidName : namesOfDupVideos) {
			Utility.msg(msgArea, "video has duplicates:", vidName);
		
			
		}
		for(Entry<String, Integer>   vEntry : videoCountByDir.entrySet()) {
			Utility.msg(msgArea, vEntry.getKey() + "    " + vEntry.getValue());
		}
		Utility.msg(msgArea, "videos by directory " + videoCountByDir.toString());
		
		Utility.msg(msgArea, "number of videos: " + videos.size());
		Utility.msg(msgArea, "number of rated videos: " + (videos.size() - unrated));
		

		Utility.msg(msgArea, "number of unrated videos: " + unrated);
		Utility.msg(msgArea, "number of unsupported videos: " + unsupportedVideos.size() );
		
		
		Utility.msg(msgArea, "number of non videos: " + nonVideos.size());
		
	
		Utility.msg(msgArea, "Number of duplicate videos : " + namesOfDupVideos.size());

		Utility.msg(msgArea, "Number of deleted/moved videos: " + deletedOrMoved.size());
		

		

	}
	

	
	void trashFilesMarkedForDeletion(TextArea ta) {
		analyze(ta);
		if(markedForDeletion.size() == 0)
			Utility.msg(ta,  "No  files  marked for deletion to trash" );
		for (Video v : markedForDeletion) {
			File f = new File(v.getDirectory(), v.fileName);
			if (f.exists()) {
				boolean wasDeleted = desktop.moveToTrash(f);
				if (wasDeleted) {
					Utility.msg(ta,  "Moving to system TRASH", f.toString() );
				}
				else {
					Utility.msg(ta,  "Failed  to move to system TRASH just deleting (soon)", f.toString() );
					f.delete();
				}
		
			} else {
				Utility.msg(ta, "file for video marked deletion no longer exists",  v.toString());
			}
		}

		videos.removeAll(markedForDeletion);
		markedForDeletion.clear();
	}
	
	void trashNonMediaFiles(TextArea ta) {
		analyze(ta);
		if(nonVideos.size() == 0)
			Utility.msg(ta,  "No non-media files to trash" );
		for (Video v : nonVideos) {
			File f = new File(v.getDirectory(), v.fileName);
			if (f.exists()) {
				desktop.moveToTrash(f);
				Utility.msg(ta,  "Moving to system TRASH non-video:", f.toString() );
		
			} else {
				Utility.msg(ta, " Non-media file f to be deleted does not exist",  v.toString());
			}
		}

		videos.removeAll(nonVideos);
		nonVideos.clear();
	}
	

//	void  trashDuplicateFiles(TextArea ta) {
//		analyze(ta);
	
	
//		for(Map.Entry<String,List<Video>>   fnList : dupIndex.entrySet() ) {
//			String name = fnList.getKey();
//			List<Video> dupVideos = fnList.getValue();
//			
//			desktop.moveToTrash(f);
//			ta.appendText("moving to trash file: " + f.toString());
//		}
//		videos.removeAll(markedForDeletion);
//		markedForDeletion.clear();
//	}

//	private void filterUnplayableVideos() {
//		videos = videos.stream().filter(v -> !EXCLUSIONS_MEDIA.contains(v.fileExtn.toLowerCase()))
//				.filter(v -> !EXCLUSIONS_OTHER.contains(v.fileExtn.toLowerCase())).collect(Collectors.toList());
//	}

	public void scanDirectories(TextArea ta) {

		analyze(ta);

		for (String dirS : directories) {
			File dirF = new File(dirS);
			if (!dirF.exists() || !dirF.isDirectory()) {
				Utility.msg(ta, dirS + " is not a directory");
				continue;
			}

			Utility.msg(ta, "scanning " + dirS + " ... ");
			File[] files = dirF.listFiles();
			if(files == null) {
				Utility.msg(ta,  dirS + " cannot be scanned ");
				deletedOrMoved = deletedOrMoved.stream().filter(v -> ! v.getDirectory().equals(dirS)).toList();
				continue;
			}
			for (File file : files) {
				if (file.isHidden() || file.isDirectory())
					continue;

				String fileName = file.getName();
				if (!Utility.MEDIA_EXTENSIONS.contains(Utility.getExtension(fileName))) {
					Utility.msg(ta, fileName + " is not a media file");
	
				}

				List<Video> vids = vidIndex.get(fileName);
				if (vids == null) {
					long date = file.lastModified();
					Video vid = new Video(fileName, dirS, date); // adds extn
					videos.add(vid);
					List<Video> newVid = new ArrayList<Video>();
					newVid.add(vid);
					vidIndex.put(fileName, newVid);
					Utility.msg(ta, " ADDED to index ", vid.toString());
				} else { // file name is in index

					if (vids.stream().anyMatch(v -> v.getDirectory().equals(dirS)))
						// entry for video found, normal case
						continue;

					// maybe it is a file that was moved from one dir to another
					Optional<Video> found = deletedOrMoved.stream().filter(v -> v.fileName.equals(fileName)).findAny();
					if (found.isPresent()) {
						Video vid = found.get();
						vid.setDirectory(dirS);
						Utility.msg(ta, vid.fileName + " MOVED from " + vid.getDirectory() + " to" + dirS,
								vid.toString());
						deletedOrMoved.remove(vid);
					}

					else { // it's a file not seen before, with the same name as an existing clip
							// it's either a new video or a duplicate (same name and length)

						long date = file.lastModified();
						Video dupVid = new Video(fileName, dirS, date); // adds extn

						long newFileLength = file.length();

						for (Video vid : vids) {
							File oldFile = new File(vid.getDirectory(), vid.fileName);
							long oldFileLength = oldFile.length();
							if (newFileLength == oldFileLength) {
								desktop.moveToTrash(file);
								Utility.msg(ta, "DUPLICATE moved to trash ", vid.toString(), dupVid.toString());
							} else {
								videos.add(dupVid);
								Utility.msg(ta, "added but DUPLICATE name found", vid.toString(), dupVid.toString());

							}

						}

					}

				}

			}
		}

		// after scanning all dirs we remove entries in videos that no longer correspond
		// to a file
		for (Video v : deletedOrMoved) {
			Utility.msg(ta, "deleted entry removed from index", v.toString());

		}
		videos.removeAll(deletedOrMoved);

		Collections.sort(videos, new Comparator<Video>() {
			@Override
			public int compare(Video v1, Video v2) {
				if (v1.date >= v2.date)
					return -1;
				else
					return 1;
			}
		});

		Utility.msg(ta, "scan complete");
	}
	
	private boolean areLikelyDuplicates(Video v1, Video v2) {
		
		return false;
	}


	public void serializeDB(String filename, TextArea ta) {
		Utility.msg(ta, "saving: " + filename );
		FileReader fis = null;
		FileWriter fos = null;
		try {
			fis = new FileReader(filename);
			fos = new FileWriter(filename + ".bak");
			int k;
			while ((k = fis.read()) != -1)
				fos.write(k);
			fis.close();
			fos.close();

		} catch (IOException e1) {
				Utility.msg(ta,  e1.getMessage() + " could not save .bak file");
		}
		
		
		DumperOptions dOpt = new DumperOptions();
		dOpt.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK); //we can switch to AUTO later
		
		Yaml yaml = new Yaml(dOpt);
		String output = yaml.dump(this);
		try {
			File file = new File(filename);
			FileWriter fileWriter = new FileWriter(file);
			fileWriter.write(output);
			fileWriter.flush();
			fileWriter.close();
		} catch (IOException e) {
			Utility.msg(ta,  e.getMessage() + " could not save .bak file");
		}
	}
	



}
