package com.example.yujo.mysabre;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.media.AudioAttributes;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    //SOUND VARIABLES
    private SoundPool soundPool;
    private AudioManager audioManager;
    //maximum streams
    private static final int MAX_STREAMS = 5;
    //stream type
    private static final int streamType = AudioManager.STREAM_MUSIC;
    private boolean loaded;
    private float volume;
    private int soundIdChosenSound;

    //SENSOR VARIABLES
    private SensorManager senSensorManager;
    private Sensor senAccelerometer;
    private long lastUpdate = 0;
    private float last_x, last_y, last_z;
    private static final int SHAKE_THRESHOLD = 300;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SoundSetup();

        LoadSoundFile();

        SensorSetup();
    }

    void SoundSetup() {
        //audioManager to adjust volume
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        //get current volume index of stream type
        float currentVolumeIndex = (float) audioManager.getStreamVolume(streamType);
        //get max volume index of stream type
        float maxVolumeIndex = (float) audioManager.getStreamMaxVolume(streamType);
        //volume from 0 to 1
        this.volume = currentVolumeIndex/maxVolumeIndex;
        //"suggest a stream whose volume should be changed by volume control"
        this.setVolumeControlStream(streamType);

        //different SoundPool setup depending on Android SDK
        if (Build.VERSION.SDK_INT >= 21) {
            AudioAttributes aa = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            SoundPool.Builder builder = new SoundPool.Builder();
            builder.setAudioAttributes(aa).setMaxStreams(MAX_STREAMS);

            this.soundPool = builder.build();
        } else {
            this.soundPool = new SoundPool(MAX_STREAMS, AudioManager.STREAM_MUSIC, 0);
        }

        //when SoundPool load complete
        this.soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                loaded = true;
            }
        });
    }

    void LoadSoundFile() {
        //load sound file into SoundPool
        this.soundIdChosenSound = this.soundPool.load(this, R.raw.whoo, 1);
    }

    void SensorSetup() {
        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        senSensorManager.registerListener(this, senAccelerometer, senSensorManager.SENSOR_DELAY_NORMAL);
    }

    //override onPause so device unregisters the sensor when app hibernates
    protected void OnPause() {
        super.onPause();
        senSensorManager.unregisterListener(this);
    }

    //when app resumes register sensor again
    protected void OnResume() {
        super.onResume();
        senSensorManager.registerListener(this, senAccelerometer, senSensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor mySensor = event.sensor;

        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            //current values
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            long curTime = System.currentTimeMillis();

            //if 100 milliseconds have passed since last update
            if ((curTime - lastUpdate) > 100) {
                long diffTime = curTime - lastUpdate;
                lastUpdate = curTime;

                float speed = Math.abs(x + y + z - last_x - last_y - last_z)/ diffTime * 10000;

                //if speed higher than threshold, play sound
                if (speed > SHAKE_THRESHOLD && loaded) {
                    TextView text = (TextView)findViewById(R.id.pass);
                    text.setText("play whoo");

                    //play sound
                    float leftVolume = volume;
                    float rightVolume = volume;
                    int streamId = this.soundPool.play(this.soundIdChosenSound, leftVolume, rightVolume, 1, 0, 1f);
                }

                last_x = x;
                last_y = y;
                last_z = z;
            }
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

}