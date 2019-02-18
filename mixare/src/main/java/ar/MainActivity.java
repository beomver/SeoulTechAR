package ar;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import ar.data.JSONDataManager;

/**
 * This is the main activity of AR Navigator, that will be opened if AR Navigator is
 * launched through the android.intent.action.MAIN the main tasks of this
 * activity is showing a prompt dialog where the user can decide to launch the
 * plugins, or not to launch the plugins. This class is also able to remember
 * those decisions, so that it can forward directly to the next activity.
 *
 * @author A.Egal
 */
public class MainActivity extends Activity {

    private Context ctx;
    private FileManager mFileManager;
    private final String usePluginsPrefs = "ARNavigatorUsePluginsPrefs";
    private final String usePluginsKey = "usePlugins";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ctx = this;
        setContentView(R.layout.main);

        LinearLayout cam = (LinearLayout) findViewById(R.id.cam);
        LinearLayout map = (LinearLayout) findViewById(R.id.map);

        cam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent cam_intent = new Intent(ctx, ARView.class);
                String[][] path = new String[1][1];
                path[0][0] = null;
                cam_intent.putExtra("ispath", false);
                //cam_intent.putExtra("query", "school");
                cam_intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                cam_intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                startActivity(cam_intent);
            }
        });
        map.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent map_intent = new Intent(ctx, MapActivity.class);
                startActivity(map_intent);
            }
        });
        try {
            mFileManager = new FileManager(this);

            String geonamesData = mFileManager.loadJSON();
            JSONObject geonamesJSONData = new JSONObject(geonamesData);

            String routesData = mFileManager.loadJSON2();
            JSONObject routesJSONData = new JSONObject(routesData);

            JSONDataManager.geonames = geonamesJSONData;

            JSONDataManager.routes = routesJSONData;
            JSONArray results = JSONDataManager.routes.getJSONArray("routes"); // 경로데이터 가져오기
            JSONObject jo;
            int top = 300;
            if (results.length() <= top) {
                top = results.length();
            }
            for (int i = 0; i < top; i++) {
                try {
                    jo = results.getJSONObject(i);
                    ArrayList<Double> coord = new ArrayList<Double>();
                    coord.add(0, jo.getDouble("latitude"));
                    coord.add(1, jo.getDouble("longitude"));
                    JSONDataManager.routeCoord.put(jo.getInt("id"), coord); //key = id , coord = 경도, 위도
                } catch (JSONException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}

//    /**
//     * Shows a dialog
//     */
//    public void showDialog() {
//        final AlertDialog.Builder dialog = new AlertDialog.Builder(this);
//
//        dialog.setTitle(R.string.launch_plugins);
//        dialog.setMessage(R.string.plugin_message);
//
//        final CheckBox checkBox = new CheckBox(ctx);
//        checkBox.setText(R.string.remember_this_decision);
//        dialog.setView(checkBox);
//
////        dialog.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
////            public void onClick(DialogInterface d, int whichButton) {
////                processCheckbox(true, checkBox);
////                Intent pluginLoader = new Intent(ctx, PluginLoaderActivity.class);
////                pluginLoader.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
////                pluginLoader.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
////                Toast.makeText(ctx, "asdfasdf", Toast.LENGTH_LONG).show();
////                startActivity(pluginLoader);
////                finish();
////            }
////        });
////
////        dialog.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
////            public void onClick(DialogInterface d, int whichButton) {
////                processCheckbox(true, checkBox);
////                Intent mixView = new Intent(ctx, ARView.class);
////                mixView.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
////                mixView.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
////                Toast.makeText(ctx, "fffffffff", Toast.LENGTH_LONG).show();
////                startActivity(mixView);
////                finish();
////            }
////        });
//
//        dialog.show();
//    }
//
//    private boolean isDecisionRemembered() {
//        SharedPreferences sharedPreferences = getSharedPreferences(usePluginsPrefs, MODE_PRIVATE);
//        return !sharedPreferences.contains(usePluginsKey);
//    }
//
//    private boolean arePluginsAvailable() {
//        PluginType[] allPluginTypes = PluginType.values();
//        for (PluginType pluginType : allPluginTypes) {
//            PackageManager packageManager = getPackageManager();
//            Intent baseIntent = new Intent(pluginType.getActionName());
//            List<ResolveInfo> list = packageManager.queryIntentServices(baseIntent,
//                    PackageManager.GET_RESOLVED_FILTER);
//
//            if (list.size() > 0) {
//                return true;
//            }
//        }
//        return false;
//    }
//
//    private void processCheckbox(boolean loadplugin, CheckBox checkBox) {
//        if (checkBox.isChecked()) {
//            SharedPreferences sharedPreferences = getSharedPreferences(usePluginsPrefs, MODE_PRIVATE);
//            Editor editor = sharedPreferences.edit();
//            editor.putBoolean(usePluginsKey, loadplugin);
//            editor.commit();
//        }
//    }
//
//    private boolean getRememberedDecision() {
//        SharedPreferences sharedPreferences = getSharedPreferences(usePluginsPrefs, MODE_PRIVATE);
//        return sharedPreferences.getBoolean(usePluginsKey, false);
//    }
//
//}