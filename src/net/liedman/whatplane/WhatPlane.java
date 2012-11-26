package net.liedman.whatplane;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import net.liedman.whatplane.filter.Filter;
import net.liedman.whatplane.filter.LowPassFilter;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.TextView;

public class WhatPlane extends Activity implements LocationListener, SensorEventListener {
    private static final int TWO_MINUTES = 1000 * 60 * 2;
    private static final int DIALOG_NO_LOCATION = 0;
    private static final String TAG = "WhatPlane";
    private DecimalFormat coordFormat = new DecimalFormat("##0.0000");
    private Updater updater = new Updater();
    private Handler handler = new Handler();
    private DateFormat dateFormat;
    private DateFormat timeFormat;
    private PlaneListAdapter planeListAdapter;
    private SensorManager sensorManager;
    private LocationManager locationManager;
    private String locationProviderName;
    private Sensor sensor;
    private Filter compassFilter = new LowPassFilter(3);
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_PROGRESS);
        setContentView(R.layout.main);

        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (locationManager == null) {
            showDialog(DIALOG_NO_LOCATION);
            return;
        }
        
        locationProviderName = locationManager.getBestProvider(new Criteria(), true);
        LocationProvider provider = locationManager.getProvider(locationProviderName);
        if (provider == null) {
            showDialog(DIALOG_NO_LOCATION);
            return;
        }
        
        dateFormat = android.text.format.DateFormat.getDateFormat(getApplicationContext());
        timeFormat = android.text.format.DateFormat.getTimeFormat(getApplicationContext());        
        
        new Thread(updater).start();

        Button updateButton = (Button)findViewById(R.id.UpdateButton);
        updateButton.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                updater.update();
            }
        });
        
        Location lastKnownLocation = locationManager.getLastKnownLocation(locationProviderName);
        updater.updateLocation(lastKnownLocation);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 60 * 1000, 0, this);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 60 * 1000, 0, this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        locationManager.removeUpdates(this);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog;
        switch (id) {
        case DIALOG_NO_LOCATION:
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Could not get a location from your device.")
                .setPositiveButton("Exit", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface arg0, int arg1) {
                        finish();
                    }
                });
            dialog = builder.create();
            break;
        default:
            dialog = null;
        }
        
        return dialog;
    }

    public void onLocationChanged(Location loc) {
        updater.updateLocation(loc);
    }

    public void onProviderDisabled(String arg0) {
        // TODO Auto-generated method stub
        
    }

    public void onProviderEnabled(String arg0) {
        // TODO Auto-generated method stub
        
    }

    public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
        // TODO Auto-generated method stub
        
    }

    public void onAccuracyChanged(Sensor arg0, int arg1) {
        // TODO Auto-generated method stub
        
    }

    public void onSensorChanged(SensorEvent se) {
        compassFilter.feed(se.values[0]);
        if (planeListAdapter != null) {
            planeListAdapter.setHeading(compassFilter.getValue());
        }
    }

    private String formatLocation(Location loc) {
        double latitude = loc.getLatitude();
        double longitude = loc.getLongitude();
        Date now = new Date(loc.getTime());
        return (latitude >= 0f ? "N" : "S") + coordFormat.format(latitude) 
                + ", " +
                (longitude >= 0f ? "E" : "W")+ coordFormat.format(longitude)
                + ", "
                + timeFormat.format(now) + " " 
                + dateFormat.format(now);
    } 
    
    /** Determines whether one Location reading is better than the current Location fix
      * @param location  The new Location that you want to evaluate
      * @param currentBestLocation  The current Location fix, to which you want to compare the new one
      */
    protected boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
        // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    /** Checks whether two providers are the same */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
          return provider2 == null;
        }
        return provider1.equals(provider2);
    }
    
    private class Updater implements Runnable {
        private Location location;
        private boolean quit = false;
        
        public void updateLocation(Location location) {
            synchronized (this) {
                if (isBetterLocation(location, this.location)) {
                    this.location = location;
                    notify();
                }
            }
        }
        
        public void update() {
            synchronized (this) {
                notify();
            }
        }
        
        public void run() {
            try {
                while (!quit) {
                    Location currentLocation = null;
                    synchronized (this) {
                        wait();
                        currentLocation = location;
                    }
                    
                    if (location != null) {                    
                        updatePlaneList(currentLocation);
                    } else {
                        handler.post(new Runnable() {                            
                            public void run() {
                                Window window = getWindow();
                                window.setFeatureInt(Window.FEATURE_PROGRESS, Window.PROGRESS_VISIBILITY_OFF);
                            }
                        });
                    }
                }
            } catch (InterruptedException e) {
                // Do nothing, just exit
            }
        }

        private void updatePlaneList(final Location location) {
            handler.post(new Runnable() {
                public void run() {
                    Window window = getWindow();
                    window.setFeatureInt(Window.FEATURE_PROGRESS, Window.PROGRESS_INDETERMINATE_ON);
                    window.setFeatureInt(Window.FEATURE_PROGRESS, Window.PROGRESS_VISIBILITY_ON);
                    Button updateButton = (Button)findViewById(R.id.UpdateButton);
                    updateButton.setEnabled(false);
                }
            });
            
            HttpClient httpClient = new DefaultHttpClient(getHttpParams());
            
            try {
                URI url = new URI("http://www.liedman.net/whatplane/json/closest?lat=" 
                        + location.getLatitude() + "&lon=" + location.getLongitude());
                String response = httpClient.execute(new HttpGet(url), new BasicResponseHandler());
                JSONArray planes = new JSONArray(response);
                final String[] groups = new String[planes.length()];
                final String[][] children = new String[planes.length()][];
                final float bearings[] = new float[planes.length()];
                for (int i = 0; i < planes.length(); i++) {
                    JSONObject plane = planes.getJSONObject(i);
                    String callsign = plane.getString("callsign");
                    String shortInfo = (callsign != null && callsign.length() > 0 ? callsign : "[unknown]")
                        + " - " + plane.getString("course_octant") + " "
                        + Math.round(Float.parseFloat(plane.getString("horizontal_distance"))) + " km";
                    ArrayList<String> info = new ArrayList<String>();
                    addIfNotNullOrEmpty(info, "Airline", plane, "airline");
                    addIfNotNullOrEmpty(info, "From", plane, "from");
                    addIfNotNullOrEmpty(info, "To", plane, "to");
                    addIfNotNullOrEmpty(info, "Model", plane, "model");                            
                    addIfNotNullOrEmpty(info, "Altitude", plane, "altitude", " ft");
                    addIfNotNullOrEmpty(info, "Speed", plane, "speed", " km/h");

                    String heading = plane.getString("heading");
                    if (heading != null && heading.length() > 0) {
                        info.add("Heading: " + plane.getString("heading_octant") + " (" + heading + "°)");
                    }
                    
                    groups[i] = shortInfo;
                    children[i] = info.toArray(new String[0]);
                    bearings[i] = (float)plane.getDouble("course");
                }
                                
                handler.post(new Runnable() {
                    public void run() {
                        ExpandableListView planeList = (ExpandableListView) findViewById(R.id.PlaneList);
                        if (planeListAdapter == null) {
                            planeListAdapter = new PlaneListAdapter(WhatPlane.this, groups, children, bearings);
                            planeList.setAdapter(planeListAdapter);
                        } else {
                            planeListAdapter.setData(groups, children, bearings);
                        }

                        TextView textView = (TextView) findViewById(R.id.CoordinateLabel);
                        textView.setText(formatLocation(location));
                    }
                });
                
                
            } catch (URISyntaxException e) {
                Log.e(TAG, "Error getting aircraft info", e);
            } catch (IOException e) {
                Log.e(TAG, "Error getting aircraft info", e);
            } catch (JSONException e) {
                Log.e(TAG, "Error getting aircraft info", e);
            } catch (RuntimeException e) {
                Log.e(TAG, "Error getting aircraft info", e);
            } finally {                
                handler.post(new Runnable() {                    
                    public void run() {
                        Window window = getWindow();
                        window.setFeatureInt(Window.FEATURE_PROGRESS, Window.PROGRESS_VISIBILITY_OFF);
                        
                        Button updateButton = (Button)findViewById(R.id.UpdateButton);
                        updateButton.setEnabled(true);
                    }
                });
            }
        }

        private HttpParams getHttpParams() {
            HttpParams params = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(params, 15000);
            HttpConnectionParams.setSoTimeout(params, 25000);
            return params;
        }

        private void addIfNotNullOrEmpty(List<String> list, String title, JSONObject object, String key, String unit) {
            String value;
            try {
                value = object.getString(key);
                if (value != null && value.length() > 0) {
                    list.add(title + ": " + value + (unit != null ? unit : ""));
                }
            } catch (JSONException e) {
                Log.i("WhatPlane", "Could not find attribute \"" + key + "\".");
            }
        }

        private void addIfNotNullOrEmpty(List<String> list, String title, JSONObject object, String key) {
            addIfNotNullOrEmpty(list, title, object, key, null);
        }
    }
}