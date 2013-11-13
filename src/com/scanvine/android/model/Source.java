package com.scanvine.android.model;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.text.Html;
import android.util.Log;

public class Source {
	public static ArrayList<Source> GetSourcesFrom(String vals) {
		ArrayList<Source> retval = new ArrayList<Source>();
		if (vals==null)
			return retval;
		try {
			JSONObject json = new JSONObject(vals);
			JSONArray sources =  json.getJSONArray("sources");
			for (int i=0; i<sources.length(); i++) {
				Source source = new Source(sources.getJSONObject(i));
				retval.add(source);
			}
		}
		catch(JSONException ex) {
			Log.e("Source", ""+ex);
			ex.printStackTrace();
		}
		return retval;
	}

	public static Source GetSourceFor(String vals) {
		if (vals==null)
			return null;
		try {
			JSONObject json = new JSONObject(vals);
			return new Source(json);
		}
		catch(JSONException ex) {
			Log.e("Source", ""+ex);
			ex.printStackTrace();
			return null;
		}
	}
	
	private String raw_json;
	public String getJSON() { return raw_json; }

	private String name;
	public String getName() { return name; }
	public String toString() { return name; }

	private String slug;
	public String getSlug() { return slug; }

	private Integer score;
	public Integer getScore() { return score; }

	public Source(JSONObject json) {
		try {
			raw_json = json.toString();
			slug = json.getString("slug");
			score = Integer.valueOf(json.getString("average_score"));
			name = ""+Html.fromHtml(json.getString("name"));
		}
		catch(JSONException ex) {
			Log.e("Source", "JSON error from "+json);
			ex.printStackTrace();
		}
	}
}
