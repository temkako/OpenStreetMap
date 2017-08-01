package net.avk.openstreetmap;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.DelayedMapListener;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;


public class MainActivity extends AppCompatActivity {

    // initial zoom level and map center coordinates at first time app launch
    double latitude = 48.34, longitude = 31.17;
    int zoomLevel = 5;
    boolean isGpsActivated, isGpsFirstTimeActivated;


    MapView mapView;
    IMapController mapController;
    Marker gpsMarker;
    TextView textViewZoomLevel;
    Button btnGps;

    String MY_PREPS = "MY_PREPS";
    SharedPreferences mySharedPrefs;

    LocationManager locationManager;
    LocationListener locationListener;
    Location lastKnownLocation;
    String gpsProvider;


    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        getPreferences();
        setContentView(R.layout.main_activity);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        //important! set your user agent to prevent getting banned from the osm servers
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        initializeMap();

        btnGps = (Button) findViewById(R.id.gps);
        btnGps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (lastKnownLocation != null) {
                    mapController.animateTo(new GeoPoint(lastKnownLocation));
                }
            }
        });

        btnGps.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (!isGpsActivated ) {
                    startNavigation();
                    Toast.makeText(MainActivity.this, "Gps activated", Toast.LENGTH_SHORT).show();
                } else {
                    stopNavigation();
                    Toast.makeText(MainActivity.this, "Gps deactivated", Toast.LENGTH_SHORT).show();
                    return true;
                }
                return false;
            }
        });

        gpsProvider = LocationManager.GPS_PROVIDER;
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {

            @Override
            public void onLocationChanged(Location location) {
                lastKnownLocation = location;
                GeoPoint geopoint = new GeoPoint(lastKnownLocation);
                gpsMarker.setPosition(geopoint);
                gpsMarker.setRotation(location.getBearing());

                if (!isGpsFirstTimeActivated) {
                    mapController.animateTo(geopoint);
                    isGpsFirstTimeActivated = true;
                }
                mapView.invalidate();
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {
                Toast.makeText(MainActivity.this, "Provider disabled", Toast.LENGTH_SHORT).show();
            }
        };
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        isGpsActivated = savedInstanceState.getBoolean("isGpsActivated");
        lastKnownLocation = savedInstanceState.getParcelable("lastKnownLocation");

    }

    @Override
    public void onResume() {
        super.onResume();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        if (isGpsActivated) {
            startNavigation();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        isGpsFirstTimeActivated = false;
        locationManager.removeUpdates(locationListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        setPreferences();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Configuration.getInstance().save(this, prefs);
    }

    @Override
    public void finish() {
        //Code provided below to avoid "MainActivity has leaked window android.widget.ZoomButtonsController"
        //see https://stackoverflow.com/questions/27254570/android-view-windowleaked-activity-has-leaked-window-android-widget-zoombuttons
        ViewGroup view = (ViewGroup) getWindow().getDecorView();
        view.removeAllViews();
        super.finish();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("isGpsActivated", isGpsActivated);
        outState.putParcelable("lastKnownLocation", lastKnownLocation);
        super.onSaveInstanceState(outState);
    }

    public void initializeMap() {
        mapView = (MapView) findViewById(R.id.map);
        //mapView.setTileSource(TileSourceFactory.HIKEBIKEMAP);
        mapView.setTileSource(TileSourceFactory.OpenTopo);
        //mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setUseDataConnection(true);
        mapView.setMaxZoomLevel(17);
        mapView.setMinZoomLevel(2);
        mapView.setMultiTouchControls(true);
        mapView.setBuiltInZoomControls(true);
        mapController = mapView.getController();
        mapController.setZoom(zoomLevel);
        mapController.setCenter(new GeoPoint(latitude, longitude));

        textViewZoomLevel = (TextView) findViewById(R.id.zoomLevel);
        textViewZoomLevel.setText(String.valueOf(zoomLevel));

        gpsMarker = new Marker(mapView);
        gpsMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_TOP);
        gpsMarker.setIcon(getResources().getDrawable(R.drawable.arrow));

        mapView.setMapListener(new DelayedMapListener(new MapListener() {
            @Override
            public boolean onScroll(ScrollEvent event) {
                return false;
            }

            @Override
            public boolean onZoom(ZoomEvent event) {
                zoomLevel = event.getZoomLevel();
                textViewZoomLevel.setText(String.valueOf(zoomLevel));
                return true;
            }
        }));

    }

    public void setPreferences() {
        mySharedPrefs = getSharedPreferences(MY_PREPS, AppCompatActivity.MODE_PRIVATE);
        SharedPreferences.Editor editor = mySharedPrefs.edit();
        editor.putInt("zoomLevel", mapView.getZoomLevel());
        editor.putFloat("latitude", (float) mapView.getMapCenter().getLatitude());
        editor.putFloat("longitude", (float) mapView.getMapCenter().getLongitude());
        editor.apply();
        editor.commit();
    }

    public void getPreferences() {
        mySharedPrefs = getSharedPreferences(MY_PREPS, AppCompatActivity.MODE_PRIVATE);
        if (mySharedPrefs.getInt("zoomLevel", 0) != 0
                && mySharedPrefs.getFloat("latitude", 0) != 0
                && mySharedPrefs.getFloat("longitude", 0) != 0) {

            zoomLevel = mySharedPrefs.getInt("zoomLevel", zoomLevel);
            latitude = mySharedPrefs.getFloat("latitude", (float) latitude);
            longitude = mySharedPrefs.getFloat("longitude", (float) longitude);
        }
    }

    public void startNavigation() {

        isGpsActivated = true;

        if (locationManager.getLastKnownLocation(gpsProvider) != null) {
            lastKnownLocation = locationManager.getLastKnownLocation(gpsProvider);
        }

        if (lastKnownLocation != null) {
            gpsMarker.setPosition(new GeoPoint(lastKnownLocation));
            gpsMarker.setRotation(lastKnownLocation.getBearing());
        }

        if (!mapView.getOverlays().contains(gpsMarker)) {
            mapView.getOverlays().add(gpsMarker);
        }

        mapView.invalidate();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }


        locationManager.requestLocationUpdates(gpsProvider, 1000, 1, locationListener);
    }

    public void stopNavigation() {
        isGpsActivated = false;
        locationManager.removeUpdates(locationListener);
        if (mapView.getOverlays().contains(gpsMarker)) {
            mapView.getOverlays().remove(gpsMarker);
            mapView.invalidate();
        }
    }
}




