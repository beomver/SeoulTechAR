package ar;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import net.daum.mf.map.api.MapPOIItem;
import net.daum.mf.map.api.MapPoint;
import net.daum.mf.map.api.MapPolyline;
import net.daum.mf.map.api.MapView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import ar.data.JSONDataManager;

import static android.view.View.GONE;
import static ar.R.string.location_not_found;

//Daum map을 그리기 위한 클래스
//geonames.json의 건물 좌표를 이용하여 지도 상에 마커생성

public class MapActivity extends FragmentActivity
        implements View.OnClickListener,
        MapView.MapViewEventListener, MapView.CurrentLocationEventListener, MapView.POIItemEventListener {

    MapView mapView;
    MapPOIItem marker;
    MapPolyline mapPolyline;
    FloydWarshallPath fwp;
    JSONArray geonamesResults;
    JSONArray routeResults;
    JSONObject currentMarker;
    MapPOIItem currentPOI;

    double[][] routeRes; // MixView Activity에 넘길 경로 데이터 [tag][0] : latitude, [tag][1] : longitude

    private LinearLayout info; // 빌딩 정보 창
    private TextView b_name, b_detail, b_find_load;
    private ImageView toCam; //카메라로 전환 버튼

    private static final String API_KEY = "6ec4855695e18ceb9e99b3ddd2c73c14";

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map2);

        currentPOI = new MapPOIItem();
        //--------------- 정보창 -----------------------------------------------
        info = (LinearLayout) findViewById(R.id.map_build_info);
        b_name = (TextView) findViewById(R.id.building_title);
        b_detail = (TextView) findViewById(R.id.building_detail);
        b_find_load = (TextView) findViewById(R.id.find_load);
        toCam = (ImageView) findViewById(R.id.toCam);

        b_find_load.setOnClickListener(this);
        toCam.setOnClickListener(this);

        mapView = new MapView(this);
        mapView.setDaumMapApiKey(API_KEY);
        mapView.setMapViewEventListener(this);
        mapView.setCurrentLocationEventListener(this);

        ViewGroup mapViewContainer = (ViewGroup) findViewById(R.id.map_view);
        mapViewContainer.addView(mapView);

        try {

            geonamesResults = JSONDataManager.geonames.getJSONArray("geonames");
            routeResults = JSONDataManager.routes.getJSONArray("routes");

            currentMarker = new JSONObject();
            ArrayList<Integer> next = new ArrayList<>();
            for (int i = 0; i < 45; i++) {
                next.add(i);
            }
            currentMarker.put("id", 45);
            currentMarker.put("name", "currentLocation");
            currentMarker.put("next", new JSONArray(next));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * geonames.json 데이터 가져오기
     * @param root
     */
    public void loadMarker(JSONArray root) {
        JSONObject jo = null;
        int top = 65;
        if (root.length() <= top) {
            top = root.length();
        }

        for (int i = 0; i < top; i++) {
            try {
                jo = root.getJSONObject(i);

                marker = new MapPOIItem();

                marker.setItemName(jo.getString("title"));  //건물이름
                marker.setTag(jo.getInt("tag"));            //건물번호

                marker.setMapPoint(MapPoint.mapPointWithGeoCoord(jo.getDouble("lat"), jo.getDouble("lng"))); //GeoCoord 좌표계를 이용하여 마커 붙이기
                marker.setMarkerType(MapPOIItem.MarkerType.RedPin);
                marker.setSelectedMarkerType(MapPOIItem.MarkerType.YellowPin);
                mapView.addPOIItem(marker);

            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        mapView.setPOIItemEventListener(this);
        mapView.setDefaultCurrentLocationMarker();
        mapView.setCurrentLocationRadius(5);
        mapView.setShowCurrentLocationMarker(true);
        mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithoutHeading);
//        mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOff);
    }

    /**
     * 경로 그리기
     * @param data
     */
    private void drawLine(String[] data) {
        mapPolyline = new MapPolyline();
        MapPoint[] tempMarkers = new MapPoint[data.length];
        routeRes = new double[data.length][data.length];

        for (int i = 0; i < data.length; i++) {
            if (Integer.parseInt(data[i]) != 45) {
                ArrayList<Double> coord = JSONDataManager.routeCoord.get(Integer.parseInt(data[i])); //경로 가져오기
                Log.d("coord", coord.get(0).toString());
                Log.d("coord", data[i]);
                tempMarkers[i] = MapPoint.mapPointWithGeoCoord(coord.get(0), coord.get(1));
                routeRes[i][0] = tempMarkers[i].getMapPointGeoCoord().latitude;
                routeRes[i][1] = tempMarkers[i].getMapPointGeoCoord().longitude;
            } else {
                tempMarkers[i] = currentPOI.getMapPoint();
                routeRes[i][0] = tempMarkers[i].getMapPointGeoCoord().latitude;
                routeRes[i][1] = tempMarkers[i].getMapPointGeoCoord().longitude;
            }

        }

        for (int i = 0; i < tempMarkers.length; i++) {

            mapPolyline.addPoint(tempMarkers[i]);

        }
        mapView.addPolyline(mapPolyline);
    }

    @Override
    public void onMapViewInitialized(MapView mapView) {
        mapView.setMapCenterPointAndZoomLevel(MapPoint.mapPointWithGeoCoord(37.629517, 127.080507), 1, true);
        loadMarker(geonamesResults);
    }

    @Override
    /**
     * 현재 위치 조회
     */
    public void onCurrentLocationUpdate(MapView mapView, MapPoint currentLocation, float accuracyInMeters) {
//        String msg = "latitude : " + currentLocation.getMapPointGeoCoord().latitude + ", longitude : " + currentLocation.getMapPointGeoCoord().longitude;
        Log.d("Daum Map", currentLocation.getMapPointGeoCoord().latitude + "," + currentLocation.getMapPointGeoCoord().longitude);
        currentPOI.setMapPoint(currentLocation);
        try {
//            currentMarker.put("latitude", currentLocation.getMapPointGeoCoord().latitude);
//            currentMarker.put("longitude", currentLocation.getMapPointGeoCoord().longitude);
            JSONArray routes = JSONDataManager.routes.getJSONArray("routes");
//            routes.put(currentMarker);
            JSONObject routesObject = new JSONObject();
            routesObject.put("routes", routes);
            String ss2 = routesObject.toString();

            fwp = new FloydWarshallPath(ss2);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCurrentLocationDeviceHeadingUpdate(MapView mapView, float v) {

    }

    @Override
    public void onCurrentLocationUpdateFailed(MapView mapView) {

    }

    @Override
    public void onCurrentLocationUpdateCancelled(MapView mapView) {
    }

    @Override
    public void onMapViewCenterPointMoved(MapView mapView, MapPoint mapCenterPoint) {
    }

    @Override
    public void onMapViewDoubleTapped(MapView mapView, MapPoint mapPoint) {
    }

    @Override
    public void onMapViewLongPressed(MapView mapView, MapPoint mapPoint) {
    }

    /**
     * 마커가 아닌 지도를 클릭 했을 때
     * @param mapView
     * @param mapPoint
     */
    public MapPOIItem d_mapPOIItem;

    @Override
    public void onMapViewSingleTapped(MapView mapView, MapPoint mapPoint) {

        info.setVisibility(GONE);
        d_mapPOIItem = null;
        mapView.removeAllPolylines();
    }

    @Override
    public void onMapViewDragStarted(MapView mapView, MapPoint mapPoint) {
    }

    @Override
    public void onMapViewDragEnded(MapView mapView, MapPoint mapPoint) {
    }

    @Override
    public void onMapViewMoveFinished(MapView mapView, MapPoint mapPoint) {
    }

    @Override
    public void onMapViewZoomLevelChanged(MapView mapView, int zoomLevel) {
    }

    /**
     * 마커를 클릭했을 때
     * @param mapView
     * @param mapPOIItem
     */
    String path;
    @Override
    public void onPOIItemSelected(MapView mapView, MapPOIItem mapPOIItem) {
        try {
            MapPoint mapPoint = mapPOIItem.getMapPoint();
            MapPoint.GeoCoordinate wcong = mapPoint.getMapPointGeoCoord();

            //정보창 띄우기
            info.setVisibility(View.VISIBLE);
            b_name.setText(mapPOIItem.getItemName());
            b_detail.setText(wcong.latitude + "," + wcong.longitude);

            int start = 45;
            int end = mapPOIItem.getTag();
            MapPoint currentMapPoint = currentPOI.getMapPoint();
            Log.d("ROUTE", "start : " + start + "    end : " + end);
            int middle = fwp.getDistanceBetweenAnotherRoute(currentMapPoint.getMapPointGeoCoord().latitude, currentMapPoint.getMapPointGeoCoord().longitude);
            path = fwp.floyd_warshall_path(middle, end);
            String[] pathInt = path.split("-");
            path = "45-"+end;
            drawLine(pathInt);
        }
        catch (Exception e) {
            Toast.makeText(MapActivity.this, R.string.location_not_found,Toast.LENGTH_SHORT).show();
            finish();
        }
//        if (d_mapPOIItem != null) {
//            int start = d_mapPOIItem.getTag();
//            int end = mapPOIItem.getTag();
//            Log.d("ROUTE", "start : " + start + "    end : " + end);
//            String path = fwp.floyd_warshall_path(start, end);
//            String[] pathInt = path.split("-");
//            drawLine(pathInt);
//            d_mapPOIItem = null;
//        } else {
//            d_mapPOIItem = mapPOIItem;
//        }
    }

    @Override
    public void onCalloutBalloonOfPOIItemTouched(MapView mapView, MapPOIItem mapPOIItem) {
    }

    @Override
    public void onCalloutBalloonOfPOIItemTouched(MapView mapView, MapPOIItem mapPOIItem, MapPOIItem.CalloutBalloonButtonType calloutBalloonButtonType) {
    }

    @Override
    public void onDraggablePOIItemMoved(MapView mapView, MapPOIItem mapPOIItem, MapPoint mapPoint) {

    }

    JSONArray jsonArray;
    public boolean isPath = false;

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.find_load:
                if (routeRes[0] != null) {
                    Intent intent = new Intent(this, ARView.class);

                    intent.putExtra("path", path);
                    intent.putExtra("ispath", true);
                    startActivity(intent);
                }
                break;
            case R.id.toCam:
                Intent intent = new Intent(this, ARView.class);
                String[][] path = new String[1][1];
                path[0][0] = null;
                intent.putExtra("path", path);
                intent.putExtra("query", "school");
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                startActivity(intent);
                finish();
                break;
        }
    }

    /**
     * 뒤로가기 버튼 클릭
     */
    @Override
    public void onBackPressed() {
        if (info.getVisibility() == GONE) {
            super.onBackPressed();
        } else {
            info.setVisibility(GONE);
        }
    }
}
