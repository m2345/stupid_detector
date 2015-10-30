package com.development.buccola.stupiddetector;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

/*
    Author:      Megan Buccola
    Application: Stupid Detector
    Uses:        Media Player, Sensor
    Output:      Media player plays a "beep" sound at a certain speed based on which direction the phone is titled
                 fast beep = title left.  slow beep = title right else medium beep.
    Purpose:     To detect stupid friends and family
 */
public class MainActivity extends ActionBarActivity {
    ImageButton btn;
    final float[] mValuesMagnet      = new float[3];
    final float[] mValuesAccel       = new float[3];
    final float[] mValuesOrientation = new float[3];
    final float[] mRotationMatrix    = new float[9];
    private Handler handler;
    private MediaPlayer mp;
    private boolean on;
    private RelativeLayout rl;
    private Runnable runnable;
    private long lastBeep;
    private TextView notice;

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopAction();
        finish();
    }

    @Override
    public void onStop(){
        super.onStop();
        stopAction();
        finish();
    }

    private void stopAction(){
        mp.stop();
        on = false;
        btn.setImageResource(R.drawable.play);
        rl.setBackgroundColor(Color.BLACK);
        handler.removeCallbacks(runnable);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        on = false;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btn = (ImageButton) findViewById(R.id.btn);
        notice = (TextView) findViewById(R.id.notice);
        rl= (RelativeLayout) findViewById(R.id.layout);
        lastBeep = System.currentTimeMillis();
        setVolumeControlStream(AudioManager.STREAM_MUSIC);  //allow user to control beep audio volume
        SensorManager sensorManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);
        rl.setBackgroundColor(Color.BLACK);
        if(!checkSound())  //check if sound is turned up on device
            notice.setText(getString(R.string.notice));

        final SensorEventListener mEventListener = new SensorEventListener() {
            public void onAccuracyChanged(Sensor sensor, int accuracy) {}
            public void onSensorChanged(SensorEvent event) {
                // Handle the events for which we registered
                switch (event.sensor.getType()) {
                    case Sensor.TYPE_ACCELEROMETER:
                        System.arraycopy(event.values, 0, mValuesAccel, 0, 3);
                        break;
                    case Sensor.TYPE_MAGNETIC_FIELD:
                        System.arraycopy(event.values, 0, mValuesMagnet, 0, 3);
                        break;
                }
            }
        };

        // You have set the event listener up, now just need to register this with the
        // sensor manager along with the sensor wanted.
        setListeners(sensorManager, mEventListener);

        btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (on) {
                    mp.stop();
                    stopAction();
                } else {
                    resetMediaPlayer();
                    beep();
                    on = true;
                    btn.setImageResource(R.drawable.stop);
                }

                handler = new Handler();
                runnable = new Runnable() {
                    public void run() {
                        if (on)
                            start();
                        handler.postDelayed(this, 1); //was 500
                    }
                };

                handler.postDelayed(runnable, 1);

            }
        });
    }

    //check if sound is turned up on device
    //if it is return true
    //else return false and notify the user.
    private boolean checkSound() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) != 0;
    }

    //reset media player
    private void resetMediaPlayer(){
        mp = MediaPlayer.create(this,R.raw.beep);
    }

    //called on button click on repeat
    private void start(){
        if(checkSound())
            notice.setText("");
        else
            notice.setText(getString(R.string.notice));
        SensorManager.getRotationMatrix(mRotationMatrix, null, mValuesAccel, mValuesMagnet);
        SensorManager.getOrientation(mRotationMatrix, mValuesOrientation);
        long temp = System.currentTimeMillis() - lastBeep;
        switch(getCurrentSpeed()) {
            case "s":
               if(temp > 2000)
                    beep();
                rl.setBackgroundColor(Color.GREEN);
                break;
            case "m":
                if (temp > 1000)
                    beep();
                rl.setBackgroundColor(Color.YELLOW);
                break;
            case "f":
            default:
                beep();
                rl.setBackgroundColor(Color.RED);
                break;
        }
    }

    //beep the audio sound and update lastBeep to current time in milliseconds
    private void beep(){
        mp.start();
        lastBeep = System.currentTimeMillis();
    }

    //get current speed of beep based on tilt of phone
    // titled right = slow and titled left = fast
    private String getCurrentSpeed(){
        if(mValuesOrientation[2] >= 0.8) //slow beep
            return "s";
        else if(mValuesOrientation[2] > -0.2 && mValuesOrientation[2] < 0.8) //medium beep
            return "m";
        else //fast beep
             return "f";
    }

    // Register the event listener and sensor type.
    public void setListeners(SensorManager sensorManager, SensorEventListener mEventListener){
        sensorManager.registerListener(mEventListener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(mEventListener, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_help) {
            final Dialog dialog = new Dialog(MainActivity.this);
            dialog.setContentView(R.layout.help);
            dialog.setTitle("About Stupid Detector");
            TextView about = (TextView) dialog.findViewById(R.id.about);
            String info = getString(R.string.about) + "\n" + getString(R.string.line2) + "\n" + getString(R.string.line3);
            about.setText(info);
            (dialog.findViewById(R.id.btnOk)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });
            dialog.show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}