package edu.csumb.UIExample;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.io.StreamCorruptedException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.ExecutionException;

import edu.csumb.UIExample.offlineSavedData;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

public class OfflineSaveActivity extends Activity implements AsyncResponse{
	
	Button backButton;
	Button saveCurrentButton;
	Button syncSelectedButton;
	Button syncAllButton;
	Button deleteAllButton;
	LinearLayout linLayout; 
	offlineSavedData offlineSavedData;
	
	HTTPAsyncRequest HTTPAsyncRequest = null;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_offlinesave);
		
		offlineSavedData=null;
		Bundle extras = getIntent().getExtras();
        if (extras != null) {
            offlineSavedData = (offlineSavedData) getIntent().getSerializableExtra("EXTRA_OBJECT_TO_SERIALIZE");
        }
        
		linLayout = (LinearLayout)findViewById(R.id.linearLayout);
		
		backButton = (Button)findViewById(R.id.backButton);
		saveCurrentButton = (Button)findViewById(R.id.saveCurrent);
		syncSelectedButton = (Button)findViewById(R.id.syncSelected);
		syncAllButton = (Button)findViewById(R.id.syncAll);
		deleteAllButton = (Button) findViewById(R.id.deleteAllButton);
		
		deleteAllButton.setOnClickListener
		(
				new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub
						File dir = OfflineSaveActivity.this.getFilesDir();
						
						for (File child : dir.listFiles()) {
							child.delete();
						}
						
						OfflineSaveActivity.this.updateListSavedFiles();
					}
				}
		);
		
		backButton.setOnClickListener
		(
			new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					
					Intent resultIntent = new Intent();

					setResult(Activity.RESULT_OK, resultIntent);
					
					if (offlineSavedData ==  null){
						resultIntent.putExtra("clearfields", true);
					}
					else{
						resultIntent.putExtra("clearfields", false);
					}
					
					finish();
				}
			}	
		);
		saveCurrentButton.setOnClickListener
		(
				new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
						if (offlineSavedData!=null){
							//validation
							if(offlineSavedData.latitude==-1 || offlineSavedData.longitude==-1) {showToast("Error: Must wait for GPS signal");return;}
							if(!offlineSavedData.lockedSensorMeasurements) {showToast("Error: Must tag location");return;}
							if (offlineSavedData.distanceEnabled && offlineSavedData.distance==0){
								showToast("Error: Must enter distance if distance adjust is checked");
								return;
							}
							if (offlineSavedData.serial.equals(""))
							{	
				                showToast("Error: Enter Serial");					                
				                return;
							}
							if(offlineSavedData.distanceEnabled && offlineSavedData.azimuth==-1){
								 showToast("Error: Bearings haven't been acquired yet");
								 return;
							}
							if(offlineSavedData.serial.length()>10){
								showToast("Error: Serial too long");
								return;
							}
							
							//Serialize the information to be saved
							byte[] theBytes = null;
							ByteArrayOutputStream bos = new ByteArrayOutputStream();
							ObjectOutput out = null;
							try {
								  out = new ObjectOutputStream(bos);   
								  out.writeObject(offlineSavedData);
								  theBytes = bos.toByteArray();
						
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} finally {
								  try {
										out.close();
								  } catch (IOException e) {
									// TODO Auto-generated catch block
										e.printStackTrace();
								  }
								  try {
										bos.close();
								  } catch (IOException e) {
									// TODO Auto-generated catch block
									  e.printStackTrace();
								  }
							}	
							
							// add date to filename
					        Calendar cal = Calendar.getInstance();			        
					        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss"); 
					        String FILENAME = "SN_" + offlineSavedData.serial + "_" + sdf.format(cal.getTime());
							
							//save serialized information to file in "files" directory
	
							FileOutputStream fos = null;
							try {
								fos = OfflineSaveActivity.this.openFileOutput(FILENAME, Context.MODE_PRIVATE);
							} catch (FileNotFoundException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							try {
								fos.write(theBytes);
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							try {
								fos.close();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							
							OfflineSaveActivity.this.updateListSavedFiles();
							offlineSavedData = null; //erase data so can't save twice in a row
						}
					}
				}
		);
		syncSelectedButton.setOnClickListener
		(
			new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					int listSavedAssets = linLayout.getChildCount();
					for (int i = 0; i < listSavedAssets; i++) {
						
				        LinearLayout horizontalLayout = (LinearLayout) linLayout.getChildAt(i);
				        boolean isChecked=false;
				        String filename="";
				        
				        for (int j=0; j<2; j++) {
				        	Object obj =  horizontalLayout.getChildAt(j);
				        
				        	if(obj.getClass().equals(CheckBox.class)){
				        		CheckBox chkBox = (CheckBox)obj;
				        		if(chkBox.isChecked()){
				        			isChecked=true;			
				        		}
				        	}
				        	if(obj.getClass().equals(TextView.class)){
				        		TextView filenameView = (TextView)obj;
				        		filename = (String) filenameView.getText();
				        	}
				        }
				        
				        if(isChecked){
				        	File dir = OfflineSaveActivity.this.getFilesDir();
							for (File child : dir.listFiles()) {
								//make sure its the selected file
								if(child.getName().equalsIgnoreCase(filename)){
									FileInputStream fis=null;
									try {
										fis = openFileInput(child.getName());
									} catch (FileNotFoundException e) {
										// TODO Auto-generated catch block							
										e.printStackTrace();
									}
									ObjectInputStream ois = null;
									try {
										ois = new ObjectInputStream(fis);
									} catch (StreamCorruptedException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									} catch (IOException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
									
									offlineSavedData loadedDataFromFile = null;
									try {
										loadedDataFromFile = (offlineSavedData) ois.readObject();
									} catch (OptionalDataException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									} catch (ClassNotFoundException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									} catch (IOException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
									
									try {
										ois.close();
									} catch (IOException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
									
									try {
										fis.close();
									} catch (IOException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
									
									
									//OfflineSaveActivity.this.updateListSavedFiles();
								}								
							}							
				        }				        
					}
					OfflineSaveActivity.this.updateListSavedFiles();
				}
			}
		);
		syncAllButton.setOnClickListener
		(
				new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub
						File dir = OfflineSaveActivity.this.getFilesDir();
						for (File child : dir.listFiles()) {
							FileInputStream fis=null;
							try {
								fis = openFileInput(child.getName());
							} catch (FileNotFoundException e) {
								// TODO Auto-generated catch block							
								e.printStackTrace();
							}
							ObjectInputStream ois = null;
							try {
								ois = new ObjectInputStream(fis);
							} catch (StreamCorruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							
							offlineSavedData loadedDataFromFile = null;
							try {
								loadedDataFromFile = (offlineSavedData) ois.readObject();
							} catch (OptionalDataException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (ClassNotFoundException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							
							try {
								ois.close();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							
							try {
								fis.close();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							
							if(uploadAsset(loadedDataFromFile)){
								child.delete();
							}
							OfflineSaveActivity.this.updateListSavedFiles();
						}
						
					}
				}
		);
		OfflineSaveActivity.this.updateListSavedFiles();
	}
	
	void updateListSavedFiles(){	
			
		if(((LinearLayout) linLayout).getChildCount() > 0) 
		    ((LinearLayout) linLayout).removeAllViews(); 
		
		File dir = OfflineSaveActivity.this.getFilesDir();
		for (File child : dir.listFiles()) {
	    // Do something with child
			  
			LinearLayout horizontalLayout = new LinearLayout(OfflineSaveActivity.this);
			horizontalLayout.setOrientation(LinearLayout.HORIZONTAL);
			  
			TextView txtCol = new TextView(this);
			txtCol.setText(child.getName());
			txtCol.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, 
			LayoutParams.WRAP_CONTENT));
			  
			CheckBox selectBox = new CheckBox(OfflineSaveActivity.this);
			  
			horizontalLayout.addView( txtCol, 0 );
			horizontalLayout.addView( selectBox, 0 );
			  
			linLayout.addView( horizontalLayout, 0 ); 
		  }
	}
	
	boolean uploadAsset(offlineSavedData data){
		
		//get data from ui and upload
		int assetType = data.type;
		
		float distance = data.distance; 
			
			//unit conversion for distance
		switch(data.distanceUnits){
			
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
		
		double dist = distance/6371;
		double brng = data.azimuth;
		double lat1 = Math.toRadians(data.latitude);
		double lon1 = Math.toRadians(data.longitude);

		double lat2 = Math.asin( Math.sin(lat1)*Math.cos(dist) + Math.cos(lat1)*Math.sin(dist)*Math.cos(brng) );
		double a = Math.atan2(Math.sin(brng)*Math.sin(dist)*Math.cos(lat1), Math.cos(dist)-Math.sin(lat1)*Math.sin(lat2));
		System.out.println("a = " +  a);
		double lon2 = lon1 + a;
		
		lat2=Math.toDegrees(lat2);
		lon2=Math.toDegrees(lon2);
		
		String serialNumber;
		serialNumber = data.serial;								
			
		String getVariablesAsset;
		String getVariablesImage;
		
		if(data.distanceEnabled)
		{
			//if check box is checked then use distance lat long
			 getVariablesAsset = String.format("type=%d&lat=%f&long=%f&username=%s&serialNumber=%s&notes=%s",assetType,lat2,lon2,data.username,serialNumber,data.notesText);
			 //filename is added later in HTTPAsyncRequest
			 getVariablesImage = String.format("lat=%f&long=%f&username=%s", lat2,lon2,data.username);
		}
		else
		{
			//if not original string is used
			getVariablesAsset = String.format("type=%d&lat=%f&long=%f&username=%s&serialNumber=%s&notes=%s",assetType,data.latitude,data.longitude,data.username,serialNumber,data.notesText);
			//filename is added later in HTTPAsyncRequest
			getVariablesImage = String.format("lat=%f&long=%f&username=%s", data.latitude,data.longitude,data.username);
		}
		getVariablesAsset = getVariablesAsset.replaceAll(" ", "_");
		getVariablesImage = getVariablesImage.replaceAll(" ", "_");				
		
		String getRequestInput[] = {
				getResources().getString(R.string.assetLoginServiceURI),
				getVariablesAsset,
				getVariablesImage
			};
		
		HTTPAsyncRequest = null;
		HTTPAsyncRequest = new HTTPAsyncRequest(OfflineSaveActivity.this,data.images,data.numberOfCurrentImages);
        HTTPAsyncRequest.delegate = this;
		
		//assetUpload
		try {
			HTTPAsyncRequest.execute(getRequestInput);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			showToast("Error: "+e.getMessage());
			return false;
		} 
			
		return true;
	}
	
	void showToast(String message){
		Context context = getApplicationContext();
		int duration = Toast.LENGTH_LONG;
	    Toast toast = Toast.makeText(context,message,duration);
	    toast.show();
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
				
		}
		else if (output.contains("assetUploadSuccess")){
			
		// If just asset info was successfully uploaded	
			String errorMessage = output.replace("assetUploadSuccess", "");
			showToast("Asset info uploaded but picture was not. \nError: "+errorMessage);
			
		}
		else if (output.contains("pictureUploadSuccess")){
			
		// If just picture was successfully uploaded
			String errorMessage = output.replace("pictureUploadSuccess", "");
			showToast("Picture was uploaded but asset info was not. \nError: "+errorMessage);
		}
		else {
		
		// Neither picture or asset info was successfully uploaded
			showToast("ERROR:Unable to establish connection with server\nMSG: " + output);
		}
			
		
	}
}





































