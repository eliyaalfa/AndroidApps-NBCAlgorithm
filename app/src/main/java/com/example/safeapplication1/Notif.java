package com.example.safeapplication1;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import java.util.HashMap;


public class Notif extends AppCompatActivity {
    public static final String KEY_EMP_ID = "id";
    public static final String KEY_EMP_trigger = "triger";
    public static final String KEY_EMP_kondisi= "kondisi";

    public static final String URL_UPDATE_EMP = "http://saltransp.com/restapi/update_trigger.php";
    /*private static String waktu;
    public static final String KEY_EMP_WAKTU = waktu;*/

    private final static String default_notification_channel_id = "default";

    private String id;
    public final static String TAG_ID = "id";

    int counter;
    Button btn_oke;
    TextView txt;
    Intent intent;
    ImageView img;
    Ringtone ringTone;

    @SuppressLint("WrongConstant")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm);

        SharedPreferences sharedpreferences = getSharedPreferences(Login.my_shared_preferences, Context.MODE_PRIVATE);
        /*id = getIntent().getStringExtra(TAG_ID);*/
        id = sharedpreferences.getString(TAG_ID,null);


        txt = (TextView) findViewById(R.id.counttime);
        btn_oke = (Button) findViewById(R.id.btn_ok);
        img = (ImageView) findViewById(R.id.img_warning);

        final TextView counttime=findViewById(R.id.counttime);

        //ALarm
        Uri alarmSound = RingtoneManager. getDefaultUri (RingtoneManager. TYPE_ALARM);
        final MediaPlayer mp = MediaPlayer. create (getApplicationContext(), alarmSound);
        mp.setLooping(true);
        mp.start();
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(Notif. this, default_notification_channel_id )
                        .setSmallIcon(R.drawable. ic_launcher_foreground )
//                        .setContentTitle( "Test" )
//                        .setContentText( "Hello! This is my first push notification" )
                        ;

        NotificationManager mNotificationManager = (NotificationManager)
                getSystemService(Context. NOTIFICATION_SERVICE );
        mNotificationManager.notify(( int ) System. currentTimeMillis () , mBuilder.build());



        final CountDownTimer yourCount = new CountDownTimer(20000,1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                counttime.setText(String.valueOf(counter));
                counter++;
            }
            @Override
            public void onFinish() {
                counttime.setText("PERTOLONGAN AKAN SEGERA DATANG!!");
                mp.stop();
                updateData();
               /* intent = new Intent(Notif.this, MapsActivity.class);
                finish();
                startActivity(intent);*/
            }
        }.start();


//            ObjectAnimator anim = ObjectAnimator.ofInt(txt,"backgroundColor", Color.WHITE, Color.RED, Color.WHITE);
//            anim.setDuration(2000);
//            anim.setEvaluator(new ArgbEvaluator());
//            anim.setRepeatMode(Animation.REVERSE);
//            anim.setRepeatCount(Animation.INFINITE);
//            anim.start();


            btn_oke.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    yourCount.cancel();
                    mp.stop();
                    updateDataok();
                    intent = new Intent(Notif.this, MapsActivity.class);
                    finish();
                    startActivity(intent);



                }
            });
    }

    public void updateData() {
        final String triger = "1";
        final String kondisi = "kecelakaan";

        class UpdateData extends AsyncTask<Void, Void, String> {
            ProgressDialog loading;
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                loading = ProgressDialog.show(Notif.this, "Updating...", "Wait...", false, false);
            }
            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                loading.dismiss();
                Toast.makeText(Notif.this, s, Toast.LENGTH_LONG).show();
            }
            @Override
            protected String doInBackground(Void... params) {
                HashMap<String,String> hashMap = new HashMap<>();
                hashMap.put(Notif.KEY_EMP_ID,id);
                hashMap.put(Notif.KEY_EMP_trigger,triger);
                hashMap.put(Notif.KEY_EMP_kondisi,kondisi);
                RequestHandler rh = new RequestHandler();
                String s = rh.sendPostRequest(Notif.URL_UPDATE_EMP,hashMap);
               /* Toast.makeText(Notif.this, "Update Location Success", Toast.LENGTH_SHORT).show();*/
                return s;
            }
        }
        UpdateData ue = new UpdateData();
        ue.execute();
    }

    public void updateDataok() {
        final String triger = "0";
        final String kondisi = "normal";

        class UpdateDataOK extends AsyncTask<Void, Void, String> {
            ProgressDialog loading;
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                loading = ProgressDialog.show(Notif.this, "Updating...", "Wait...", false, false);
            }
            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                loading.dismiss();
                Toast.makeText(Notif.this, s, Toast.LENGTH_LONG).show();
            }
            @Override
            protected String doInBackground(Void... params) {
                HashMap<String,String> hashMap = new HashMap<>();
                hashMap.put(Notif.KEY_EMP_ID,id);
                hashMap.put(Notif.KEY_EMP_trigger,triger);
                hashMap.put(Notif.KEY_EMP_kondisi,kondisi);
                RequestHandler rh = new RequestHandler();
                String s = rh.sendPostRequest(Notif.URL_UPDATE_EMP,hashMap);
                /* Toast.makeText(Notif.this, "Update Location Success", Toast.LENGTH_SHORT).show();*/
                return s;
            }
        }
        UpdateDataOK ok = new UpdateDataOK();
        ok.execute();
    }

    /*@Override
    public void onBackPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setMessage("Do you want to Exit?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //if user pressed "yes", then he is allowed to exit from application
                finish();
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //if user select "No", just cancel this dialog and continue with app
                dialog.cancel();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }
*/








}
