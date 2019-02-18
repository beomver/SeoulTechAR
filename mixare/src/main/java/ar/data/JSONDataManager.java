package ar.data;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by jongholee on 2017. 4. 20..
 */

public class JSONDataManager {
    public static JSONObject geonames;
    public static JSONObject routes;
    public static Map<Integer, ArrayList<Double>> routeCoord = new HashMap<Integer, ArrayList<Double>>();
}
