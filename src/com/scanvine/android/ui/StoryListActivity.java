package com.scanvine.android.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;

import com.google.analytics.tracking.android.EasyTracker;
import com.scanvine.android.R;
import com.scanvine.android.model.Source;
import com.scanvine.android.model.Story;

/**
 * An activity representing a list of Stories.
 * <p>
 * The list of items is a {@link StoryListFragment} and the item details (if present) is a {@link StoryDetailFragment}.
 * <p>
 * Implements the required {@link StoryListFragment.Callbacks} interface to listen for item selections.
 */
public class StoryListActivity extends FragmentActivity implements StoryListFragment.Callbacks {
	public static String SOURCE				= "source";
	private static String OPEN_IN_BROWSER	= "open_browser";

	private boolean twoPane;
	private boolean openInBrowser;
	private boolean downloadsShown;
	private String currentTime = "Latest";
	private String currentSection = null;
	private Source currentSource = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);  
		if (getIntent().getExtras()!=null)  {
			String json = getIntent().getExtras().getString(SOURCE);
			currentSource = Source.GetSourceFor(json);
		}

		setContentView(R.layout.activity_story_list);
		String title = currentSource==null ? currentTime : currentSource.getName(); 
		setTitle(getString(R.string.app_name)+" - "+title);

		if (findViewById(R.id.story_detail_container) != null) {
			twoPane = true;
			((StoryListFragment) getSupportFragmentManager().findFragmentById(R.id.story_list)).setActivateOnItemClick(true);
		}

		SharedPreferences preferences = getPreferences(MODE_PRIVATE);
		openInBrowser = preferences.getBoolean(OPEN_IN_BROWSER, false);
		// TODO: If exposing deep links, handle intents here.
	}

	@Override
	protected void onStart() {
		super.onStart();
		EasyTracker.getInstance(this).activityStart(this);
	}

	@Override
	protected void onStop() {
		EasyTracker.getInstance(this).activityStop(this);
		super.onStop();
	}

	/**
	 * Callback method from {@link StoryListFragment.Callbacks} indicating that
	 * the item with the given ID was selected.
	 */
	@Override
	public void onItemSelected(String json) {
		if (openInBrowser) {
			Story story = Story.GetStoryFor(json);
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setData(Uri.parse(story.getURL()));
			startActivity(intent);
		}
		else if (twoPane) {
			Bundle arguments = new Bundle();
			arguments.putString(StoryDetailFragment.STORY_JSON, json);
			StoryDetailFragment fragment = new StoryDetailFragment();
			fragment.setArguments(arguments);
			getSupportFragmentManager().beginTransaction().replace(R.id.story_detail_container, fragment).commit();

		}
		else {
			Intent detailIntent = new Intent(this, StoryDetailActivity.class);
			detailIntent.putExtra(StoryDetailFragment.STORY_JSON, json);
			startActivity(detailIntent);
		}
	}

	@Override
	public void showProgress(Boolean progress) {
		this.setProgressBarIndeterminateVisibility(progress);
		if (progress==false) {
			if (downloadsShown)
				setTitle(R.string.downloads);
			else {
				String title = currentSource==null ? currentTime : currentSource.getName(); 
				setTitle(getString(R.string.app_name)+" - "+title);
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    // Inflate the menu items for use in the action bar
	    MenuInflater inflater = getMenuInflater();
	    int menuID = currentSource==null ? R.menu.story_list_actions : R.menu.source_stories_actions;
	    inflater.inflate(menuID, menu);
	    return super.onCreateOptionsMenu(menu);
	}
	
	private String getSourceSlug() {
		return currentSource==null ? null : currentSource.getSlug();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle presses on the action bar items
	    switch (item.getItemId()) {
	        case R.id.action_time:
	            openTimePopup();
	            return true;
	        case R.id.action_section:
	            openSectionsPopup();
	            return true;
	        case R.id.action_downloads:
	            showDownloads();
	            return true;
	        case R.id.action_sources:
	            showSources();
	            return true;
	        case R.id.action_about:
	            showAbout();
	            return true;
	        case R.id.action_open_in:
	        	openOpenInPopup();
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
	
	public void openTimePopup() {
		final String[] times = {"Firehose","Latest","Last Day","Last Week","Last Month"};
		final String[] timeKeys = {"Firehose","Latest","Last1","Last7","Last30"};
        PopupMenu popupMenu = new PopupMenu(this, findViewById(R.id.action_time));
        Menu menu = popupMenu.getMenu();
        for (int i=0; i<times.length; i++) {
        	boolean addCheck = currentTime!=null && currentTime.equalsIgnoreCase(timeKeys[i]);
        	menu.add(Menu.NONE, i, Menu.NONE, addCheck ? times[i]+"✓" : times[i]);
        }
        popupMenu.setOnMenuItemClickListener(new OnMenuItemClickListener() {
        	@Override
			public boolean onMenuItemClick(MenuItem item) {
        		currentTime = timeKeys[item.getItemId()];
        		StoryListFragment slf = (StoryListFragment) getSupportFragmentManager().findFragmentById(R.id.story_list);
        		slf.refreshList(currentTime, currentSection, getSourceSlug());
        		return false;
        	}
        });
        popupMenu.show();
	}
	
	public void openSectionsPopup() {
        final String[] sections = {"All","World","Tech","Business","Entertainment","Sports","Life"};
        PopupMenu popupMenu = new PopupMenu(this, findViewById(R.id.action_section));
        Menu menu = popupMenu.getMenu();
        for (int i=0; i<sections.length; i++) {
        	boolean addCheck = sections[i].equalsIgnoreCase(currentSection) || currentSection==null && i==0;
        	menu.add(Menu.NONE, i, Menu.NONE, addCheck ? sections[i]+"✓" : sections[i]);
        }
        popupMenu.setOnMenuItemClickListener(new OnMenuItemClickListener() {
        	@Override
			public boolean onMenuItemClick(MenuItem item) {
        		currentSection = item.getItemId()==0 ? null : (""+item.getTitle()).replace("✓","");
        		StoryListFragment slf = (StoryListFragment) getSupportFragmentManager().findFragmentById(R.id.story_list);
        		slf.refreshList(currentTime, currentSection, getSourceSlug());
        		return false;
        	}
        });
        popupMenu.show();
	}
	
	public void openOpenInPopup() {
        PopupMenu popupMenu = new PopupMenu(this, findViewById(R.id.action_section));
        Menu menu = popupMenu.getMenu();
        menu.add(Menu.NONE, 0, Menu.NONE, openInBrowser ? "Browser✓" : "Browser");
        menu.add(Menu.NONE, 1, Menu.NONE, openInBrowser ? "Scanvine" : "Scanvine✓");
        popupMenu.setOnMenuItemClickListener(new OnMenuItemClickListener() {
        	@Override
			public boolean onMenuItemClick(MenuItem item) {
        		SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        		SharedPreferences.Editor editor = preferences.edit();
        		editor.putBoolean(OPEN_IN_BROWSER, item.getItemId()==0);
        		editor.commit();
        		openInBrowser = preferences.getBoolean(OPEN_IN_BROWSER, false);
        		return false;
        	}
        });
        popupMenu.show();
	}
	
	public void showDownloads() {
		StoryListFragment slf = (StoryListFragment) getSupportFragmentManager().findFragmentById(R.id.story_list);
		slf.showDownloads();
		downloadsShown = true;
	}

	public void showSources() {
		Intent sourcesIntent = new Intent(this, SourceListActivity.class);
		startActivity(sourcesIntent);
	}

	public void showAbout() {
		Intent detailIntent = new Intent(this, StoryDetailActivity.class);
		detailIntent.putExtra(StoryDetailFragment.ABOUT, true);
		startActivity(detailIntent);
	}

	@Override
    public void onBackPressed() {
		if (downloadsShown) {
			StoryListFragment slf = (StoryListFragment) getSupportFragmentManager().findFragmentById(R.id.story_list);
    		slf.refreshListFromCache(currentTime, currentSection, getSourceSlug());
    		downloadsShown = false;
		}
		else
			super.onBackPressed();
    }
}
