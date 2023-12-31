package com.example.travel;
import android.app.ActivityManager;
import android.os.Bundle;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.example.travel.HomeAdapter.FeaturedAdapter;
import com.example.travel.HomeAdapter.FeaturedHelperClass;
import com.example.travel.searchh.searchAdapter;
import com.example.travel.searchh.searchmodel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import android.os.Handler;

import android.widget.ImageView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SearchView;

public class MainActivity extends AppCompatActivity {
    private boolean isExtraActivityVisible = false;

    private boolean isAppInBackground = true;
    RecyclerView featuredRecycler;
    RecyclerView.Adapter adapter;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final String TAG = "MyAPIRequest";
    private SearchView searchView;


    private RequestQueue requestQueue;
    private int[] images = new int[]{R.drawable.everest, R.drawable.sagarmatha, R.drawable.sunsetphewa, R.drawable.begnas, R.drawable.manang, R.drawable.mustangcaves, R.drawable.morningpanchase}; // Add your image resources here
    private int currentIndex = 0;
    private ImageView imageView;
    private static boolean isActivityVisible;

    private LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        featuredRecycler=findViewById(R.id.featured_recycler);
        ImageView logout=findViewById(R.id.logout);
        imageView = findViewById(R.id.imageSlider);
        ImageView redirectAbout =findViewById(R.id.redirectToAbout);
        ImageView suggestButton = findViewById(R.id.suggestButton);;
        RequestQueueSingleton requestQueueSingleton = RequestQueueSingleton.getInstance(this);
        requestQueue = requestQueueSingleton.getRequestQueue();
        featuredRecycler();
        searchView = findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String query) {
                Toast.makeText(MainActivity.this, "HElloe", Toast.LENGTH_LONG);
//                // Pass the search query to the next activity
//                Intent intent = new Intent(MainActivity.this, Btnsearch.class);
//                intent.putExtra("SEARCH_QUERY", query);
//                startActivity(intent);
//                String searchQuery = intent.getStringExtra("SEARCH_QUERY");
                // Use the searchQuery as needed
                SearchAPi(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Not used in this example
                return false;
            }
        });

        searchView.setIconifiedByDefault(false);
        searchView.setQueryHint("Search Here");

        redirectAbout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent =new Intent(MainActivity.this,Extra.class);
                startActivity(intent);
            }
        });
        suggestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check for location permission
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    // Request location permission if not granted
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
                } else {
                    // If permission is already granted, retrieve precise location
                    try {
                        LocationListener locationListener = new LocationListener() {
                            @Override
                            public void onLocationChanged(Location location) {
                                // Handle location updates if needed
                                double userLatitude = location.getLatitude();
                                double userLongitude = location.getLongitude();
                                sendLocationToFastAPI(userLatitude, userLongitude);
                                locationManager.removeUpdates(this); // Remove updates after receiving precise location
                            }

                            @Override
                            public void onStatusChanged(String provider, int status, Bundle extras) {
                            }

                            @Override
                            public void onProviderEnabled(String provider) {
                            }

                            @Override
                            public void onProviderDisabled(String provider) {
                            }
                        };

                        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                        if (locationManager != null) {
                            // Remove previous updates if any
                            locationManager.removeUpdates(locationListener);
                            // Request location updates
                            locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, locationListener, null);
                        } else {
                            Toast.makeText(MainActivity.this, "Location Manager is null", Toast.LENGTH_SHORT).show();
                        }
                    } catch (SecurityException e) {
                        e.printStackTrace();
                        Toast.makeText(MainActivity.this, "Permission denied. Please grant location access.", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
        final Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (currentIndex == images.length) {
                    currentIndex = 0;
                }
                imageView.setImageResource(images[currentIndex]);
                currentIndex++;
                handler.postDelayed(this, 2000); // Change image every 2 seconds
            }
        };
        handler.post(runnable);
    }

    private void featuredRecycler() {
        featuredRecycler.setHasFixedSize(true);
        featuredRecycler.setLayoutManager(new LinearLayoutManager(this,LinearLayoutManager.HORIZONTAL,false));
        ArrayList<FeaturedHelperClass> featuredLocations=new ArrayList<>();

        featuredLocations.add(new FeaturedHelperClass(R.drawable.sagarmatha,"Mt EVEREST", "Highest Peak"));
        featuredLocations.add(new FeaturedHelperClass(R.drawable.sunsetphewa,"Phewa Lake", "Sunset View of Phewa Lake"));
        featuredLocations.add(new FeaturedHelperClass(R.drawable.mustangcaves,"Mustang", "Caves of Mustang"));
        featuredLocations.add(new FeaturedHelperClass(R.drawable.morningpanchase,"Panchase", "View of Panchase"));
        adapter=new FeaturedAdapter(featuredLocations);
        featuredRecycler.setAdapter(adapter);



    }


    private void getLocation() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        try {
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                Toast.makeText(this, "GPS is disabled", Toast.LENGTH_SHORT).show();
                return;
            }

            LocationListener locationListener = new LocationListener() {
                public void onLocationChanged(Location location) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();

                    locationManager.removeUpdates(this);
//                    Log.e('latlon', "onLocationChanged: "+latitude +' '+longitude );
                    Toast.makeText(getApplicationContext(), ""+latitude + ' '+ longitude, Toast.LENGTH_SHORT).show();
                }
            };

            locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, locationListener, null);
        } catch (SecurityException e) {
            e.printStackTrace();
            Toast.makeText(this, "Permission denied. Please grant location access.", Toast.LENGTH_SHORT).show();
        }
    }



    private void sendLocationToFastAPI(double latitude, double longitude) {
        String url = urls.BASE_URL+urls.GET_LOCATION;

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("latitude", latitude);
            jsonBody.put("longitude", longitude);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, jsonBody,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            if (response.has("top_5_nearest")) {
                                JSONArray placesArray = response.getJSONArray("top_5_nearest");
                                handleTop5Nearest(placesArray);
                            } else if (response.has("error")) {
                                String errorMessage = response.getString("error");
                                Toast.makeText(MainActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(MainActivity.this, "Unknown response format", Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(MainActivity.this, "Error parsing response from FastAPI", Toast.LENGTH_SHORT).show();
                        }
                    }

                    private void handlePlace(JSONObject place) throws JSONException {
                        String name = place.getString("name");
                        double distance = place.getDouble("distance");

                        String message = "Place: " + name + ", Distance: " + distance;
                        Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                    private void handleTop5Nearest(JSONArray placesArray) throws JSONException {
                        ArrayList<String> placeNames = new ArrayList<>();
                        ArrayList<String> distances = new ArrayList<>();

                        for (int i = 0; i < placesArray.length(); i++) {
                            JSONObject place = placesArray.getJSONObject(i);
                            String name = place.getString("name");
                            double distance = place.getDouble("distance");

                            placeNames.add(name);
                            distances.add(String.valueOf(distance));
                        }

                        Intent intent = new Intent(MainActivity.this, display.class);
                        intent.putStringArrayListExtra("placeNames", placeNames);
                        intent.putStringArrayListExtra("distances", distances);
                        intent.putExtra("latitude", latitude); // Pass latitude to the display activity
                        intent.putExtra("longitude", longitude); // Pass longitude to the display activity

                        // Pass the requestQueue and TAG (if available)
//                        intent.putExtra("requestQueue", requestQueue);
                        intent.putExtra("TAG", TAG);
                        startActivity(intent);
                    }
                },

                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
//                        Toast.makeText(MainActivity.this, "Error sending location to FastAPI", Toast.LENGTH_SHORT).show();
                        Toast.makeText(MainActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();

                    }
                });
        jsonObjectRequest.setTag("REQUEST_TAG");

// Add the request to the RequestQueue
        requestQueue.add(jsonObjectRequest);


    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocation();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
    // Inside your activity

    // Click listener method for Temple button

    public void onTempleClick(View view) {
        Intent templeIntent = new Intent(this, Temple.class);
        templeIntent.putExtra("Temple", "Temple");
        startActivity(templeIntent);
    }

    // Click listener method for natural button
    public void onNatureClick(View view) {
        Intent natureIntent = new Intent(this, natural.class);
        natureIntent.putExtra("Nature", "Nature");
        startActivity(natureIntent);
    }

    // Click listener method for lake button

    public void onlakeClick(View view) {
        Intent natureIntent = new Intent(this, lake.class);
        natureIntent.putExtra("Nature", "Nature");
        startActivity(natureIntent);
    }



    // Method to clear session data
    public void loginpage(View view) {
        // Call the method to clear the session and perform logout
        clearSession();
    }

    // Method to clear session data
    private void clearSession() {
        SharedPreferences sharedPreferences = getSharedPreferences("my_session", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // Clear all stored data related to the session
        editor.clear();
        editor.apply();

        // Navigate to the login screen or perform other necessary actions
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    protected void onStop() {
        super.onStop();

        // Update the app's background state
        isAppInBackground = !isAppInForeground();

        // Clear session only when the whole app is going to the background
        if (isAppInBackground) {
            clearSession();
        }
    }
    private boolean isAppInForeground() {
        // Logic to check if the app is in the foreground
        // For example, you can check if the app has any visible activity
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        if (appProcesses != null) {
            for (ActivityManager.RunningAppProcessInfo processInfo : appProcesses) {
                if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                    for (String activeProcess : processInfo.pkgList) {
                        if (activeProcess.equals(getPackageName())) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
//Search button API
private void SearchAPi(String searchQuery) {
    Thread thread = new Thread(new Runnable() {
        @Override
        public void run() {
            try {
                URL url = new URL("http://192.168.1.67/travel/get_info.php?name=" + searchQuery);

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                String jsonResponseStr = response.toString();
                try {
                    JSONArray jsonArray = new JSONArray(jsonResponseStr);
                    ArrayList<searchmodel> searchList=new ArrayList<>();
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        String name = jsonObject.getString("Name");
                        String imageUrl = jsonObject.optString("image");
                        String description = jsonObject.optString("description");
                        String baseLocalhostUrl = "http://192.168.1.67/travel/images/"  ; // Replace ipAddress with your machine's local IP address
                        String finaleimageUrl = baseLocalhostUrl + imageUrl;

                        searchmodel searchData = new searchmodel(finaleimageUrl, name, description);
                        searchList.add(searchData);
                    }

                    runOnUiThread(() -> {
                        RecyclerView recyclerView = findViewById(R.id.myRecyclerView);
                        recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
                        searchAdapter adapter = new searchAdapter(searchList, MainActivity.this);
                        recyclerView.setAdapter(adapter);
                    });

                } catch (JSONException e) {
                    e.printStackTrace();
                    String errorInfo = "Error parsing JSON: " + e.getMessage();
                    Log.e("JSON Parsing Error", errorInfo);
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, errorInfo, Toast.LENGTH_SHORT).show();
                    });
                }

            } catch (Exception error) {
                error.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }
    });
    thread.start();
}





}
