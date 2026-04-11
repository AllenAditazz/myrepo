package allen;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import static java.nio.file.StandardCopyOption.*;

import java.awt.Desktop;

import javafx.scene.control.TextArea;
import javafx.scene.media.Media;
import javafx.scene.media.MediaException;


public class FileMover {



	Config config;

	public FileMover(Config configArg, TextArea msg) {
		config = configArg;
		config.analyze(msg);
		Desktop  desktop = Desktop.getDesktop();
		String baseDirName = config.importBaseDirectory;


		Path newFileDir = Paths.get(baseDirName, "comp");
		if (! Files.exists(newFileDir)) {
			Utility.msg(msg,"comp does not exist");
			return;
		}

		Path destinationDir = Paths.get(baseDirName, "vids");

		if (!Files.exists(destinationDir)) {
			Utility.msg(msg,"vids does not exist");
			return;
		}

		
		Stream<Path> pathStream = null;
		try {
			pathStream = Files.find(newFileDir, Integer.MAX_VALUE, 
					(p, bfa) -> Utility.isMediaFileType(p.getFileName()));
		} catch (IOException e) {
			Utility.msg(msg,"I/O error visiting comp directory. " + e.getMessage());
			return;
		}

		List<Path> newMediaFiles = pathStream.collect(Collectors.toList());
		Utility.msg(msg, "New media Files");
		newMediaFiles.stream().forEach(p -> Utility.msg(msg, p.toString()));
		fileLoop: for (Path p : newMediaFiles) {
			
			//determine if this file is a dup (same file name and length of existing)
			//if so toss
			//if not possibly rename if an alias exists in the destination directory
			
			
			File mediaFile = p.toFile();
			List<Video> dups = config.vidIndex.get(p.getFileName().toString());
			if(dups != null) {
				
				long  newFileLength = mediaFile.length();
				
		
				for(Video vid : dups) {
					File oldFile = new  File(vid.getDirectory(), vid.fileName);
					long oldFileLength = oldFile.length();
		
					if(newFileLength == oldFileLength ) {
						Utility.msg(msg, p.toString() + " already exists (with same length). New file moved to trash.");
						desktop.moveToTrash(mediaFile);
						continue fileLoop;	
					}
					else if (vid.getDirectory().equals(destinationDir.toString())) { //attempt rename by adding _1 to file name keep in new file dir
						String s = p.getFileName().toString();
						int i = s.lastIndexOf(".");
						String fn = s.substring(0, i) +  "_1" + s.substring(i);
						Path newPath = Paths.get(p.getParent().toString(), fn);
						try {
							Files.move(p, newPath);  //rename file in new file dir (comp) 
						} catch (IOException e) {
							Utility.msg(msg, p.getFileName().toString() + " already exists in target directory, but renamed file could not be written", e.getMessage());
						}
						Utility.msg(msg, p.toString() + " already exists in target directory.");
						
						mediaFile = newPath.toFile();
						p = newPath;  
						
					}
				}		
			}
			
			//now do the move, if file is not supported by fx add tag BAD_CODE
			//mediaFile is file object in comp
			//p is path to comp

			
				try {
					Path destinationPath  =  Paths.get(destinationDir.toString(), p.getFileName().toString());
					Files.move(p, destinationPath, REPLACE_EXISTING);
					boolean isPlaybale = Utility.isFXSupportedMedia(destinationPath, msg);
					Video vid =  isPlaybale ? new Video(destinationPath) :   new Video(destinationPath, Config.BAD_CODEC);
					List<Video>  newVids = new ArrayList<Video>();
					newVids.add(vid);
					config.videos.add(vid);
					config.vidIndex.put(p.getFileName().toString(), newVids);
					Utility.msg(msg, " ADDED to index ", vid.toString());
				} catch (IOException e) {
					Utility.msg(msg, "couldn't move file to vids" + p.toString());
		
			}

		}
		Collections.sort(config.videos, new Comparator<Video>() {
			@Override
			public int compare(Video v1, Video v2) {
				if (v1.date >= v2.date)
					return -1;
				else
					return 1;
			}
		});
		
		config.serializeDB(App.configFilename, msg);
	}


}
