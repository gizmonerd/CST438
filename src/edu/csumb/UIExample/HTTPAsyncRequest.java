package edu.csumb.UIExample;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

public class HTTPAsyncRequest extends AsyncTask<String, Void, String> {

	HttpURLConnection connection = null;
    DataOutputStream outputStream = null;
    DataInputStream inputStream = null;
    public AsyncResponse delegate=null;
    Context context;
    ProgressDialog dialog;
    
    File[] images;
    int numberOfImages=0;
    
    public HTTPAsyncRequest(MainActivity context,File[]images,int numberOfImages) {
        this.context=context;
        this.images = images;
        this.numberOfImages = numberOfImages;
    }
    
    public HTTPAsyncRequest(LoginActivity context,File[]images,int numberOfImages){
    	this.context=context;
    	this.images = images;
    	this.numberOfImages = numberOfImages;
    }
    
    public HTTPAsyncRequest(OfflineSaveActivity context,File[]images,int numberOfImages){
    	this.context=context;
    	this.images = images;
    	this.numberOfImages = numberOfImages;
    }
    
    @Override
	protected void onPreExecute()
	{
		dialog= new ProgressDialog(context);
		dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		dialog.setCancelable(false);
		dialog.setMessage("Contacting Server...");
		dialog.show(); 
	}
	
	@Override
	protected String doInBackground(String... args){
		// TODO Auto-generated method stub
		//send request to check if user exists
		HttpClient httpclient = new DefaultHttpClient();
		
		String uri = args[0];
		String vars = args[1];

		//**************************************
		//          upload asset code          *
		//**************************************
				
		HttpGet httpget = new HttpGet(uri+vars);
		String assetLoginServiceReturnMessage = "";
		
		try {
	        // Execute HTTP Request
	        HttpResponse response = httpclient.execute(httpget);
	        HttpEntity ht = response.getEntity();
	        BufferedHttpEntity buf = new BufferedHttpEntity(ht);
	        InputStream is = buf.getContent();
	        BufferedReader r = new BufferedReader(new InputStreamReader(is));

	        assetLoginServiceReturnMessage = r.readLine();
	        
	    } catch (ClientProtocolException e) {
	        // TODO Auto-generated catch block
	    	e.printStackTrace();
	    	return e.getMessage();
	    	
	    } catch (IOException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
	        return e.getMessage();
	        
	    }	
		
		// If there is a picture to upload here is where it is sent
		// Else, just the result from the http request above is returned.
		if (args.length>2){		
			
			//*************************************
			//      Shauns Upload Picture Code    *
			//*************************************
			
	    	//params = args[2];
	        //String pathToOurFile = params;
	        String urlServer = context.getResources().getString(R.string.uploadPicuresURI);
	
	        try
	        {
	        	//String url = "";
	            HttpClient client = new DefaultHttpClient();
	            HttpPost post = new HttpPost(urlServer);
	            MultipartEntity mpEntity = new MultipartEntity();
	            //Path of the file to be uploaded
	            //String filepath = "";
	            
	            // load images to send with request
	            for (int i=0;i<numberOfImages;i++){
	            	
		            File file = new File(images[i].getAbsolutePath());		            
		            ContentBody cbFile = new FileBody(file, "image/jpeg");         
	
		            //Add the data to the multipart entity
		            mpEntity.addPart("image"+Integer.toString(i), cbFile);
	            }
	            mpEntity.addPart("numberimages", new StringBody(Integer.toString(numberOfImages), Charset.forName("UTF-8")));
	            
	            post.setEntity(mpEntity);
	            
	            
	            
	            //Execute the post request
	            HttpResponse response1 = client.execute(post);
	            //Get the response from the server
	            HttpEntity resEntity = response1.getEntity();
	            String Response=EntityUtils.toString(resEntity);
	            Log.d("Response:", Response);
	            //Generate the array from the response
	            JSONArray jsonarray = new JSONArray("["+Response+"]");
	            JSONObject jsonobject = jsonarray.getJSONObject(0);
	            //Get the result variables from response 
	            String result = (jsonobject.getString("result"));
	            String msg = (jsonobject.getString("msg"));
	            //Close the connection
	            client.getConnectionManager().shutdown();	            
	            
	            // if not successful do not return pictureTrue
	            if (result.equals("Success")){
	            	            	
	            	//Iterate through the files if success, in order to upload filename to database for 
	            	//each image
	            		            	
	            	for (int i=0;i<numberOfImages;i++) {
	            		
	            		String filename = images[i].getName();
	            		
	            		//the third argument for this method contains pre-made get variables for images of this upload
	            		//so just the filename is concatenated to this
	            		vars = String.format("%s&FileName=%s", args[2],filename);
	            		
	            		httpget = null;
		        		httpget = new HttpGet(uri+vars);
		        		
		        		assetLoginServiceReturnMessage = "";
		        		
		        		//dialog.setMessage("Updating Database: "+filename);
		        		
		        		try {
		        	        // Execute HTTP Request
		        	        HttpResponse response = httpclient.execute(httpget);
		        	        HttpEntity ht = response.getEntity();
		        	        BufferedHttpEntity buf = new BufferedHttpEntity(ht);
		        	        InputStream is = buf.getContent();
		        	        BufferedReader r = new BufferedReader(new InputStreamReader(is));

		        	        assetLoginServiceReturnMessage = r.readLine();
		        	        
		        	        @SuppressWarnings("unused")
							String temp = "";
		        	        
		        	        
		        	    } catch (ClientProtocolException e) {
		        	        // TODO Auto-generated catch block
		        	    	e.printStackTrace();
		        	    	return "assetUploadSuccess " + e.getMessage();
		        	    	
		        	    } catch (IOException e) {
		        	        // TODO Auto-generated catch block
		        	        e.printStackTrace();
		        	        return "assetUploadSuccess " + e.getMessage();
		        	        
		        	    }
	            	}
	            	
	            	return "assetUploadSuccess pictureUploadSuccess";
	            }
	            else {
	            	return "assetUploadSuccess " + msg;
	            }	            
	        }
	        catch (Exception ex)
	        {
	              return "assetUploadSuccess "+ex.getMessage();
	        }		
		}
		else {
		// This is the return result for the login request
			return assetLoginServiceReturnMessage;	
		}
        
	}
	
	@Override
	protected void onPostExecute(String x) {
	
	    if (dialog!=null) {
	        dialog.dismiss();
	    }
	    if (delegate!=null)
	    	delegate.processFinish(x);
	    
	}

}

