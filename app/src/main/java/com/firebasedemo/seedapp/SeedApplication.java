package com.firebasedemo.seedapp;

import android.app.Application;

import com.firebase.client.Firebase;

public class SeedApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        Firebase.setAndroidContext(this);
    }
}
