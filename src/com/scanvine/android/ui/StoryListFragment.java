package com.scanvine.android.ui;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.scanvine.android.R;
import com.scanvine.android.model.Source;
import com.scanvine.android.model.Story;
import com.scanvine.android.util.Util;

/**
 * A list fragment representing a list of Stories. This fragment also supports
 * tablet devices by allowing list items to be given an 'activated' state upon
 * selection. This helps indicate which item is currently being viewed in a
 * {@link StoryDetailFragment}.
 * <p>
 * Activities containing this fragment MUST implement the {@link Callbacks} interface.
 */
public class StoryListFragment extends ListFragment {

	private static final String STATE_ACTIVATED_POSITION = "activated_position"; //only used on tables
	private int mActivatedPosition = ListView.INVALID_POSITION; //only used on tables
	private Callbacks mCallbacks = sDummyCallbacks;
	private ArrayList<Story> stories = new ArrayList<Story>();

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
	public StoryListFragment() { }

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
        lv.setDivider(null);
        lv.setDividerHeight(0);
		return lv;

    }
    
    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		String slug = null;
		if (getActivity().getIntent().getExtras()!=null) {  
			String json = getActivity().getIntent().getExtras().getString(StoryListActivity.SOURCE);
			Source source = Source.GetSourceFor(json);
			slug = source.getSlug();
		}
		refreshList(null, null, slug);
	}
	
    @Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		if (savedInstanceState != null && savedInstanceState.containsKey(STATE_ACTIVATED_POSITION))
			setActivatedPosition(savedInstanceState.getInt(STATE_ACTIVATED_POSITION));
	}

	@Override
	public void onListItemClick(ListView listView, View view, int position,
			long id) {
		super.onListItemClick(listView, view, position, id);
		mCallbacks.onItemSelected(stories.get(position).getJSON());
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (mActivatedPosition != ListView.INVALID_POSITION) {
			// Serialize and persist the activated item position.
			outState.putInt(STATE_ACTIVATED_POSITION, mActivatedPosition);
		}
	}

	private void renderStories() {
		Activity activity = getActivity();
		if (activity!=null)
			setListAdapter(new StoryArrayAdapter(activity, this, R.id.story_list, stories));
	}
	
	public void showDownloads() {
		new ListDownloadsTask().execute();
	}

	public void selectItem(int n) {
		String json = stories.get(n).getJSON();
		mCallbacks.onItemSelected(json);
	}
	
	public void refreshList(String time, String section, String source) {
		refreshList(time, section, source, false);
	}

	public void refreshListFromCache(String time, String section, String source) {
		refreshList(time, section, source, true);
	}

	private void refreshList(String time, String section, String source, boolean cached) {
		if (source!=null && "Firehose".equalsIgnoreCase(time))
			time = null;
		if (source==null && "Latest".equalsIgnoreCase(time))
			time = null;
		if (time==null && section==null && source==null)
			time = "Latest";
		new StoryFetchTask().execute(time, section, source, cached ? "cached" : null);
	}

	public void setActivateOnItemClick(boolean activateOnItemClick) {
		getListView().setChoiceMode(activateOnItemClick ? ListView.CHOICE_MODE_SINGLE : ListView.CHOICE_MODE_NONE);
	}

	private void setActivatedPosition(int position) {
		if (position == ListView.INVALID_POSITION)
			getListView().setItemChecked(mActivatedPosition, false);
		else
			getListView().setItemChecked(position, true);
		mActivatedPosition = position;
	}

	public final class StoryFetchTask extends AsyncTask<String, Boolean, String> {
		@Override
		protected String doInBackground(String... params) {
	        Log.i(""+this, "Fetching stories, params: "+Arrays.toString(params)+", online: "+Util.AreWeOnline(getActivity()));
		    publishProgress(true);
		    String result = null;
		    String time = (params==null || params.length<1 ? null : params[0]);
		    String section = (params==null || params.length<2 ? null : params[1]);
		    String source = (params==null || params.length<3 ? null : params[2]);
		    String url = "http://www.scanvine.com/api/1";

		    if (source!=null && source.length()>0)
		    	url += "/source/"+source;
		    else if (section!=null && section.length()>0)
				url += "/"+section;
		    if (time!=null && time.length()>0)
		    	url += "/"+time;

		    String fileName = Util.ConvertUrlToFilename(url);
            File storiesFile = new File(getActivity().getCacheDir(), fileName);

            if (storiesFile.exists()) {
            	boolean useCache = !Util.AreWeOnline(getActivity());
            	useCache = useCache || params.length>3 && "cached".equalsIgnoreCase(params[3]);
            	long age = System.currentTimeMillis() - storiesFile.lastModified();
            	useCache = useCache || (age>0 && age < 600*1000);
                if (useCache) {
                	Log.i(""+this, "Using cached stories file");
                	result = Util.ConvertFileToString(storiesFile);
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
                if (storiesFile.exists())
                	result = Util.ConvertFileToString(storiesFile);
                return result;
		    } catch (Exception ex) {
		    	Log.e(""+this, "Fetch error: "+ex);
		    	ex.printStackTrace();
		    }
		    
		    if (result!=null && result.length()>0) {
			    //write to cache
		        try {
		            FileWriter out = new FileWriter(storiesFile);
		            out.write(result);
		            out.close();
		            Log.i(""+this, "Cached results to "+storiesFile);
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
			    stories = Story.GetStoriesFrom(result);
		    }
		    else
		    	stories = new ArrayList<Story>();
		    renderStories();
		}
	}

	public final class ListDownloadsTask extends AsyncTask<String, Boolean, String> {
		@Override
		protected String doInBackground(String... params) {
		    publishProgress(true);
			File[] files = getActivity().getFilesDir().listFiles();
			Arrays.sort(files, new Comparator<File>() {
			    public int compare(File f1, File f2) {
			    	return Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
			    }
			});

			ArrayList<Story> downloads = new ArrayList<Story>();
			for (File child : files) {
				if (child.getName().startsWith(Story.FILE_PREFIX)) {
					Log.i(""+this, "Found "+child);
					String contents = Util.ConvertFileToString(child);
					try {
						JSONObject json = new JSONObject(contents);
						Story story = Story.GetStoryFor(json.getString("story"));
						downloads.add(story);
					}
					catch(Exception ex) {
						Log.e(""+this, "Couldn't get story from "+child);
					}
				}
			}

			stories = downloads;
		    return "done";
		}
		
	    @Override
		protected void onProgressUpdate(Boolean... progress) {
		    mCallbacks.showProgress(progress[0]);
		}

		@Override
		protected void onPostExecute(String result) {
		    publishProgress(false);
		    renderStories();
		}
	}
}
