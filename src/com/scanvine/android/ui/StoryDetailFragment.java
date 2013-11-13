package com.scanvine.android.ui;

import java.io.File;

import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.scanvine.android.R;
import com.scanvine.android.model.Story;
import com.scanvine.android.util.SVDownloadManager;
import com.scanvine.android.util.Util;

/**
 * A fragment representing a single Story detail screen. This fragment is either
 * contained in a {@link StoryListActivity} in two-pane mode (on tablets) or a
 * {@link StoryDetailActivity} on handsets.
 */
public class StoryDetailFragment extends Fragment {
	public static final String STORY_JSON = "json";
	public static String ABOUT	= "about";

	private boolean isAbout;
	private Story story;
	private Callbacks mCallbacks = sDummyCallbacks;
	private SVDownloadManager downloadManager = new SVDownloadManager();

	/**
	 * A callback interface that all activities containing this fragment must
	 * implement. This mechanism allows activities to be notified of page loading.
	 */
	public interface Callbacks {
	    public void showProgress(Boolean progress);
	}

	// A dummy implementation for use when not attached to an activity.
	private static Callbacks sDummyCallbacks = new Callbacks() {
		@Override
	    public void showProgress(Boolean progress) {}
	};

	// Mandatory empty constructor
	public StoryDetailFragment() { }

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		isAbout = getArguments().containsKey(ABOUT) && getArguments().getBoolean(ABOUT);
		if (isAbout)
			getActivity().setTitle(R.string.about);
		else if (getArguments().containsKey(STORY_JSON)) {
			story = Story.GetStoryFor(getArguments().getString(STORY_JSON));
			getActivity().setTitle(story.getSourceName()+" - "+story.getTitle());
		}
	}

	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_story_detail, container, false);
		load(story, rootView);
		return rootView;
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		// Activities containing this fragment must implement its callbacks.
		if (!(activity instanceof Callbacks)) {
			throw new IllegalStateException("Activity must implement fragment's callbacks.");
		}

		mCallbacks = (Callbacks) activity;
	}

	@SuppressLint("SetJavaScriptEnabled")
	private void load(Story story, View rootView) {
		WebView webView = (WebView) rootView.findViewById(R.id.story_detail);

	    if (isAbout)
			webView.loadUrl("file:///android_asset/about.html");
	    else if (story != null) {
			webView.setWebViewClient(getWebViewClient());
			WebSettings webSettings = webView.getSettings();
			webSettings.setJavaScriptEnabled(true);
		    webSettings.setLoadWithOverviewMode(true);
		    webSettings.setUseWideViewPort(true);
		    webSettings.setBuiltInZoomControls(true);
		    webSettings.setDisplayZoomControls(false);
		    webSettings.setSupportZoom(true);
			mCallbacks.showProgress(true);
		    if (downloadManager.downloadExistsFor(getActivity(), story)) {
				File file = new File(getActivity().getFilesDir(), story.getFilename());
				try {
					String contents = Util.ConvertFileToString(file);
					JSONObject json = new JSONObject(contents);
					String html = json.getString("file");
					html = html==null ? "" : html;
					html = html.replace("<iframe", "<!--").replace("</iframe>","-->");
					Log.i(""+this, "Got downloaded story of length "+html.length());
					webView.loadDataWithBaseURL(story.getHost(), html, "text/html", "utf-8", null);;
				}
				catch(Exception ex) {
					Log.e(""+this, "Couldn't get story from "+file);
					webView.loadUrl(story.getURL());
				}
			}
			else
				webView.loadUrl(story.getURL());
		}

	}

	private WebViewClient getWebViewClient() {
		return new WebViewClient() {
	        @Override
	        public boolean shouldOverrideUrlLoading(WebView view, String url) {
	            view.loadUrl(url);
	            return true;
	        }
	        
	        @Override
	        public void onPageFinished(WebView view, String url) {
	            mCallbacks.showProgress(false);
	        }
	        
	        @Override
	        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
	            Toast toast = Toast.makeText(getActivity(), description, Toast.LENGTH_SHORT);
	            toast.show();
	        }
		};
	}
}
