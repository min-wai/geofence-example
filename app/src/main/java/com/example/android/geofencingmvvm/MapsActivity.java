package com.example.android.geofencingmvvm;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener, GoogleMap.OnInfoWindowLongClickListener {

    private GoogleMap mMap;
    private Marker marker;
    private MyViewModel myViewModel;
    private GeofencingClient geofencingClient;
    private PendingIntent mGeofencePendingIntent;
    private Circle circle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        geofencingClient = LocationServices.getGeofencingClient(this);

        myViewModel = ViewModelProviders.of(this).get(MyViewModel.class);
    }

    private void observeDestination() {
        myViewModel.destinationLivedata().observe(this, new Observer<LatLng>() {
            @Override
            public void onChanged(LatLng latLng) {
                // Destination null means no destination at all, so remove marker and circle respectively
                if (latLng == null) {
                    if (marker != null) {
                        marker.remove();
                        marker = null;
                    }
                    if (circle != null) {
                        circle.remove();
                        circle = null;
                    }
                    return;
                }
                // Initialize marker and circle if not done yet, otherwise use the existing marker and circle
                if (marker == null) {
                    marker = mMap.addMarker(new MarkerOptions()
                            .title(Common.MARKER_TITLE)
                            .snippet(Common.MARKER_SNIPPET)
                            .position(latLng));
                } else {
                    marker.setPosition(latLng);
                }
                // Programmatically show the info window so the user sees
                marker.showInfoWindow();

                if (circle == null) {
                    circle = mMap.addCircle(new CircleOptions()
                            .center(latLng)
                            .radius(Common.RADIUS_IN_METER)
                            .fillColor(0x40ff0000)
                            .strokeColor(Color.TRANSPARENT)
                            .strokeWidth(2));
                } else {
                    circle.setCenter(latLng);
                }
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void addGeofence(LatLng destinationLatLng) {
        geofencingClient.addGeofences(getGeofencingRequest(destinationLatLng), getGeofencePendingIntent())
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(MapsActivity.this, "Geofence added!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MapsActivity.this, GeofenceErrorMessages.getErrorString(MapsActivity.this, task.getException()), Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MapsActivity.this, GeofenceErrorMessages.getErrorString(MapsActivity.this, e), Toast.LENGTH_SHORT).show();
                    }
                });
        myViewModel.setGeofenceStatus(GeofenceStatus.NONE);
    }

    private void removeGeofence() {
        geofencingClient.removeGeofences(getGeofencePendingIntent())
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Toast.makeText(MapsActivity.this, "Geofence cleared!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MapsActivity.this, GeofenceErrorMessages.getErrorString(MapsActivity.this, e), Toast.LENGTH_SHORT).show();
                    }
                });
        myViewModel.setGeofenceStatus(GeofenceStatus.NONE);
    }

    private GeofencingRequest getGeofencingRequest(LatLng destinationLatLng) {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();

        // The INITIAL_TRIGGER_ENTER flag indicates that geofencing service should trigger a
        // GEOFENCE_TRANSITION_ENTER notification when the geofence is added and if the device
        // is already inside that geofence.
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofence(new Geofence.Builder()
                .setRequestId(marker.getTitle())
                .setCircularRegion(destinationLatLng.latitude, destinationLatLng.longitude, Common.RADIUS_IN_METER)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .build());

        return builder.build();
    }

    private PendingIntent getGeofencePendingIntent() {
        // Reuse the PendingIntent if we already have it.
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }

        Intent intent = new Intent(this, GeofenceBroadcastReceiver.class);
        // We use FLAG_UPDATE_CURRENT so that we gdestinationLatLng = latLng;et the same pending intent back when calling
        // addGeofences() and removeGeofences().
        mGeofencePendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return mGeofencePendingIntent;
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
    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setMyLocationEnabled(true);
        mMap.setOnMapClickListener(this);
        mMap.setOnInfoWindowLongClickListener(this);

        LocationServices.getFusedLocationProviderClient(this)
                .getLastLocation()
                .addOnCompleteListener(new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            Location location = task.getResult();
                            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                        }
                    }
                });

        // Stuff to do after map is loaded and map is clicked for the first time.
        observeDestination();
    }

    @Override
    public void onMapClick(LatLng latLng) {
        // Set a new destination and geofence
        myViewModel.setDestination(latLng);
        addGeofence(latLng);
    }

    @Override
    public void onInfoWindowLongClick(Marker marker) {
        // Remove the marker and geofence, removing marker just means setting destination to null, observer will take care of the rest
        myViewModel.setDestination(null);
        removeGeofence();
    }
}
