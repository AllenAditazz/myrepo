package allen;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import javafx.scene.control.TextArea;

public class Utility {

	// Which files a directory scan should treat as video. libvlc will play far
	// more than this and needs no allow-list of its own, but a scan still has to
	// tell videos apart from the text files and images sitting beside them, so
	// this stays - deliberately broad rather than absent.
	protected static final List<String> MEDIA_EXTENSIONS = Arrays.asList(
			"3gp", "3g2", "asf", "avi", "divx", "dv", "f4v", "flv", "fxm", "m2ts", "m2v",
			"m4v", "mkv", "mov", "mp4", "mp4v", "mpeg", "mpg", "mpv", "mts", "mxf", "ogm",
			"ogv", "rm", "rmvb", "ts", "vob", "webm", "wmv", "wtv");

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
		return st;
	}


	static protected  String getExtension(String fn) {
		int i =fn.lastIndexOf(".");
		if (i != -1)
			return fn.substring(i + 1);
		else
			return  "";

	}

	static  protected boolean isMediaFileType(Path fn) {
		String extn = getExtension(fn.toString().toLowerCase());
		return MEDIA_EXTENSIONS.contains(extn);

	}

}
