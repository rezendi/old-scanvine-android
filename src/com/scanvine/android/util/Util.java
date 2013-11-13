package com.scanvine.android.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

public class Util {
	public static String ConvertUrlToFilename(String url) {
		if (url==null)
			return null;
		String file = url.replace("http://","").replace("https://","").replace("/", "_").replace(" ","_").replace(".","_");
		file = file.replaceAll("[^a-zA-Z0-9_\\-\\s]", "");
		if (file.endsWith("_"))
			file = file.substring(0, file.length()-1);
		return file;
	}
	
	public static boolean AreWeOnline(Context context) {
		ConnectivityManager cm =
		        (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
		 
		NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
		return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
	}
	
    public static String ConvertStreamToString(InputStream is) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

    public static String ConvertFileToString(File file) {
    	Log.i("Util", "Converting file at "+file+" to string");
    	String retval = null;
	    try {
	        InputStream inputStream = new FileInputStream(file);
	        if ( inputStream != null ) {
	            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
	            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
	            String receiveString = "";
	            StringBuilder sb = new StringBuilder();
	            while ( (receiveString = bufferedReader.readLine()) != null ) {
	                sb.append(receiveString);
	            }

	            inputStream.close();
	            retval = sb.toString();
	        }
	    }
	    catch (FileNotFoundException ex) {
	        Log.e("Util", "File not found: " + ex.toString());
	    } catch (IOException ex) {
	        Log.e("Util", "Can not read file: " + ex.toString());
	    }
	    return retval;
    }

    public static void WriteToFile(Context context, String filename, String data) {
        try {
        	FileOutputStream fos = context.openFileOutput(filename, Context.MODE_PRIVATE);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fos);
            outputStreamWriter.write(data);
            outputStreamWriter.flush();
            outputStreamWriter.close();
        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        } 
    }

}
