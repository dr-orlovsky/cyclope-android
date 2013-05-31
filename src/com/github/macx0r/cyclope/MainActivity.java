package com.github.macx0r.cyclope;

import android.hardware.*;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.*;
import android.widget.*;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.github.macx0r.cyclope.IonInfo;

public class MainActivity extends Activity implements SensorEventListener, OnCheckedChangeListener, OnItemSelectedListener {
	private SensorManager mSensorManager;
	private Sensor mMagneticSensor;
	private boolean mInterference = false;
	private boolean mMeasuring = true;
	private boolean mCalculations = true;
	private int mIonIndex = 0;
	private int mIonCharge = 1;
	private double mFieldStrength = 0.;
	private double mFieldUnits = 1.;
	private double mCycloFrequency = 0.;
	private int mRedShift = 0;
	private int mHarmonicIndex = HARMONIC_MIDDLE;
	private int mIntegrationCycles = 0;
	private int mIntegrationCycle = 0;
	private double mIntegrationValue = 0.;

	static final int DEFAULT_INTEGRATION_CYCLES = 25;

	static final double[] harmonics = { .25, .5, 1, 2, 3, 4 };
	static final int HARMONIC_MIDDLE = 2;
	
	static final double[] fieldUnits = { 1., 10., 1., 42.6 };
	
	private AlertDialog mShiftDialog;
	private NumberPicker mShiftPicker;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		initInterface();
		initSensors();
	}
	
	@Override
	public void onPause() {
		if (mMeasuring)
			this.stopMagnetometer();
		super.onPause();
	}
	
	@Override
	public void onResume() {
		if (mMeasuring)
			this.startMagnetometer();
		super.onResume();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		configureActionItem(menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.menu_pause) {
			toggleCalculations();
			item.setIcon(mCalculations ? R.drawable.ic_pause : R.drawable.ic_play);
			item.setTitle(mCalculations ? R.string.menu_pause : R.string.menu_continue);
		}
		if (item.getItemId() == R.id.menu_integrate) {
			toggleIntegration();
			item.setIcon(mIntegrationCycles == 0 ? R.drawable.ic_integrate : R.drawable.ic_raw);
			item.setTitle(mIntegrationCycles == 0 ? R.string.menu_integrate : R.string.menu_raw);
		}
		if (item.getItemId() == R.id.menu_shift) {
			mShiftDialog.show();
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		if (buttonView.getId() != R.id.runMagnetometer)
			return;
		if (isChecked) {
			startMagnetometer();
		} else {
			stopMagnetometer();
		}
	}
	
	@Override
    public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
		int id1 = parentView.getId();
		if (id1 == R.id.ionSpinner)
			updateIon(position);
		if (id1 == R.id.fieldUnits)
			updateUnits(position);
		if (id1 == R.id.ionChargeSpinner) {
			if (position >= IonInfo.mainIons[mIonIndex].maxCharge) {
				Toast.makeText(getApplicationContext(), R.string.toast_wrong_charge, Toast.LENGTH_LONG).show();
			} else {
				updateCharge(position);
			}
		}
    }
	
    @Override
    public void onNothingSelected(AdapterView<?> parentView) {
    	return;
    }

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() != Sensor.TYPE_MAGNETIC_FIELD)
			return;
		
		if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
			toggleInterferenceInfo(true);
			return;
		}
		
    	float axisX = event.values[0];
        float axisY = event.values[1];
        float axisZ = event.values[2];
        mFieldStrength = Math.sqrt(axisX*axisX + axisY*axisY + axisZ*axisZ);

		double freq = MainActivity.calculateCyclotroneFrequency(mIonIndex, mIonCharge, harmonics[mHarmonicIndex], mFieldStrength);
		if (mIntegrationCycles > 0) {
			mIntegrationCycle++;
			mIntegrationValue += freq;
			if (mIntegrationCycle >= mIntegrationCycles) {
				mIntegrationCycle = 0;
				mCycloFrequency = mIntegrationValue / mIntegrationCycles;
				mIntegrationValue = 0;
			}
		} else {
			mCycloFrequency = freq;
		}

		if (mInterference)
			toggleInterferenceInfo(false);

        this.updateDataAndView(false);
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int arg1) {
	}

	public void onPrevClicked(View view) {
		if (mHarmonicIndex <= 0) {
			Toast.makeText(this, R.string.toast_first_harmonic, Toast.LENGTH_LONG).show();
		} else {
			mHarmonicIndex--;
			updateDataAndView(true);
		}
	}

	public void onNextClicked(View view) {
		if (mHarmonicIndex >= harmonics.length - 1) {
			Toast.makeText(this, R.string.toast_last_harmonic, Toast.LENGTH_LONG).show();
		} else {
			mHarmonicIndex++;
			updateDataAndView(true);
		}
	}
	
	// -------
	
	private void initInterface() {
		Spinner ionSpinner = (Spinner) findViewById(R.id.ionSpinner);
		ArrayAdapter<IonInfo> adapter = new ArrayAdapter<IonInfo>(this, R.layout.ion_spinner, IonInfo.mainIons);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		ionSpinner.setAdapter(adapter);
		ionSpinner.setOnItemSelectedListener(this);
		
		Spinner unitSpinner = (Spinner) findViewById(R.id.fieldUnits);
		unitSpinner.setOnItemSelectedListener(this);

		Spinner chargeSpinner = (Spinner) findViewById(R.id.ionChargeSpinner);
		chargeSpinner.setOnItemSelectedListener(this);
		
	    View npView = (View) getLayoutInflater().inflate(R.layout.number_picker_dialog_layout, null);
	    mShiftPicker = (NumberPicker) npView.findViewById(R.id.redShiftPicker);
	    mShiftPicker.setMinValue(0);
	    mShiftPicker.setMaxValue(100);
	    mShiftDialog = new AlertDialog.Builder(this)
	        .setTitle(R.string.shift_dlg_title)
	        .setView(npView)
	        .setPositiveButton(R.string.dialog_set,
	            new DialogInterface.OnClickListener() {
	                public void onClick(DialogInterface dialog, int whichButton) {
	                	updateShift(mShiftPicker.getValue());
	                }
	            })
	            .setNegativeButton(R.string.dialog_cancel, null)
	            .create();
		
		updateDataAndView(true);
	}

	private void initSensors() {
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		if ((mMagneticSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)) != null) {
			this.startMagnetometer();
		} else {
			Toast.makeText(getApplicationContext(), R.string.toast_nomagnet, Toast.LENGTH_LONG).show();
		}
	}
	
	private void configureActionItem(Menu menu) {
		Switch activitySwitch = (Switch) menu.findItem(R.id.activity_switch).getActionView().findViewById(R.id.runMagnetometer);
		activitySwitch.setChecked(mMeasuring);
		activitySwitch.setOnCheckedChangeListener(this);
	}
	
	public void stopMagnetometer() {
		if (mMagneticSensor != null) {
			mSensorManager.unregisterListener(this, mMagneticSensor);
			mMeasuring = false;
		}
	}
	
	public void startMagnetometer() {
		if (mMagneticSensor != null) {
			mMeasuring = true;
			mSensorManager.registerListener(this, mMagneticSensor, SensorManager.SENSOR_DELAY_FASTEST);
		}
	}
	
	public void toggleIntegration () {
		if (mIntegrationCycles == 0)
			mIntegrationCycles = DEFAULT_INTEGRATION_CYCLES;
		else
			mIntegrationCycles = 0;
		mIntegrationCycle = 0;
		mIntegrationValue = 0;
	}
	
	public void toggleCalculations() {
		if (mCalculations)
			pauseCalculations();
		else
			continueCalculations();
	}
	
	public void pauseCalculations() {
		mCalculations = false;
	}
	
	public void continueCalculations() {
		mCalculations = true;
		updateDataAndView(false);
	}
	
	public void toggleInterferenceInfo(boolean interference) {
		mIntegrationCycle = 0;
		TextView ifLabel = (TextView) findViewById(R.id.notificationLabel);
		ifLabel.setVisibility(interference ? View.VISIBLE : View.GONE);
		ViewGroup display = (ViewGroup) findViewById(R.id.fieldLayout);
		display.setVisibility(interference ? View.GONE : View.VISIBLE);
		pauseCalculations();
	}
	
	public void updateIon(int ion) {
		mIonIndex = ion;
		mIonCharge = IonInfo.mainIons[ion].charge;
		Spinner chargeSpinner = (Spinner) findViewById(R.id.ionChargeSpinner);
		chargeSpinner.setSelection(mIonCharge - 1);
		updateDataAndView(true);
	}
	
	public void updateCharge(int index) {
		mIonCharge = index + 1;
		updateDataAndView(false);
	}
	
	public void updateUnits(int unit) {
		if (unit >= fieldUnits.length || unit < 0)
			return;
		double oldUnits = mFieldUnits;
		mFieldUnits = fieldUnits[unit];
		mFieldStrength *= mFieldUnits / oldUnits;
		updateDataAndView(false);
	}
	
	public void updateShift(int shift) {
		mRedShift = shift;
		updateDataAndView(true);
	}
	
	public void updateHarmonic(int harmonicIndex) {
		if (harmonicIndex >= harmonics.length || harmonicIndex < 0)
			return;
		mHarmonicIndex = harmonicIndex;
		updateDataAndView(true);
	}
	
	public void updateDataAndView(boolean all) {
		TextView label;
		if (mMeasuring) {
			label = (TextView) findViewById(R.id.fieldLabel);
			label.setText(String.format("%.2f", mFieldStrength * mFieldUnits));
		}
			
		if (mCalculations) {
			label = (TextView) findViewById(R.id.frequencyLabel);
			label.setText(String.format(getString(R.string.freq_format), mCycloFrequency - mRedShift));
		}
		
		if (all) {
			label = (TextView) findViewById(R.id.ionWeightLabel);
			label.setText(String.format(getString(R.string.ion_weight_format), IonInfo.mainIons[mIonIndex].mass));

			label = (TextView) findViewById(R.id.detailsLabel);
			int harmonic = mHarmonicIndex - HARMONIC_MIDDLE;
			harmonic = (harmonic >= 0) ? harmonic + 1 : harmonic;
			label.setText(String.format(getString(mRedShift == 0 ? R.string.freq_description_format : R.string.freq_description_shift_format), harmonic, mRedShift));
		}
	}

	static public double calculateCyclotroneFrequency(int ionIndex, int charge, double harmonic, double fieldStrength) {
		double q, B, m;
		IonInfo info = IonInfo.mainIons[ionIndex];
		B = fieldStrength / 1000000;
		q = charge * 1.602176565E-19;
		m = info.mass * 1.660538921E-27;
		return q * B * harmonic / (2 * Math.PI * m);
	}
}
