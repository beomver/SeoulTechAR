package ar.data.convert;

import android.content.Context;
import android.graphics.Color;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import ar.ARView;
import ar.POIMarker;
import ar.data.DataHandler;
import ar.data.JSONDataManager;
import kunpeng.ar.lib.HtmlUnescape;
import kunpeng.ar.lib.marker.Marker;

/**
 * Created by Beom on 2017-04-13.
 */

public class JsonDataProcessor extends DataHandler implements DataProcessor {

    Context ctx;
    public static final int MAX_JSON_OBJECTS = 100;

    @Override
    public String[] getDataMatch() {
        String[] str = {"json"};
        return str;
    }

    @Override
    public List<Marker> load(String name) throws JSONException {
        ctx = ARView.ctx;

        List<Marker> markers = new ArrayList<Marker>();
        try {
            JSONArray results = new JSONArray();

            if (name == "geonames") {
                results = JSONDataManager.geonames.getJSONArray(name);
            }

            int top = Math.min(MAX_JSON_OBJECTS, results.length());

            for (int i = 0; i < top; i++) {
                Marker ma = null;
                try {
                    JSONObject jo = results.getJSONObject(i);

                    if (name == "geonames") {
                        if (jo.has("title") && jo.has("lat") && jo.has("lng")
                                && jo.has("elevation")) {

                            ma = new POIMarker(
                                    "",
                                    jo.getString("title"),
                                    jo.getDouble("lat"),
                                    jo.getDouble("lng"),
                                    jo.getDouble("elevation"),
                                    jo.getString("url"),
                                    -1, Color.RED);
                        }
                    }
//                    else if (name == "routes") {
//                        if (jo.has("name") && jo.has("latitude") && jo.has("longitude")) {
//
//                            ma = new POIMarker(
//                                    "",
//                                    HtmlUnescape.unescapeHTML(jo.getString("name"), 0),
//                                    jo.getDouble("latitude"),
//                                    jo.getDouble("longitude"),
//                                    40,
//                                    "",
//                                    -1, Color.RED);
//                        }
//                    }
                    markers.add(ma);
                } catch (JSONException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return markers;
    }

    @Override
    public List<Marker> load(JSONArray jsonArray) throws JSONException {
        ctx = ARView.ctx;

        List<Marker> markers = new ArrayList<Marker>();
        try {
            int top = Math.min(MAX_JSON_OBJECTS, jsonArray.length());

            for (int i = 0; i < top; i++) {
                Marker ma = null;
                try {
                    JSONObject jo = jsonArray.getJSONObject(i);
                    if (jo.has("name") && jo.has("latitude") && jo.has("longitude")) {

                        ma = new POIMarker(
                                "",
                                HtmlUnescape.unescapeHTML(jo.getString("name"), 0),
                                jo.getDouble("latitude"),
                                jo.getDouble("longitude"),
                                40,
                                "",
                                -1, Color.RED);
                    }
                    markers.add(ma);
                } catch (JSONException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return markers;
    }
}
