package com.example.kristjan.runningwatchapp;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v4.app.NotificationCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.text.DecimalFormat;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, LocationListener {
    private final static String TAG = "MainActivity";
    DecimalFormat decimalFormat;

    private GoogleMap mGoogleMap;
    private Menu mOptionsMenu;

    private LocationManager locationManager;
    private String provider;

    private Location startLocation;
    private Location wayPoint;
    private Location resetLocation;
    private Location locationPrevious;

    private int markerCount = 0;
    private double totalDistance = 0;
    //private double totalLineDistance = 0;
    private double wayPointDistance = 0;
    private double wayPointLineDistance = 0;
    private double cresetDistance = 0;
    private double cresetLineDistance = 0;

    private Polyline mPolyline;
    private PolylineOptions mPolylineOptions;

    private TextView textViewWPCount;
    private TextView textViewSpeed;
    private TextView textViewTotalDistance;
    private TextView textViewCResetDistance;
    private TextView textViewTotalLine;
    private TextView textViewCResetLine;
    private TextView textViewWpDistance;
    private TextView textViewWpLine;

    private NotificationManager mNotificationManager;
    private BroadcastReceiver mBroadcastReceiver;

    RemoteViews remoteView;
    NotificationCompat.Builder mBuilder;

    /*
    PowerManager.WakeLock wakeLock;
    private boolean screenOff = false;
    */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.gmap);
        mapFragment.getMapAsync(this);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        // get the location provider (GPS/CEL-towers, WIFI)
        provider = locationManager.getBestProvider(criteria, false);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "No COARSE location permissions!");
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "No FINE location permissions!");
        }

        locationPrevious = startLocation = locationManager.getLastKnownLocation(provider);
        wayPoint = null;
        resetLocation = null;

        textViewWPCount = (TextView) findViewById(R.id.textview_wpcount);
        textViewSpeed= (TextView) findViewById(R.id.textview_speed);
        textViewTotalDistance = (TextView) findViewById(R.id.textview_total_distance);
        textViewTotalLine = (TextView) findViewById(R.id.textview_total_line);
        textViewCResetDistance = (TextView) findViewById(R.id.textview_creset_distance);
        textViewCResetLine = (TextView) findViewById(R.id.textview_creset_line);
        textViewWpDistance = (TextView) findViewById(R.id.textview_wp_distance);
        textViewWpLine = (TextView) findViewById(R.id.textview_wp_line);

        decimalFormat = new DecimalFormat("#.##");

        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals("notification-broadcast-addwaypoint")){
                    addNewWayPoint();
                }
                if (intent.getAction().equals("notification-broadcast-resettripmeter")){
                    cResetApp();
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("notification-broadcast-addwaypoint");
        intentFilter.addAction("notification-broadcast-resettripmeter");
        registerReceiver(mBroadcastReceiver, intentFilter);

        initNotification();

        /*
        // ?????
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "MyWakelockTag");
        wakeLock.acquire();


        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                    screenOff = false;
                    notifyScreen();

                } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                    screenOff = true;
                }
            }
        }, intentFilter);
        */
    }

    private void initNotification() {
        // get the view layout
        remoteView = new RemoteViews(
                getPackageName(), R.layout.custom_notification);

        // define intents
        PendingIntent pIntentAddWaypoint = PendingIntent.getBroadcast(
                this,
                0,
                new Intent("notification-broadcast-addwaypoint"),
                0
        );

        PendingIntent pIntentResetTripmeter = PendingIntent.getBroadcast(
                this,
                0,
                new Intent("notification-broadcast-resettripmeter"),
                0
        );

        // bring back already running activity
        // in manifest set android:launchMode="singleTop"
        PendingIntent pIntentOpenActivity = PendingIntent.getActivity(
                this,
                0,
                new Intent(this, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        // attach events
        remoteView.setOnClickPendingIntent(R.id.buttonAddWayPoint, pIntentAddWaypoint);
        remoteView.setOnClickPendingIntent(R.id.buttonResetTripmeter, pIntentResetTripmeter);
        remoteView.setOnClickPendingIntent(R.id.buttonOpenActivity, pIntentOpenActivity);

        // build notification
        mBuilder =
                new NotificationCompat.Builder(this)
                        .setContent(remoteView)
                        .setSmallIcon(R.drawable.ic_my_location_white_48dp);

        // notify
        mNotificationManager.notify(0, mBuilder.build());
    }

    public void updateNotification() {
        if (mBuilder == null) return;
        remoteView.setTextViewText(R.id.textViewTripmeterMetrics, decimalFormat.format(totalDistance));
        remoteView.setTextViewText(R.id.textViewWayPointMetrics, decimalFormat.format(wayPointDistance));
        mNotificationManager.notify(4, mBuilder.build());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        mOptionsMenu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_mylocation:
                item.setChecked(!item.isChecked());
                updateMyLocation();
                return true;

            case R.id.menu_trackposition:
                item.setChecked(!item.isChecked());
                updateTrackPosition();
                return true;

            case R.id.menu_keepmapcentered:
                item.setChecked(!item.isChecked());
                return true;

            case R.id.menu_maptype_hybrid:
            case R.id.menu_maptype_none:
            case R.id.menu_maptype_normal:
            case R.id.menu_maptype_satellite:
            case R.id.menu_maptype_terrain:

                item.setChecked(true);
                changeMapType();
                return true;

            case R.id.menu_map_zoom_10:
            case R.id.menu_map_zoom_15:
            case R.id.menu_map_zoom_20:
            case R.id.menu_map_zoom_in:
            case R.id.menu_map_zoom_out:
            case R.id.menu_map_zoom_fittrack:
                updateMapZoomLevel(item.getItemId());
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void updateMyLocation() {
        if (mOptionsMenu.findItem(R.id.menu_mylocation).isChecked()) {
            mGoogleMap.setMyLocationEnabled(true);

            return;
        }

        mGoogleMap.setMyLocationEnabled(false);
    }

    private void updateTrackPosition(){
        if (!checkReady()) {
            return;
        }

        if (mOptionsMenu.findItem(R.id.menu_trackposition).isChecked()) {
            mPolylineOptions = new PolylineOptions().width(5).color(Color.RED);
            mPolyline = mGoogleMap.addPolyline(mPolylineOptions);
        }
    }

    private void updateMapZoomLevel(int itemId){
        if (!checkReady()) {
            return;
        }

        switch (itemId) {
            case R.id.menu_map_zoom_10:
                mGoogleMap.moveCamera(CameraUpdateFactory.zoomTo(10));
                break;

            case R.id.menu_map_zoom_15:
                mGoogleMap.moveCamera(CameraUpdateFactory.zoomTo(15));
                break;

            case R.id.menu_map_zoom_20:
                mGoogleMap.moveCamera(CameraUpdateFactory.zoomTo(20));
                break;

            case R.id.menu_map_zoom_in:
                mGoogleMap.moveCamera(CameraUpdateFactory.zoomIn());
                break;

            case R.id.menu_map_zoom_out:
                mGoogleMap.moveCamera(CameraUpdateFactory.zoomOut());
                break;

            case R.id.menu_map_zoom_fittrack:
                updateMapZoomFitTrack();
                break;
        }
    }

    private void updateMapZoomFitTrack(){
        if (mPolyline==null){
            return;
        }

        List<LatLng> points = mPolyline.getPoints();

        if (points.size() <= 1){
            return;
        }

        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        for (LatLng point : points) {
            builder.include(point);
        }

        LatLngBounds bounds = builder.build();
        int padding = 0; // offset from edges of the map in pixels
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
    }

    private boolean checkReady() {
        if (mGoogleMap == null) {
            Toast.makeText(this, R.string.map_not_ready, Toast.LENGTH_SHORT).show();

            return false;
        }
        return true;
    }

    private void changeMapType(){
        if (mGoogleMap == null){
            return;
        }

        if (mOptionsMenu.findItem(R.id.menu_maptype_hybrid).isChecked()){
            mGoogleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

        } else if (mOptionsMenu.findItem(R.id.menu_maptype_none).isChecked()){
            mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NONE);

        } else if (mOptionsMenu.findItem(R.id.menu_maptype_normal).isChecked()){
            mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        } else if (mOptionsMenu.findItem(R.id.menu_maptype_satellite).isChecked()){
            mGoogleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);

        } else if (mOptionsMenu.findItem(R.id.menu_maptype_terrain).isChecked()){
            mGoogleMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
        }
        //Log.d(TAG,"Map type changed!");
    }

    public void buttonAddWayPointClicked(View view){
        addNewWayPoint();
    }

    private void addNewWayPoint() {
        if (locationPrevious==null){
            return;
        }

        wayPoint = locationPrevious;
        wayPointDistance = wayPointLineDistance = 0;
        textViewWpDistance.setText(decimalFormat.format(wayPointDistance));
        textViewWpLine.setText(decimalFormat.format(wayPointLineDistance));

        markerCount++;

        mGoogleMap.addMarker(new MarkerOptions().position(new LatLng(locationPrevious.getLatitude(), locationPrevious.getLongitude())).title(Integer.toString(markerCount)));

        textViewWPCount.setText(Integer.toString(markerCount));
    }

    public void buttonCResetClicked(View view){
        cResetApp();
    }

    private void cResetApp() {
        resetLocation = locationPrevious;
        cresetDistance = cresetLineDistance = 0;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;

        // set zoom level to 15 - street
        mGoogleMap.moveCamera(CameraUpdateFactory.zoomTo(17));

        // if there was initial location received, move map to it
        if (locationPrevious != null) {
            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(locationPrevious.getLatitude(), locationPrevious.getLongitude())));
        }
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
    }

    @Override
    public void onLocationChanged(Location location) {
        if (mGoogleMap == null) return;

        LatLng newPoint = new LatLng(location.getLatitude(), location.getLongitude());

        if (mOptionsMenu.findItem(R.id.menu_keepmapcentered).isChecked() || locationPrevious == null) {
            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(newPoint));
        }

        if (mOptionsMenu.findItem(R.id.menu_trackposition).isChecked()) {
            List<LatLng> points = mPolyline.getPoints();
            points.add(newPoint);
            mPolyline.setPoints(points);
        }

        double distance = (double) locationPrevious.distanceTo(location) / 1000; // in kilometers

        long timeBetweenLocations = (location.getTime() - locationPrevious.getTime()) / 1000; // in seconds
        double secondsPerKM = timeBetweenLocations / distance;
        int minutesOfSpeed = (int) secondsPerKM / 60;
        int secondsOfSpeed = (int) secondsPerKM % 60;

        String speed = minutesOfSpeed + ":" + secondsOfSpeed;
        totalDistance += distance;
        double totalLineDistance = (double) startLocation.distanceTo(location) / 1000;

        if (wayPoint != null) {
            wayPointDistance += distance;
            wayPointLineDistance = (double) wayPoint.distanceTo(location) / 1000; // in kilometers

            textViewWpDistance.setText(decimalFormat.format(wayPointDistance));
            textViewWpLine.setText(decimalFormat.format(wayPointLineDistance));
        }

        if (resetLocation != null) {
            cresetDistance += distance;
            cresetLineDistance = (double) resetLocation.distanceTo(location) / 1000; // in kilometers

            textViewCResetDistance.setText(decimalFormat.format(cresetDistance));
            textViewCResetLine.setText(decimalFormat.format(cresetLineDistance));
        }

        textViewSpeed.setText(speed);
        textViewTotalDistance.setText(decimalFormat.format(totalDistance));
        textViewTotalLine.setText(decimalFormat.format(totalLineDistance));

        updateNotification();

        locationPrevious = location;
    }



    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    protected void onResume(){
        super.onResume();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "No COARSE location permissions!");
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "No FINE location permissions!");
        }

        if (locationManager!=null){
            locationManager.requestLocationUpdates(provider, 500, 1, this);
        }
    }

    @Override
    protected void onPause(){
        super.onPause();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "No COARSE location permissions!");
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "No FINE location permissions!");
        }

        if (locationManager!=null){
            locationManager.removeUpdates(this);
        }
    }




    /*
    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }


    @Override
    protected void onDestroy() {
        wakeLock.release();
        super.onDestroy();
    }
    */
}
