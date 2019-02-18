/*
 * Copyright (C) 2010- Peer internet solutions
 *
 * This file is part of AR Navigator.
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>
 */
package ar;

/**
 * This class is the main application which uses the other classes for different
 * functionalities.
 * It sets up the camera screen and the augmented screen which is in front of the
 * camera screen.
 * It also handles the main sensor events, touch events and location events.
 */

import android.app.Activity;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.hardware.Camera;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.GLU;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.provider.Settings;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL;
import javax.microedition.khronos.opengles.GL10;

import ar.Cluster.Label;
import ar.Cluster.Label.State;
import ar.data.DataHandler;
import ar.data.DataSourceStorage;
import ar.data.JSONDataManager;
import ar.utility.KalmanFilter;
import kunpeng.ar.lib.MixUtils;
import kunpeng.ar.lib.gui.PaintScreen;
import kunpeng.ar.lib.marker.Marker;
import kunpeng.ar.lib.render.Matrix;

import static android.hardware.SensorManager.SENSOR_DELAY_GAME;

public class ARView extends Activity implements SensorEventListener, OnTouchListener {

    private CameraSurface camScreen;
    private AugmentedView augScreen;
    private AugmentedGLView augGLScreen;

    private boolean isInited;
    private static PaintScreen dWindow;
    private static DataView dataView;
    private boolean fError;
    public static final float orientationValues[] = new float[3];
    // ----------
    private MixViewDataHolder mixViewData;

    // TAG for logging
    public static final String TAG = "AR";

    /* string to name & access the preference file in the internal storage */
    public static final String PREFS_NAME = "MyPrefsFileForMenuItems";

    private FrameLayout mframe;

    static int mRotation = 1;

    public static Context ctx;

    /* MapActivity에서 가져온 길 찾기 결과 */
    private String routeArrayStn;
    public JSONArray routeArray;
    private JSONObject jo;
    Marker mroute;
    Boolean ispath;
    static List<Marker> rmaker = new ArrayList<Marker>();

    public TextView dist;
    public FrameLayout distframeLayout;

    // KalmanFilter
    KalmanFilter mKalGravX, mKalGravY, mKalGravZ, mKalMagX, mKalMagY, mKalMagZ;
    double filteredGravX, filteredGravY, filteredGravZ, filteredMagX, filteredMagY, filteredMagZ;

    public MixViewDataHolder getMixViewData() {
        if (mixViewData == null) {
            // TODO: VERY inportant, only one!
            mixViewData = new MixViewDataHolder(new MixContext(this));
        }
        return mixViewData;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ctx = this;
        try {
            mKalGravX = new KalmanFilter(0.0f);
            mKalGravY = new KalmanFilter(0.0f);
            mKalGravZ = new KalmanFilter(0.0f);

            mKalMagX = new KalmanFilter(0.0f);
            mKalMagY = new KalmanFilter(0.0f);
            mKalMagZ = new KalmanFilter(0.0f);

            distframeLayout = new FrameLayout(this);
            dist = new TextView(this);
            dist.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
            dist.setGravity(Gravity.CENTER);

            handleIntent(getIntent());

            final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            getMixViewData().setmWakeLock(
                    pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK,
                            "My Tag"));

            killOnError();
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);

            distframeLayout.addView(dist);
            distframeLayout.setBackgroundColor(Color.WHITE);
            int value = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, this.getResources().getDisplayMetrics());

            mframe = new FrameLayout(this);
            maintainCamera();
            maintainAugmentGLView();
            maintainAugmentR();

            setContentView(mframe);

            if (!isInited) {
                setdWindow(new PaintScreen());
                setDataView(new DataView(getMixViewData().getMixContext()));

				/* set the radius in data view to the last selected by the user */
                setZoomLevel();
                isInited = true;
            }
            /*
             * Get the preference file PREFS_NAME stored in the internal memory
			 * of the phone
			 */
            SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);

			/* check if the application is launched for the first time */
            if (settings.getBoolean("firstAccess", false) == false) {
                firstAccess(settings);
            }

            // 경로 가져오기
            Intent routeIntent = getIntent();
            ispath = routeIntent.getBooleanExtra("ispath", false);
            if (ispath == true) {
                routeArrayStn = routeIntent.getStringExtra("path");
                String[] pathIndex = routeArrayStn.split("-");

                for (int i = 1; pathIndex.length > i; i++) {
                    int index = Integer.parseInt(pathIndex[i]);
                    Log.d("Success", index + "");
                    ArrayList<Double> coord = JSONDataManager.routeCoord.get(index);
                    double lat = coord.get(0);
                    double lon = coord.get(1);

                    mroute = new POIMarker("", i + "",
                            lat, lon, 40, "", -1, 0x00000000);

                    rmaker.add(mroute);
                }
                getDataView().setRouteListMarker(rmaker);

                addContentView(distframeLayout, new FrameLayout.LayoutParams(
                        LayoutParams.MATCH_PARENT, value,
                        Gravity.CENTER | Gravity.BOTTOM));
            }
            maintainToolBar();

        } catch (Exception ex) {
            doError(ex);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        try {
            this.getMixViewData().getmWakeLock().release();

            try {
                getMixViewData().getMixContext().cancelMsgPopUp();
                getMixViewData().getSensorMgr().unregisterListener(this,
                        getMixViewData().getSensorGrav());
                getMixViewData().getSensorMgr().unregisterListener(this,
                        getMixViewData().getSensorMag());
                getMixViewData().setSensorMgr(null);

                getMixViewData().getMixContext().getLocationFinder()
                        .switchOff();
                augGLScreen.onPause();

                mframe.removeAllViews();
                camScreen = null;
                augScreen = null;
                augGLScreen = null;

            } catch (Exception ignore) {
            }

            if (fError) {
                finish();
            }
        } catch (Exception ex) {
            doError(ex);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {
            try {
                Log.d(TAG + " WorkFlow", "MixView - onDestroy called");
                setDataView(null);
            } catch (Exception ignore) {
            }

            if (fError) {
                finish();
            }
        } catch (Exception ex) {
            doError(ex);
        }
    }

    /**
     * {@inheritDoc} Mixare - Receives results from other launched activities
     * Base on the result returned, it either refreshes screen or not. Default
     * value for refreshing is false
     */
    protected void onActivityResult(final int requestCode,
                                    final int resultCode, Intent data) {
        Log.d(TAG + " WorkFlow", "MixView - onActivityResult Called");
        // check if the returned is request to refresh screen (setting might be changed)
        try {
            if (data.getBooleanExtra("RefreshScreen", false)) {
                Log.d(TAG + " WorkFlow",
                        "MixView - Received Refresh Screen Request .. about to refresh");
                repaint();
            }

        } catch (Exception ex) {
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        try {

            this.getMixViewData().getmWakeLock().acquire();

            killOnError();
            getMixViewData().getMixContext().doResume(this);

            repaint();
            getDataView().doStart();
            getDataView().clearEvents();
            getDataView().setRouteListMarker(rmaker);

            float angleX = 0, angleY = 0;
            int marker_orientation = -90;

            // display text from left to right and keep it horizontal
            angleX = (float) Math.toRadians(marker_orientation);
            getMixViewData().getM1().set(1f, 0f, 0f, 0f,
                    (float) Math.cos(angleX),
                    (float) -Math.sin(angleX), 0f,
                    (float) Math.sin(angleX),
                    (float) Math.cos(angleX));

            angleX = (float) Math.toRadians(marker_orientation);
            angleY = (float) Math.toRadians(marker_orientation);
            getMixViewData().getM2().set((float) Math.cos(angleX), 0f,
                    (float) Math.sin(angleX), 0f, 1f, 0f,
                    (float) -Math.sin(angleX), 0f,
                    (float) Math.cos(angleX));
            getMixViewData().getM3().set(1f, 0f, 0f, 0f,
                    (float) Math.cos(angleY),
                    (float) -Math.sin(angleY), 0f,
                    (float) Math.sin(angleY),
                    (float) Math.cos(angleY));

            getMixViewData().getM4().toIdentity();

            for (int i = 0; i < getMixViewData().getHistR().length; i++) {
                getMixViewData().getHistR()[i] = new Matrix();
            }

            getMixViewData().setSensorMgr(
                    (SensorManager) getSystemService(SENSOR_SERVICE));

            getMixViewData().setSensors(
                    getMixViewData().getSensorMgr().getSensorList(
                            Sensor.TYPE_ACCELEROMETER));
            if (getMixViewData().getSensors().size() > 0) {
                getMixViewData().setSensorGrav(
                        getMixViewData().getSensors().get(0));
            }

            getMixViewData().setSensors(
                    getMixViewData().getSensorMgr().getSensorList(
                            Sensor.TYPE_MAGNETIC_FIELD));
            if (getMixViewData().getSensors().size() > 0) {
                getMixViewData().setSensorMag(
                        getMixViewData().getSensors().get(0));
            }
            getMixViewData().getSensorMgr().registerListener(this,
                    getMixViewData().getSensorGrav(), SENSOR_DELAY_GAME);
            getMixViewData().getSensorMgr().registerListener(this,
                    getMixViewData().getSensorMag(), SENSOR_DELAY_GAME);

            try {
                GeomagneticField gmf = getMixViewData().getMixContext()
                        .getLocationFinder().getGeomagneticField();
                if (gmf != null) {
                    angleY = (float) Math.toRadians(-gmf.getDeclination());
                }
                getMixViewData().getM4().set((float) Math.cos(angleY), 0f,
                        (float) Math.sin(angleY), 0f, 1f, 0f,
                        (float) -Math.sin(angleY), 0f,
                        (float) Math.cos(angleY));
            } catch (Exception ex) {
                Log.d("AR Navigator", "GPS Initialize Error", ex);
            }

            getMixViewData().getMixContext().getLocationFinder().switchOn();
            augGLScreen.onResume();
        } catch (Exception ex) {
            doError(ex);
            try {
                if (getMixViewData().getSensorMgr() != null) {
                    getMixViewData().getSensorMgr().unregisterListener(this,
                            getMixViewData().getSensorGrav());
                    getMixViewData().getSensorMgr().unregisterListener(this,
                            getMixViewData().getSensorMag());
                    getMixViewData().setSensorMgr(null);
                }

                if (getMixViewData().getMixContext() != null) {
                    getMixViewData().getMixContext().getLocationFinder()
                            .switchOff();
                }
            } catch (Exception ignore) {
            }
        }
        ARView.getDataView().setRadius(0.5f);
        Log.d("----------------------", "resume");
    }

    /**
     * {@inheritDoc} Customize Activity after switching back to it. Currently it
     * maintain and ensures view creation.
     */
    protected void onRestart() {
        super.onRestart();
        mframe.removeAllViewsInLayout();
        mframe.removeAllViews();
        maintainCamera();
        maintainAugmentGLView();
        maintainAugmentR();
        maintainToolBar();
    }

	/* ********* Operators ********** */

    public void repaint() {

        setDataView(null); // It's smelly code, but enforce garbage collector to release data.
        setDataView(new DataView(getMixViewData().getMixContext()));
        setdWindow(new PaintScreen());
        setZoomLevel(); //@TODO Caller has to set the zoom. This function
    }

    /**
     * Checks camScreen, if it does not exist, it creates one.
     */
    private void maintainCamera() {
        if (camScreen == null) {
            camScreen = new CameraSurface(this);
        }
        mframe.addView(camScreen);
    }

    /**
     * Checks augScreen, if it does not exist, it creates one.
     */
    private void maintainAugmentR() {
        if (augScreen == null) {
            augScreen = new AugmentedView(this);
        }
        mframe.addView(augScreen);
    }

    /**
     * Creates a AugmentGLView and adds it to view.
     */
    private void maintainAugmentGLView() {
        if (augGLScreen == null) {
            augGLScreen = new AugmentedGLView(this);
        }

        mframe.addView(augGLScreen);
        augGLScreen.setZOrderMediaOverlay(true);// ��Ҫ
    }

    /**
     * Creates a tool bar and adds it to view.
     */
    private void maintainToolBar() {
        FrameLayout frameLayout = createToolBar();
        addContentView(frameLayout, new FrameLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
                Gravity.RIGHT | Gravity.TOP));
    }

    public void refresh() {
        getDataView().refresh();
    }

    public void setErrorDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.connection_error_dialog));
        builder.setCancelable(false);

		/* Retry */
        builder.setPositiveButton(R.string.connection_error_dialog_button1,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        fError = false;
                        // TODO improve
                        try {
                            maintainCamera();
                            maintainAugmentGLView();
                            maintainAugmentR();

                            repaint();
                            setZoomLevel();
                        } catch (Exception ex) {
                        }
                    }
                });
        /* Open settings */
        builder.setNeutralButton(R.string.connection_error_dialog_button2,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Intent intent1 = new Intent(
                                Settings.ACTION_WIRELESS_SETTINGS);
                        startActivityForResult(intent1, 42);
                    }
                });
        /* Close application */
        builder.setNegativeButton(R.string.connection_error_dialog_button3,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        System.exit(0); // wouldn't be better to use finish (to
                        // stop the app normally?)
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    /**
     * Handle First time users. It display license agreement and store user's
     * acceptance.
     *
     * @param settings
     */
    private void firstAccess(SharedPreferences settings) {
        SharedPreferences.Editor editor = settings.edit();
        AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
        builder1.setMessage(getString(R.string.license));
        builder1.setNegativeButton(getString(R.string.close_button),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
        AlertDialog alert1 = builder1.create();
        alert1.setTitle(getString(R.string.license_title));
        editor.putBoolean("firstAccess", true);

        // default is 5
        editor.putInt("osmMaxObject", 5);
        editor.commit();

        // add the default datasources to the preferences file
        DataSourceStorage.getInstance().fillDefaultDataSources();
    }

    /**
     * Create tool bar and returns FrameLayout. FrameLayout is created to be
     * hidden and not added to view, Caller needs to add the frameLayout to
     * view, and enable visibility when needed.
     *
     * @return FrameLayout Hidden Zoom Bar
     */


    private FrameLayout createToolBar() {
        int numColumns = 2;
        if (ispath) {
            numColumns = 1;
        }

        int rowWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50f,
                getApplicationContext().getResources().getDisplayMetrics());
        GridView myToolBar = new GridView(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(rowWidth * 2,
                LayoutParams.WRAP_CONTENT);
        myToolBar.setLayoutParams(params);
        myToolBar.setNumColumns(numColumns);
        myToolBar.setGravity(Gravity.RIGHT);
        myToolBar.setHorizontalSpacing(rowWidth / 5);

        ImageAdapter imageAdapter = new ImageAdapter(this);
        imageAdapter.setispath(ispath);
        myToolBar.setAdapter(imageAdapter);
        myToolBar.setOnTouchListener(this);
        myToolBar.setVisibility(View.VISIBLE);

        myToolBar.setOnItemClickListener(new GridView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View arg1,
                                    int position, long arg3) {
                for (int i = 0; i < parent.getCount(); i++) {
                    if (position == i) {
                        switch (position) {
                        /* List view */
                            case 0:
                                if (getDataView().getDataHandler().getMarkerCount() > 0) {
                                    Intent intent1 = new Intent(ARView.this,
                                            ARListView.class);
                                    startActivityForResult(intent1, 42);
                                }
                            /* if the list is empty */
                                else {
                                    getMixViewData().getMixContext().doMsgPopUp(
                                            R.string.empty_list);
                                }
                                break;
                            // MapActivity2
                            case 1:
                                Intent intent = new Intent(ARView.this, MapActivity.class);
                                startActivity(intent);
                                finish();
                                break;
                            default:
                                break;
                        }
                    } else {
                    }
                }
            }
        });

        getMixViewData().setMyToolBar(myToolBar);

        FrameLayout frameLayout = new FrameLayout(this);
        frameLayout.addView(getMixViewData().getMyToolBar());

        int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20f,
                getApplicationContext().getResources().getDisplayMetrics());
        frameLayout.setPadding(0, padding, padding, 0);// left,top,right,bottom

        return frameLayout;
    }


    public void onSensorChanged(SensorEvent evt) {
        try {

            if (evt.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                filteredGravX = mKalGravX.update(evt.values[0]);
                filteredGravY = mKalGravY.update(evt.values[1]);
                filteredGravZ = mKalGravZ.update(evt.values[2]);

                getMixViewData().getGrav()[0] = (float)filteredGravX;
                getMixViewData().getGrav()[1] = (float)filteredGravY;
                getMixViewData().getGrav()[2] = (float)filteredGravZ;

                if (augScreen != null)
                    augScreen.postInvalidate();
            } else if (evt.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                filteredMagX = mKalMagX.update(evt.values[0]);
                filteredMagY = mKalMagY.update(evt.values[1]);
                filteredMagZ = mKalMagZ.update(evt.values[2]);
                getMixViewData().getMag()[0] = (float)filteredMagX;
                getMixViewData().getMag()[1] = (float)filteredMagY;
                getMixViewData().getMag()[2] = (float)filteredMagZ;

                if (augScreen != null)
                    augScreen.postInvalidate();
            }

            SensorManager.getRotationMatrix(getMixViewData().getRTmp(),
                    getMixViewData().getI(), getMixViewData().getGrav(),
                    getMixViewData().getMag());

            mRotation = Compatibility.getRotation(this);
            if (mRotation == 1) {
                SensorManager.remapCoordinateSystem(getMixViewData().getRTmp(),
                        SensorManager.AXIS_X, SensorManager.AXIS_MINUS_Y,
                        getMixViewData().getRot());
            } else {
                SensorManager.remapCoordinateSystem(getMixViewData().getRTmp(),
                        SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_Z,
                        getMixViewData().getRot());
            }

            getMixViewData().getTempR().set(getMixViewData().getRot()[0],
                    getMixViewData().getRot()[1], getMixViewData().getRot()[2],
                    getMixViewData().getRot()[3], getMixViewData().getRot()[4],
                    getMixViewData().getRot()[5], getMixViewData().getRot()[6],
                    getMixViewData().getRot()[7], getMixViewData().getRot()[8]);

            getMixViewData().getFinalR().toIdentity();
            getMixViewData().getFinalR().prod(getMixViewData().getM4());
            getMixViewData().getFinalR().prod(getMixViewData().getM1());
            getMixViewData().getFinalR().prod(getMixViewData().getTempR());
            getMixViewData().getFinalR().prod(getMixViewData().getM3());
            getMixViewData().getFinalR().prod(getMixViewData().getM2());
            getMixViewData().getFinalR().invert();

            getMixViewData().getHistR()[getMixViewData().getrHistIdx()]
                    .set(getMixViewData().getFinalR());
            getMixViewData().setrHistIdx(getMixViewData().getrHistIdx() + 1);
            if (getMixViewData().getrHistIdx() >= getMixViewData().getHistR().length)
                getMixViewData().setrHistIdx(0);

            getMixViewData().getSmoothR().set(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f,
                    0f);
            for (int i = 0; i < getMixViewData().getHistR().length; i++) {
                getMixViewData().getSmoothR().add(
                        getMixViewData().getHistR()[i]);
            }
            getMixViewData().getSmoothR().mult(
                    1 / (float) getMixViewData().getHistR().length);

            getMixViewData().getMixContext().updateSmoothRotation(
                    getMixViewData().getSmoothR());
            SensorManager.getOrientation(getMixViewData().getRot(),
                    orientationValues);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        try {
            int ea = event.getAction();
            switch (ea) {
                case MotionEvent.ACTION_DOWN:
                    break;
                case MotionEvent.ACTION_MOVE:
                    break;
                case MotionEvent.ACTION_UP:
                    break;
            }
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            return super.onTouchEvent(event);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        try {
            killOnError();

            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if (getDataView().isDetailsView()) {
                    getDataView().keyEvent(keyCode);
                    getDataView().setDetailsView(false);
                    return true;
                } else {
                    // TODO handle keyback to finish app correctly
                    return super.onKeyDown(keyCode, event);
                }
            } else if (keyCode == KeyEvent.KEYCODE_MENU) {
                return super.onKeyDown(keyCode, event);
            } else {
                getDataView().keyEvent(keyCode);
                return false;
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            return super.onKeyDown(keyCode, event);
        }
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        if (sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD
                && accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE
                && getMixViewData().getCompassErrorDisplayed() == 0) {
            getMixViewData().getMixContext().doMsgPopUp(
                    R.string.compass_data_error);
            getMixViewData().setCompassErrorDisplayed(
                    getMixViewData().getCompassErrorDisplayed() + 1);
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return false;
    }

	/* ************ Handlers ************ */

    public void doError(Exception ex1) {
        if (!fError) {
            fError = true;

            setErrorDialog();

            ex1.printStackTrace();
            try {
            } catch (Exception ex2) {
                ex2.printStackTrace();
            }
        }

        try {
            augScreen.invalidate();
        } catch (Exception ignore) {
        }
    }

    public void killOnError() throws Exception {
        if (fError)
            throw new Exception();
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            doMixSearch(query);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        handleIntent(intent);
    }

    @SuppressWarnings("deprecation")
    private void doMixSearch(String query) {
        DataHandler jLayer = getDataView().getDataHandler();
        DataHandler rLayer = getDataView().getRouteDataHandler();
        if (!getDataView().isFrozen()) {
            ARListView.originalMarkerList = jLayer.getMarkerList();
            ARListView.routeMarkerList = rLayer.getMarkerList();
        }

        ArrayList<Marker> searchResults = new ArrayList<Marker>();
        Log.d("SEARCH-----------0", "" + query);
        if (jLayer.getMarkerCount() > 0) {
            for (int i = 0; i < jLayer.getMarkerCount(); i++) {
                Marker ma = jLayer.getMarker(i);
                if (ma.getTitle().toLowerCase().indexOf(query.toLowerCase()) != -1) {
                    searchResults.add(ma);
                    /* the website for the corresponding title */
                }
            }
        }
        if (searchResults.size() > 0) {
            getDataView().setFrozen(true);
            jLayer.setMarkerList(searchResults);
            rLayer.setMarkerList(searchResults);
        } else {
            getMixViewData().getMixContext().doMsgPopUp(
                    R.string.search_failed_notification);
        }
    }

    /* ******* Getter and Setters ********** */
    public String getZoomLevel() {
        return getMixViewData().getZoomLevel();
    }

    /**
     * @return the dWindow
     */
    static PaintScreen getdWindow() {
        return dWindow;
    }

    /**
     * @param dWindow the dWindow to set
     */
    static void setdWindow(PaintScreen dWindow) {
        ARView.dWindow = dWindow;
    }

    /**
     * @return the dataView
     */
    static DataView getDataView() {
        return dataView;
    }

    /**
     * @param dataView the dataView to set
     */
    static void setDataView(DataView dataView) {
        if (getDataView() != null && dataView == null) {
            // clear stored data
            getDataView().clearEvents();
            getDataView().cancelRefreshTimer();
            getDataView().setRouteListMarker(rmaker);
        }
        ARView.dataView = dataView;
    }

    private void setZoomLevel() {
        float myout = 0.1f;
        getDataView().setRadius(myout);
        getMixViewData().setZoomLevel(String.valueOf(myout));
    }
}

/**
 * @author daniele
 */
class CameraSurface extends SurfaceView implements SurfaceHolder.Callback {
    ARView app;
    SurfaceHolder holder;
    Camera camera;

    CameraSurface(Context context) {
        super(context);

        try {
            app = (ARView) context;

            holder = getHolder();
            holder.addCallback(this);
            holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        } catch (Exception ex) {

        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
        try {
            if (camera != null) {
                try {
                    camera.stopPreview();
                } catch (Exception ignore) {
                }
                try {
                    camera.release();
                } catch (Exception ignore) {
                }
                camera = null;
            }

            camera = Camera.open();
            camera.setDisplayOrientation(90); //세로 모드로
            camera.setPreviewDisplay(holder);
        } catch (Exception ex) {
            try {
                if (camera != null) {
                    try {
                        camera.stopPreview();
                    } catch (Exception ignore) {
                    }
                    try {
                        camera.release();
                    } catch (Exception ignore) {
                    }
                    camera = null;
                }
            } catch (Exception ignore) {

            }
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        try {
            if (camera != null) {
                try {
                    camera.stopPreview();
                } catch (Exception ignore) {
                }
                try {
                    camera.release();
                } catch (Exception ignore) {
                }
                camera = null;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        try {
            Camera.Parameters parameters = camera.getParameters();
            try {
                List<Camera.Size> supportedSizes = null;
                // On older devices (<1.6) the following will fail
                // the camera will work nevertheless
                supportedSizes = Compatibility
                        .getSupportedPreviewSizes(parameters);

                // preview form factor
                float ff = (float) w / h;
                Log.d(ARView.TAG, "Screen res: w:" + w + " h:" + h
                        + " aspect ratio:" + ff);

                // holder for the best form factor and size
                float bff = 0;
                int bestw = 0;
                int besth = 0;
                Iterator<Camera.Size> itr = supportedSizes.iterator();

                while (itr.hasNext()) {
                    Camera.Size element = itr.next();
                    // current form factor
                    float cff = (float) element.width / element.height;

                    Log.d(ARView.TAG, "Candidate camera element: w:"
                            + element.width + " h:" + element.height
                            + " aspect ratio:" + cff);
                    if ((ff - cff <= ff - bff) && (element.width <= w)
                            && (element.width >= bestw)) {
                        bff = cff;
                        bestw = element.width;
                        besth = element.height;
                    }
                }
                Log.d(ARView.TAG, "Chosen camera element: w:" + bestw + " h:"
                        + besth + " aspect ratio:" + bff);

                // default values: 480x320
                if ((bestw == 0) || (besth == 0)) {
                    Log.d(ARView.TAG, "Using default camera parameters!");
                    bestw = 480;
                    besth = 320;
                }
                parameters.setPreviewSize(bestw, besth);
            } catch (Exception ex) {
                parameters.setPreviewSize(480, 320);
            }

            camera.setParameters(parameters);
            camera.startPreview();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}

class Cluster {
    private List<Label> labels = null;

    Cluster() {
        labels = new Vector<Label>();
    }

    void addLabel(int id, float x, float y, Marker ma) {
        Label label = new Label();
        label.setId(id);
        label.setLocation(x, y);
        label.setMarker(ma);
        labels.add(label);
    }

    void sort(boolean nearToFar) {
        if (nearToFar) {
            Collections.sort(labels);
        } else {
            Collections.reverse(labels);
        }
    }

    void clear() {
        labels.clear();
    }

    int size() {
        return labels.size();
    }

    Label get(int location) {
        return labels.get(location);
    }

    public void setScreenLocation(int index, float x, float y) {
        labels.get(index).setScreenLocation(x, y);
    }

    public void setTextLocation(int index, float x, float y) {
        labels.get(index).setTextLocation(x, y);
    }

    public void setTextSize(int index, float width, float height) {
        labels.get(index).setTextSize(width, height);
    }

    static float intersect(float a1, float a2, float b1, float b2) {
        float pMax = Math.max(a2, b2);
        float pMin = Math.min(a1, b1);
        float width = pMax - pMin + 1;
        float aWidth = a2 - a1 + 1;
        float bWidth = b2 - b1 + 1;
        float result = (aWidth + bWidth) - width;
        return result > 0 ? result : 0;
    }

    static boolean isOverlap(float x1, float y1, float w1, float h1, float x2,
                             float y2, float w2, float h2) {
        float s1 = w1 * h1;
        float w = intersect(x1, x1 + w1, x2, x2 + w2);
        float h = intersect(y1, y1 + h1, y2, y2 + h2);
        float s = w * h;
        if (s / s1 > 0.5) {
            return true;
        }
        return false;
    }

    private boolean isOverlap(int a, int b) {
        float x1 = labels.get(a).getTextX();
        float y1 = labels.get(a).getTextY();
        float w1 = labels.get(a).getTextWidth();
        float h1 = labels.get(a).getTextHeight();
        float s1 = w1 * h1;

        float x2 = labels.get(b).getTextX();
        float y2 = labels.get(b).getTextY();
        float w2 = labels.get(b).getTextWidth();
        float h2 = labels.get(b).getTextHeight();
        float s2 = w2 * h2;

        float w = intersect(x1, x1 + w1, x2, x2 + w2);
        float h = intersect(y1, y1 + h1, y2, y2 + h2);
        float s = w * h;
        if (s / s1 > 0.1 && s / s2 > 0.1) {
            return true;
        }
        return false;
    }

    public void process(Cluster cluster) {
        for (int i = 0; i < cluster.size(); ++i) {
            for (int j = i + 1; j < cluster.size(); ++j) {
                boolean re = isOverlap(i, j);
                if (re) {
                    if ((getState(i) == State.SINGLE || getState(i) == State.FATHER)
                            && getState(j) == State.SINGLE) {
                        addChild(i, j);
                    }
                } else {
                    // Todo.
                }
            }
        }
    }

    void setState(int index, State state) {
        labels.get(index).setState(state);
    }

    State getState(int index) {
        return labels.get(index).getState();
    }

    void addChild(int father, int child) {
        setState(father, State.FATHER);
        setState(child, State.CHILD);
        labels.get(father).getChildList().add(labels.get(child));
    }

    public static class Label implements Comparable {
        private float x;
        private float y;
        private int id;
        private Marker marker;
        private float screenX;
        private float screenY;
        public float textX;
        public float textY;
        public float textW;
        public float textH;
        public float labelX;
        public float labelY;
        public float labelWidth;
        public float labelHeight;
        private Vector<Label> childList = null;
        public State state;

        public enum State {
            CHILD, FATHER, SINGLE
        }

        ;

        Label() {
            childList = new Vector<Label>();
            state = State.SINGLE;
        }

        public Vector<Label> getChildList() {
            return childList;
        }

        public void setState(State state) {
            this.state = state;
        }

        public State getState() {
            return state;
        }

        public void setId(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public void setLocation(float x, float y) {
            this.x = x;
            this.y = y;
        }

        public float getLocationX() {
            return x;
        }

        public float getLocationY() {
            return y;
        }

        public void setMarker(Marker marker) {
            this.marker = marker;
        }

        public Marker getMarker() {
            return marker;
        }

        public float getScreenX() {
            return screenX;
        }

        public float getScreenY() {
            return screenY;
        }

        public void setScreenLocation(float x, float y) {
            this.screenX = x;
            this.screenY = y;
        }

        public void setTextLocation(float x, float y) {
            this.textX = x;
            this.textY = y;
        }

        public void setTextSize(float w, float h) {
            this.textW = w;
            this.textH = h;
        }

        public float getTextWidth() {
            return textW;
        }

        public float getTextHeight() {
            return textH;
        }

        public float getTextX() {
            return textX;
        }

        public float getTextY() {
            return textY;
        }

        @Override
        public int compareTo(Object another) {
            if (this.marker.getDistance() > ((Label) another).marker
                    .getDistance()) {
                return 1;
            } else if (this.marker.getDistance() < ((Label) another).marker
                    .getDistance()) {
                return -1;
            }
            return 0;
        }
    }
}

class OpenGLRenderer implements Renderer {
    float one = 1f;
    ARView app;
    AugmentedGLView parent;
    int width = 0;
    int height = 0;
    private Projector mProjector;
    private LabelMaker mLabels;
    private Paint mLabelPaint;
    private Cluster cluster = null;
    private boolean isStop;
    int rcount = 0;
    String distance;

    //textbox 설정값
    public OpenGLRenderer(AugmentedGLView parent) {
        int fontsize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 15f,    //pixel to dp
                app.ctx.getResources().getDisplayMetrics());
        mProjector = new Projector();
        mLabelPaint = new Paint();
        mLabelPaint.setTextSize(fontsize);
        mLabelPaint.setAntiAlias(true);
        mLabelPaint.setARGB(0xff, 0xff, 0xff, 0xff);
        cluster = new Cluster();
        this.parent = parent;
    }

    public void stopRenderer() {
        isStop = true;
    }

    public void resumeRenderer() {
        isStop = false;
    }

    public boolean isStop() {
        return isStop;
    }

    public Buffer bufferUtil(int[] arr) {
        IntBuffer mBuffer;

        ByteBuffer qbb = ByteBuffer.allocateDirect(arr.length * 4);
        qbb.order(ByteOrder.nativeOrder());

        mBuffer = qbb.asIntBuffer();
        mBuffer.put(arr);
        mBuffer.position(0);

        return mBuffer;
    }

    private Buffer bufferUtil(float[] arr) {
        FloatBuffer mBuffer;

        ByteBuffer qbb = ByteBuffer.allocateDirect(arr.length * 4);
        qbb.order(ByteOrder.nativeOrder());

        mBuffer = qbb.asFloatBuffer();
        mBuffer.put(arr);
        mBuffer.position(0);

        return mBuffer;
    }

    public void setMixView(ARView mixView) {
        this.app = mixView;
    }

    public void onSurfaceCreated(GL10 gl, EGLConfig config) {

        gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_FASTEST);
        gl.glClearColor(0, 0, 0, 0);
        gl.glShadeModel(GL10.GL_SMOOTH);

        gl.glClearDepthf(1.0f);
        gl.glEnable(GL10.GL_DEPTH_TEST);
        gl.glDepthFunc(GL10.GL_LEQUAL);

        if (mLabels != null) {
            mLabels.shutdown(gl);
        } else {
            mLabels = new LabelMaker(true, 512, 1024, app, parent);
        }

        mLabels.initialize(gl);
    }

    public void onDrawFrame(GL10 gl) {
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
        gl.glPushMatrix();
        gl.glLoadIdentity();

        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);

        gl.glEnableClientState(GL10.GL_COLOR_BUFFER_BIT);
        gl.glColor4f(1.0f, 0.0f, 0.0f, 1.0f);

        GLU.gluLookAt(gl, 0, 0, 1, 0, 0, 0, 0, 1, 0);
        gl.glTranslatef(0f, -1f, 0f);

        float trRot = 0;
        if (ARView.mRotation == 0) {
            trRot = 90;
        } else if (ARView.mRotation == 1) {
            trRot = 0;
        }

        float tr0 = ARView.orientationValues[2]; //x축
        gl.glRotatef((float) (-tr0 * 180 / Math.PI) - trRot, 1.0f, 0.0f, 0.0f);

        float tr1 = ARView.orientationValues[1]; //y축
        gl.glRotatef((float) (tr1 * 180 / Math.PI), 0.0f, 1.0f, 0.0f);

        float tr2 = ARView.orientationValues[0]; //z축
        gl.glRotatef((float) (-tr2 * 180 / Math.PI), 0.0f, 0.0f, 1.0f);

        gl.glRotatef(90, 0.0f, 0.0f, 1.0f);

        float tr3 = (float) (tr2 * 180 / Math.PI);
        int radius = 30;

        gl.glPushMatrix();
        /**
         * 평면 그리기
         */
//		for (float i = -radius * one; i <= radius * one; i = i + side * one) {
//			gl.glVertexPointer(3, GL10.GL_FLOAT, 0, bufferUtil(new float[] {
//					-i, radius * one, 0, -i, -radius * one, 0 }));
//			gl.glDrawArrays(GL10.GL_LINES, 0, 2); //가로 평면
//			gl.glVertexPointer(3, GL10.GL_FLOAT, 0, bufferUtil(new float[] {
//					-radius * one, -i, 0, (radius + 1) * one, -i, 0 }));
//			gl.glDrawArrays(GL10.GL_LINES, 0, 2); //세로 평면
//		}

        /**
         * 좌표축 그리기
         */
        // 카메라 앞쪽 x축
//        gl.glColor4f(0.0f, 1.0f, 0.0f, 1.0f);
//        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, bufferUtil(
//                new float[]{0, 0, 0,
//                        radius * one, 0, 0}));
//        gl.glDrawArrays(GL10.GL_LINES, 0, 2);
//
//        //y축
//        gl.glColor4f(0.0f, 1.0f, 1.0f, 1.0f);
//        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, bufferUtil(
//                new float[]{0, 0, 0,
//                        0, radius * one, 0}));
//        gl.glDrawArrays(GL10.GL_LINES, 0, 2);
//
//        //z축
//        gl.glColor4f(1.0f, 0.0f, 0.0f, 1.0f);
//        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, bufferUtil(
//                new float[]{0, 0, 0,
//                        0, 0, radius * one}));
//        gl.glDrawArrays(GL10.GL_LINES, 0, 2);

        gl.glPopMatrix();

        gl.glColor4f(1.0f, 0.0f, 0.0f, 1.0f);
        gl.glTranslatef(0f, 1f, 0f);

        mProjector.getCurrentModelView(gl);

        if (ARView.getDataView() != null && cluster != null) {
            float scale = ARView.getDataView().getRadius() * 1000 / radius;
            DataHandler jLayer = ARView.getDataView().getDataHandler();
            DataHandler rLayer = ARView.getDataView().getRouteDataHandler();

            cluster.clear();
            mLabels.beginAdding(gl);
            float rx = 0, ry = 0;

            // 건물 마커 좌표변경
            // lat, lng --> physic location
            for (int i = 0; i < jLayer.getMarkerCount(); i++) {
                Marker pm = jLayer.getMarker(i);

                if (pm.getDistance() < 100) { // 100m
                    //lat, lng --> physic location --> x, y
                    float x = (pm.getLocationVector().x / scale) * one;
                    float y = (pm.getLocationVector().z / scale) * one;

                    int id = mLabels.add(gl, pm.getTitle(), mLabelPaint); // text 추가
                    cluster.addLabel(id, x, y, pm);

                }
            }

            // 길찾기 좌표평면 변경
            // lat, lng --> physic location
            if (rLayer.getMarkerCount() != 0 && rcount <= rLayer.getMarkerCount()) {
                Marker rm = rLayer.getMarker(rcount);
                rx = (rm.getLocationVector().x / scale) * one;
                ry = (rm.getLocationVector().y / scale) * one;

                distance = String.format("%.2f" , rm.getDistance());
                run();

                gl.glTranslatef(0.0f, -1.0f, 0.0f);
                Line line = new Line(new float[]{0, 0, 0,
                        rm.getLocationVector().x, rm.getLocationVector().z, -one});
                gl.glColor4f(1.0f, 1.0f, 0.0f, 0.3f);
                line.draw(gl);

//                Line line2 = new Line(new float[]{0, 0, 0,
//                        rm.getLocationVector().x, rm.getLocationVector().y, -rm.getLocationVector().z});
//                gl.glColor4f(0.0f, 1.0f, 1.0f, 0.3f);
//                line2.draw(gl);
                gl.glTranslatef(0.0f, 1.0f, 0.0f);

                Log.d("route x, y : ", rx + " / " + ry);
            }

            mLabels.endAdding(gl);
            cluster.sort(false);
            for (int i = 0; i < cluster.size(); ++i) {

                float[] scratch = new float[8];
                scratch[0] = cluster.get(i).getLocationX();
                scratch[1] = cluster.get(i).getLocationY();
                scratch[2] = 0.0f;
                scratch[3] = 1.0f;
                mProjector.project(scratch, 0, scratch, 4);
                float sx = scratch[4];
                float sy = scratch[5];
                cluster.setScreenLocation(i, sx, sy);
                float labelHeight = mLabels.getHeight(i);
                float labelWidth = mLabels.getWidth(i);
                cluster.setTextSize(i, labelWidth, labelHeight);
                float tx = sx - labelWidth * 0.5f;
                float ty = sy - labelHeight * 0.5f;
                cluster.setTextLocation(i, tx, ty);
            }


            for (int i = 0; i < cluster.size(); ++i) {
                gl.glBindTexture(GL10.GL_TEXTURE_2D, 0);
                float x = cluster.get(i).getLocationX();
                float y = cluster.get(i).getLocationY();

                if (cluster.get(i).getState() == State.FATHER) {

                    float k = (float) Math.tan(tr2);
                    if (((tr3 > 0 && tr3 < 90) && (x / k + y < 0))
                            || ((tr3 > 90 && tr3 < 180) && (x / k + y < 0))
                            || ((tr3 > -90 && tr3 < 0) && (x / k + y > 0))
                            || ((tr3 > -180 && tr3 < -90) && (x / k + y > 0))) {
                        RectF labelRect = new RectF();
                        mLabels.drawInOne(gl, cluster.get(i).getTextX(),
                                cluster.get(i).getTextY(), cluster.get(i)
                                        .getId(), width, height, 1, labelRect);
                        cluster.get(i).labelX = labelRect.left;
                        cluster.get(i).labelY = labelRect.top;
                        cluster.get(i).labelWidth = labelRect.right
                                - labelRect.left;
                        cluster.get(i).labelHeight = labelRect.bottom
                                - labelRect.top;
                    }

                } else if (cluster.get(i).getState() == State.SINGLE) {
                    // 마커까지 선 그리기
                    if (rLayer.getMarkerCount() != 0) {
                        gl.glTranslatef(0.0f, -1.0f, 0.0f);
//                        Line line = new Line(new float[]{0, 0, 0,
//                                rx, ry+1, -one});
//                        gl.glColor4f(1.0f, 0.0f, 0.0f, 0.3f);
//
//                        line.draw(gl);
                        gl.glTranslatef(0.0f, 1.0f, 0.0f);
                    }

                    float k = (float) Math.tan(tr2);
                    if (((tr3 > 0 && tr3 < 90) && (x / k + y < 0))
                            || ((tr3 > 90 && tr3 < 180) && (x / k + y < 0))
                            || ((tr3 > -90 && tr3 < 0) && (x / k + y > 0))
                            || ((tr3 > -180 && tr3 < -90) && (x / k + y > 0))) {
                        RectF labelRect = new RectF();
                        mLabels.drawInOne(gl, cluster.get(i).getTextX(),
                                cluster.get(i).getTextY(), cluster.get(i)
                                        .getId(), width, height, 0, labelRect);

                        cluster.get(i).labelX = labelRect.left;
                        cluster.get(i).labelY = labelRect.top;
                        cluster.get(i).labelWidth = labelRect.right
                                - labelRect.left;
                        cluster.get(i).labelHeight = Math.abs(labelRect.top
                                - labelRect.bottom);
                    }
                }
            }
        }

        gl.glDisableClientState(GL10.GL_COLOR_BUFFER_BIT);
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glPopMatrix();
    }

    public void run() {

        Message msg = handler.obtainMessage();
        handler.sendMessage(msg);
    }

    final Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            app.dist.setText(distance + " m 남았습니다.");

            if (Double.parseDouble(distance) < 10) {
                Toast.makeText(ARView.ctx, "다음 경로로 안내합니다.", Toast.LENGTH_SHORT).show();
                rcount++;
            }
        }
    };

    public void onTouch(float xPress, float yPress) {
        if (cluster != null) {
            int r = 20;
            Vector<Label> labels = new Vector<Label>();
            for (int i = 0; i < cluster.size(); ++i) {
                if (cluster.get(i) != null) {

                    float textX = cluster.get(i).labelX;
                    float textY = cluster.get(i).labelY;
                    float textWidth = cluster.get(i).labelWidth;
                    float textHeight = cluster.get(i).labelHeight;

                    if (Cluster.isOverlap(xPress - r, yPress - r, 2 * r, 2 * r,
                            textX, textY, textWidth, textHeight)) {
                        labels.add(cluster.get(i));
                    }
                }
            }

            if (labels.size() == 1) {
                Message msg = new Message();
                msg.obj = labels;
                parent.getmHandler().sendMessage(msg);
            } else if (labels.size() > 1) {
                Message msg = new Message();
                msg.obj = labels;
                parent.getmHandler().sendMessage(msg);
            }
        }
    }

    public void onSurfaceChanged(GL10 gl, int width, int height) {
        float radio = (float) width / height;

        this.width = width;
        this.height = height;

        gl.glViewport(0, 0, width, height);
        mProjector.setCurrentView(0, 0, width, height);
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();

        gl.glFrustumf(-radio, radio, -1, 1, 1, 30);
        mProjector.getCurrentProjection(gl);
        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glLoadIdentity();
    }

    void setLabels(Cluster cluster) {
        this.cluster = cluster;
    }
}

class AugmentedGLView extends GLSurfaceView implements OnTouchListener {
    ARView app;
    private OpenGLRenderer mRenderer;
    private Cluster cluster = null;

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            Vector<Label> labels = (Vector<Label>) msg.obj;

            ARView.getDataView().getState().nextLStatus = MixState.NOT_STARTED;
            String webpage = MixUtils.parseAction(labels.get(0).getMarker()
                    .getURL());
            ARView.getDataView().getState().setDetailsView(true);
            try {
                ARView.getDataView().getContext()
                        .loadMixViewWebPage(webpage);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    };

    public AugmentedGLView(Context context) {
        super(context);
        this.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        getHolder().setFormat(PixelFormat.TRANSLUCENT);

        mRenderer = new OpenGLRenderer(this);
        cluster = new Cluster();
        this.setGLWrapper(new GLSurfaceView.GLWrapper() {
            public GL wrap(GL gl) {
                return new MatrixTrackingGL(gl);
            }
        });
        this.setRenderer(mRenderer);
        mRenderer.setLabels(cluster);

        try {
            app = (ARView) context;
            app.killOnError();
            mRenderer.setMixView(app);
        } catch (Exception ex) {
            app.doError(ex);
        }
    }

    public boolean onTouchEvent(final MotionEvent me) {

        if (me.getAction() == MotionEvent.ACTION_UP) {
            final float height = this.getHeight();
            queueEvent(new Runnable() {
                // �������������Ⱦ�߳��ﱻ����
                public void run() {
                    mRenderer.onTouch(me.getX(), height - me.getY());
                }
            });
        }
        return true;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        // TODO Auto-generated method stub
        return false;
    }

    public Handler getmHandler() {
        return mHandler;
    }

    public void setmHandler(Handler mHandler) {
        this.mHandler = mHandler;
    }

}

class AugmentedView extends View {
    ARView app;

    public AugmentedView(Context context) {
        super(context);

        try {
            app = (ARView) context;

            app.killOnError();
        } catch (Exception ex) {
            app.doError(ex);
        }
    }

    @Override
    // TODO Auto-generated method stub
    protected void onDraw(Canvas canvas) {

        try {
            app.killOnError();
            // TODO Auto-generated catch block

            ARView.getdWindow().setWidth(canvas.getWidth());
            ARView.getdWindow().setHeight(canvas.getHeight());

            ARView.getdWindow().setCanvas(canvas);

            Log.d(ARView.TAG + " WorkFlow", "MixView - onDraw called");
            if (ARView.getDataView() != null) {
                if (!ARView.getDataView().isInited()) {
                    ARView.getDataView().init(ARView.getdWindow().getWidth(),
                            ARView.getdWindow().getHeight());
                }
                Log.d(ARView.TAG + " WorkFlow",
                        "MixView - getDataView() != null");
                ARView.getDataView().draw(ARView.getdWindow(), ARView.rmaker);
            }

        } catch (Exception ex) {
            app.doError(ex);
        }
    }
}

/**
 * Internal class that holds Mixview field Data.
 *
 * @author A B
 */
class MixViewDataHolder {
    private final MixContext mixContext;
    private float[] RTmp;
    private float[] Rot;
    private float[] I;
    private float[] grav;
    private float[] mag;
    private SensorManager sensorMgr;
    private List<Sensor> sensors;
    private Sensor sensorGrav;// TYPE_ACCELEROMETER
    private Sensor sensorMag;// TYPE_MAGNETIC_FIELD
    private int rHistIdx;
    private Matrix tempR;
    private Matrix finalR;
    private Matrix smoothR;
    private Matrix[] histR;
    private Matrix m1;
    private Matrix m2;
    private Matrix m3;
    private Matrix m4;
    private WakeLock mWakeLock;
    private int compassErrorDisplayed;
    private String zoomLevel;
    private TextView searchNotificationTxt;
    private GridView myToolBar;

    public MixViewDataHolder(MixContext mixContext) {
        this.mixContext = mixContext;
        this.RTmp = new float[9];
        this.Rot = new float[9];
        this.I = new float[9];
        this.grav = new float[3];
        this.mag = new float[3];
        this.rHistIdx = 0;
        this.tempR = new Matrix();
        this.finalR = new Matrix();
        this.smoothR = new Matrix();
        this.histR = new Matrix[60];
        this.m1 = new Matrix();
        this.m2 = new Matrix();
        this.m3 = new Matrix();
        this.m4 = new Matrix();
        this.compassErrorDisplayed = 0;
    }

    /* ******* Getter and Setters ********** */
    public MixContext getMixContext() {
        return mixContext;
    }

    public float[] getRTmp() {
        return RTmp;
    }

    public void setRTmp(float[] rTmp) {
        RTmp = rTmp;
    }

    public float[] getRot() {
        return Rot;
    }

    public void setRot(float[] rot) {
        Rot = rot;
    }

    public float[] getI() {
        return I;
    }

    public void setI(float[] i) {
        I = i;
    }

    public float[] getGrav() {
        return grav;
    }

    public void setGrav(float[] grav) {
        this.grav = grav;
    }

    public float[] getMag() {
        return mag;
    }

    public void setMag(float[] mag) {
        this.mag = mag;
    }

    public SensorManager getSensorMgr() {
        return sensorMgr;
    }

    public void setSensorMgr(SensorManager sensorMgr) {
        this.sensorMgr = sensorMgr;
    }

    public List<Sensor> getSensors() {
        return sensors;
    }

    public void setSensors(List<Sensor> sensors) {
        this.sensors = sensors;
    }

    public Sensor getSensorGrav() {
        return sensorGrav;
    }

    public void setSensorGrav(Sensor sensorGrav) {
        this.sensorGrav = sensorGrav;
    }

    public Sensor getSensorMag() {
        return sensorMag;
    }

    public void setSensorMag(Sensor sensorMag) {
        this.sensorMag = sensorMag;
    }

    public int getrHistIdx() {
        return rHistIdx;
    }

    public void setrHistIdx(int rHistIdx) {
        this.rHistIdx = rHistIdx;
    }

    public Matrix getTempR() {
        return tempR;
    }

    public void setTempR(Matrix tempR) {
        this.tempR = tempR;
    }

    public Matrix getFinalR() {
        return finalR;
    }

    public void setFinalR(Matrix finalR) {
        this.finalR = finalR;
    }

    public Matrix getSmoothR() {
        return smoothR;
    }

    public void setSmoothR(Matrix smoothR) {
        this.smoothR = smoothR;
    }

    public Matrix[] getHistR() {
        return histR;
    }

    public void setHistR(Matrix[] histR) {
        this.histR = histR;
    }

    public Matrix getM1() {
        return m1;
    }

    public void setM1(Matrix m1) {
        this.m1 = m1;
    }

    public Matrix getM2() {
        return m2;
    }

    public void setM2(Matrix m2) {
        this.m2 = m2;
    }

    public Matrix getM3() {
        return m3;
    }

    public void setM3(Matrix m3) {
        this.m3 = m3;
    }

    public Matrix getM4() {
        return m4;
    }

    public void setM4(Matrix m4) {
        this.m4 = m4;
    }

    public WakeLock getmWakeLock() {
        return mWakeLock;
    }

    public void setmWakeLock(WakeLock mWakeLock) {
        this.mWakeLock = mWakeLock;
    }

    public int getCompassErrorDisplayed() {
        return compassErrorDisplayed;
    }

    public void setCompassErrorDisplayed(int compassErrorDisplayed) {
        this.compassErrorDisplayed = compassErrorDisplayed;
    }

    public String getZoomLevel() {
        return zoomLevel;
    }

    public void setZoomLevel(String zoomLevel) {
        this.zoomLevel = zoomLevel;
    }

    public TextView getSearchNotificationTxt() {
        return searchNotificationTxt;
    }

    public void setSearchNotificationTxt(TextView searchNotificationTxt) {
        this.searchNotificationTxt = searchNotificationTxt;
    }

    public GridView getMyToolBar() {
        return myToolBar;
    }

    public void setMyToolBar(GridView myToolBar) {
        this.myToolBar = myToolBar;
    }

}

