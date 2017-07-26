package org.md2k.omron;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class ActivityWeightScaleManual extends AppCompatActivity {

    EditText editTextWeight;



    Button button_1;
    Button button_2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weight_scale_manual);
        editTextWeight = (EditText) findViewById(R.id.edit_text_systolic);

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
                    String messageWeight = editTextWeight.getText().toString();

                    Intent intent = new Intent();
                    double weight[]=new double[1];
                    weight[0]=Double.parseDouble(messageWeight);
                    intent.putExtra("WEIGHT", weight);

                    setResult(2, intent);
                    finish();//finishing activity
                }catch (Exception e){
                    Toast.makeText(ActivityWeightScaleManual.this, "Error: Invalid input...",Toast.LENGTH_LONG).show();
                }
            }
        });
    }


}
