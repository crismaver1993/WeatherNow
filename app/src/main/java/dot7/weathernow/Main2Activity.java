package dot7.weathernow;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import dot7.weathernow.utilidades.GPSTracker;

public class Main2Activity extends AppCompatActivity {
    TextView txtWeather, txtCity, txtTemp,txtMinMax, txtNoInternet;
    private LinearLayout contentWeather;
    private Toolbar toolbar;
    private BottomSheetBehavior bottomSheetBehavior;
    private ArrayAdapter<String> adapter;
    ProgressDialog myDialog;
    ProgressDialog dialog;
    private NestedScrollView mScrollView;
    private SwipeRefreshLayout pullToRefresh;
    private ArrayAdapter<String> mForecastAdapter;
    private View view;
    ;
    private ListView listView;
    private String origin;
    private String forecastJsonStr = null;
    private LatLng origCurrentPoint;
    double latitude, longitude;
    private LocationManager locationMangaer = null;
    private LocationListener locationListener = null;
    InputStream directorioJson;
    String result;
    private int Request_Location = 1;
    private static final String TAG = "xxxLocation";
    public static final String OPEN_WEATHER_MAP_API_KEY = "dca28c01e3b24926ed6f24e406309e1c";
    private Boolean flag = false;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.registerReceiver(this.mConnReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        View bottomSheet = findViewById(R.id.bottom_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);

        txtWeather = (TextView) findViewById(R.id.txtWeather);
        txtCity = (TextView) findViewById(R.id.txtCity);
        txtTemp = (TextView) findViewById(R.id.txtTemp);
        txtMinMax = (TextView) findViewById(R.id.txtMinMax);
        contentWeather = (LinearLayout) findViewById(R.id.contentWeather);
        txtNoInternet = (TextView) findViewById(R.id.txtNoInternet);
        mScrollView = (NestedScrollView) findViewById(R.id.mScrollView);

        pullToRefresh = (SwipeRefreshLayout) findViewById(R.id.refresh);


        mForecastAdapter =
                new ArrayAdapter<String>(
                        Main2Activity.this, // The current context (this activity)
                        R.layout.list_item_forecast, // The name of the layout ID.
                        R.id.txtItemList, // The ID of the textview to populate.
                        new ArrayList<String>());

        listView = (ListView) findViewById(R.id.listview_forecast);

        listView.setAdapter(mForecastAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                String forecast = mForecastAdapter.getItem(position);
                Intent intent = new Intent(Main2Activity.this, DetailActivity.class)
                        .putExtra(Intent.EXTRA_TEXT, forecast)
                        .putExtra("info", forecast);
                startActivity(intent);
            }
        });

        pullToRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {

            @Override
            public void onRefresh() {
                if (checkConnectivity()) {
                    checkPermissionsLocation();
                    loadingInfo();
                    loadingInfo2();
                } else {
                    pullToRefresh.setRefreshing(false);
                }
            }
        });


        adapter = new ArrayAdapter<>(Main2Activity.this, R.layout.maps_ubication_item);

        myDialog = new ProgressDialog(Main2Activity.this);
        myDialog.setMessage("Loading...");
        myDialog.setCancelable(false);

        final String[] result = new String[1];

        directorioJson = getResources().openRawResource(R.raw.mexico);


        if (checkConnectivity()) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, Request_Location);
                }
                return;
            } else {
                dialog = ProgressDialog.show(Main2Activity.this, "",
                        "Loading. Please wait...", true);
                loadingInfo();
                loadingInfo2();
            }
        } else {
            txtNoInternet.setVisibility(View.VISIBLE);
            txtNoInternet.setText("\n\nSin conexión a Internet...");
        }
        leerJson(directorioJson);
    }

    private BroadcastReceiver mConnReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            boolean noConnectivity = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
            String reason = intent.getStringExtra(ConnectivityManager.EXTRA_REASON);
            boolean isFailover = intent.getBooleanExtra(ConnectivityManager.EXTRA_IS_FAILOVER, false);

            NetworkInfo currentNetworkInfo = (NetworkInfo) intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
            NetworkInfo otherNetworkInfo = (NetworkInfo) intent.getParcelableExtra(ConnectivityManager.EXTRA_OTHER_NETWORK_INFO);

            if (currentNetworkInfo.isConnected()) {

                loadingInfo();
                loadingInfo2();
                txtNoInternet.setVisibility(View.GONE);
            } else {
                txtNoInternet.setVisibility(View.VISIBLE);
                txtNoInternet.setText("\n\nSin conexión a Internet...");

            }
        }
    };

    private void loadingInfo() {

        if (origCurrentPoint == null) {
            GPSTracker gps = new GPSTracker(Main2Activity.this);

            if (gps.canGetLocation()) {
                latitude = gps.getLatitude();
                longitude = gps.getLongitude();
                updateWeather();

            } else {
                gps.showSettingsAlert();
            }
        }
    }

    private void updateWeather() {
        FetchWeatherTask weatherTask = new FetchWeatherTask();
        String lat = String.valueOf(latitude);
        String lon = String.valueOf(longitude);

        Log.v("xxxLat1",""+lat);
        Log.v("xxxLong1",""+lon);
      weatherTask.execute(lat, lon);


    }

    public class FetchWeatherTask extends AsyncTask<String, Void, String[]> {

        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();

        private String getReadableDateString(long time) {

            SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
            return shortenedDateFormat.format(time);
        }

        private String formatHighLows(double high, double low, String unitType) {

            if (unitType.equals(getString(R.string.pref_units_imperial))) {
                high = (high * 1.8) + 32;
                low = (low * 1.8) + 32;
            } else if (!unitType.equals(getString(R.string.pref_units_metric))) {
                Log.d(LOG_TAG, "Unit type not found: " + unitType);
            }

            long roundedHigh = Math.round(high);
            long roundedLow = Math.round(low);

            String highLowStr = roundedHigh + "/" + roundedLow;
            return highLowStr;
        }


        private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays)
                throws JSONException {

            // These are the names of the JSON objects that need to be extracted.
            final String OWM_CITY = "city";
            final String OWM_NAMECITY = "name";
            final String OWM_COUNTRY = "country";
            final String OWM_LIST = "list";
            final String OWM_WEATHER = "weather";
            final String OWM_TEMPERATURE = "temp";
            final String OWM_MAX = "max";
            final String OWM_MIN = "min";
            final String OWM_DESCRIPTION = "main";
            final String OWM_MORN = "morn";
            final String OWM_EVE = "eve";
            final String OWM_NIGHT = "night";
            final String OWM_DAY = "day";
            final String OWM_PRESSURE = "pressure";
            final String OWM_HUMIDITY = "humidity";
            final String OWM_DESC = "description";

            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);
            JSONObject valores;
            String location = "", currentTemp = "";


            Time dayTime = new Time();
            dayTime.setToNow();

            // we start at the day returned by local time. Otherwise this is a mess.
            int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

            // now we work exclusively in UTC
            dayTime = new Time();

            String[] resultStrs = new String[numDays];

            SharedPreferences sharedPrefs =
                    PreferenceManager.getDefaultSharedPreferences(Main2Activity.this);
            String unitType = sharedPrefs.getString(
                    getString(R.string.pref_units_key),
                    getString(R.string.pref_units_metric));


            for (int posL = 0; posL < forecastJson.length(); posL++) {
                valores = forecastJson.getJSONObject(OWM_CITY);
                location = valores.getString(OWM_NAMECITY) + "," + valores.getString(OWM_COUNTRY);
            }

            for (int i = 0; i < weatherArray.length(); i++) {
                // For now, using the format "Day, description, hi/low"
                String day, description, highAndLow;
                String dayT, descT, night, morn, eve, pressure, humidity;

                // Get the JSON object representing the day
                JSONObject dayForecast = weatherArray.getJSONObject(i);

                long dateTime;
                // Cheating to convert this to UTC time, which is what we want anyhow
                dateTime = dayTime.setJulianDay(julianStartDay + i);
                day = getReadableDateString(dateTime);

                // description is in a child array called "weather", which is 1 element long.
                JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                description = weatherObject.getString(OWM_DESCRIPTION);
                descT = weatherObject.getString(OWM_DESC);

                // Temperatures are in a child object called "temp".  Try not to name variables
                // "temp" when working with temperature.  It confuses everybody.
                JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
                double high = temperatureObject.getDouble(OWM_MAX);
                double low = temperatureObject.getDouble(OWM_MIN);
                dayT = temperatureObject.getString(OWM_DAY);
                night = temperatureObject.getString(OWM_NIGHT);
                morn = temperatureObject.getString(OWM_MORN);
                eve = temperatureObject.getString(OWM_EVE);
                pressure = dayForecast.getString(OWM_PRESSURE);
                humidity = dayForecast.getString(OWM_HUMIDITY);

                //Check current time Mor,Day, Eve,Night
                Calendar c = Calendar.getInstance();
                int timeOfDay = c.get(Calendar.HOUR_OF_DAY);

                if (timeOfDay >= 0 && timeOfDay < 12) {
                    currentTemp = morn;
                } else if (timeOfDay >= 12 && timeOfDay < 16) {
                    currentTemp = dayT;
                } else if (timeOfDay >= 16 && timeOfDay < 21) {
                    currentTemp = eve;
                } else if (timeOfDay >= 21 && timeOfDay < 24) {
                    currentTemp = night;
                }

                highAndLow = formatHighLows(high, low, unitType);
                resultStrs[i] = location + "-" + currentTemp + "-" +
                        descT + "-" + description + "-" + highAndLow;
            }
            return resultStrs;

        }

        @Override
        protected String[] doInBackground(String... params) {

            // If there's no zip code, there's nothing to look up.  Verify size of params.
            if (params.length == 0) {
                return null;
            }

            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.


            String format = "json";
            String units = "metric";
            int numDays = 1;


            try {
                // Construct the URL for the OpenWeatherMap query
                // Possible parameters are avaiable at OWM's forecast API page, at
                // http://openweathermap.org/API#forecast
                final String FORECAST_BASE_URL =
                        "http://api.openweathermap.org/data/2.5/forecast/daily?";
                final String QUERY_PARAM = "lat";
                final String QUERY_PARAM2 = "lon";
                final String FORMAT_PARAM = "mode";
                final String UNITS_PARAM = "units";
                final String DAYS_PARAM = "cnt";
                final String APPID_PARAM = "APPID";

                Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                        .appendQueryParameter(QUERY_PARAM, params[0])
                        .appendQueryParameter(QUERY_PARAM2, params[1])
                        .appendQueryParameter(FORMAT_PARAM, format)
                        .appendQueryParameter(UNITS_PARAM, units)
                        .appendQueryParameter(DAYS_PARAM, Integer.toString(numDays))
                        .appendQueryParameter(APPID_PARAM, OPEN_WEATHER_MAP_API_KEY)
                        .build();

                URL url = new URL(builtUri.toString());


                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                forecastJsonStr = buffer.toString();

              //  Log.v("xxxInfotoda!", "" + forecastJsonStr);


            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attemping
                // to parse it.
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            try {
                return getWeatherDataFromJson(forecastJsonStr, numDays);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }
            // This will only happen if there was an error getting or parsing the forecast.
            return null;
        }

        @Override
        protected void onPostExecute(String[] result) {
            if (result != null) {

                try {
                    JSONObject forecastJson = new JSONObject(forecastJsonStr);


                } catch (JSONException e) {
                    e.printStackTrace();
                }
                String[] info = result[0].split("-");

                txtCity.setText(info[0].toUpperCase());
                txtTemp.setText(info[1].toUpperCase() + "°");
                txtWeather.setText(info[2].toUpperCase());
                txtMinMax.setText(info[4].toUpperCase());

                if (info[3].toUpperCase().equals("CLEAR")) {
                    contentWeather.setBackgroundResource(R.drawable.clear);
                } else if (info[3].toUpperCase().equals("RAIN")) {
                    contentWeather.setBackgroundResource(R.drawable.rain);
                } else if (info[3].toUpperCase().equals("CLOUD")) {
                    contentWeather.setBackgroundResource(R.drawable.cloud);
                } else if (info[3].toUpperCase().equals("FEW CLOUDS")) {
                    contentWeather.setBackgroundResource(R.drawable.cloud);
                }


                if (dialog != null) {
                    dialog.dismiss();
                }
                pullToRefresh.setRefreshing(false);
                // New data is back from the server.  Hooray!
            }
        }
    }

    public void loadingInfo2() {


        if (origCurrentPoint == null) {
            GPSTracker gps = new GPSTracker(Main2Activity.this);

            if (gps.canGetLocation()) {
                latitude = gps.getLatitude();
                longitude = gps.getLongitude();
                updateWeather2();

            } else {
                gps.showSettingsAlert();
            }
        }
    }

    private void updateWeather2() {
        FetchWeatherTask2 weatherTask = new FetchWeatherTask2();
        String lat = String.valueOf(latitude);
        String lon = String.valueOf(longitude);

        Log.v("xxxLat2",""+lat);
        Log.v("xxxLong2",""+lon);
        weatherTask.execute(lat, lon);
    }

    public class FetchWeatherTask2 extends AsyncTask<String, Void, String[]> {

        private final String LOG_TAG = FetchWeatherTask2.class.getSimpleName();


        private String getReadableDateString(long time) {
            SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
            return shortenedDateFormat.format(time);
        }

        private String formatHighLows(double high, double low, String unitType) {

            if (unitType.equals(getString(R.string.pref_units_imperial))) {
                high = (high * 1.8) + 32;
                low = (low * 1.8) + 32;
            } else if (!unitType.equals(getString(R.string.pref_units_metric))) {
                Log.d(LOG_TAG, "Unit type not found: " + unitType);
            }

            // For presentation, assume the user doesn't care about tenths of a degree.
            long roundedHigh = Math.round(high);
            long roundedLow = Math.round(low);

            String highLowStr = roundedHigh + "/" + roundedLow;
            return highLowStr;
        }


        private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays)
                throws JSONException {

            // These are the names of the JSON objects that need to be extracted.
            final String OWM_LIST = "list";
            final String OWM_WEATHER = "weather";
            final String OWM_TEMPERATURE = "temp";
            final String OWM_MAX = "max";
            final String OWM_MIN = "min";
            final String OWM_DESCRIPTION = "main";

            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

            Time dayTime = new Time();
            dayTime.setToNow();

            // we start at the day returned by local time. Otherwise this is a mess.
            int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

            // now we work exclusively in UTC
            dayTime = new Time();

            String[] resultStrs = new String[numDays];

            SharedPreferences sharedPrefs =
                    PreferenceManager.getDefaultSharedPreferences(Main2Activity.this);
            String unitType = sharedPrefs.getString(
                    getString(R.string.pref_units_key),
                    getString(R.string.pref_units_metric));

            for (int i = 0; i < weatherArray.length(); i++) {
                String day;
                String description;
                String highAndLow;


                JSONObject dayForecast = weatherArray.getJSONObject(i);

                long dateTime;

                dateTime = dayTime.setJulianDay(julianStartDay + i);
                day = getReadableDateString(dateTime);

                JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                description = weatherObject.getString(OWM_DESCRIPTION);

                JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
                double high = temperatureObject.getDouble(OWM_MAX);
                double low = temperatureObject.getDouble(OWM_MIN);

                highAndLow = formatHighLows(high, low, unitType);
                resultStrs[i] = day + " - " + description + " - " + highAndLow;
            }
            return resultStrs;

        }

        @Override
        protected String[] doInBackground(String... params) {
            if (params.length == 0) {
                return null;
            }

            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            String forecastJsonStr = null;

            String format = "json";
            String units = "metric";
            int numDays = 7;


            try {

                final String FORECAST_BASE_URL =
                        "http://api.openweathermap.org/data/2.5/forecast/daily?";
                final String QUERY_PARAM = "lat";
                final String QUERY_PARAM2 = "lon";
                final String FORMAT_PARAM = "mode";
                final String UNITS_PARAM = "units";
                final String DAYS_PARAM = "cnt";
                final String APPID_PARAM = "APPID";

                Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                        .appendQueryParameter(QUERY_PARAM, params[0])
                        .appendQueryParameter(QUERY_PARAM2, params[1])
                        .appendQueryParameter(FORMAT_PARAM, format)
                        .appendQueryParameter(UNITS_PARAM, units)
                        .appendQueryParameter(DAYS_PARAM, Integer.toString(numDays))
                        .appendQueryParameter(APPID_PARAM, OPEN_WEATHER_MAP_API_KEY)
                        .build();

                URL url = new URL(builtUri.toString());

                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    return null;
                }
                forecastJsonStr = buffer.toString();
                //Log.v("xxxFOrecastResult", "" + forecastJsonStr);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            try {
                return getWeatherDataFromJson(forecastJsonStr, numDays);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String[] result) {
            if (result != null) {
                mForecastAdapter.clear();
                for (String dayForecastStr : result) {
                    mForecastAdapter.add(dayForecastStr);
                }

            }
        }
    }

    private boolean checkConnectivity() {
        boolean enabled = true;

        ConnectivityManager connectivityManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connectivityManager.getActiveNetworkInfo();

        if ((info == null || !info.isConnected() || !info.isAvailable())) {
            Toast.makeText(Main2Activity.this, "Internet o datos mòviles son necesarios..", Toast.LENGTH_SHORT).show();
            enabled = false;
        }
        return enabled;
    }

    public boolean checkPermissionsLocation() {
        if (Build.VERSION.SDK_INT >= 23) {

            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                    Toast.makeText(Main2Activity.this, "Permiso de Localización es necesario..", Toast.LENGTH_SHORT).show();
                }
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, Request_Location);
            }
        } else {
            return true;
        }
        return true;
    }//checkRequest

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == Request_Location) {
            if (checkConnectivity()) {
                checkPermissionsLocation();
                loadingInfo();
                loadingInfo2();
            } else {
                pullToRefresh.setRefreshing(false);
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    public void clickMore(View v) {
        ubicationDialogPicker();
    }

    private void ubicationDialogPicker() {

        LayoutInflater layoutInflater = LayoutInflater.from(Main2Activity.this);
        View promptView = layoutInflater.inflate(R.layout.maps_ubication_dialog, null);
        final AlertDialog alertD = new AlertDialog.Builder(Main2Activity.this).create();
        final ListView lvPlaces = (ListView) promptView.findViewById(R.id.listUbicaciones);
        final TextView txtSearch = (TextView) promptView.findViewById(R.id.txtBuscar);
        lvPlaces.setAdapter(adapter);


        lvPlaces.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterList, View v, int position, long arg3) {

                origin = adapterList.getItemAtPosition(position).toString();

                txtSearch.setText("");
                alertD.dismiss();
            }
        });

        txtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence cs, int arg1, int arg2, int arg3) {
                adapter.getFilter().filter(cs);
            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
            }

            @Override
            public void afterTextChanged(Editable arg0) {
            }
        });
        alertD.setView(promptView);
        alertD.show();
    }

    private void leerJson(InputStream directorioJson) {
        adapter = new ArrayAdapter<>(Main2Activity.this, R.layout.maps_ubication_item);
        JSONArray directory = null;
        result = getStringFromInputStream(directorioJson);
        JSONObject valores;
        try {
            directory = new JSONArray(result);
            for (int i = 0; i < directory.length(); i++) {
                valores = directory.optJSONObject(i);
                adapter.add(String.valueOf(valores.optString("name")));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        myDialog.dismiss();
    }

    // convert InputStream to String
    private static String getStringFromInputStream(InputStream json) {
        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();
        String line;

        try {
            br = new BufferedReader(new InputStreamReader(json));
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return sb.toString();
    }
}