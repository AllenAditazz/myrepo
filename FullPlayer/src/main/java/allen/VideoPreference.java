package allen;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.regex.PatternSyntaxException;

import javafx.scene.control.TextArea;

public class VideoPreference {

	public List<String> tags;
	public List<String> directories;
	public Integer rating = null; // null means no rating
	public boolean equalRating;
	public boolean mostRecent;
	public boolean oldest;
	public boolean random;
	public boolean alphabetical;
	public boolean onlyUnrated;
	public boolean andTags;
	public Integer plays; // null means any number of plays
	public Double focusDate;
	public Double rangeDate;
	public String searchString;
	




	
	boolean matches(Video v, boolean supportedP) {
		if( supportedP && ! Utility.FX_SUPPORTED_MEDIA_EXTENSIONS.contains(v.fileExtn))
				return false;
		

		//location
		if (!directories.isEmpty()) {
			if (!directories.contains(v.getDirectory()))
				return false;
		}
		//rating
		if (onlyUnrated) {
			if (v.rating != null)
				return false;
		} else {
			
			if (rating != null) {
				if (v.rating == null || (v.rating < rating) || (v.rating > rating && equalRating))
					return false;
			}
		}
		
		//number of plays
		if(plays != null && v.plays > plays)
				return false;
		
		
		//tags
		if ( tags.size() > 0) {

			boolean hasTag = false;
			
		if (andTags	) {
			if(! v.tags.containsAll(tags))
				return false;
			
		}
		else {
			
				for (String t : tags) {
					if (v.tags.contains(t)) {
						hasTag = true;
						break;
					}
				}
				if (!hasTag)
					return false;
			}
		}
		
		if(searchString.length() > 0 && ! v.fileName.toLowerCase().matches(searchString))
			return false;
		
		
		if(supportedP  &&  ! tags.contains(Config.BAD_CODEC)  && v.tags.contains(Config.BAD_CODEC ))
			return false;
		
		
		if( ! supportedP &&  ! v.tags.contains(Config.BAD_CODEC ))
			return false;
		

	

		return true;
	}
	
	public List<Video> getAllPlayableClips(List<Video> playList, TextArea msg) {
		return getAllMatchingClips(playList, true, msg);
	}
	
	public List<Video> getAllUnPlayableClips(List<Video> playList, TextArea msg) {
		return getAllMatchingClips(playList, false, msg);
	}
 
	public List<Video> getAllMatchingClips(List<Video> playList, boolean supportedP, TextArea msg) {

		List<Video> matchingVideos = new ArrayList<Video>();
		int middle = (int) (playList.size() * focusDate/100.0);
		int range = (int) (playList.size() * rangeDate / 200.0);
		int low = Integer.max(0, middle - range);
		int high = Integer.min(playList.size() - 1, middle + range);

		SimpleDateFormat df =   new SimpleDateFormat("MM/dd/yyyy");
		
		Utility.msg(msg, "Date Range " + df.format(new Date(playList.get(low).date)) + " -- " 
				+ df.format(new Date(playList.get(high).date)));

		
		for (int i = low; i <= high; i++) {
			if (matches(playList.get(i), supportedP))
				matchingVideos.add(playList.get(i));
		}
		return matchingVideos;

	}

	public List<Video> orderClips(List<Video> matchingClips) {
		if (oldest) { // sort so oldest is first (reverse)
			Collections.sort(matchingClips, new Comparator<Video>() {
				@Override
				public int compare(Video v1, Video v2) {
					if (v1.date <= v2.date)
						return -1;
					else
						return 1;
				}
			});
			return matchingClips;
		} else if (random) {
			List<Video> retval = new ArrayList<Video>();
			int[] perm = createPerm(matchingClips.size());
			for (int i = 0; i < matchingClips.size(); i++) {
				retval.add(matchingClips.get(perm[i]));
			}
			return retval;
		} else if (alphabetical) {
			Collections.sort(matchingClips, new Comparator<Video>() {
				@Override
				public int compare(Video v1, Video v2) {
					return v1.fileName.compareTo(v2.fileName);
				}
			});

			return matchingClips;
		}
		// newest
		return matchingClips;
	}

	private int[] createPerm(int n) {

		int[] a = new int[n];

		// insert integers 0..n-1
		for (int i = 0; i < n; i++)
			a[i] = i;

		// shuffle
		for (int i = 0; i < n; i++) {
			int r = (int) (Math.random() * (i + 1)); // int between 0 and i
			int swap = a[r];
			a[r] = a[i];
			a[i] = swap;
		}
		return a;
	}

}
