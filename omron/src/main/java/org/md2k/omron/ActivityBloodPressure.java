package org.md2k.omron; /**
 * Created by monowar on 6/26/16.
 */

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import com.github.paolorotolo.appintro.AppIntro;

import org.md2k.datakitapi.DataKitAPI;
import org.md2k.datakitapi.exception.DataKitException;
import org.md2k.datakitapi.messagehandler.OnConnectionListener;
import org.md2k.datakitapi.source.platform.PlatformType;
import org.md2k.omron.configuration.Configuration;

public class ActivityBloodPressure extends AppIntro {
    private DataKitAPI dataKitAPI = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(Configuration.getDeviceAddress(PlatformType.OMRON_BLOOD_PRESSURE)==null){
            Toast.makeText(this, "ERROR: Blood Pressure device is not configured...", Toast.LENGTH_SHORT).show();
            finish();
        }else {
            dataKitAPI = DataKitAPI.getInstance(getApplicationContext());
            try {
                dataKitAPI.connect(new OnConnectionListener() {
                    @Override
                    public void onConnected() {
                    }
                });
            } catch (DataKitException e) {
                Toast.makeText(this, "Datakit Connection Error", Toast.LENGTH_SHORT).show();
                finish();
            }
            addSlide(Fragment_1_Measure.newInstance(PlatformType.OMRON_BLOOD_PRESSURE, "Measure Blood Pressure", "Please put on the pressure cuff and initiate a measurement", R.drawable.omron_bp));
            addSlide(Fragment_2_Read_BP.newInstance("Blood Pressure Reading"));
            setBarColor(ContextCompat.getColor(ActivityBloodPressure.this, R.color.teal_500));
            setSeparatorColor(ContextCompat.getColor(ActivityBloodPressure.this, R.color.deeporange_500));
            showDoneButton(false);
            setSwipeLock(true);
            setNextPageSwipeLock(true);
            setProgressButtonEnabled(false);
        }
    }

    @Override
    public void onSkipPressed(Fragment currentFragment) {
        super.onSkipPressed(currentFragment);
        // Do something when users tap on Skip button.
    }

    public void nextSlide() {
        pager.setCurrentItem(pager.getCurrentItem() + 1);
    }

    public void prevSlide() {
        pager.setCurrentItem(pager.getCurrentItem() - 1);
    }

    @Override
    public void onDonePressed(Fragment currentFragment) {
        super.onDonePressed(currentFragment);
        // Do something when users tap on Done button.
    }

    @Override
    public void onSlideChanged(@Nullable Fragment oldFragment, @Nullable Fragment newFragment) {
        super.onSlideChanged(oldFragment, newFragment);
        // Do something when the slide changes.
    }

    @Override
    public void onDestroy() {
        if(dataKitAPI!=null) {
            dataKitAPI.disconnect();
        }
        super.onDestroy();
    }
}
