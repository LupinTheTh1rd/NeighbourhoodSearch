package com.example.hp.map;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Telephony;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.tasks.Task;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private GoogleMap mMap;
    private GoogleApiClient client;
    private LocationRequest locationrequest;
    private Location lastlocation;
    private Marker currentlocationmarker;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    public static final int REQUEST_LOCATION_CODE = 99;

    //    LocationManager locationManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkLocationPermission();
        }
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

//        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        PlaceAutocompleteFragment placeAutoComplete = (PlaceAutocompleteFragment) getFragmentManager().findFragmentById(R.id.place_autocomplete);
        placeAutoComplete.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                String name = place.getName().toString();
                try {
                    goToLocation(name);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(Status status) {
                Toast.makeText(MapsActivity.this, "Error!!", Toast.LENGTH_SHORT).show();
            }
        });
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_LOCATION_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        if (client == null) {
                            buildGoogleApiClient();
                        }
                        mMap.setMyLocationEnabled(true);
                    }
                } else {
                    Toast.makeText(this, "Permission Denied!", Toast.LENGTH_LONG).show();
                }
                return;
        }

    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            buildGoogleApiClient();
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
        }
        LatLng college = new LatLng(28.5036, 77.0498);
        mMap.addMarker(new MarkerOptions().position(college)).setTitle("Marker set on NorthCap");
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(college, 13.0f));

    }

    protected synchronized void buildGoogleApiClient() {
        client = new GoogleApiClient.Builder(this).addConnectionCallbacks(this).addOnConnectionFailedListener(this).addApi(LocationServices.API).build();
        client.connect();
    }

    @Override
    public void onLocationChanged(Location location) {
        lastlocation = location;

        if (currentlocationmarker != null) {
            currentlocationmarker.remove();
        }

        LatLng latlng = new LatLng(location.getLatitude(), location.getLongitude());
        MarkerOptions markeroptions = new MarkerOptions();
        markeroptions.position(latlng);
        markeroptions.title("current location");
        markeroptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
        currentlocationmarker = mMap.addMarker(markeroptions);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latlng));
        mMap.animateCamera(CameraUpdateFactory.zoomBy(10));
        if (client != null) {
            //LocationServices.FusedLocationApi.removeLocationUpdates(client,this);
        }
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        locationrequest = new LocationRequest();
        locationrequest.setInterval(1000);
        locationrequest.setFastestInterval(1000);
        locationrequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
            Task location = mFusedLocationProviderClient.getLastLocation();
            Location currentLocation = (Location) location.getResult();
            onLocationChanged(currentLocation);
        }
        //LocationServices.FusedLocationApi.requestLocationUpdates(client, locationrequest, this);
    }

    public boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_CODE);
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_CODE);
            }
            return false;
        }
        return true;
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }


    public void showLocation(View view) {
        LocationManager locationManager;
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
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
        if(locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, new android.location.LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    LatLng current_loc = new LatLng(latitude,longitude);
                    Geocoder geocoder = new Geocoder(getApplicationContext());
                    try {
                        List<Address> addressList = geocoder.getFromLocation(latitude,longitude,1);
                        String loc_name = addressList.get(0).getLocality() + ",";
                        loc_name += addressList.get(0).getCountryName();
                        mMap.addMarker(new MarkerOptions().position(current_loc).title(loc_name));
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(current_loc,12.0f));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }

                @Override
                public void onStatusChanged(String s, int i, Bundle bundle) {

                }

                @Override
                public void onProviderEnabled(String s) {

                }

                @Override
                public void onProviderDisabled(String s) {

                }
            });
        }
        else if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, new android.location.LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    LatLng current_loc = new LatLng(latitude,longitude);
                    Geocoder geocoder = new Geocoder(getApplicationContext());
                    try {
                        List<Address> addressList = geocoder.getFromLocation(latitude,longitude,1);
                        String loc_name = addressList.get(0).getLocality() + ",";
                        loc_name += addressList.get(0).getCountryName();
                        mMap.addMarker(new MarkerOptions().position(current_loc).title(loc_name));
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(current_loc,12.0f));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onStatusChanged(String s, int i, Bundle bundle) {

                }

                @Override
                public void onProviderEnabled(String s) {

                }

                @Override
                public void onProviderDisabled(String s) {

                }
            });
        }
    }

//    public void searchLocation(View view) throws IOException {
//        EditText locationSearch = findViewById(R.id.search);
//        String location = locationSearch.getText().toString().trim();
//        List<Address> addressList = null;
//
//        if(!location.equals("")) {
//            Geocoder geocoder = new Geocoder(this);
//            addressList = geocoder.getFromLocationName(location, 1);
//            double latitude = addressList.get(0).getLatitude();
//            double longitude = addressList.get(0).getLongitude();
//            LatLng searchedLocation = new LatLng(latitude, longitude);
//            String loc_name = addressList.get(0).getLocality() + "," + addressList.get(0).getCountryName();
//            mMap.addMarker(new MarkerOptions().position(searchedLocation).title(loc_name));
//            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(searchedLocation, 12.0f));
//        }
//    }

    private void goToLocation(String name) throws IOException {
        List<Address> addressList = null;

        if(!name.equals("")) {
            Geocoder geocoder = new Geocoder(this);
            addressList = geocoder.getFromLocationName(name,1);
            double latitude = addressList.get(0).getLatitude();
            double longitude = addressList.get(0).getLongitude();
            LatLng searchedLocation = new LatLng(latitude,longitude);
            mMap.addMarker(new MarkerOptions().position(searchedLocation).title(name));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(searchedLocation,12.0f));
        }
    }

}
