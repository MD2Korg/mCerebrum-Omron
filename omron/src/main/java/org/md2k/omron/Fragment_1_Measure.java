package org.md2k.omron;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.paolorotolo.appintro.AppIntro;

import org.md2k.datakitapi.source.platform.PlatformType;

/**
 * Created by monowar on 6/26/16.
 */
public class Fragment_1_Measure extends Fragment {
    String platformType;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_1_measure, container, false);
        platformType=getArguments().getString(PlatformType.class.getSimpleName());

        TextView tv = (TextView) v.findViewById(R.id.text_view_title);
        tv.setText(getArguments().getString("title"));
        tv = (TextView) v.findViewById(R.id.text_view_message);
        tv.setText(getArguments().getString("message"));
        ImageView imageView=(ImageView) v.findViewById(R.id.image_view_image);
        imageView.setImageResource(getArguments().getInt("image"));
        Button button=(Button) v.findViewById(R.id.button_done);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(platformType.equals(PlatformType.OMRON_BLOOD_PRESSURE)) {
                    ActivityBloodPressure activity = (ActivityBloodPressure) getActivity();
                    activity.nextSlide();
                }else{
                    ActivityWeightScale activity = (ActivityWeightScale) getActivity();
                    activity.nextSlide();
                }
            }
        });


        return v;
    }

    public static Fragment_1_Measure newInstance(String platformType, String title, String message, int image) {

        Fragment_1_Measure f = new Fragment_1_Measure();
        Bundle b = new Bundle();
        b.putString(PlatformType.class.getSimpleName(), platformType);
        b.putString("title", title);
        b.putString("message",message);
        b.putInt("image",image);
        f.setArguments(b);
        return f;
    }

}