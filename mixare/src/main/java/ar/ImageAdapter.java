package ar;

import android.content.Context;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

/**
 * Created by Beom on 2017-05-11.
 * 우측상단 메뉴 버튼을 위한 Adapter
 */

public class ImageAdapter extends BaseAdapter {
    private Context mContext;
    private boolean ispath;
    private Integer ic[] = {R.drawable.marker_list, R.drawable.map};

    public ImageAdapter(Context c) {
        mContext = c;
    }

    public void setispath(boolean is) {
        ispath = is;
        if(ispath)
            ic[1] = 0;
    }

    public int getCount() {
        return ic.length;
    }

    public Object getItem(int position) {
        return ic[position];
    }

    public long getItemId(int position) {
        return position;
    }

    // 아이템뷰 이미지 생성
    public View getView(int position, View convertView, ViewGroup parent) {

        int rowWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40f,
                mContext.getResources().getDisplayMetrics());

        ImageView imageView;
        if (convertView == null) {
            imageView = new ImageView(mContext);
            imageView.setLayoutParams(new GridView.LayoutParams(rowWidth, rowWidth));
            imageView.setScaleType(ImageView.ScaleType.FIT_XY);
        } else {
            imageView = (ImageView) convertView;
        }
        imageView.setImageResource(ic[position]);
        return imageView;
    }
}