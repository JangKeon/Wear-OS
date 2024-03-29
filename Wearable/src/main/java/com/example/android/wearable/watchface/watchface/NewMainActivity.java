package com.example.android.wearable.watchface.watchface;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.example.android.wearable.watchface.R;
import org.json.JSONException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;

public class NewMainActivity extends Activity {
    String jsonInput = "";
    TextView userText;
    TextView hourMinuteText;
    TextView secondText;
    TextView monthDayText;
    TextView amPmText;

    TextView heartrate;
    public float hearttemp=0,locationtemp=0,steptemp=0;
    UserInfo userInfo;
    JsonParser jsonParser;
    String name;
    String group;
    String birthday;
    String skin;
    String protective;
    String maxHeartRate;

    private LocationListener locationListener;
    private LocationManager locationManager;
    Time time;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_newmainpage);
        bindService(new Intent(NewMainActivity.this, BackService.class),mConnection, Context.BIND_AUTO_CREATE);

        /* Text Views */
        userText = (TextView) findViewById(R.id.Name);
        hourMinuteText = (TextView) findViewById(R.id.HourMinute);
        secondText = (TextView) findViewById(R.id.Second);
        monthDayText = (TextView) findViewById(R.id.MonthDay);
        amPmText = (TextView) findViewById(R.id.AMPM);

        /* Threads */
        time = new Time();
        RequestThread requestThread = new RequestThread();
        requestThread.start();
        TimeThread timeThread = new TimeThread();
        timeThread.start();

        /* input parsing */
        jsonParser = new JsonParser();
        userInfo = new UserInfo();
        // default value -- can't be connected network
        name = userInfo.getName();
        group = userInfo.getGroup();
        birthday = userInfo.getBirthday();
        skin = userInfo.getSkin();
        protective = userInfo.getProtective();
        maxHeartRate = userInfo.getMaxHeartRate();
        updateInfo();
        }
        public void LocationFind(){
            locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            locationListener = new LocationListener() {

                @Override
                public void onLocationChanged(Location location) {

                    Log.d("dd",String.valueOf(Math.round(location.getLatitude()*100)/100));
                    //sendMsgToActivity(Math.round(location.getLongitude()*100)/100 ,"LANGI");
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
            configureButton();

        }
    public void configureButton() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationManager.requestLocationUpdates("gps", 5000, 0, locationListener);

    }
    private Messenger mServiceMessenger =null;
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.d("test","onServiceConnected!!!!!!!!1");
            mServiceMessenger = new Messenger(iBinder);
            try {
                Message msg = Message.obtain(null, BackService.MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                Log.d("test","onServiceConnected!!!!!!!!1!1111");

                mServiceMessenger.send(msg);
            }
            catch (RemoteException e) {
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
        }
    };
    private final Messenger mMessenger = new Messenger(new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if(msg.getData().getFloat("HEART")!=0){
                hearttemp = msg.getData().getFloat("HEART");}

            if(msg.getData().getFloat("STEP")!=0){
                steptemp = msg.getData().getFloat("STEP");}
            return false;
        }
    }));
    @Override
    protected void onStart() {
        super.onStart();
        updateInfo();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    public void updateInfo(){
        try { // Parsing and Store
            userInfo = jsonParser.getUserInfo(jsonInput); // 유저 정보 객체
            // update
            name = userInfo.getName();
            group = userInfo.getGroup();
            birthday = userInfo.getBirthday();
            skin = userInfo.getSkin();
            protective = userInfo.getProtective();
            maxHeartRate = userInfo.getMaxHeartRate();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        userText.setText(name);
    }

    @SuppressLint("SetTextI18n")
    public void setTime(){
        time.setCalendar();
        String newMonth = time.transformMonth();
        String newWeek = time.transformWeek();
        String newHour = time.transformHour();
        String newMin = time.transformMin();
        monthDayText.setText(newMonth + " " + time.getDay() + " " + newWeek);
        hourMinuteText.setText(newHour + ":" + newMin);
        secondText.setText(String.valueOf(time.getSecond()));
        if(time.getAmPm() == 0)
            amPmText.setText("AM");
        else
            amPmText.setText("PM");
    }

    class TimeThread extends Thread{
        @Override
        public void run() {
            while(!isInterrupted()){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setTime();
                        heartrate=findViewById(R.id.HeartRateValue);
                        heartrate.setText(String.valueOf((int)hearttemp));
                        TextView step=findViewById(R.id.StepValue);
                        step.setText(String.valueOf((int)steptemp));
                        double distancetemp=steptemp*0.5;
                        TextView distance=findViewById(R.id.DistanceValue);
                        distance.setText((int)distancetemp+"m");
                        double calorytemp=Math.round((steptemp*388/10000)*100)/100;
                        TextView calory=findViewById(R.id.CaloriesValue);
                        calory.setText(String.valueOf((int)calorytemp));
                        LocationFind();
                        //secondText.setText(second);
                    }
                });
                try{
                    Thread.sleep(1000);
                } catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
        }
    }

    class RequestThread extends Thread {
        public String urlStr = "http://15.164.45.229:8888/users/MDg6OTc6OTg6MEU6RTY6REE=";
        Handler handler = new Handler();

        @Override
        public void run() {
            try {
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                if (conn != null) {
                    conn.setConnectTimeout(10000); // 10초 동안 기다린 후 응답이 없으면 종료
                    conn.setRequestMethod("GET");
                    conn.setDoInput(true);

                    int resCode = conn.getResponseCode();

                    if (resCode == HttpURLConnection.HTTP_OK) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        String line = null;
                        while (true) {
                            line = reader.readLine();
                            if (line == null)
                                break;
                            println(line);
                        }
                        reader.close();
                    }
                    conn.disconnect();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void println(final String data) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    String temp = "";
                    int temp2 = 0;
                    try {
                        for (int i = 0; i < data.length(); i++) {
                            if (data.charAt(i) == ':')
                                temp2 = i;
                        }
                        temp = AES256s.decryptToString(data.substring(temp2 + 2, data.length() - 2), "08:97:98:0E:E6:DA");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    jsonInput = temp;
                }
            });
        }
    }
}