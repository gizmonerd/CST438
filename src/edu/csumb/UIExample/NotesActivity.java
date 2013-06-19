package edu.csumb.UIExample;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class NotesActivity extends Activity {
	
	EditText notesTextView;
	Button saveNotesButton;
	String notesText;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_notes);
		
		notesTextView = (EditText)findViewById(R.id.NotesTextView);
		saveNotesButton = (Button)findViewById(R.id.submitNotes);
		saveNotesButton.setOnClickListener
		(
				new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub
						
						Intent resultIntent = new Intent();
						
						resultIntent.putExtra("notes", notesTextView.getText().toString());
						// TODO Add extras or a data URI to this intent as appropriate.
						setResult(Activity.RESULT_OK, resultIntent);
						finish();
					}
				}
		);
		
		//gather notes if already entered
		notesText="";
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            notesText = extras.getString("EXTRA_NOTES_TEXT");
        }
        notesTextView.setText(notesText);
	}
}
