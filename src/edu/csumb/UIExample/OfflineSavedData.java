package edu.csumb.UIExample;

import java.io.File;
import java.io.Serializable;

@SuppressWarnings("serial")
class offlineSavedData implements Serializable
{
	File []images;
	int numberOfCurrentImages;
	float latitude;
	float longitude;
	float azimuth;
	String serial;
	int type;
	String notesText;
	boolean distanceEnabled;
	boolean lockedSensorMeasurements;
	int distance;
	int distanceUnits;
	String username;
	
	offlineSavedData(){
		images=null;
		numberOfCurrentImages=0;
		latitude=0;
		longitude=0;
		azimuth=0;
		serial="";
		type=0;
		notesText="";
		username="";
		distanceEnabled=false;
		lockedSensorMeasurements=false;
		distance=0;
		distanceUnits=0;
	}
}
