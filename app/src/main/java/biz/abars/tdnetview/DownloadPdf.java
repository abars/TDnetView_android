package biz.abars.tdnetview;

import android.app.Activity;
import android.app.Fragment;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.content.Context;
import androidx.core.content.FileProvider;

class DownloadPdf{
	
String dest_file_path = "tdnet_view.pdf";
int downloadedSize = 0, totalsize;
String download_file_url = "";
String download_file_url_secondary = "";
float per = 0;
Activity activity;

private Activity getActivity(){
	return activity;
}

void downloadAndOpenPDF(String pdf_name,String external_cache_dir,Activity activity) {
	this.activity=activity;
	download_file_url=TDnetRegx.TDNET_BASE_URL+pdf_name;
	download_file_url_secondary=TDnetRegx.APPENGINE_BASE_URL+pdf_name;
	dest_file_path=external_cache_dir+"/"+pdf_name;
	
    new Thread(new Runnable() {
        public void run() {
        	File download_file=downloadFile(download_file_url);
        	if(download_file==null){
        		download_file_url=download_file_url_secondary;
        		download_file=downloadFile(download_file_url);
            	if(download_file==null){
            		setTextError("PDF download failed");
            		return;
            	}
        	}

        	// Android 6
            /*
            Uri path = Uri.fromFile(download_file);
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(path, "application/pdf");
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                getActivity().startActivity(intent);
            } catch (ActivityNotFoundException e) {
            	setTextError("PDF Reader application is not installed in your device");
            }
            */

            // Android 7
            // https://sankame.github.io/blog/2018-07-23-android_use_fileprovider/
            Uri path = FileProvider.getUriForFile(
                    getActivity()
                    , getActivity().getApplicationContext().getPackageName() + ".provider"
                    , download_file);
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(path, "application/pdf");
                intent.putExtra(Intent.EXTRA_STREAM, path);
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                getActivity(). startActivity(intent);
            } catch (ActivityNotFoundException e) {
                setTextError("PDF Reader application is not installed in your device");
            }
        }
    }).start();

}

File downloadFile(String dwnload_file_path) {
    File cache_file = new File(dest_file_path);  
    if(cache_file.exists()){
    	return cache_file;
    }

	File file = null;
        
    try {
    	Log.i("",dwnload_file_path);
    	
        setText("Starting PDF download...");

        URL url = new URL(dwnload_file_path);
        HttpURLConnection urlConnection = (HttpURLConnection) url
                .openConnection();

        urlConnection.setRequestMethod("GET");
        //urlConnection.setDoOutput(true);

        // connect
        urlConnection.connect();
        
        // get response code
        int code = urlConnection.getResponseCode();
        if(code != urlConnection.HTTP_OK){
            //setTextError("Some http error occured. "+code);
            return null;
        }

        // set the path where we want to save the file
        //File SDCardRoot = Environment.getExternalStorageDirectory();
        // create a new file, to save the downloaded file
        file = new File(dest_file_path);//SDCardRoot, dest_file_path);

        FileOutputStream fileOutput = new FileOutputStream(file);

        // Stream used for reading the data from the internet
        InputStream inputStream = urlConnection.getInputStream();

        // this is the total size of the file which we are
        // downloading
        totalsize = urlConnection.getContentLength();

        // create a buffer...
        byte[] buffer = new byte[1024 * 1024];  
        int bufferLength = 0;

        while ((bufferLength = inputStream.read(buffer)) > 0) {
            fileOutput.write(buffer, 0, bufferLength);
            downloadedSize += bufferLength;
            per = ((float) downloadedSize / totalsize) * 100;
            /*setText("Total PDF File size  : "
                    + (totalsize / 1024)
                    + " KB\n\nDownloading PDF " + (int) per
                    + "% complete");*/
        }
        // close the output stream when complete //
        fileOutput.close();
        //setText("Download Complete. Open PDF Application installed in the device.");

    } catch (final MalformedURLException e) {
        setTextError("Some url error occured. Press back and try again."
    	         );
        return null;
    } catch (final IOException e) {
    	setTextError("Some io error occured. Press back and try again."
    	        );
    	e.fillInStackTrace();
    	Log.i("", "message", e);
        return null;
    } catch (final Exception e) {
    	setTextError(
    	        "Failed to download image. Please check your internet connection."
    	   );
        return null;
    }
    return file;
}

void setTextError(final String message) {
    getActivity().runOnUiThread(new Runnable() {
        public void run() {
        	((MainActivity)getActivity()).mNavigationDrawerFragment.onMessage(message);
        	//tv_loading.setTextColor(color);
            //tv_loading.setText(message);
        	Log.e("",message);
        }
    });

}

void setText(final String txt) {
    getActivity().runOnUiThread(new Runnable() {
        public void run() {
        	((MainActivity)getActivity()).mNavigationDrawerFragment.onMessage(txt);
        	//tv_loading.setText(txt);
        	//Log.e("",txt);
        }
    });

}
}

   