
/**
 * Created by jongho lee on 2016-11-29.
 */
package ar;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import java.util.Arrays;

import ar.utility.Utility;

public class FloydWarshallPath {
    private static int N=47;
    private static int INF = 54321;

    public int[][] W = new int[N][N];
    public int[][] D = new int[N][N];
    public String [][] M = new String[N][N];
    private JSONArray routes;
    Utility util = new Utility();

    FloydWarshallPath(String data) {
        W = input(data);
        makePath();
    }

    public int[][] input (String dataStr) {
        int[][] LEN = new int[N][N];
        for(int i=0; i<N; i++) {
            Arrays.fill(LEN[i], INF);
            LEN[i][i]=0;
        }

        try {
            JSONObject data = new JSONObject(dataStr);
            routes = data.getJSONArray("routes");
            for(int i=0; i<routes.length() ; i++) {
                JSONObject route = routes.getJSONObject(i);
                int id = route.getInt("id");
                double lat = route.getDouble("latitude");
                double lon = route.getDouble("longitude");
                JSONArray next = route.getJSONArray("next");

                for(int j=0 ; j<next.length() ; j++) {

                        double lat2 = routes.getJSONObject(next.getInt(j)).getDouble("latitude");
                        double lon2 = routes.getJSONObject(next.getInt(j)).getDouble("longitude");
                        int distance = util.getDistanceFromTwoLatLon(lat, lon, lat2, lon2);
                        LEN[id][next.getInt(j)] = LEN[next.getInt(j)][id] = distance;

                }
            }
        }catch(Exception e) {

        }
        for (int m=0; m<LEN[0].length ; m++) {
            String p = "";
            for (int n=0; n<LEN[0].length ; n++) {
                p+= LEN[m][n] + "-";
            }
        }
        return LEN;
    }

    private void makePath() {

        //D를 무한대로 입력
        for(int row[]:D)
            Arrays.fill(row, INF);
        int i,j,k;

        for(i=0;i<N-1;i++) {
            for(j=0;j<N-1;j++) {
                D[i][j]=W[i][j];
                M[i][j] = j+"";
            }
        }

        for(i=0;i<N;i++) {
            for(j=0;j<N;j++) {
                for(k=0;k<N;k++) {
                    if(D[j][k]>D[j][i]+D[i][k]){
                        D[j][k]=D[j][i]+D[i][k];
                        M[j][k]= M[j][i] + "-" + M[i][k];
                    }
                }
            }
        }
        String a = M[44][41];
        Log.d("PathDebug", a);

    }

    public  String floyd_warshall_path(int v1,int v2)
    {
        String P = "45-" + v1 +"-";
//        int min = D[v1][0];
//        int min_index = 0;
//        for (int i =0 ; D[v1].length>i ; i++) {
//            Log.d("MIN(45)", D[v1][i]+"");
//            if(min>D[v1][i] && i!=45) {
//                min = D[v1][i];
//                min_index=i;
//            }
//        }
//
//        int middle = min_index;
//        Log.d("MIX_INDEX", middle+"");

//        P += middle + "-";

        int temp = v2;

        String[] str = M[v1][temp].split("-");
        for(int i=0 ; i<str.length-1 ; i++) {
            P+=str[i] + "-";
        }
        P += v2+"";

        Log.d("Success", P);
        return P;
    }

    public int getDistanceBetweenAnotherRoute(double lat, double lon) {
        int index = 0;
        double minimumDistance =0;
        try {
            for(int i=0; i<routes.length() ; i++) {
                JSONObject route = routes.getJSONObject(i);
                double lat2 = route.getDouble("latitude");
                double lon2 = route.getDouble("longitude");
                double distance = util.getDistanceFromTwoLatLon(lat, lon, lat2, lon2);
                if(i==1) {
                    minimumDistance = distance;
                    index = i;
                } else {
                    if(minimumDistance > distance) {
                        minimumDistance = distance;
                        index = i;
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }


        return index;
    }
}
