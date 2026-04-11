package allen;

import java.io.File;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;



public class Video {
	

	
	public String fileName;
	public List<String> tags;	
	private String directory;
	public Integer rating;
	public int plays;
	public Long date;
	public String fileExtn ;
	public  boolean markedForDeletion;
	public String codecInfo;
	
	
	
	public Video() {}
	public Video(String fn, String dirName, long dte) {
		fileName = fn;
		setDirectory(dirName);
		date = dte;
		rating = null;
		tags = new ArrayList<String>();
		markedForDeletion = false;
		int i = fn.lastIndexOf(".");
		if(i != -1)
			fileExtn = fn.substring(i+1);

	}
	
	protected Video(Path p, String ...  tagsArg) { 
		String pString = p.toString();
		fileExtn = Utility.getExtension(pString);
		setDirectory(p.getParent().toString());
		fileName = p.getFileName().toString();
		File f = new File(Utility.getModifiedPath(pString));
		date = f.lastModified();
		tags = Arrays.asList(tagsArg);

	}
		

	@Override
	public String toString() {
		return "Video [fileName=" + fileName + ", directory=" + getDirectory() + ", tags=" + tags.toString() + "]";
		//use dateformat to add date\
	}
	public void addTag(String newTag) {
		if (! tags.contains(newTag))
			tags.add(newTag);	
		
	}
	
	public void removeTag(String tag) {
			tags.remove(tag);	
		
	}
	public String getDirectory() {
		return directory;
	}
	public void setDirectory(String directory) {
		this.directory = directory;
	}

}
