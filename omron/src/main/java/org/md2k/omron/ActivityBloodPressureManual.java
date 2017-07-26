package org.md2k.omron;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.md2k.utilities.UI.AlertDialogs;

public class ActivityBloodPressureManual extends AppCompatActivity {
    EditText editTextSystolic;
    EditText editTextDiastolic;
    EditText editTextPulseRate;


    Button button_1;
    Button button_2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blood_pressure_manual);
        editTextSystolic = (EditText) findViewById(R.id.edit_text_systolic);
        editTextDiastolic = (EditText) findViewById(R.id.edit_text_diastolic);
        editTextPulseRate = (EditText) findViewById(R.id.edit_text_pulse_rate);

        button_1 = (Button) findViewById(R.id.button_1);
        button_1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(1);
                finish();
            }
        });
        button_2 = (Button) findViewById(R.id.button_2);
        button_2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                try {
                    String messageSystolic = editTextSystolic.getText().toString();
                    String messageDiastolic = editTextDiastolic.getText().toString();
                    String messagePulse = editTextPulseRate.getText().toString();
                    double bloodPressure[] = new double[3];
                    bloodPressure[0] = Double.parseDouble(messageSystolic);
                    bloodPressure[1] = Double.parseDouble(messageDiastolic);
                    bloodPressure[2] = -1;
                    double heartRate[] = new double[2];
                    heartRate[0] = Double.parseDouble(messagePulse);
                    heartRate[1] = -1;
                    Intent intent = new Intent();
                    intent.putExtra("BLOOD_PRESSURE", bloodPressure);
                    intent.putExtra("HEART_RATE", heartRate);
                    setResult(2, intent);
                    finish();//finishing activity
                }catch (Exception e){
                    Toast.makeText(ActivityBloodPressureManual.this, "Error: Invalid input...",Toast.LENGTH_LONG).show();
                }
            }
        });
    }


}
