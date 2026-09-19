package allen;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import javafx.scene.control.TextArea;

import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.media.Media;
import uk.co.caprica.vlcj.media.MediaParsedStatus;

public class Utility {

	protected static final List<String> MEDIA_EXTENSIONS = Arrays.asList("wmv", "mpg", "mp4", "flv", "fxm", "avi", "mov", "asf", "mpeg",
			"mkv", "m4v");

	protected static final  List<String> FX_SUPPORTED_MEDIA_EXTENSIONS = Arrays.asList("mp4", "mp4v",  "flv", "fxm");

	// how long to let libvlc try to parse a file before giving up on it
	private static final int PARSE_TIMEOUT_MS = 5000;

	// One libvlc-backed factory, shared for the process lifetime, used only to
	// probe whether a file can be understood by libvlc. No player/video surface
	// needed for this - just parsing.
	private static MediaPlayerFactory mediaPlayerFactory;

	private static synchronized MediaPlayerFactory mediaPlayerFactory() {
		if (mediaPlayerFactory == null) {
			mediaPlayerFactory = new MediaPlayerFactory();
		}
		return mediaPlayerFactory;
	}

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


	/**
	 * Whether libvlc can make sense of this file: asks it to parse the file
	 * (which requires it to at least identify a container and its tracks,
	 * without actually decoding/playing anything) and waits for a result.
	 */
	static  protected boolean isSupportedMedia(Path fn, TextArea msg) {

		String mrl = fn.toAbsolutePath().toString();
		Media media = mediaPlayerFactory().media().newMedia(mrl);
		try {
			media.parsing().parse(PARSE_TIMEOUT_MS);

			MediaParsedStatus status = media.parsing().status();
			long deadline = System.currentTimeMillis() + PARSE_TIMEOUT_MS + 1000;
			while (status != MediaParsedStatus.DONE
					&& status != MediaParsedStatus.FAILED
					&& status != MediaParsedStatus.TIMEOUT
					&& status != MediaParsedStatus.SKIPPED
					&& System.currentTimeMillis() < deadline) {
				try {
					Thread.sleep(25);
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					break;
				}
				status = media.parsing().status();
			}

			boolean supported = status == MediaParsedStatus.DONE;
			if (!supported) {
				Utility.msg(msg, "unsupported media " + fn.toString() + " parse status: " + status);
			}
			return supported;
		} finally {
			media.release();
		}
	}

	static  protected boolean isSupportedMedia(Video v, TextArea msg) {
		Path p = Paths.get(v.getDirectory(), v.fileName);
		return isSupportedMedia(p,msg);
	}

	static  protected boolean isSupportedMedia(File f, TextArea msg) {
		Path p = f.toPath();
		return isSupportedMedia(p,  msg);

	}





}
