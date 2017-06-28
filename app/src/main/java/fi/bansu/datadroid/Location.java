package fi.bansu.datadroid;

import android.Manifest;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

class Location {

    protected static final String TAG = Location.class.getSimpleName();
    protected static Location instance;
    public static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;
    public static final int REQUEST_CHECK_SETTINGS = 0x1; // Constant used in the location settings dialog

    protected AppCompatActivity window;
    protected FusedLocationProviderClient mFusedLocationClient; // Provides the entry point to the Fused Location Provider API
    protected android.location.Location mLastLocation;
    protected SettingsClient mSettingsClient; // Provides access to the Location Settings API
    protected LocationRequest mLocationRequest; // Stores parameters for requests to the Fused Location Provider API
    protected LocationSettingsRequest mLocationSettingsRequest;
    protected LocationCallback mLocationCallback;
    protected Boolean mRequestingLocationUpdates = false; // Tracks the status of the location updates request.

    private String mLatitudeLabel;
    private String mLongitudeLabel;
    private TextView mLatitudeText;
    private TextView mLongitudeText;

    protected Location(AppCompatActivity window) {
        this.window = window;
        this.mFusedLocationClient = LocationServices.getFusedLocationProviderClient(window);
        this.mSettingsClient = LocationServices.getSettingsClient(window);
        this.mLatitudeLabel = "Latitude";
        this.mLongitudeLabel = "Longitude";
        this.mLatitudeText = (TextView) window.findViewById(R.id.latitude_text);
        this.mLongitudeText = (TextView) window.findViewById(R.id.longitude_text);
    }

    public static synchronized Location getInstance(AppCompatActivity window) {
        if (instance == null) {
            instance = new Location(window);
        }
        return instance;
    }

    public void setWindow(AppCompatActivity window) {
        this.window = window;

    }

    public boolean hasPermission() {
        int PermissionState = ActivityCompat.checkSelfPermission(window, Manifest.permission.ACCESS_FINE_LOCATION);
        return PermissionState == PackageManager.PERMISSION_GRANTED;
    }

    public void askPermission() {
        boolean shouldProviveRationale = ActivityCompat.shouldShowRequestPermissionRationale(window, Manifest.permission.ACCESS_FINE_LOCATION);
        if (shouldProviveRationale) {
            Log.w(TAG, "TRUE");
            showSnackbar(R.string.permission_rationale, android.R.string.ok,
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            ActivityCompat.requestPermissions(window,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    REQUEST_PERMISSIONS_REQUEST_CODE);
                        }
                    });
        } else {
            Log.w(TAG, "FALSE");
            ActivityCompat.requestPermissions(window,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    public void showSnackbar(final String text) {
        View container = window.findViewById(R.id.main_activity_container);
        if (container != null) {
            Snackbar.make(container, text, Snackbar.LENGTH_LONG).show();
        }
    }

    public void showSnackbar(final int mainTextStringId, final int actionStringId, View.OnClickListener listener) {
        Snackbar.make(window.findViewById(android.R.id.content),
                window.getString(mainTextStringId),
                Snackbar.LENGTH_INDEFINITE)
                .setAction(window.getString(actionStringId), listener).show();
    }

    // NOTE: This will not update after first request. We'll need starting requesting location
    // updates and stopping it when you get the first location.
    public void requestLastLocation() {
        try {
            Log.w(TAG, "getLastLocation");
            mFusedLocationClient.getLastLocation()
                    .addOnCompleteListener(window, new OnCompleteListener<android.location.Location>() {
                        @Override
                        public void onComplete(@NonNull Task<android.location.Location> task) {
                            Log.w(TAG, "onComplete");
                            if (task.isSuccessful() && task.getResult() != null) {
                                Log.w(TAG, "onComplete task success");
                                mLastLocation = task.getResult();

                                mLatitudeText.setText(String.format(Locale.ENGLISH, "%s: %f",
                                        mLatitudeLabel,
                                        mLastLocation.getLatitude()));
                                mLongitudeText.setText(String.format(Locale.ENGLISH, "%s: %f",
                                        mLongitudeLabel,
                                        mLastLocation.getLongitude()));
                            } else {
                                Log.w(TAG, "getLastLocation:exception", task.getException());
                                showSnackbar(window.getString(R.string.no_location_detected));
                            }
                        }
                    });
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    public void requestLocation() {
        if (mRequestingLocationUpdates) {
            Log.i(TAG, "requestLocation - ignoring - already waiting for an update");
            return;
        } else {
            mRequestingLocationUpdates = true;
        }

        // Create location request
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(500);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // Build location settings request
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();

        // Creates a callback for receiving location events
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                Log.w(TAG, locationResult.getLastLocation().toString());
                //mCurrentLocation = locationResult.getLastLocation();
                //mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
                //updateLocationUI();
                if (mLocationCallback != null) {
                    mFusedLocationClient.removeLocationUpdates(mLocationCallback);
                    mLocationCallback = null;
                }
            }
        };

        mSettingsClient.checkLocationSettings(mLocationSettingsRequest)
                .addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {
                    @Override
                    public void onComplete(@NonNull Task<LocationSettingsResponse> task) {
                        mRequestingLocationUpdates = false;
                    }
                })
                .addOnSuccessListener(window, new OnSuccessListener<LocationSettingsResponse>() {
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        Log.i(TAG, "All location settings are satisfied.");

                        //noinspection MissingPermission
                        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());

                        //updateUI();
                    }
                })
                .addOnFailureListener(window, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        int statusCode = ((ApiException) e).getStatusCode();
                        switch (statusCode) {
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                Log.i(TAG, "Location settings are not satisfied. Attempting to upgrade " +
                                        "location settings ");
                                try {
                                    // Show the dialog by calling startResolutionForResult(), and check the
                                    // result in onActivityResult().
                                    ResolvableApiException rae = (ResolvableApiException) e;
                                    rae.startResolutionForResult(window, REQUEST_CHECK_SETTINGS);
                                } catch (IntentSender.SendIntentException sie) {
                                    Log.i(TAG, "PendingIntent unable to execute request.");
                                }
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                String errorMessage = "Location settings are inadequate, and cannot be " +
                                        "fixed here. Fix in Settings.";
                                Log.e(TAG, errorMessage);
                                Toast.makeText(window, errorMessage, Toast.LENGTH_LONG).show();
                                mRequestingLocationUpdates = false;
                        }

                        //updateUI();
                    }
                });
    }
}