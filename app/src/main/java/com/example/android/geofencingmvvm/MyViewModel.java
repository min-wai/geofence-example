package com.example.android.geofencingmvvm;

import android.app.Application;

import com.google.android.gms.maps.model.LatLng;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

public class MyViewModel extends AndroidViewModel {
    private Repository repository;

    public MyViewModel(@NonNull Application application) {
        super(application);
        repository = Repository.getInstance(application);
    }

    public Repository getRepository() {
        return repository;
    }


    public LiveData<LatLng> destinationLivedata() {
        return repository.getDestinationLivedata();
    }

    public void setDestination(LatLng latLng) {
        repository.setDestinationLatlng(latLng);
    }

    public LiveData<GeofenceStatus> geofenceStatusLiveData() {
        return repository.getGeofenceStatus();
    }

    public void setGeofenceStatus(GeofenceStatus geofenceStatus) {
        repository.setGeofenceStatus(geofenceStatus);
    }
}