package com.scanvine.android.ui;

import android.content.Intent;
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

public class SourceListActivity extends FragmentActivity implements SourceListFragment.Callbacks {
	
	private String currentSection = null;
	private boolean sortByName = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);  
		setContentView(R.layout.activity_source_list);
		setTitle(getString(R.string.app_name)+" - "+getString(R.string.sources));
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
		Intent sourceIntent = new Intent(this, StoryListActivity.class);
		sourceIntent.putExtra(StoryListActivity.SOURCE, json);
		startActivity(sourceIntent);
	}

	@Override
	public void showProgress(Boolean progress) {
		this.setProgressBarIndeterminateVisibility(progress);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    // Inflate the menu items for use in the action bar
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.source_list_actions, menu);
	    return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle presses on the action bar items
	    switch (item.getItemId()) {
	        case R.id.action_section:
	            openSectionPopup();
	            return true;
	        case R.id.action_sort:
	            openSortPopup();
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
	
	public void openSectionPopup() {
        final String[] sections = {"All","World","Tech","Business","Entertainment","Sports","Life"};
        PopupMenu popupMenu = new PopupMenu(this, findViewById(R.id.action_section));
        Menu menu = popupMenu.getMenu();
        for (int i=0; i<sections.length; i++) {
        	boolean addCheck = currentSection!=null &&
        					   (currentSection.equalsIgnoreCase(sections[i]) ||
        					   currentSection.length()==0 && "All".equalsIgnoreCase(sections[i]));
        	menu.add(Menu.NONE, i, Menu.NONE, addCheck ? sections[i]+"✓" : sections[i]);
        }
        popupMenu.setOnMenuItemClickListener(new OnMenuItemClickListener() {
        	@Override
			public boolean onMenuItemClick(MenuItem item) {
        		currentSection = item.getItemId()==0 ? null : (""+item.getTitle()).replace("✓","");
        		SourceListFragment slf = (SourceListFragment) getSupportFragmentManager().findFragmentById(R.id.source_list);
        		slf.refreshList(currentSection);
        		return false;
        	}
        });
        popupMenu.show();
	}

	public void openSortPopup() {
        PopupMenu popupMenu = new PopupMenu(this, findViewById(R.id.action_section));
        Menu menu = popupMenu.getMenu();
        menu.add(Menu.NONE, 0, Menu.NONE, sortByName ? "Name✓" : "Name");
        menu.add(Menu.NONE, 1, Menu.NONE, sortByName ? "Score" : "Score✓");
        popupMenu.setOnMenuItemClickListener(new OnMenuItemClickListener() {
        	@Override
			public boolean onMenuItemClick(MenuItem item) {
        		sortByName = item.getItemId()==0;
        		SourceListFragment slf = (SourceListFragment) getSupportFragmentManager().findFragmentById(R.id.source_list);
        		slf.sortList(sortByName);
        		return false;
        	}
        });
        popupMenu.show();
	}
	
}
