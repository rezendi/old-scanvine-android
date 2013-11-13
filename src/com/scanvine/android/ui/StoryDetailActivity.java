package com.scanvine.android.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import android.view.Window;

import com.google.analytics.tracking.android.EasyTracker;
import com.scanvine.android.R;

/**
 * An activity representing a single Story detail screen. This activity is only
 * used on handset devices. On tablet-size devices, item details are presented
 * side-by-side with a list of items in a {@link StoryListActivity}.
 * <p>
 * This activity is mostly just a 'shell' activity containing nothing more than
 * a {@link StoryDetailFragment}.
 */
public class StoryDetailActivity extends FragmentActivity implements StoryDetailFragment.Callbacks {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);  
		setContentView(R.layout.activity_story_detail);
		getActionBar().setDisplayHomeAsUpEnabled(true); // Show the Up button in the action bar.
		
		if (savedInstanceState == null) {
			Bundle arguments = new Bundle();
			arguments.putString(StoryDetailFragment.STORY_JSON, getIntent().getStringExtra(StoryDetailFragment.STORY_JSON));
			arguments.putBoolean(StoryDetailFragment.ABOUT, getIntent().getBooleanExtra(StoryDetailFragment.ABOUT, false));

			StoryDetailFragment fragment = new StoryDetailFragment();
			fragment.setArguments(arguments);

			getSupportFragmentManager().beginTransaction().add(R.id.story_detail_container, fragment).commit();
		}
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

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpTo(this, new Intent(this, StoryListActivity.class));
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void showProgress(Boolean progress) {
		this.setProgressBarIndeterminateVisibility(progress);
	}

}
