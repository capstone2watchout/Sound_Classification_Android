package com.example.tensorflow_sound_classification;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class Model {

    private static String TAG = "TF_Model";

    private static final String LABEL_PATH = "labels.txt";
    private static final String MODEL_PATH = "frozen_model.tflite";

    private Interpreter tflite;

    private List<String> labelList = null;
    private float labelProbArray[][] = new float[1][30];


    Model(Activity activity) throws IOException {
        tflite = new Interpreter(loadModelFile(activity, MODEL_PATH));
        labelList = loadLabelList(activity);
    }


    public void runModel(float[] inp) {

        tflite.run(inp, labelProbArray);

        String m = null;
        float max = 0;
        for (int i=0; i<labelList.size(); i++) {
           if (max < labelProbArray[0][i]) {
               max = labelProbArray[0][i];
               m = labelList.get(i);
           }
        }
        System.out.println(m + ":" + max);

    }
    private MappedByteBuffer loadModelFile(Activity activity, String modelPath) throws IOException {
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private List<String> loadLabelList(Activity activity) throws IOException {
        List<String> labelList = new ArrayList<String>();
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(activity.getAssets().open(LABEL_PATH)));
        String line;
        while ((line = reader.readLine()) != null) {
            labelList.add(line);
        }
        reader.close();
        return labelList;
    }

    public void close() {
        tflite.close();
        tflite = null;
    }
}
