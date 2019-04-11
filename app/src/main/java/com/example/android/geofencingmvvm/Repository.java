package com.example.android.geofencingmvvm;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

enum GeofenceStatus {
    NONE,
    ADD,
    REMOVE
}

public class Repository {
    private static Repository ourInstance;

    private static final String TAG = "GeofenceTrnasitionsLog";

    private SharedPreferences sharedPreferences;

    private MutableLiveData<LatLng> destinationLivedata = new MutableLiveData<>();

    private MutableLiveData<GeofenceStatus> geofenceRequest = new MutableLiveData<>();

    public static Repository getInstance(Application application) {
        if (ourInstance == null) {
            ourInstance = new Repository(application);
        }
        return ourInstance;
    }

    private Repository(Application application) {
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(application);
        destinationLivedata.setValue(getSavedDestination());
        geofenceRequest.setValue(getSavedGeofenceStatus());
    }

    public LiveData<LatLng> getDestinationLivedata() {
        return destinationLivedata;
    }

    public LiveData<GeofenceStatus> getGeofenceStatus() {
        return geofenceRequest;
    }

    public void setDestinationLatlng(LatLng latLng) {
        String destination = latLng == null ? null : String.format("%f, %f", latLng.latitude, latLng.longitude);

        Log.d(TAG, "Set/Save destination: " + destination);
        destinationLivedata.setValue(latLng);
        sharedPreferences.edit()
                .putString(Common.DESTINATION_KEY, destination)
                .apply();
    }

    public void setGeofenceStatus(GeofenceStatus status) {
        Log.d(TAG, "Set/Save Geofence status: " + status.name());
        sharedPreferences.edit()
                .putInt(Common.GEOFENCE_STATUS, status.ordinal())
                .apply();
        geofenceRequest.setValue(status);
    }

    private GeofenceStatus getSavedGeofenceStatus() {
        final int ordinal = sharedPreferences
                .getInt(Common.GEOFENCE_STATUS, GeofenceStatus.NONE.ordinal());
        Log.d(TAG, "Get saved Geofence status: " + getGeofenceStatusByOrdinal(ordinal));
        return getGeofenceStatusByOrdinal(ordinal);
    }

    private GeofenceStatus getGeofenceStatusByOrdinal(int ordinal) {
        if (ordinal == GeofenceStatus.ADD.ordinal()) {
            return GeofenceStatus.ADD;
        } else if (ordinal == GeofenceStatus.REMOVE.ordinal()) {
            return GeofenceStatus.REMOVE;
        } else {
            return GeofenceStatus.NONE;
        }
    }

    private LatLng getSavedDestination() {
        String latLngString = sharedPreferences
                .getString(Common.DESTINATION_KEY, null);

        Log.d(TAG, "Get saved destination: " + latLngString);

        if (latLngString == null) {
            return null;
        } else {
            String[] latlngArray = latLngString.split(", ");
            return new LatLng(
                    Double.parseDouble(latlngArray[0]),
                    Double.parseDouble(latlngArray[1]));
        }
    }
}
