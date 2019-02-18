package ar;

import android.content.Context;

import org.json.JSONObject;


import java.io.IOException;
import java.io.InputStream;


public class FileManager {

    JSONObject geonames;
    JSONObject path;
    Context ctx;

    public FileManager(Context ctx) {
        this.ctx = ctx;
    }

    /**
     * 건물정보 JSON load
     * @return
     */
    public String loadJSON() {
        String json = null;

        try {
            InputStream is = ctx.getResources().openRawResource(R.raw.geonames);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");

        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

    /**
     * 길찾기에 필요한 점들 load
     * @return
     */
    public String loadJSON2() {
        String json = null;

        try {
            InputStream is = ctx.getResources().openRawResource(R.raw.routes);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");

        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

    public JSONObject getGeonames() {
        return this.geonames;
    }
}
