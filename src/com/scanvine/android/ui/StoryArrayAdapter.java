package com.scanvine.android.ui;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.List;

import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.scanvine.android.R;
import com.scanvine.android.model.Story;
import com.scanvine.android.util.SVDownloadManager;
import com.scanvine.android.util.Util;

public class StoryArrayAdapter extends ArrayAdapter<Story> {

    Context context;
    WeakReference<StoryListFragment> fragmentRef;
    SVDownloadManager downloadManager;

    public StoryArrayAdapter(Context context, StoryListFragment fragment, int resourceId, List<Story> objects) {
        super(context, resourceId, objects);
        this.context = context;
        this.fragmentRef = new WeakReference<StoryListFragment>(fragment);
        this.downloadManager = new SVDownloadManager();
    }

    /*private view holder class*/
    private class ViewHolder {
        ImageView image;
        ImageButton share;
        ImageButton download;
        TextView txtTitle;
        TextView txtBlurb;
        TextView txtByline;
        View margin;
        
        public void setDownloadButton(final Story story) {
            if (downloadManager.downloadExistsFor(context, story)) {
            	download.setImageResource(R.drawable.briefcase);
    	        download.setOnClickListener(new OnClickListener() {
    	            @Override
    	            public void onClick(View v) {
    	                AlertDialog.Builder builder = new AlertDialog.Builder(context);
    	                builder.setMessage(R.string.delete_this_download)
    	                       .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
    	                           public void onClick(DialogInterface dialog, int id) {
    	                        	   	downloadManager.deleteDownload(context, story.getFilename());
    	                           		setDownloadButton(story);
    	                           }
    	                       })
    	                       .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
    	                           public void onClick(DialogInterface dialog, int id) {
    	                           }
    	                       });
    	                AlertDialog dialog = builder.create();
    	                dialog.show();
    	            }
    	        });
            }
            else {
            	download.setImageResource(R.drawable.download);
    	        download.setOnClickListener(new OnClickListener() {
    	            @Override
    	            public void onClick(View v) {
    	            	if (!Util.AreWeOnline(context))
    	            		Toast.makeText(context, "Not online", Toast.LENGTH_SHORT).show();
    	            	else {
    	                	download.setImageResource(R.drawable.repeat);
	    	            	Handler handler = new FileHandler(context.getApplicationContext(), story, download);
	    	            	downloadManager.fetchFileOnThread(context, story.getURL(), handler);
    	            	}
    	            }
    	        });
            }
        }
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder holder = convertView==null ? new ViewHolder() : (ViewHolder) convertView.getTag();
        final Story story = getItem(position);

        LayoutInflater mInflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.story_card, null);
            holder.txtTitle = (TextView) convertView.findViewById(R.id.title);
            holder.txtBlurb = (TextView) convertView.findViewById(R.id.blurb);
            holder.txtByline = (TextView) convertView.findViewById(R.id.byline);
            holder.image = (ImageView) convertView.findViewById(R.id.image);
            holder.share = (ImageButton) convertView.findViewById(R.id.share);
            holder.download = (ImageButton) convertView.findViewById(R.id.download);
            holder.margin = (View) convertView.findViewById(R.id.margin);
            convertView.setTag(holder);
        }

        holder.txtTitle.setText(story.getTitleLine());
        holder.txtByline.setText(story.getByline());
        holder.txtBlurb.setText(story.getSafeBlurb());
        holder.txtBlurb.setVisibility(View.GONE);
        holder.margin.setVisibility(story.isClustered() ? View.VISIBLE : View.GONE);
        
    	holder.image.setImageDrawable(context.getResources().getDrawable(R.drawable.scanvine));
    	downloadManager.fetchDrawableOnThread(context, story.getImageURL(), new DrawableHandler(holder.image));
    	
    	holder.setDownloadButton(story);

        holder.image.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
            	int currentViz = holder.txtBlurb.getVisibility();
            	holder.txtBlurb.setVisibility(currentViz==View.VISIBLE ? View.GONE : View.VISIBLE);
            }
        });
        
        holder.share.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
            	String title = story.getTitle();
            	if (title.length()>115)
            		title = title.substring(0,115)+"...";
            	String text = "\""+title+"\""+" via @Scanvine: "+story.getURL();
        		Intent shareIntent = new Intent(Intent.ACTION_SEND);
        		shareIntent.putExtra(Intent.EXTRA_TEXT, text);
        		shareIntent.setType("text/plain");
        	    context.startActivity(shareIntent);
            }
        });

        convertView.setOnClickListener(new OnItemClickListener(position));
        return convertView;
    }
    
    private class OnItemClickListener implements OnClickListener{       
        private int position;
        OnItemClickListener(int n){
            position = n;
        }
        @Override
        public void onClick(View arg) {
        	StoryListFragment fragment = fragmentRef.get();
        	if (fragment!=null)
        		fragment.selectItem(position);
        }       
    }
    
    private static class DrawableHandler extends Handler {
    	private final WeakReference<ImageView> wrImageView;
    	
    	public DrawableHandler(ImageView imageView) {
    		wrImageView = new WeakReference<ImageView>(imageView);
    	}
    	
    	@Override
    	public void handleMessage(Message msg) {
    		ImageView imageView = wrImageView.get();
    		if (imageView != null) {
    			Drawable d = Drawable.createFromPath(""+msg.obj);
    			imageView.setImageDrawable(d);
    		}
    	}
    }

    private static class FileHandler extends Handler {
    	private Context context;
    	private final WeakReference<ImageButton> wrImageButton;
    	private final WeakReference<Story> wrStory;
    	
    	public FileHandler(Context ctx, Story story, ImageButton imageButton) {
    		wrImageButton = new WeakReference<ImageButton>(imageButton);
    		wrStory = new WeakReference<Story>(story);
    		context = ctx;
    	}
    	
    	@Override
    	public void handleMessage(Message msg) {
    		ImageButton imageButton = wrImageButton.get();
    		Story story = wrStory.get();
    		if (imageButton != null && story != null) {
    			File file = (File) msg.obj;
    			Log.i(""+this, "Downloaded: "+file);
    			imageButton.setImageResource(R.drawable.briefcase);
    			try {
	    			JSONObject serialized = new JSONObject();
	    			serialized.put("story", story.getJSON());
	    			serialized.put("file", Util.ConvertFileToString(file));
	    			Util.WriteToFile(context, story.getFilename(), ""+serialized);
	    			Log.i(""+this, "Serialized: "+story+" to "+context.getFilesDir()+"/"+story.getFilename());
	    			file.delete();
    			}
    			catch(Exception ex) {
    				Log.i(""+this, "Could not serialize: "+story);
    			}
    		}
    	}
    }
}
