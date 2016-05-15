package com.example.kristjan.runningwatchapp;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    private final static String TAG = "MainActivity";
    private GoogleMap mGoogleMap;
    private Menu mOptionsMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.gmap);
        mapFragment.getMapAsync(this);
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
            case R.id.menu_maptype_hybrid:
            case R.id.menu_maptype_none:
            case R.id.menu_maptype_normal:
            case R.id.menu_maptype_satellite:
            case R.id.menu_maptype_terrain:

                item.setChecked(true);
                changeMapType();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
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

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
    }
}
