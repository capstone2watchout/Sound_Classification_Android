package com.example.tensorflow_sound_classification;

import android.content.pm.PackageManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.mfcc.MFCC;

import static android.media.AudioFormat.CHANNEL_IN_MONO;
import static android.media.AudioFormat.ENCODING_PCM_16BIT;

public class MainActivity extends AppCompatActivity {

    private static String TAG = "MainActivity";

    private static final int SAMPLING_RATE = 16000;
    private static final int FRAME_SIZE = 800; // 20 msec
    private static final int FRAME_OVERLAP = 400;  // 10 msec
    private static final int FEATURE_PER_FRAME = 40; // 40 Frame -> 1 feature input
    private static final int NUM_MEL_FILTER_BANK = 40;  // 40 dimension Filter bank
    private static final int MFCC_PER_FRAME = 40;   // only for mfcc
    static final float LOWER_FILTER_FREQ = 25.0f;
    static final float UPPER_FILTER_FREQ = SAMPLING_RATE / 2;

    private static final int REQUEST_RECORD_AUDIO = 13;

    private Model model = null;

    public int currentFrame = 0;
    float [] inp = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestMicrophonePermission();

        final MFCC mfcc = new MFCC(FRAME_SIZE, SAMPLING_RATE, MFCC_PER_FRAME, NUM_MEL_FILTER_BANK, LOWER_FILTER_FREQ, UPPER_FILTER_FREQ);
        inp = new float[FEATURE_PER_FRAME * NUM_MEL_FILTER_BANK];


        try {
            model = new Model(MainActivity.this);
        } catch (IOException e) {
            Log.e(TAG, "Fail load model");
        }

        AudioDispatcher dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(SAMPLING_RATE,FRAME_SIZE ,0);

        dispatcher.addAudioProcessor(new AudioProcessor() {
            @Override
            public boolean process(AudioEvent audioEvent) {
                float [] audioFloatBuffer = audioEvent.getFloatBuffer().clone();
                //Log.d("AudioData", "Length: " + audioFloatBuffer.length + " Data: " + Arrays.toString(audioFloatBuffer));
                float bin[] = mfcc.magnitudeSpectrum(audioFloatBuffer);
                //Log.d("Spectrum", "Length: " + bin.length + " Data: " + Arrays.toString(bin));
                float fbank[] = mfcc.melFilter(bin, mfcc.getCenterFrequencies());
                //Log.d("MelFBank", "Length: " + fbank.length + " Data: " + Arrays.toString(fbank));
                //float f[] = mfcc.nonLinearTransformation(fbank).clone();
                //Log.d("FbankLinear", "Length: " + f.length + " Data: " + Arrays.toString(f));
                //float m[] = mfcc.cepCoefficients(f);
                //Log.d("MFCC", "Length: " + m.length + " Data: " + Arrays.toString(m));
                System.arraycopy(fbank, 0, inp, currentFrame * NUM_MEL_FILTER_BANK, NUM_MEL_FILTER_BANK);

                currentFrame++;

                if (currentFrame == FEATURE_PER_FRAME && model != null) {
                    Log.d("MelFBank", "Length: " + inp.length + " Data: " + Arrays.toString(inp));
                    currentFrame = 0;
                    model.runModel(inp);
                    Arrays.fill(inp, 0);
                }

                return false;
            }

            @Override
            public void processingFinished() {
                currentFrame++;
            }
        });

        Thread audioThread = new Thread(dispatcher, "Audio Thread");

        audioThread.start();

    }

    private void requestMicrophonePermission() {
        requestPermissions(
                new String[] {android.Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_RECORD_AUDIO
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        }
    }
}
