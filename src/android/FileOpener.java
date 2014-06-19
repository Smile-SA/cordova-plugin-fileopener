package fr.smile.cordova.fileopener;

import java.io.File;
import java.util.HashMap;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import android.os.Environment;
import android.content.pm.PackageManager;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.database.Cursor;

import android.content.ActivityNotFoundException;

/**
 * This class echoes a string called from JavaScript.
 */
public class FileOpener extends CordovaPlugin {

    private static final HashMap<String, String> MIME_TYPES;
        static
        {
            MIME_TYPES = new HashMap<String, String>();
            MIME_TYPES.put(".pdf", "application/pdf");
            MIME_TYPES.put(".doc", "application/msword");
            MIME_TYPES.put(".docx", "application/msword");
            MIME_TYPES.put(".xls", "application/vnd.ms-powerpoint");
            MIME_TYPES.put(".xlsx", "application/vnd.ms-powerpoint");
            MIME_TYPES.put(".rtf", "application/vnd.ms-excel");
            MIME_TYPES.put(".wav", "audio/x-wav");
            MIME_TYPES.put(".gif", "image/gif");
            MIME_TYPES.put(".jpg", "image/jpeg");
            MIME_TYPES.put(".jpeg", "image/jpeg");
            MIME_TYPES.put(".png", "image/png");
            MIME_TYPES.put(".txt", "text/plain");
            MIME_TYPES.put(".mpg", "video/*");
            MIME_TYPES.put(".mpeg", "video/*");
            MIME_TYPES.put(".mpe", "video/*");
            MIME_TYPES.put(".mp4", "video/*");
            MIME_TYPES.put(".avi", "video/*");
            MIME_TYPES.put(".ods", "application/vnd.oasis.opendocument.spreadsheet");
            MIME_TYPES.put(".odt", "application/vnd.oasis.opendocument.text");
            MIME_TYPES.put(".ppt", "application/vnd.ms-powerpoint");
            MIME_TYPES.put(".pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");
            MIME_TYPES.put(".apk", "application/vnd.android.package-archive");
        }

    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        Context context= cordova.getActivity().getApplicationContext();
        boolean hasExtension = false;
        String extension = null;
        JSONObject obj = new JSONObject();
        
        if(isCorrectRequest(args,callbackContext)){
        	extension = args.getString(0).substring(args.getString(0).lastIndexOf("."));
        	hasExtension = true;
        }

        if ("canOpenFile".equals(action)) {
            if(hasExtension){
                obj.put("extension", extension);
                obj.put("canBeOpen", this.canOpenFile(extension,context));
                callbackContext.success(obj);
            }
            return true;
        }
        else if ("openFile".equals(action)) {
        	if(hasExtension){
        		 this.downloadAndOpenFile(context,args.getString(0),callbackContext);
	        }
	        return true;
        }
        else{
            return false;
        }

    }

    private boolean isCorrectRequest(final JSONArray args, CallbackContext callbackContext) throws JSONException {
    	JSONObject obj = new JSONObject();
    	if(args.length() > 0) {
    		String url = args.getString(0);
    		if(url.lastIndexOf(".") > -1){
    			String extension = url.substring(url.lastIndexOf("."));
    			if(hasMimeType(extension)){
    			    return true;
    			}
    			else{
                    obj.put("message", "This extension: "+extension+" is not supported by the FileOpener plugin");
                    callbackContext.error(obj);
                    return false;
    			}
    		}
    		else{
    			obj.put("message", "This file :"+url+" has no extension");
        		callbackContext.error(obj);
        		return false;
    		}
    	}
    	else{
    		obj.put("message", "Parameter is missing");
    		callbackContext.error(obj);
    		return false;
    	}
    }

    private boolean hasMimeType(String extension) {
        return MIME_TYPES.containsKey(extension);
    }

    private String getMimeType(String extension) {
            return MIME_TYPES.get(extension);
    }

    private boolean canOpenFile(String extension, Context context) {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            final File tempFile = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "test"+extension);
            i.setDataAndType(Uri.fromFile(tempFile), getMimeType(extension));
            return context.getPackageManager().queryIntentActivities(i, PackageManager.MATCH_DEFAULT_ONLY).size() > 0;
        }

    private void openFile(Uri localUri, String extension, Context context, CallbackContext callbackContext) throws JSONException {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.setDataAndType(localUri, getMimeType(extension));
            JSONObject obj = new JSONObject();

            try{
                context.startActivity(i);
                obj.put("message","successfull downloading and openning");
                callbackContext.success(obj);
            }
            catch(ActivityNotFoundException e){
                obj.put("message","Failed to open the file, no reader found");
                obj.put("ActivityNotFoundException",e.getMessage());
                callbackContext.error(obj);
            }


        }

     private void downloadAndOpenFile(final Context context, final String fileUrl, final CallbackContext callbackContext) {
            final String filename = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
            final String extension = fileUrl.substring(fileUrl.lastIndexOf("."));
            final File tempFile = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), filename);

            if (tempFile.exists()) {
                 try{
                    openFile(Uri.fromFile(tempFile),extension,context,callbackContext);
                }
                catch(JSONException e)
                {
                    e.printStackTrace();
                }
                return;
            }

            DownloadManager.Request r = new DownloadManager.Request(Uri.parse(fileUrl));
            r.setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, filename);
            final DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            BroadcastReceiver onComplete = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {

                    context.unregisterReceiver(this);

                    long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                    Cursor c = dm.query(new DownloadManager.Query().setFilterById(downloadId));

                    if (c.moveToFirst()) {
                        int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
                        if (status == DownloadManager.STATUS_SUCCESSFUL)
                            try{
                               openFile(Uri.fromFile(tempFile),extension,context,callbackContext);
                            }
                            catch(JSONException e){e.printStackTrace(); }
                    }
                    c.close();
                }
            };
            context.registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

            dm.enqueue(r);
        }

}