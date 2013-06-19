package edu.csumb.UIExample;

import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutionException;

import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;

public class MainActivity extends Activity implements AsyncResponse{
	
	//**************
	//   SENSORS   *
	//**************
	boolean lockedSensorMeasurements=false;
	
	//for compass
	float[] mGravity;
	float[] mGeomagnetic;
	float azimuth;
	SensorManager mSensorManager;
	Sensor magnetField;	
	SensorEventListener sensorListener;
	Sensor accelerometer;
	
	//for lat/long
    LocationManager mlocManager; 
    LocationListener mlocListener; 
	private double longitude;
	private double latitude;
	
	
	//************************************************************************************
	//Set true for simulator to use these values for coords so calculation can be tested *
	//************************************************************************************
	final boolean simulated = false;
	
	final float latsimvalue=(float) 37.0;
	final float longsimvalue=(float) -122.0;
	final float azimuthsimvalue = (float) Math.toRadians(50);
	//*************************************************************************************
	
	//notes feature
	String notesText="";
	
	//asset types
	Spinner Items;
	
	//File image;
	EditText serialTextField;
	Button mcamera;
	Button mtag;
	Button logout;
	Button notesSubmit;
	Button syncSave;
	
	//TextView longitudeText;
    TextView coordsText;
    TextView usernameTextView;
    EditText distanceField;
    CheckBox checkBox;
    
    //for login session
    SharedPreferences userSessionSettings;
    SharedPreferences.Editor prefEditor;
    
    //for images
	int numberOfCurrentImages;
	File[] images;
	TextView numberImagesText;	
	
	String username ="";
	offlineSavedData offlineSavedData;
	HTTPAsyncRequest HTTPAsyncRequest = null;
	//private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
	//private Uri fileUri;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        //gather signed in username
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            username = extras.getString("EXTRA_USER_ID");
        }
        
        //initialize to out of range value, so when upload asset we know if lat/long has been set
        longitude=-1;
        latitude=-1;
        azimuth=-1;
        
        //bearings=-1;
        notesText="";
        
        numberOfCurrentImages=0;
        images = new File[10];
        for(int i=0;i<10;i++){
        	images[i]=null;
        }
        
        //numberImagesLabel.setText("Images: 0");
       
        setContentView(R.layout.activity_main);
        
        //fill asset list-box from string array resource
        List<String> SpinnerArray =  new ArrayList<String>();
    	
        String[] assets = getResources().getStringArray(R.array.asset_array);
        for (String asset : assets) {
        	SpinnerArray.add(asset);
        }
   	
        checkBox = (CheckBox) findViewById(R.id.checkBox1);
		distanceField =(EditText) findViewById(R.id.DistanceField);
		distanceField.setBackgroundColor(Color.argb(150, 178, 218, 247));
		final Spinner distancespin = (Spinner) findViewById(R.id.distanceSpinner);
        
		//initialize compass		
		mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
		magnetField = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		
		
		lockedSensorMeasurements = false;
		
		
		
		//initialize lat/long
		mlocManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
	    mlocListener = new MyLocationListener();
	    mlocManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mlocListener);
	
		sensorListener = new SensorEventListener()
		{
			// compass listener
			@Override
			public void onAccuracyChanged(Sensor sensor, int accuracy) { }

			@Override
			public void onSensorChanged(SensorEvent event) {
				// TODO Auto-generated method stub			
				// calculate compass degrees using accelerometer and mag field sensors
				
					
				if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
					mGravity = event.values;
				if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
					mGeomagnetic = event.values;
				
				if (mGravity != null && mGeomagnetic != null) {
				      float R[] = new float[16];
				      float I[] = new float[16];
				      boolean success = android.hardware.SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
				      if (success) {
				        float orientation[] = new float[3];
				        float[] outR = new float[16];
				        SensorManager.remapCoordinateSystem(R, SensorManager.AXIS_X, SensorManager.AXIS_Z, outR); 
				        android.hardware.SensorManager.getOrientation(outR, orientation);
				        
				        if(!lockedSensorMeasurements){
				        	azimuth = orientation[0]; // orientation contains: azimut, pitch and roll
				        	
				        	if(latitude!=-1 && longitude!=-1 && azimuth!=-1){
				        		coordsText.setText("LAT:"+Double.toString(latitude)+"\nLONG:"+Double.toString(longitude)+"\nBEARING: " + Double.toString((float)Math.toDegrees (azimuth)));
				        	}
				        	else if (azimuth!=-1){
				        		coordsText.setText("LAT/LONG: waiting for signal"+"\nBEARING: " + Double.toString((float)Math.toDegrees (azimuth)));
				        	}
				        	else {
				        		coordsText.setText("LAT/LONG: waiting for signal"+"\nBEARING: loading");
				        	}
				        }
				      }
				}				
			}			
		};
		mSensorManager.registerListener(sensorListener, magnetField, SensorManager.SENSOR_DELAY_NORMAL);
		mSensorManager.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
		
		//tag location button disables sensor listeners so coordinates are frozen
		mtag = (Button) findViewById(R.id.tagButton);
		
		mtag.setOnClickListener
    	(
    		new View.OnClickListener() {
    			public void onClick(View v) { 
    				
    				if (lockedSensorMeasurements){
    					lockedSensorMeasurements = false;
    					coordsText.setTextColor(Color.BLACK);
    				}
    				else{ 					
    					lockedSensorMeasurements = true;
    					coordsText.setTextColor(Color.RED);
    				}
    				
    			}	
    		}
    	);
		
    	ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, SpinnerArray);
    	adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    	Items = (Spinner) findViewById(R.id.spatial);
    	Items.setAdapter(adapter);   	 	
        
        mcamera = (Button) findViewById(R.id.cameraButton);
        serialTextField  = (EditText) findViewById(R.id.serialTextField);
        //longitudeText = (TextView) findViewById(R.id.longitude);
        coordsText = (TextView) findViewById(R.id.lattitude);
        coordsText.setText("LAT/LONG: waiting for signal"+"\nBEARING: loading");
        numberImagesText = (TextView) findViewById(R.id.numImagesLabel);
        userSessionSettings = this.getSharedPreferences("tagapppref", Context.MODE_PRIVATE);
        prefEditor = userSessionSettings.edit();
        usernameTextView = (TextView) findViewById(R.id.usernameLabel);
        usernameTextView.setText("Login as: "+username.toString());
        notesSubmit = (Button) findViewById(R.id.notesButton);
        syncSave = (Button) findViewById(R.id.syncSaveButton);
        offlineSavedData = new offlineSavedData();
        
        syncSave.setOnClickListener
        (
        		new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
						// load UI data into object
						if(simulated){
							offlineSavedData.latitude = (float)latsimvalue;
							offlineSavedData.longitude = (float)longsimvalue;
						}else{
							offlineSavedData.latitude = (float)latitude;
							offlineSavedData.longitude = (float)longitude;
						}
						offlineSavedData.azimuth = (float)azimuth;
						if (!serialTextField.getText().toString().equals(""))
							offlineSavedData.serial = serialTextField.getText().toString();
						offlineSavedData.images = images;
						offlineSavedData.type = Items.getSelectedItemPosition()+1;
						offlineSavedData.username = username;
						offlineSavedData.numberOfCurrentImages = numberOfCurrentImages;
						offlineSavedData.lockedSensorMeasurements = lockedSensorMeasurements;
						
						if(checkBox.isChecked()){
							offlineSavedData.distanceEnabled = true;
							if (!distanceField.getText().toString().equals(""))
								offlineSavedData.distance = Integer.parseInt(distanceField.getText().toString());
							offlineSavedData.distanceUnits = distancespin.getSelectedItemPosition();
						}
						offlineSavedData.notesText = notesText;						
						
						Intent intent = new Intent(MainActivity.this,OfflineSaveActivity.class);
						intent.putExtra("EXTRA_OBJECT_TO_SERIALIZE", (Serializable)offlineSavedData);
						
						//start the second Activity				        
						MainActivity.this.startActivityForResult(intent,3);
					}
				}
        );
        
        //for logging out of session
        logout = (Button) findViewById(R.id.logoutButton);
        
        notesSubmit.setOnClickListener
        (
        		new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub
						Intent intent = new Intent(MainActivity.this,NotesActivity.class);
						intent.putExtra("EXTRA_NOTES_TEXT", notesText);
				 		        										
						//start the second Activity				        
						MainActivity.this.startActivityForResult(intent,0);
					}
				}
        );
        
        //listener for logging out button
        logout.setOnClickListener
        (
        		new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub
						// logout session
						prefEditor.putBoolean("SignedIn", false);
						prefEditor.putString("User", "");
						prefEditor.commit();  
						
						Intent intent = new Intent(MainActivity.this,LoginActivity.class);
						MainActivity.this.startActivity(intent);
						
					}
				}
        );
                      
        List<String> SpinnerArrayDistance =  new ArrayList<String>();
        String[] distassets = getResources().getStringArray(R.array.distance_array);
        for (String distasset : distassets) {
        	SpinnerArrayDistance.add(distasset);
        }
        
        ArrayAdapter<String> distadapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, SpinnerArrayDistance);
    	distadapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    	distancespin.setAdapter(distadapter);
        
        Button uploadAsset = (Button) findViewById(R.id.uploadAssetButton);
        Button clearButton = (Button) findViewById(R.id.clearFieldsButton);
        
        clearButton.setOnClickListener
        (
    		new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					
					//delete image files
					for (int i=0; i<numberOfCurrentImages ;i++){
						images[i].delete();
					}
					
					//reset UI after clear
					numberOfCurrentImages = 0;
					serialTextField.setText("");
					Items.setSelection(0);
					longitude=-1;
			        latitude=-1;
			        azimuth=-1;
			        lockedSensorMeasurements = false;
					coordsText.setTextColor(Color.BLACK);
					notesText="";
			        
			       
			        
			        distanceField.setText("");
			        checkBox.setChecked(false);
					numberImagesText.setText("Images: "+numberOfCurrentImages);
					notesText = "";
				}
			}
        );
        
        uploadAsset.setOnClickListener
        (
        			new View.OnClickListener() {		
						@Override
						public void onClick(View v) {
							
							if (simulated) {
								latitude = latsimvalue;
								longitude = longsimvalue;
								azimuth = azimuthsimvalue;
							}
							
							//make sure fields are filled and valid
							
							if(!lockedSensorMeasurements) {showToast("Error: Must tag location");return;}
							if(latitude==-1 || longitude==-1) {showToast("Error: Must wait for GPS signal");return;}
							
							if (checkBox.isChecked() && distanceField.getText().toString().equals("")){
								showToast("Error: Must enter distance if distance adjust is checked");
								return;
							}
							if (serialTextField.getText().toString().equals(""))
							{	
				                showToast("Error: Enter Serial");					                
				                return;
							}
							if(checkBox.isChecked() && azimuth==-1){
								 showToast("Error: Bearings haven't been acquired yet");
								 return;
							}
							if(serialTextField.getText().toString().length()>10){
								showToast("Error: Serial number too long");
								return;
							}
							
							
							//get data from ui and upload
							int assetType = Items.getSelectedItemPosition()+1;
							
							float distance;
							
							if (distanceField.getText().toString().equals("")){
								distance = 0;
							}
							else{
								distance = (float) Double.parseDouble(distanceField.getText().toString());
								
								//unit conversion for distance
								switch(distancespin.getSelectedItemPosition()){
								
									case 0://Kilometers
										break;
										
									case 1://Miles
										distance *= 1.60934;break;
										
									case 2://Meters
										distance *= 0.001;break;
										
									case 3://Yards
										distance *= 0.0009144;break;
										
									case 4://Feet
										distance *= 0.0003048;break;										
								}						
							}
							
							double dist = distance/6371;
							double brng = azimuth;
							double lat1 = Math.toRadians(latitude);
							double lon1 = Math.toRadians(longitude);

							double lat2 = Math.asin( Math.sin(lat1)*Math.cos(dist) + Math.cos(lat1)*Math.sin(dist)*Math.cos(brng) );
							double a = Math.atan2(Math.sin(brng)*Math.sin(dist)*Math.cos(lat1), Math.cos(dist)-Math.sin(lat1)*Math.sin(lat2));
							System.out.println("a = " +  a);
							double lon2 = lon1 + a;
							
							lat2=Math.toDegrees(lat2);
							lon2=Math.toDegrees(lon2);
							
							String serialNumber;
							serialNumber = serialTextField.getText().toString();								
								
							String getVariablesAsset;
							String getVariablesImage;
							
							if(checkBox.isChecked())
							{
								//if check box is checked then use distance lat long
								 getVariablesAsset = String.format("type=%d&lat=%f&long=%f&username=%s&serialNumber=%s&notes=%s",assetType,lat2,lon2,username,serialNumber,notesText);
								 //filename is added later in HTTPAsyncRequest
								 getVariablesImage = String.format("lat=%f&long=%f&username=%s", lat2,lon2,username);
							}
							else
							{
								//if not original string is used
								getVariablesAsset = String.format("type=%d&lat=%f&long=%f&username=%s&serialNumber=%s&notes=%s",assetType,latitude,longitude,username,serialNumber,notesText);
								//filename is added later in HTTPAsyncRequest
								getVariablesImage = String.format("lat=%f&long=%f&username=%s", latitude,longitude,username);
							}
							getVariablesAsset = getVariablesAsset.replaceAll(" ", "_");
							getVariablesImage = getVariablesImage.replaceAll(" ", "_");				
							
							String getRequestInput[] = {
									getResources().getString(R.string.assetLoginServiceURI),
									getVariablesAsset,
									getVariablesImage
								};
							
							HTTPAsyncRequest = null;
							HTTPAsyncRequest = new HTTPAsyncRequest(MainActivity.this,images,numberOfCurrentImages);
					        HTTPAsyncRequest.delegate = MainActivity.this;
							
							//assetUpload
							try {
								HTTPAsyncRequest.execute(getRequestInput);
							} catch (Exception e){
								showToast("Error: "+e.getMessage());
							}
							
							//reset UI after upload/save
							numberOfCurrentImages = 0;
							serialTextField.setText("");
							Items.setSelection(0);
							longitude=-1;
					        latitude=-1;
					        azimuth=-1;
					        lockedSensorMeasurements = false;
	    					coordsText.setTextColor(Color.BLACK);
	    					notesText="";
					        
					        
					        
					        notesText = "";

					        distanceField.setText("");
					        checkBox.setChecked(false);
					        
							numberImagesText.setText("Images: "+numberOfCurrentImages);
							
						}
					}
        	);
        
        mcamera.setOnClickListener( new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				//check if max pictures taken
				if(numberOfCurrentImages<10){
					
					Intent imageIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			        File imagesFolder = new File(Environment.getExternalStorageDirectory(), "MyImages");
			        imagesFolder.mkdirs(); 
			        
			        // add date to filename
			        Calendar cal = Calendar.getInstance();
			        
			        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss"); 
			        String filename = username + "_SN(" + serialTextField.getText().toString() + ")_" + sdf.format(cal.getTime()) + ".jpg";
			        
			        images[numberOfCurrentImages] = new File(imagesFolder, filename);
			        
			        Uri uriSavedImage = Uri.fromFile(images[numberOfCurrentImages]);
			        imageIntent.putExtra(MediaStore.EXTRA_OUTPUT, uriSavedImage);
			        
			        //case 5 in the onactivityresult method handles result of camera intent
			        startActivityForResult(imageIntent,5);
			        
			        //images[numberOfCurrentImages].getAbsolutePath();
			        /*
			        if (images[numberOfCurrentImages]!=null){
				        numberOfCurrentImages++;
				        
				        //update label
				        numberImagesText.setText("Images: "+numberOfCurrentImages);
			        }*/
				}
				else{
					showToast("Max images captured for this asset -Please save or upload asset");
				}
			}
		});
    }
    
    void showToast(String message){
    	Context context = getApplicationContext();
		int duration = Toast.LENGTH_LONG;
        Toast toast = Toast.makeText(context,message,duration);
        toast.show();
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
      super.onActivityResult(requestCode, resultCode, data);
      switch(requestCode) {
        case (0) : {
          if (resultCode == Activity.RESULT_OK) {
        	  // TODO Extract the data returned from the child Activity.
        	  notesText = data.getStringExtra("notes");
          }
          break;
        }
        
        case (3) : {
            if (resultCode == Activity.RESULT_OK) {
            	if(data.getBooleanExtra("clearfields", false)){
	            	//delete image files
            		/*
					for (int i=0; i<numberOfCurrentImages ;i++){
						images[i].delete();
					}*/
					
					//reset UI after clear
					numberOfCurrentImages = 0;
					serialTextField.setText("");
					Items.setSelection(0);
					longitude=-1;
			        latitude=-1;
			        azimuth=-1;
			        lockedSensorMeasurements = false;
					coordsText.setTextColor(Color.BLACK);
					notesText="";
			        
			       
			        
			        distanceField.setText("");
			        checkBox.setChecked(false);
					numberImagesText.setText("Images: "+numberOfCurrentImages);
					notesText = "";
            	}
            }
            break;
        }
        
        case (5) : {
        	if (resultCode == Activity.RESULT_OK) {
        		if (images[numberOfCurrentImages]!=null){
			        numberOfCurrentImages++;
			        
			        //update label
			        numberImagesText.setText("Images: "+numberOfCurrentImages);
		        }
        		
        		
        	}
        	break;
        }
      } 
    }
    
    private class MyLocationListener implements LocationListener
    {

      @Override
      public void onLocationChanged(Location loc)
      {
    	if (!lockedSensorMeasurements) {  
	        loc.getLatitude();
	        loc.getLongitude();
	
	        latitude = loc.getLatitude();
	        longitude = loc.getLongitude();
	        
	        coordsText.setText("LAT:"+Double.toString(latitude)+"\nLONG:"+Double.toString(longitude)+"\nBEARING: " + Double.toString((float)Math.toDegrees (azimuth)));
    	}
      }

      @Override
      public void onProviderDisabled(String provider)
      {
        Toast.makeText( getApplicationContext(), "Gps Disabled", Toast.LENGTH_SHORT ).show();
      }

      @Override
      public void onProviderEnabled(String provider)
      {
        Toast.makeText( getApplicationContext(), "Gps Enabled", Toast.LENGTH_SHORT).show();
      }

      @Override
      public void onStatusChanged(String provider, int status, Bundle extras)
      {

      }
    }

	@Override
	public void processFinish(String output) {
		// TODO Auto-generated method stub
		//***************************************************
		// This is where we can present user with option to *
		// save the data for later, if upload doesn't work  *
		//***************************************************
		
		// If everything was success display ok message
		if (output.contains("assetUploadSuccess")
				&& output.contains("pictureUploadSuccess"))
		{
			showToast("Success");
			
			//delete files after upload
			for (int i=0; i<numberOfCurrentImages ;i++){
				images[i].delete();
			}
		}
		else if (output.contains("assetUploadSuccess")){
			
		// If just asset info was successfully uploaded	
		// TODO: store picture for later upload
			String errorMessage = output.replace("assetUploadSuccess", "");
			showToast("Asset info uploaded but picture was not. \nError: "+errorMessage);
			
		}
		else if (output.contains("pictureUploadSuccess")){
			
		// If just picture was successfully uploaded
		// TODO: store asset info for later upload
			String errorMessage = output.replace("pictureUploadSuccess", "");
			showToast("Picture was uploaded but asset info was not. \nError: "+errorMessage);
		}
		else {
		
		// Neither picture or asset info was successfully uploaded
		// TODO: store both asset info and picture for later upload
			showToast("ERROR:Unable to establish connection with server\nMSG: " + output);
		}
	}
}
		
        
        
        
        
