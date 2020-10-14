package com.example.safeapplication1;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Timer;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,GoogleApiClient.OnConnectionFailedListener, LocationListener {
    GoogleApiClient mGoogleApiClient;
    private GoogleMap mMap;
    private Marker mCurrLocationMarker;
    private Location mLastLocation;
    public static final String KEY_EMP_ID = "id";
    public static final String KEY_EMP_LAT = "lat";
    public static final String KEY_EMP_LONGG = "longg";
    public static final String KEY_EMP_kondisi= "kondisi";
    public static final String KEY_EMP_WAKTU = "waktu";

    public static final String URL_UPDATE_EMP = "http://saltransp.com/restapi/update_data.php";
    public static final String URL_UPDATEKondisi_EMP = "http://saltransp.com/restapi/update_kondisiNBC.php";

    private String id;
    SharedPreferences sharedpreferences;
    public final static String TAG_ID = "id";

    Intent intent;

    //UI Component
    TextView Text_X_Gyro, Text_Y_Gyro, Text_Z_Gyro;
    TextView Text_X_Acclero, Text_Y_Acclero, Text_Z_Acclero;
    Button buttonStart;
    Button buttonStop;
    TextView Text_Terkecil, Text_Aktivitas, Text_NBC;

    //Sensor Component
    SensorManager sensorManager;
    Sensor gyro_sensor;
    Sensor acclero_sensor;

    //Bolean(Penanda/ flagging untuk merekam file)
    boolean isRunning;

    //File Stream
    FileWriter writer;
    final String TAG = "SensorLog";

    //Float Variabel
    double x_gyro;
    double y_gyro;
    double z_gyro;
    double x_acclero;
    double y_acclero;
    double z_acclero;

    //Float Variabel Filter
    double x_filter_acc;
    double y_filter_acc;
    double z_filter_acc;
    double x_filter_gyro;
    double y_filter_gyro;
    double z_filter_gyro;

    //time
    SimpleDateFormat dateFormat;
    String time_string;

    double alpha = 0.1;

    //Naive Bayes
    float mean;
    float varian;
    float stdev;
    float array2;

    Timer timer;

    public static String kondisi;
    public static int kelasterbesar = 0;
    public static double max = 0;
    public static long waktu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        kondisi="";

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        //mMap.setMyLocationEnabled(true);
        sharedpreferences = getSharedPreferences(Login.my_shared_preferences, Context.MODE_PRIVATE);
        /*id = getIntent().getStringExtra(TAG_ID);*/
        id = sharedpreferences.getString(TAG_ID,null);


        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        gyro_sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        acclero_sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        isRunning = false;

        time_string = "";
        dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        gyro_sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        acclero_sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

     /*   Text_X_Gyro = (TextView) findViewById(R.id.text_x_gyro);
        Text_Y_Gyro = (TextView) findViewById(R.id.text_y_gyro);
        Text_Z_Gyro = (TextView) findViewById(R.id.text_z_gyro);

        Text_X_Acclero = (TextView) findViewById(R.id.text_x_acclero);
        Text_Y_Acclero = (TextView) findViewById(R.id.text_y_acclero);
        Text_Z_Acclero = (TextView) findViewById(R.id.text_z_acclero);*/
        Text_Aktivitas = findViewById(R.id.text_aktivitas);

        buttonStart = findViewById(R.id.button_start);
        buttonStop = findViewById(R.id.button_stop);

        /*StartRecord();*/

        buttonStart.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                buttonStart.setEnabled(false);
                buttonStop.setEnabled(true);

                sensorManager.registerListener(gyroListener, gyro_sensor, SensorManager.SENSOR_DELAY_NORMAL);
                sensorManager.registerListener(accleroListener, acclero_sensor, SensorManager.SENSOR_DELAY_NORMAL);


                Log.d(TAG, "Writing to " + getStorageDir());
                try {
                    writer = new FileWriter(new File(getStorageDir(), "sensors_" + System.currentTimeMillis() + ".csv"));
                    writer.write(String.format("%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s\n ",
                            "Waktu", "x_gyro", "y_gyro", "z_gyro", "x_acclero", "y_acclero", "z_acclero", "x_filtergyro", "y_filtergyro", "z_filtergyro", "x_filteraccl", "y_filteraccl", "z_filteraccl", "Kondisi"));

                    isRunning = true;
                    new MyThread("wew");
                } catch (IOException e) {
                    e.printStackTrace();
                }

            /*UpdateAktivitas();*/

                return true;
            }
        });

        buttonStop.setOnTouchListener(new View.OnTouchListener() {
            @TargetApi(Build.VERSION_CODES.KITKAT)
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                buttonStart.setEnabled(true);
                buttonStop.setEnabled(false);
                isRunning = false;
                sensorManager.flush(gyroListener);
                sensorManager.flush(accleroListener);
                sensorManager.unregisterListener(gyroListener);
                sensorManager.unregisterListener(accleroListener);

                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return true;
            }
        });
    }

    /*----------------------------------------------------------------------------------------PROSES ALGORITMA NBC---------------------------------------------------------------------------------*/

    public static void hitung(double mean[][], double stdev[][], double array2[]) {
        double pdf, max, hasil;
        double kelas[] = new double[4];
        kondisi = "";
        for (int i = 0; i < mean.length; i++) {
            hasil = 1;
            for(int j=0; j<mean[i].length; j++) {
                double exponent = Math.exp(-(Math.pow(array2[j] -  mean[i][j], 2) / (2 * Math.pow(stdev[i][j], 2))));
                pdf = (1 / (stdev[i][j] * Math.sqrt(2 * Math.PI))) * exponent;
                hasil = hasil*pdf;

                /*Log.d("Debug", " Nilai mean ke - " + Integer.toString(j) + " - " + Integer.toString(i) + " : " + Double.toString(mean[i][j]));
                Log.d("Debug", " Nilai stdev ke - " + Integer.toString(j) + " - " + Integer.toString(i) + " : " + Double.toString(stdev[i][j]));
                Log.d("Debug", " Nilai array2 ke - " + Integer.toString(j) + " : " + Double.toString(array2[j]));*/
//                Log.d("Debug", " Nilai pdf ke - " + Integer.toString(j) + " - " + Integer.toString(i) + " : " + Double.toString(pdf));
            };
            kelas[i] = hasil;
            Log.d("Debug", " Nilai hasil ke - " + Integer.toString(i) + " : " + Double.toString(hasil));
        };
        max = kelas[0];
        kelasterbesar = 0;
        for(int k = 0; k < kelas.length; k++){
            if(max < kelas[k]){
                max = kelas[k];
                kelasterbesar = k;
            }
        }
        switch (kelasterbesar) {
            case 0:
                kondisi = "Kiri";
                break;
            case 1:
                kondisi = "Kanan";
                break;
            case 2:
                kondisi = "Lurus";
                break;
            case 3:
                kondisi = "Menyala";
                break;
        }
        Log.d("Debug", " Nilai max dari kelas " + Integer.toString(kelasterbesar) + " (" + kondisi + ") = " + Double.toString(max));
    }


    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(gyroListener, gyro_sensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(accleroListener, acclero_sensor, SensorManager.SENSOR_DELAY_NORMAL);
    }


    private void StopRecord(){
        isRunning = false;
        sensorManager.flush(gyroListener);
        sensorManager.flush(accleroListener);
        sensorManager.unregisterListener(gyroListener);
        sensorManager.unregisterListener(accleroListener);

        try {
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
            /*intent = new Intent(MapsActivity.this, MainActivity.class);
            finish();
            startActivity(intent);*/
        }
    }

    private String getStorageDir() {
        return this.getExternalFilesDir(null).getAbsolutePath();
    }


    @Override
    protected void onDestroy(){
        StopRecord();
        super.onDestroy();
       /* intent = new Intent(MapsActivity.this, MainActivity.class);
        finish();
        startActivity(intent);*/
    }

    /*----------------------------------------------------------------------------------------PROSES UPDATE ALGORITMA NBC---------------------------------------------------------------------------------*/

    private void UpdateAktivitas() {
        final String aktivitas = String.valueOf(kondisi);
        final String time = String.valueOf(waktu);

        class UpdateAktivitas extends AsyncTask<Void, Void, String> {
            ProgressDialog loading;
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                /*loading = ProgressDialog.show(MapsActivity.this, "Updating...", "Wait...", false, false);*/
            }
            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                    /*loading.dismiss();
                    Toast.makeText(MapsActivity.this, s, Toast.LENGTH_LONG).show();*/
            }
            @Override
            protected String doInBackground(Void... params) {
                HashMap<String,String> hashMap = new HashMap<>();
                hashMap.put(MapsActivity.KEY_EMP_ID,id);
                hashMap.put(MapsActivity.KEY_EMP_kondisi,aktivitas);
                hashMap.put(MapsActivity.KEY_EMP_WAKTU,time);
                RequestHandler rhh = new RequestHandler();
                String s = rhh.sendPostRequest(MapsActivity.URL_UPDATEKondisi_EMP,hashMap);
                /*Toast.makeText(MapsActivity.this, "Update Location Success", Toast.LENGTH_SHORT).show();*/
                return s;
            }
        }
        UpdateAktivitas ak = new UpdateAktivitas();
        ak.execute();
    }

/*----------------------------------------------------------------------------------------PROSES RECORD SENSOR---------------------------------------------------------------------------------*/

    public SensorEventListener gyroListener = new SensorEventListener() {
        public void onAccuracyChanged(Sensor sensor, int acc) {
        }

        //Mendapatkan Nilai Sensor GYROSCOPE
        public void onSensorChanged(SensorEvent event) {
            x_gyro = event.values[0];
            y_gyro = event.values[1];
            z_gyro = event.values[2];
            dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
            time_string = dateFormat.format(new Date());


          /*  Text_X_Gyro.setText("X : " + (int) x_gyro + " rad/s");
            Text_Y_Gyro.setText("Y : " + (int) y_gyro + " rad/s");
            Text_Z_Gyro.setText("Z : " + (int) z_gyro + " rad/s");*/

            return;
        }
    };

    public SensorEventListener accleroListener = new SensorEventListener() {
        public void onSensorChanged(SensorEvent event) {
            x_acclero = event.values[0];
            y_acclero = event.values[1];
            z_acclero = event.values[2];
            //waktu dan tanggal record
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
            time_string = dateFormat.format(new Date());


            /*Text_X_Acclero.setText("X : " + (int) x_acclero + " m/s2");
            Text_Y_Acclero.setText("Y : " + (int) y_acclero + " m/s2");
            Text_Z_Acclero.setText("Z : " + (int) z_acclero + " m/s2");
*/
            Text_Aktivitas.setText("Klasifikasi: "+kondisi);

            return;

        }

        public void onAccuracyChanged(Sensor sensor, int i) {
        }
    };

    class MyThread implements Runnable {
        String name;
        Thread t;

        MyThread(String thread) {
            name = thread;
            t = new Thread(this, name);
            t.start();
        }

        public void run() { saveData();}
    }

    // menyimpan data .csv
    @SuppressLint("DefaultLocale")
    void saveData() {
        while (isRunning) {
            long millis = System.currentTimeMillis();
            try {
                long start = System.nanoTime();

                double data[] = new double[6];
                data[0] = x_gyro;
                data[1] = y_gyro;
                data[2] = z_gyro;
                data[3] = x_acclero;
                data[4] = y_acclero;
                data[5] = z_acclero;

                double filter[] = new double[6];
                filter[0] = x_filter_gyro;
                filter[1] = y_filter_gyro;
                filter[2] = z_filter_gyro;
                filter[3] = x_filter_acc;
                filter[4] = y_filter_acc;
                filter[5] = z_filter_acc;

                prev(filter, data);

                double mean[][] = {{2.95E-02,2.22E-02,0.058626562,-0.084254062,-0.016300625,0.052038125},   //Kiri
                        {0.022319811,-0.019549057,-0.046849057,0.060749057,-0.024544654,0.039940252},    //Kanan
                        {1.22E-05,-0.000104179,-0.000542388,-0.004090448,-0.009542687,0.004550149},      //Lurus
                        {1.68E-05,-4.12E-05,3.80E-05,-0.000575072,-0.000324058,0.000874783},         //Berhenti
                };
                double stdev [][] = {{0.02782061,0.022587893,0.070674002,0.111684681,0.059204804,0.084541133},    //0
                        {0.022818997,0.02256375,0.061564727,0.13442867,0.122968068,0.098871227},                //3
                        {0.003922238,0.004836926,0.008967476,0.018737072,0.05981754,0.051571771},                 //2
                        {0.000232545,0.000499987,0.000999466,0.009805677,0.011639534,0.007800982},                //1
                };

                double array2[] = new double[6];
                array2[0] = filter[0];
                array2[1] = filter[1];
                array2[2] = filter[2];
                array2[3] = filter[3];
                array2[4] = filter[4];
                array2[5] = filter[5];
                //double haha[]=probability.hitung(mean, stdev, array2);

                hitung(mean, stdev, array2);


                long end = System.nanoTime();
                waktu = (end - start) / 1000000000;
                UpdateAktivitas();

                //outlier jika lebih dari ini maka kecelakaan. jika kurang dari ini akan masuk filter
                if (
                    //ini coba 1
                        /*(data[0] >= (-0.5) && data[0] <= (2)) &&
                                ((data[1] >= (-2) && data[1] <= (1)) || (data[1] >= (0.5) && data[1] <= 1)
                                ) && (data[2] >= (-2.5) && data[2] <= 2) && (data[3] >= (-8) && data[3] <= (8))*/

                    //ini cobaa 2
                   /* (data[0]<-0.3 && data[3]>4 && data[1]<-2 && data[2]<-2.5  || data[0]>1.5 && data[3]<-3 && data[1]>1 && data[2]>2)*/
                        x_acclero>3 || x_acclero<-4 /*&& x_gyro<-0.3 || x_gyro>1.5 */&& y_gyro<-2 || y_gyro>1 && z_gyro<-2 || z_gyro>2) {
                    //cek kondisi
                    kondisi="kecelakaan";
                    max = 0;
                    kelasterbesar = -1;
                }

                Log.d("Debug", " Nilai Kecelakaan x_acclero" +"" + x_acclero);
                Log.d("Debug", " Nilai Kecelakaan y_gyro" + "" + y_gyro);
                Log.d("Debug", " Nilai Kecelakaan z_gyro" +"" + z_gyro);


                writer.write(String.format("%s, %.4f, %.4f, %.4f, %.4f, %.4f, %.4f, %.4f, %.4f, %.4f, %.4f, %.4f, %.4f, %s \n ",
                       time_string, x_gyro, y_gyro, z_gyro, x_acclero, y_acclero, z_acclero, filter[0], filter[1], filter[2], filter[3], filter[4], filter[5], kondisi));

                Thread.sleep(500 - millis % 500);


                if (kondisi =="kecelakaan"){
                    isRunning = false;
                    sensorManager.flush(gyroListener);
                    sensorManager.flush(accleroListener);
                    sensorManager.unregisterListener(gyroListener);
                    sensorManager.unregisterListener(accleroListener);
                    writer.close();

                    updateData();
                    Intent intent = new Intent(MapsActivity.this, Notif.class);
                    intent.putExtra(TAG_ID, id);
                    finish();
                    startActivity(intent);
                }
                Log.d("Debug", " Nilai max dari kelas " + kelasterbesar + " (" + kondisi + ") = " + max);


            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /*public void filter1(long millis)
    {
        return;
    }*/

    // INI RUMUS FILTER
    private double[] prev(double[] filter, double[] data) {
        if (data == null || filter == null)
            throw new NullPointerException("input and prev float arrays must be non-NULL");
        if (data.length != filter.length)
            throw new IllegalArgumentException("input and prev must be the same length");

        for (int i = 0; i < data.length; i++) {
            filter[i] = filter[i] + alpha * (data[i] - filter[i]);

            Log.d("Debug", " Nilai filter ke - " + Integer.toString(i) + " : " + Double.toString(filter[i]));
        }
        return filter;
    }
/*----------------------------------------------------------------------------------------------UPDATE LOKASI--------------------------------------------------------------------------*/
    //UPDATE DATA LOKASI DI DATABASE
    public void updateData() {
        final String lat = String.valueOf(mLastLocation.getLatitude());
        final String longg = String.valueOf(mLastLocation.getLongitude());
        final String kondisi = "kecelakaan";

        class UpdateData extends AsyncTask<Void, Void, String> {
            ProgressDialog loading;
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
               /* loading = ProgressDialog.show(MapsActivity.this, "Updating...", "Wait...", false, false);*/
            }
            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                /*loading.dismiss();*/
                /*Toast.makeText(MapsActivity.this, s, Toast.LENGTH_LONG).show();*/
            }
            @Override
            protected String doInBackground(Void... params) {
                HashMap<String,String> hashMap = new HashMap<>();
                hashMap.put(MapsActivity.KEY_EMP_ID,id);
                hashMap.put(MapsActivity.KEY_EMP_LONGG,longg);
                hashMap.put(MapsActivity.KEY_EMP_LAT,lat);
                hashMap.put(MapsActivity.KEY_EMP_kondisi,kondisi);

                RequestHandler rh = new RequestHandler();

                String s = rh.sendPostRequest(MapsActivity.URL_UPDATE_EMP,hashMap);

                return s;
            }
        }
       UpdateData ue = new UpdateData();
        ue.execute();

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
        mMap.setMyLocationEnabled(true);
        /*mMap.setMyLocationEnabled(true);*/
       /* mMap = googleMap;
        mMap.setMyLocationEnabled(true);*/
        buildGoogleApiClient();
        mGoogleApiClient.connect();

    }

    @Override
    public void onPause() {
        super.onPause();
        sensorManager.unregisterListener(gyroListener);
        sensorManager.unregisterListener(accleroListener);

        //Unregister for location callbacks:
        if (mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
    }



    private void buildGoogleApiClient(){
        Toast.makeText(this,"buildGoogleApiClient",Toast.LENGTH_SHORT).show();
        mGoogleApiClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(this).addOnConnectionFailedListener(this).addApi(LocationServices.API).build();
        /*mGoogleApiClient.connect();*/

    }
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Toast.makeText(this,"onConnected",Toast.LENGTH_SHORT).show();
        Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);

        if (mLastLocation != null) {
            //place marker at current position
            mMap.clear();
            LatLng latLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());

            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude())).zoom(14).build();
            mMap.animateCamera(CameraUpdateFactory
                    .newCameraPosition(cameraPosition));

            // create markerOptions
            MarkerOptions markerOptions = new MarkerOptions().position(new LatLng(
                    mLastLocation.getLatitude(), mLastLocation.getLongitude()));
            // ROSE color icon
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));
            markerOptions.position(latLng);
            // adding markerOptions
            Marker marker = mMap.addMarker(markerOptions);
            //dropPinEffect(marker);
        }
       LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        /*LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);*/

      /*  LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);*/
    }

    @Override
    public void onConnectionSuspended(int i) {
        Toast.makeText(this,"onConnectionSuspended",Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(this,"onConnectionFailed",Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        if (mCurrLocationMarker != null) {
            mCurrLocationMarker.remove();

        }
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions().position(latLng).title("I Am Here");
        markerOptions.position(latLng);
        markerOptions.title("Current Position");
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
        mCurrLocationMarker = mMap.addMarker(markerOptions);

        /*speed = location.getSpeed();
        updateUI();*/

       /* Toast.makeText(this,"Update Location \n " +
                "Latitude : "+ mLastLocation.getLatitude()+
                "\nLongitude : "+mLastLocation.getLongitude(),Toast.LENGTH_SHORT).show();*/

    }

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    public boolean checkLocationPermission(){
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // Izin diberikan.
                    if (ContextCompat.checkSelfPermission(this,
                            android.Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        if (mGoogleApiClient == null) {
                            buildGoogleApiClient();
                        }
                        mMap.setMyLocationEnabled(true);
                    }

                } else {
                    // Izin ditolak.
                    Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }

    @Override
    public void onBackPressed() {
        intent = new Intent(MapsActivity.this, MainActivity.class);
        finish();
        startActivity(intent);
    }




}