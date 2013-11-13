package com.scanvine.android.model;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.net.Uri;
import android.text.Html;
import android.util.Log;

public class Story {
	public static String FILE_PREFIX = "sv-dl-";

	public static ArrayList<Story> GetStoriesFrom(String vals) {
		ArrayList<Story> retval = new ArrayList<Story>();
		if (vals==null)
			return retval;
		try {
			JSONObject json = new JSONObject(vals);
			JSONArray stories =  json.getJSONArray("stories");
			for (int i=0; i<stories.length(); i++) {
				Story story = new Story(stories.getJSONObject(i));
				retval.add(story);
			}
		}
		catch(JSONException ex) {
			Log.e("Story", ""+ex);
			ex.printStackTrace();
		}
		return retval;
	}

	public static Story GetStoryFor(String vals) {
		if (vals==null)
			return null;
		try {
			JSONObject json = new JSONObject(vals);
			return new Story(json);
		}
		catch(JSONException ex) {
			Log.e("Story", ""+ex);
			ex.printStackTrace();
			return null;
		}
	}
	
	public static SimpleDateFormat dateParser = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US); 
	public static SimpleDateFormat dateFormatter = new SimpleDateFormat("MMM d", Locale.US); 
	
	private String raw_json;
	public String getJSON() { return raw_json; }

	private String id;
	public String getId() { return id; }
	public String getFilename() { return FILE_PREFIX+id; }

	private String title;
	public String getTitle() { return title; }
	public String toString() { return title; }
	public String getTitleLine() {
		if (rank==null)
			return ""+Html.fromHtml(""+title);
		else
			return ""+rank+". "+Html.fromHtml(""+title);
	}

	private String rank;
	public String getRank() { return rank; }

	private String blurb;
	public String getBlurb() { return blurb; }
	public String getSafeBlurb() { return ""+Html.fromHtml(""+blurb); }

	private String image;
	public String getImageURL() { return image; }

	private String url;
	public String getURL() { return url; }
	public String getHost() { return Uri.parse(url).getHost(); }
	
	private String sourceName;
	public String getSourceName() { return sourceName; }
	
	private Date timePublished;
	public Date getTimePublished() { return timePublished; }
	
	private String datePublished;
	public String getDatePublished() { return datePublished; }
	
	public String getByline() { return ""+sourceName+", "+datePublished; }
	
	private boolean clustered;
	public boolean isClustered() { return clustered; }

	public Story(JSONObject json) {
		try {
			raw_json = json.toString();
			id = json.getString("id");
			url = ""+Html.fromHtml(json.getString("url"));
			title = json.getString("title");
			blurb = json.getString("blurb");
			image = ""+Html.fromHtml(json.getString("image"));
			rank = json.getString("rank");
			clustered = json.has("clustered") && json.getBoolean("clustered");
			String timeString = json.getString("timePublished");
			try {
				timePublished = dateParser.parse(timeString);
				datePublished = dateFormatter.format(timePublished);
			}
			catch(ParseException ex) {
				timePublished = null;
				datePublished = "";
			}

			if (json.has("source")) {
				JSONObject source = json.getJSONObject("source");
				sourceName = source.getString("name");
			}
		}
		catch(JSONException ex) {
			Log.e("Story", "JSON error from "+json);
			ex.printStackTrace();
		}
	}
}
