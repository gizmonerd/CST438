package edu.csumb.UIExample;

import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.Toast;

public class LoginActivity extends Activity implements OnClickListener,AsyncResponse {
	
	SharedPreferences userSessionSettings;
    SharedPreferences.Editor prefEditor ;
	
    HTTPAsyncRequest isValidUser = null;
    
	@SuppressLint("CommitPrefEdits")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.second);
		
		//get the Button reference
		//Button is a subclass of View
		//buttonClick if from main.xml "@+id/buttonClick"
	        View v = findViewById(R.id.loginButton);
		//set event listener
	        v.setOnClickListener(this);
	        
	        //load session file for user login
	        userSessionSettings = this.getSharedPreferences("tagapppref", Context.MODE_PRIVATE);
	        prefEditor = userSessionSettings.edit();
	       	        
			boolean signedIn = userSessionSettings.getBoolean("SignedIn", false);
			if (signedIn){
				Intent intent = new Intent(this,MainActivity.class);
				intent.putExtra("EXTRA_USER_ID", userSessionSettings.getString("User", ""));
				this.startActivity(intent);
			}
	}
	
	private EditText userInput;
	@Override
	public void onClick(View arg0) {
		// TODO Auto-generated method stub
		
		if(arg0.getId() == R.id.loginButton){
			//collect user login input
			userInput = (EditText)findViewById(R.id.passwordText);
			String password = userInput.getText().toString();
			userInput = (EditText)findViewById(R.id.emailText);
			String email = userInput.getText().toString();
			
			boolean validEmail = this.isEmailValid(email);
			
			if( validEmail && password.length() > 0 ){
								
				String getRequestInput[] = {
										getResources().getString(R.string.assetLoginServiceURI),
										String.format("email=%s&password=%s",email,password)
									};
				
				String isValidUser = "";
				
				
				
				try {
					isValidUser = new HTTPAsyncRequest(LoginActivity.this,null,0).execute(getRequestInput).get();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ExecutionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			    if ( !isValidUser.equals("") && isValidUser.equalsIgnoreCase("true")){
					//define a new Intent for the second Activity
					Intent intent = new Intent(this,MainActivity.class);
					intent.putExtra("EXTRA_USER_ID", email);
			 
					//Sign user into session so doesn't need to login next time					
			        prefEditor.putBoolean("SignedIn", true);  
			        prefEditor.putString("User", email);  
			        prefEditor.commit();  
			       			        										
					//start the second Activity
			        
					this.startActivity(intent);
			    }
			    else {
			    	int duration = Toast.LENGTH_LONG;
			    	Context context = getApplicationContext();
			        Toast toast = Toast.makeText(context, "Invalid Login", duration);
	                toast.show();
			    }
			}
			else{
				int duration = Toast.LENGTH_LONG;
				Context context = getApplicationContext();
				Toast toast;
				
				if(password.length()<1){
			    	toast = Toast.makeText(context, "Enter Password", duration);
	                toast.show();
				}
				else if(!validEmail){
					toast = Toast.makeText(context, "Invalid Email Entered", duration);
	                toast.show();
				}
			}
		}
	}
	
	/**
	 * method is used for checking valid email id format.
	 * 
	 * @param email
	 * @return boolean true for valid false for invalid
	 */
	public boolean isEmailValid(String email) {
	    boolean isValid = false;

	    String expression = "^[\\w\\.-]+@([\\w\\-]+\\.)+[A-Z]{2,4}$";
	    CharSequence inputStr = email;

	    Pattern pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE);
	    Matcher matcher = pattern.matcher(inputStr);
	    if (matcher.matches()) {
	        isValid = true;
	    }
	    
	    return isValid;
	}

	@Override
	public void processFinish(String output) {
		// TODO Auto-generated method stub
		
	}
}



