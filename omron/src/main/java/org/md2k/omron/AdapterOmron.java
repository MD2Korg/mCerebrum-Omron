package org.md2k.omron;

/**
 * Created by smhssain on 11/4/2015.
 */

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.md2k.datakitapi.source.platform.PlatformType;

import java.util.List;

public class AdapterOmron extends BaseAdapter {

    private LayoutInflater layoutinflater;
    private List<ViewContent> listStorage;
    private Context context;

    public AdapterOmron(Context context, List<ViewContent> customizedListView) {
        this.context = context;
        layoutinflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        listStorage = customizedListView;
    }

    @Override
    public int getCount() {
        return listStorage.size();
    }

    @Override
    public Object getItem(int position) {
        return position;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder listViewHolder;
        if (convertView == null) {
            listViewHolder = new ViewHolder();
            convertView = layoutinflater.inflate(R.layout.listview_with_text_image, parent, false);
            listViewHolder.textInListView = (TextView) convertView.findViewById(R.id.textView);
            listViewHolder.imageInListView = (ImageView) convertView.findViewById(R.id.imageView);
            convertView.setTag(listViewHolder);
        } else {
            listViewHolder = (ViewHolder) convertView.getTag();
        }

        listViewHolder.textInListView.setText(listStorage.get(position).getName());
        if (listStorage.get(position).getPlatformType().equals(PlatformType.OMRON_BLOOD_PRESSURE))
            listViewHolder.imageInListView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_blood_pressure_teal_48dp));
        else if (listStorage.get(position).getPlatformType().equals(PlatformType.OMRON_WEIGHT_SCALE))
            listViewHolder.imageInListView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_weight_scale_48dp));
        return convertView;
    }


    static class ViewHolder {
        TextView textInListView;
        ImageView imageInListView;
    }
}