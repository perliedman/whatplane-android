package net.liedman.whatplane;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
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
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;

public class WhatPlane extends Activity implements LocationListener {
    private static final int DIALOG_NO_LOCATION = 0;
    private DecimalFormat coordFormat = new DecimalFormat("##0.0000");
    private Updater updater = new Updater();
    private Handler handler = new Handler();
    private DateFormat dateFormat = DateFormat.getTimeInstance(DateFormat.SHORT);
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_PROGRESS);
        setContentView(R.layout.main);
        
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (locationManager == null) {
            showDialog(DIALOG_NO_LOCATION);
            return;
        }
        
        String providerName = locationManager.getBestProvider(new Criteria(), true);
        LocationProvider provider = locationManager.getProvider(providerName);
        if (provider == null) {
            showDialog(DIALOG_NO_LOCATION);
            return;
        }
        
        new Thread(updater).start();

        Button updateButton = (Button)findViewById(R.id.UpdateButton);
        updateButton.setEnabled(false);
        updateButton.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                Window window = getWindow();
                window.setFeatureInt(Window.FEATURE_PROGRESS, Window.PROGRESS_INDETERMINATE_ON);
                window.setFeatureInt(Window.FEATURE_PROGRESS, Window.PROGRESS_VISIBILITY_ON);
                updater.update();
            }
        });
        
        updater.updateLocation(locationManager.getLastKnownLocation(providerName));
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 60 * 1000, 0f, this);
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
        TextView textView = (TextView) findViewById(R.id.CoordinateLabel);
        textView.setText(formatLocation(loc));
        Window window = getWindow();
        window.setFeatureInt(Window.FEATURE_PROGRESS, Window.PROGRESS_INDETERMINATE_ON);
        window.setFeatureInt(Window.FEATURE_PROGRESS, Window.PROGRESS_VISIBILITY_ON);
        updater.updateLocation(loc);
        Button updateButton = (Button)findViewById(R.id.UpdateButton);
        updateButton.setEnabled(true);
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

    private String formatLocation(Location loc) {
        double latitude = loc.getLatitude();
        double longitude = loc.getLongitude();
        return (latitude >= 0f ? "N" : "S") + coordFormat.format(latitude) 
                + ", " +
                (longitude >= 0f ? "E" : "W")+ coordFormat.format(longitude)
                + ", "
                + dateFormat.format(new Date(loc.getTime()));
    }       
    
    private class Updater implements Runnable {
        private Location location;
        private boolean quit = false;
        
        public void updateLocation(Location location) {
            synchronized (this) {
                this.location = location;
                notify();
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
                    synchronized (this) {
                        wait();
                    }
                    
                    if (location != null) {                    
                        updatePlaneList();
                    }
                }
            } catch (InterruptedException e) {
                // Do nothing, just exit
            }
        }

        private void updatePlaneList() {
            InputStream stream = null;
            InputStreamReader reader = null;
            try {
                URL url = new URL("http://www.liedman.net/whatplane/json/closest?lat=" 
                        + location.getLatitude() + "&lon=" + location.getLongitude());
                stream = url.openStream();
                reader = new InputStreamReader(stream);
                StringBuilder b = new StringBuilder();
                char[] buffer = new char[1024];
                int charsRead;
                while ((charsRead = reader.read(buffer)) > 0) {
                    b.append(buffer, 0, charsRead);
                }
                
                JSONArray planes = new JSONArray(b.toString());
                final List<String> groups = new ArrayList<String>();
                final List<String[]> children = new ArrayList<String[]>();
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
                    
                    groups.add(shortInfo);
                    children.add(info.toArray(new String[0]));
                }
                
                handler.post(new Runnable() {
                    public void run() {
                        ExpandableListAdapter adapter = new PlaneListAdapter(WhatPlane.this, groups.toArray(new String[0]), children.toArray(new String[0][0]));
                        ExpandableListView planeList = (ExpandableListView) findViewById(R.id.PlaneList);
                        planeList.setAdapter(adapter);
                        Window window = getWindow();
                        window.setFeatureInt(Window.FEATURE_PROGRESS, Window.PROGRESS_VISIBILITY_OFF);
                    }
                });
                
                
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        // Ok, forget it
                    }
                }
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e) {
                        // Ok, forget it
                    }
                }
            }
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