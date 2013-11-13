/*
 Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/

package com.scanvine.android.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.scanvine.android.model.Story;

public class SVDownloadManager {
	public File cacheFor(Context context, String urlString) {
        String fileName = Util.ConvertUrlToFilename(urlString);
        return new File(context.getCacheDir(), fileName);
	}

	public boolean cacheExistsFor(Context context, String urlString) {
		File localFile = cacheFor(context, urlString);
        return localFile.exists() && localFile.length()>0;
	}
	
	public boolean downloadExistsFor(Context context, Story story) {
		File download = new File(context.getFilesDir(),story.getFilename());
        return download.exists() && download.length()>0;
	}
	
	public void deleteDownload(Context context, String fileName) {
		File file = new File(context.getFilesDir(),fileName);
		if (file.exists())
			file.delete();
	}

	public void fetchDrawableOnThread(final Context context, final String urlString, final Handler handler) {
    	if (urlString==null || urlString.trim().length()==0 || "null".equalsIgnoreCase(urlString))
    		return;
    	fetchUrlOnThread(context, urlString, handler, true);
    }

    public void fetchFileOnThread(final Context context, final String urlString, final Handler handler) {
    	if (urlString==null || urlString.trim().length()==0 || "null".equalsIgnoreCase(urlString))
    		return;
    	fetchUrlOnThread(context, urlString, handler, false);
    }

    private void fetchUrlOnThread(final Context context, final String urlString, final Handler handler, final boolean isImage) {
    	if (urlString==null || urlString.trim().length()==0)
    		return;

        Thread thread = new Thread() {
            @Override
            public void run() {
            	boolean success = false;
                File localFile = cacheFor(context, urlString);
                if (localFile.exists())
                	success = true;
                else {
                	Log.i(""+this, "Fetching: "+urlString);
	                success = isImage ? fetchDrawable(context, urlString, localFile) : fetchFile(context, urlString, localFile);
                }
                if (success) {
                    Message message = handler.obtainMessage(1, localFile);
                    handler.sendMessage(message);
                }
            }
        };
        thread.start();
    }
    
    private boolean fetchDrawable(Context context, String urlString, File localFile) {
        try {
            InputStream is = fetch(urlString);
            Drawable drawable = Drawable.createFromStream(is, "src");
            if (drawable==null)
            	return false;

            Bitmap b = ((BitmapDrawable)drawable).getBitmap();
    	    Bitmap resized = Bitmap.createScaledBitmap(b, 100, 100, false);
        	localFile.createNewFile();
            FileOutputStream out = new FileOutputStream(localFile); 
            resized.compress(Bitmap.CompressFormat.JPEG, 80, out); 
            out.flush();    
            out.close();
            return true;
        } catch (Exception ex) {
            Log.e(""+this, "fetchDrawable failed", ex);
            return false;
        }
    }
    
    private boolean fetchFile(Context context, String urlString, File localFile) {
        try {
            InputStream is = fetch(urlString);
            FileOutputStream out = new FileOutputStream(localFile); 
            byte[] buffer = new byte[4096];
            int len = 0;
            while ((len = is.read(buffer)) >=0) {
                out.write(buffer, 0, len);
            }
            out.flush();    
            out.close();
            return true;
        } catch (Exception ex) {
            Log.e(""+this, "fetchFile failed", ex);
            return false;
        }
    }

    private InputStream fetch(String urlString) throws MalformedURLException, IOException {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpGet request = new HttpGet(urlString);
        HttpResponse response = httpClient.execute(request);
        return response.getEntity().getContent();
    }
    
}