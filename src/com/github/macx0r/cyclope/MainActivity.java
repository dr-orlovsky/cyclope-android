package com.github.macx0r.cyclope;

import java.util.*;
import android.hardware.*;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class MainActivity extends Activity implements SensorEventListener {
	private SensorManager mSensorManager;
	private Sensor mMagneticSensor;
	private boolean mInterference;
	private int mIonIndex;
	private double mMagneticFieldStrength;
	
	static public class IonInfo {
		public double mass;
		public double charge;
		public String name;
		public String briefName;
		public IonInfo(double m, double c, String n, String sn) {
			this.mass = m;
			this.charge = c;
			this.name = n;
			this.briefName = sn;
		}
	};
	static IonInfo[] Ions = {
		new IonInfo(24.305, 2, "Magnesium", "Mg⁺"),
		new IonInfo(40.078, 2, "Calcium", "Ca²⁺"),
		new IonInfo(06.941, 1, "Lithium", "Li⁺"),
		new IonInfo(22.989769, 1, "Sodium", "Na⁺"),
		new IonInfo(39.0983, 1, "Potassium", "K⁺")
	};
	
	Switch mRunMagnetometer;
	Spinner mIonSpinner;
	EditText mMFValue;
	EditText mCFValue;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		mInterference = false;
		mRunMagnetometer = (Switch) findViewById(R.id.runMagnetometer);
		mRunMagnetometer.setChecked(true);
		
		mIonIndex = 0;
		mIonSpinner = (Spinner) findViewById(R.id.ionSpinner);
		ArrayList<String> ions = new ArrayList<String>();
		for (MainActivity.IonInfo ion : MainActivity.Ions) {
			ions.add(ion.name);
		}
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, ions);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mIonSpinner.setAdapter(adapter);
		mIonSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
		    public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
				mIonIndex = position;
		    }

		    @Override
		    public void onNothingSelected(AdapterView<?> parentView) {
		    	return;
		    }
		});
		
		mMFValue = (EditText) findViewById(R.id.magnetometerValue);
		mCFValue = (EditText) findViewById(R.id.cyclotroneFreq);
		
		//mMFValue.setEnabled(false);
		//mCFValue.setEnabled(false);
		mMFValue.setFocusable(false);
		mCFValue.setFocusable(false);
		
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		if ((mMagneticSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)) != null) {
			this.startMagnetometer();
		} else {
			Toast.makeText(getApplicationContext(), "No magnetometer present on device", Toast.LENGTH_LONG).show();
			mMFValue.setText("–");
		}
	
		mRunMagnetometer.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					startMagnetometer();
				} else {
					stopMagnetometer();
				}
			}
		});

		InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(mMFValue.getWindowToken(), 0);
	}
	
	@Override
	public void onPause() {
		this.stopMagnetometer();
		super.onPause();
	}
	
	@Override
	public void onResume() {
		this.startMagnetometer();
		super.onResume();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
			if (mInterference == false) {
				Toast.makeText(getApplicationContext(), "Magnetometer interference", Toast.LENGTH_LONG).show();
				mMFValue.setHint("Interference");
			}
			mInterference = true;
            return;
		}
		
		mInterference = false;
		mMFValue.setHint("");
		switch (event.sensor.getType()) {
        case Sensor.TYPE_MAGNETIC_FIELD:
        	float axisX = event.values[0];
            float axisY = event.values[1];
            float axisZ = event.values[2];
            mMagneticFieldStrength = Math.sqrt(axisX*axisX + axisY*axisY + axisZ*axisZ);
            mMFValue.setText(String.format("%.2f", mMagneticFieldStrength));
            this.updateCyclotronicFrequency();
            break;
        } 
	}

	public void stopMagnetometer() {
		if (mMagneticSensor != null) {
			mSensorManager.unregisterListener(this, mMagneticSensor);
		}
	}
	
	public void startMagnetometer() {
		if (mMagneticSensor != null) {
			mSensorManager.registerListener(this, mMagneticSensor, SensorManager.SENSOR_DELAY_FASTEST);
		}
	}
	
	public void updateCyclotronicFrequency() {
		double freq = MainActivity.calculateCyclotroneFrequency(mIonIndex, mMagneticFieldStrength);
		mCFValue.setText(String.format("%.2f", freq));
	}

	static public double calculateCyclotroneFrequency(int ionIndex, double fieldStrength) {
		double q, B, m;
		IonInfo info = MainActivity.Ions[ionIndex];
		B = fieldStrength / 1000000;
		q = info.charge * 1.602176565E-19;
		m = info.mass * 1.660538921E-27;
		return q * B / (2 * Math.PI * m);
	}
}
