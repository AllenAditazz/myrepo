package allen;

import java.awt.Desktop;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ProcessBuilder.Redirect;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Pane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaException;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;

public class MediaConverter {

	static Desktop desktop = Desktop.getDesktop();



	static protected void convert(Video v, TextArea msg) {
		

		String fold = Utility.getFullPath(v).toString();
		String foldModified = Utility.getModifiedPath(fold);
		String fnew = Utility.setExtnToMp4(fold);
		String fnewModified = Utility.getModifiedPath(fnew);
		String extn = Utility.getExtension(fold);

		String output = "";

		if (extn.equalsIgnoreCase("mkv")) {// first try to repackage container
			output = convertFile(foldModified, fnewModified, msg, false);
			File out = new File(fnewModified);
			
			
			
			
			//if (output.startsWith("FAIL") || output.indexOf("Video: hevc (Main)") > 0)
			if (output.startsWith("FAIL") ||  out.length() == 0  ||  ! Utility.isFXSupportedMedia(Path.of(fnewModified), msg))
				output = convertFile(foldModified, fnewModified, msg, true);
		}

		else {
			if(extn.equalsIgnoreCase("mp4")) {
				int i =fnew.lastIndexOf(".");
				fnewModified = fnewModified.substring(0,i) + "_.mp4";
			}
			output = convertFile(foldModified, fnewModified, msg, true);
		}

		File out = new File(fnewModified);
		if (out.length() > 0 && ! output.startsWith("FAIL") &&  Utility.isFXSupportedMedia(Path.of(fnew), msg) ) {
			Utility.msg(msg, "Transcoding succeeded on vid", v.toString());
			v.addTag(Config.CONVERTED);
			v.removeTag(Config.BAD_CODEC);

			v.fileName = fnewModified;
			v.fileExtn = "mp4";


			File f = new File(foldModified);
			desktop.moveToTrash(f);
			
		}

		else {
			Utility.msg(msg, "Transcoding FAILED on vid", v.toString());
			if (out.length() > 0)
				desktop.moveToTrash(out);
				
		}

	

	}

	static private String convertFile(String inputFile, String outputFile, TextArea msg, boolean transcode) {

		String ffCommandCopy = "/Users/atg/ffmpeg -y  -hide_banner  -copy_unknown  -i  \"" + inputFile
				+ "\"  -c copy    \"" + outputFile + "\"";

		String ffCommandtranscode = "/Users/atg/ffmpeg -y  -hide_banner     -i  \"" + inputFile
				+ "\"  -c:v h264  -c:a aac    \"" + outputFile + "\"";

		

		String shellscript = transcode ? ffCommandtranscode : ffCommandCopy;

		Utility.msg(msg, transcode ? "transcoding file" : "muxing file" + inputFile);

		Process process = null;
		String output = null;
		int returnCode = 0;
		try {
			File file = new File("shellScript.sh");
			FileWriter fileWriter = new FileWriter(file);
			fileWriter.write(shellscript);
			fileWriter.flush();
			fileWriter.close();
		} catch (IOException e) {
			Utility.msg(msg, e.getMessage() + "could not write shellscript");
		}

		try {
			process = Runtime.getRuntime().exec("./shellscript.sh");
		} catch (IOException e2) {
			Utility.msg(msg, "could not create process to run trancode shellscript");
			e2.printStackTrace();
		}

		try {
			output = (transcode ? "transcoded " : "muxed ") + new String(process.getErrorStream().readAllBytes());
		} catch (IOException e1) {
			Utility.msg(msg, "I/O error reading process stream");
			e1.printStackTrace();
		}

		try {
			returnCode = process.waitFor();
		} catch (InterruptedException e) {
			Utility.msg(msg, e.getMessage() + "shellscript interrupted");
			e.printStackTrace();
			return output;
		}

		output = shellscript + trim(output) + "\n\n\n";

		Utility.msg(msg, "ff output:", output);

		if (returnCode > 0) {
			Utility.msg(msg, "ffmpeg transcode/mux failed on " + inputFile);
			output = "FAIL " + output;
		} else
			Utility.msg(msg, "ffmpeg succeded on " + inputFile);

		try {
			Path convertLog = Paths.get("convert.txt");
			Files.write(convertLog, output.getBytes(), StandardOpenOption.APPEND);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return output;

	}

	static private String trim(String out) {

		int ind = out.indexOf("frame=");
		if (ind > 0)
			return out.substring(0, ind);
		else
			return out.lines().limit(25).reduce("", (a, b) -> a + b);

	}

	static private void copy(InputStream in, OutputStream out) throws IOException {
		while (true) {
			int c = in.read();
			if (c == -1)
				break;
			out.write((char) c);
		}
	}

}
