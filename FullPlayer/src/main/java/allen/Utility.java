package allen;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import javafx.scene.control.TextArea;
import javafx.scene.media.Media;
import javafx.scene.media.MediaException;

public class Utility {
	
	protected static final List<String> MEDIA_EXTENSIONS = Arrays.asList("wmv", "mpg", "mp4", "flv", "fxm", "avi", "mov", "asf", "mpeg",
			"mkv", "m4v");
	
	protected static final  List<String> FX_SUPPORTED_MEDIA_EXTENSIONS = Arrays.asList("mp4", "mp4v",  "flv", "fxm");
	
	
	static protected void msg (TextArea ta, String... msgs) {
		for(String s : msgs) {
			System.out.print(s + " \t " );
			if(ta != null) {
				//ta.appendText(s + " \t ");
			}
		}
		System.out.println();
		if(ta != null)
			ta.appendText( "\n");
	}
	
	static String getModifiedPath(String st) {
		if(Config.fileSubstitutionPrefix == null)
			return st;
		
		if(st.startsWith("/")) 
			return "/" + Config.fileSubstitutionPrefix + st.substring(st.indexOf("/", 1));

		return Config.fileSubstitutionPrefix + st.substring(st.indexOf("/"));
	}
	
	
	static protected  String getExtension(String fn) {
		int i =fn.lastIndexOf(".");
		if (i != -1)
			return fn.substring(i + 1);
		else
			return  "";
		
	}
	
	static protected  String setExtnToMp4(String fn) {
		return getBaseName(fn) + ".mp4";
		
	}
	
	//get the part of a simple filename before the extn  eg foo in foo.mp4
	static protected  String getBaseName(String fn) {
		int i =fn.lastIndexOf(".");
		if (i != -1)
			return fn.substring(0,i);
		else
			return  "";
		
	}
	
	static protected  Path getFullPath(Video v) {
		return Paths.get(v.getDirectory(), v.fileName);
	}
	

	
	static  protected boolean isMediaFileType(Path fn) {
		String extn = getExtension(fn.toString().toLowerCase());
		return MEDIA_EXTENSIONS.contains(extn);

	}
	

	static  protected boolean isFXSupportedMedia(Path fn, TextArea msg) {

		String url = null;
		try {
			url = fn.toUri().toURL().toString();
			new Media(url);
		} catch (MediaException e) {
			Utility.msg(msg, "unsupported media "  + fn.toString() + " type: " + e.getType());
			return false;
		} catch (MalformedURLException e) {
			Utility.msg(msg,"malformed URL " + url);
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	static  protected boolean isFXSupportedMedia(Video v, TextArea msg) {
		Path p = Paths.get(v.getDirectory(), v.fileName);
		return isFXSupportedMedia(p,msg);
	}
	
	static  protected boolean isFXSupportedMedia(File f, TextArea msg) {
		Path p = f.toPath();	
		return isFXSupportedMedia(p,  msg);

	}
	
	
	
	
	

}
