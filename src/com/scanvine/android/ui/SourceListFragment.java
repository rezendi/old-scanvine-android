package com.scanvine.android.ui;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.scanvine.android.R;
import com.scanvine.android.model.Source;
import com.scanvine.android.util.Util;

public class SourceListFragment extends ListFragment {
	private Callbacks mCallbacks = sDummyCallbacks;
	private ArrayList<Source> sources = null;

	public interface Callbacks {
		public void onItemSelected(String json);
	    public void showProgress(Boolean progress);
	}

	// A dummy implementation for use when not attached to an activity.
	private static Callbacks sDummyCallbacks = new Callbacks() {
		@Override
		public void onItemSelected(String json) {}
	    public void showProgress(Boolean progress) {}
	};

	// Mandatory empty constructor for the fragment manager to instantiate the fragment.
	public SourceListFragment() { }

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (!(activity instanceof Callbacks))
			throw new IllegalStateException("Activity must implement fragment's callbacks.");
		mCallbacks = (Callbacks) activity;
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mCallbacks = sDummyCallbacks;
	}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ListView lv = (ListView) inflater.inflate(R.layout.fragment_story_list, container, false);
		lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		return lv;
    }
    
    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		new SourcesFetchTask().execute();
	}
	
	@Override
	public void onListItemClick(ListView listView, View view, int position,
			long id) {
		super.onListItemClick(listView, view, position, id);
		mCallbacks.onItemSelected(sources.get(position).getJSON());
	}

	private void renderSources() {
		final Context ctx = getActivity().getApplicationContext();
	    ArrayAdapter<Source> adapter = new ArrayAdapter<Source>(ctx, R.layout.source_line, sources) {
	        @Override
	        public View getView(int position, View convertView, ViewGroup parent){
	        	View view = convertView;
	            if(view == null){
	                LayoutInflater inflater = (LayoutInflater)ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	                view = inflater.inflate(R.layout.source_line, null);                    
	            }

	            Source source = sources.get(position);
	            TextView txt = (TextView) view.findViewById(R.id.source_name);
	            txt.setText(source.getName());
	            txt = (TextView) view.findViewById(R.id.source_score);
	            txt.setText(""+source.getScore());
	 
	            return view;
	        }
	    };
	    setListAdapter(adapter);
	}

	public void refreshList(String section) {
		new SourcesFetchTask().execute(section);
	}

	public void sortList(final boolean byName) {
		Collections.sort(sources, new Comparator<Source>() {
		    public int compare(Source a, Source b) {
		    	if (byName)
		    		return a.getName().compareTo(b.getName());
		    	Integer v1 = Integer.valueOf(0-a.getScore());
		    	Integer v2 = Integer.valueOf(0-b.getScore());
		    	return v1.compareTo(v2);
		    }
		});
		renderSources();
	}

	public final class SourcesFetchTask extends AsyncTask<String, Boolean, String> {
		@Override
		protected String doInBackground(String... params) {
	        Log.i(""+this, "Fetching sources, online: "+Util.AreWeOnline(getActivity()));
		    publishProgress(true);
		    String result = null;
		    String url = "http://www.scanvine.com/api/1/sources";
		    String section = params.length==0 ? null : params[0];
		    if (section!=null && section.trim().length()>0)
		    	url+="/"+section;
            String fileName = Util.ConvertUrlToFilename(url);
            File sourcesFile = new File(getActivity().getCacheDir(), fileName);

            if (sourcesFile.exists()) {
            	boolean useCache = !Util.AreWeOnline(getActivity());
            	long age = System.currentTimeMillis() - sourcesFile.lastModified();
            	useCache = useCache || (age >0 && age < 4*60*60*1000);
                if (useCache) {
                	Log.i(""+this, "Using cached sources file");
                	result = Util.ConvertFileToString(sourcesFile);
                	return result;
                }
            }

		    HttpClient httpclient = new DefaultHttpClient();
	        Log.i(""+this, "About to fetch "+url);
		    try {
			    HttpGet httpget = new HttpGet(url); 
		    	HttpResponse response = httpclient.execute(httpget);
		        Log.i(""+this, "Fetch result: "+response);
		        HttpEntity entity = response.getEntity();
		        if (entity != null) {
		            InputStream instream = entity.getContent();
		            result = Util.ConvertStreamToString(instream);
		            instream.close();
		        }
		    }
		      catch (org.apache.http.NoHttpResponseException ex) {
                if (sourcesFile.exists())
                	result = Util.ConvertFileToString(sourcesFile);
                return result;
		    } catch (Exception ex) {
		    	Log.e(""+this, "Fetch error: "+ex);
		    	ex.printStackTrace();
		    }
		    
		    if (result!=null && result.length()>0) {
			    //write to cache
		        try {
		            FileWriter out = new FileWriter(sourcesFile);
		            out.write(result);
		            out.close();
		            Log.i(""+this, "Cached results to "+sourcesFile);
		        } catch (IOException ex) {
		            Log.e(""+this, "Write error: "+ex);
		            ex.printStackTrace();
		        }
		    }
		    return result;
		}
		
	    @Override
		protected void onProgressUpdate(Boolean... progress) {
		    mCallbacks.showProgress(progress[0]);
		}

		@Override
		protected void onPostExecute(String result) {
		    publishProgress(false);
		    if (result!=null && result.length()>0) {
		    	Log.i(""+this, "Got response of length "+result.length());
			    sources = Source.GetSourcesFrom(result);
		    }
		    else
		    	sources = new ArrayList<Source>();
		    renderSources();
		}
	}

}
