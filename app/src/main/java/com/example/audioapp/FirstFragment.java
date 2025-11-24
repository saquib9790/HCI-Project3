package com.example.audioapp;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.ArrayMap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;


import com.example.audioapp.entity.SensorData;
import com.example.audioapp.utils.AudioRecorder;
import com.example.audioapp.utils.FileUtil;

import android.provider.MediaStore;
import android.content.ContentValues;
import android.net.Uri;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FirstFragment extends Fragment {
    private TextView motionText = null;

    private SensorManager mSensorManager;
    private TextView mSensorText = null;
    private Sensor mSensor;
    private SensorData mData = new SensorData();

    private SensorManager mSensorManager2;
    private Sensor mSensor2;

    private String[] HEADER = new String[]{"Time", "X-acc", "Y-acc", "Z-acc", "Accuracy", "X-Rot", "Y-Rot", "Z-Rot"};

    private FileWriter csvWriter = null;

    private TextView phrases;
    private Button clickShow;
    private int phrase_type;
    private Button start, pause, play, stop, type, pause2, next, previous, loop, switch_btn;
    private TextView wavplaying;
    private AudioRecorder mAudioRecorder = null;
    MediaPlayer player = new MediaPlayer();
    private SurfaceView mvideo;
    String[] musiclist;
    int listIndex;
    int listLength;

    private Button button_camera;
    private ActivityResultLauncher<Uri> takePictureLauncher;
    private Uri imageUri;

    private SensorEventListener mListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            mData.setX(event.values[0]);
            mData.setY(event.values[1]);
            mData.setZ(event.values[2]);
            updateSensorStateText();
            Log.d("sensor", event.sensor.getName());
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            mData.setAccuracy(accuracy);
            updateSensorStateText();
        }
    };

    private SensorEventListener mListener2 = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            mData.setRotx(event.values[0]);
            mData.setRoty(event.values[1]);
            mData.setRotz(event.values[2]);
            updateSensorStateText();
            Log.d("sensor", event.sensor.getName());
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            mData.setAccuracy(accuracy);
            updateSensorStateText();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        mSensorManager2 = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        mSensor2 = mSensorManager2.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mSensorManager.registerListener(mListener, mSensor, 50000);
        mSensorManager2.registerListener(mListener2, mSensor2, 50000);

        initializeCameraLauncher();
    }

    private class myOnTouchListener implements View.OnTouchListener {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            motionText.setText("Motion: " + event.getAction() + ", " + event.getPressure());
            return true;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("sensor", "sensor destroyed");
        mSensorManager.unregisterListener(mListener);
        mSensorManager2.unregisterListener(mListener2);
        closeCSVWriter();
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_first, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    private void updateSensorStateText() {
        if (null != mSensorText) {
            mSensorText.setText(mData.getText());
            try {
                writeWithCsvBeanWriter();
            } catch (Exception exception) {
                Log.e("FirstFragment_CSV", "Error in updateSensorStateText", exception);
            }
        }
    }

    private void initCSVFile(String fileName) {
        try {
            File file = new File(fileName);
            if (!file.exists()) {
                file.createNewFile();
            }
            csvWriter = new FileWriter(file);

            // Write header manually
            StringBuilder headerBuilder = new StringBuilder();
            for (int i = 0; i < HEADER.length; i++) {
                headerBuilder.append(HEADER[i]);
                if (i < HEADER.length - 1) {
                    headerBuilder.append(",");
                }
            }
            headerBuilder.append("\n");
            csvWriter.write(headerBuilder.toString());
            csvWriter.flush();

        } catch (IOException exception) {
            Log.e("FirstFragment_CSV", "Error initializing CSV file", exception);
        }
    }

    private void writeWithCsvBeanWriter() throws Exception {
        if (csvWriter == null) {
            return;
        }

        SimpleDateFormat formater = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.getDefault());
        String time = formater.format(new Date());

        StringBuilder rowBuilder = new StringBuilder();
        rowBuilder.append(time).append(",");
        rowBuilder.append(mData.getX()).append(",");
        rowBuilder.append(mData.getY()).append(",");
        rowBuilder.append(mData.getZ()).append(",");
        rowBuilder.append(mData.getAccuracy()).append(",");
        rowBuilder.append(mData.getRotx()).append(",");
        rowBuilder.append(mData.getRoty()).append(",");
        rowBuilder.append(mData.getRotz()).append("\n");

        csvWriter.write(rowBuilder.toString());
    }

    private void closeCSVWriter() {
        if (csvWriter != null) {
            try {
                csvWriter.flush();
                csvWriter.close();
                csvWriter = null;
            } catch (IOException e) {
                Log.e("FirstFragment_CSV", "Error closing CSV writer", e);
            }
        }
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.e("111", "onViewCreated");
        FileUtil.setBasePath(getActivity().getExternalFilesDir(null).toString());
        view.setOnTouchListener(new myOnTouchListener());
        motionText = view.findViewById(R.id.textView2);
        mSensorText = view.findViewById(R.id.textview_first);
        mAudioRecorder = AudioRecorder.getInstance();
        start = view.findViewById(R.id.start);
        pause = view.findViewById(R.id.pause);
        pause.setEnabled(false);
        play = view.findViewById(R.id.play);
        stop = view.findViewById(R.id.stop);
        stop.setEnabled(false);
        phrases = view.findViewById(R.id.phrases);
        clickShow = view.findViewById(R.id.click_display);
        phrase_type = 0;
        wavplaying = view.findViewById(R.id.textView);
        mvideo = view.findViewById(R.id.surfaceView);
        type = view.findViewById(R.id.type);
        loop = view.findViewById(R.id.loop);
        pause2 = view.findViewById(R.id.pause2);
        pause2.setEnabled(false);
        next = view.findViewById(R.id.next);
        previous = view.findViewById(R.id.previous);
        mvideo.getHolder().setKeepScreenOn(true);
        musiclist = FileUtil.getWavFilesStrings();
        listIndex = 0;
        listLength = musiclist.length;

        button_camera = view.findViewById(R.id.button_camera);
        button_camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchCamera();
            }
        });

        previous.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listLength == 0) {
                    Toast.makeText(getContext(), "No files available.", Toast.LENGTH_SHORT).show();
                } else {
                    listIndex = (listIndex - 1 + listLength) % listLength;
                    play.performClick();
                    Toast.makeText(getContext(), "Previous", Toast.LENGTH_SHORT).show();
                }
            }
        });

        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listLength == 0) {
                    Toast.makeText(getContext(), "No files available.", Toast.LENGTH_SHORT).show();
                } else {
                    listIndex = (listIndex + 1) % listLength;
                    play.performClick();
                    Toast.makeText(getContext(), "Next", Toast.LENGTH_SHORT).show();
                }
            }
        });

        pause2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (pause2.getText().toString().equals("pause")) {
                    player.pause();
                    pause2.setText("resume");
                    Toast.makeText(getContext(), "Pause", Toast.LENGTH_SHORT).show();
                } else {
                    player.start();
                    pause2.setText("pause");
                    Toast.makeText(getContext(), "Resume", Toast.LENGTH_SHORT).show();
                }
            }
        });

        clickShow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (phrase_type == 0) {
                    phrases.setText("Can I help you?");
                } else if (phrase_type == 1) {
                    phrases.setText("What's your name?");
                } else if (phrase_type == 2) {
                    phrases.setText("Nice to meet you.");
                } else if (phrase_type == 3) {
                    phrases.setText("I need your help.");
                }
                phrase_type = (phrase_type + 1) % 4;
            }
        });

        type.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (type.getText().toString().equals(".wav")) {
                    type.setText(".video");
                    musiclist = FileUtil.getVideoFilesStrings();
                    listLength = musiclist.length;
                    Log.d("file", "onClickvideo: " + musiclist.length);
                    Toast.makeText(getContext(), "Videos selected", Toast.LENGTH_SHORT).show();
                } else {
                    type.setText(".wav");
                    musiclist = FileUtil.getWavFilesStrings();
                    listLength = musiclist.length;
                    Log.d("file", "onClickwav: " + musiclist.length);
                    Toast.makeText(getContext(), "Wav selected", Toast.LENGTH_SHORT).show();
                }
            }
        });

        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listLength == 0) {
                    Toast.makeText(getContext(), "No files available.", Toast.LENGTH_SHORT).show();
                } else {
                    pause2.setEnabled(true);
                    pause2.setText("pause");
                    stop.setEnabled(true);
                    int index = musiclist[listIndex].lastIndexOf("/");
                    String fileName = musiclist[listIndex].substring(index + 1);
                    if (wavplaying == null || wavplaying.getText().toString().isEmpty()) {
                        wavplaying.setText("Playing: " + fileName);
                        try {
                            player.setDisplay(mvideo.getHolder());
                            player.setDataSource(musiclist[listIndex]);
                            Log.d("indexplaying", "onClick: " + listIndex);
                            player.prepare();
                            player.start();
//                            player.setLooping(true);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        player.stop();
                        player.reset();
                        wavplaying.setText("Playing: " + fileName);
                        try {
                            player.setDisplay(mvideo.getHolder());
                            player.setDataSource(musiclist[listIndex]);
                            Log.d("indexplaying", "onClick: " + listIndex);
                            player.prepare();
                            player.start();
//                            player.setLooping(true);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });

        loop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (loop.getText().toString().equals("loop off")) {
                    loop.setText("loop on");

                    Toast.makeText(getContext(), "LOOP ON", Toast.LENGTH_SHORT).show();
                } else {
                    loop.setText("loop off");
                    Toast.makeText(getContext(), "LOOP OFF", Toast.LENGTH_SHORT).show();
                }
            }
        });

        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
//                Log.d("completion", "onCompletion: ");
                if (loop.getText().toString().equals("loop on")) {
                    listIndex -= 1;
                }
                next.performClick();
            }
        });

        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (wavplaying.getText().toString().equals("Stopped") || wavplaying.getText().toString().isEmpty()) {
                } else {
                    player.stop();
                    pause2.setEnabled(false);
                    pause2.setText("pause");
                    stop.setEnabled(false);
                    wavplaying.setText("Stopped");
                    Toast.makeText(getContext(), "Stop", Toast.LENGTH_SHORT).show();
                }
            }
        });

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("encheck", "onclick: " + mAudioRecorder.getStatus());
                try {
                    if (mAudioRecorder.getStatus() == AudioRecorder.Status.STATUS_NO_READY) {
                        Log.d("encheck", "after click1: " + mAudioRecorder.getStatus());
                        pause.setEnabled(true);
                        //
                        String fileName = new SimpleDateFormat("yyyyMMddhhmmss").format(new Date());
                        mAudioRecorder.createDefaultAudio(fileName);
                        mAudioRecorder.startRecord(null);
//                        start.setText("Stop Recording");
                        start.setText("Stop Recording");
//                        pause.setVisibility(View.VISIBLE);
                        initCSVFile(FileUtil.getCSVFileAbsolutePath(fileName));
                        Log.d("encheck", "after click2: " + mAudioRecorder.getStatus());
                    } else {
                        Log.d("encheck", "stop1: " + mAudioRecorder.getStatus());
                        pause.setEnabled(false);
                        // Stop Recording
                        mAudioRecorder.stopRecord();
                        closeCSVWriter();
                        start.setText("Start Recording");
                        pause.setText("Pause Recording");
                        Log.d("encheck", "stop2: " + mAudioRecorder.getStatus());
                    }
                } catch (IllegalStateException e) {
                    Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
        view.findViewById(R.id.btn_first_list).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), ListActivity.class);
                intent.putExtra("type", "wav");
                startActivity(intent);
            }
        });
        view.findViewById(R.id.btn_first_pcm).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), ListActivity.class);
                intent.putExtra("type", "pcm");
                startActivity(intent);
            }
        });
        view.findViewById(R.id.btn_first_json).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), ListActivity.class);
                intent.putExtra("type", "csv");
                startActivity(intent);
            }
        });
        view.findViewById(R.id.btn_videolist).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), ListActivity.class);
                intent.putExtra("type", "mp4");
                startActivity(intent);
            }
        });
        pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("encheck", "click pause: " + mAudioRecorder.getStatus());
                try {
                    if (mAudioRecorder.getStatus() == AudioRecorder.Status.STATUS_START) {
                        //pause
                        mAudioRecorder.pauseRecord();
                        pause.setText("Keep Recording");
                        Log.d("encheck", "after pause: " + mAudioRecorder.getStatus());
                    } else {
                        Log.d("encheck", "click keep: " + mAudioRecorder.getStatus());
                        mAudioRecorder.startRecord(null);
                        pause.setText("Pause Recording");
                        Log.d("encheck", "after keep: " + mAudioRecorder.getStatus());
                    }
                } catch (IllegalStateException e) {
                    Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        verifyPermissions(getActivity());
    }

    private static final int GET_RECODE_AUDIO = 1;

    private static String[] PERMISSION_ALL = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
    };

    /**
     * get the permission of recording
     **/
    public static void verifyPermissions(Activity activity) {
        boolean permission = (ActivityCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
                || (ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                || (ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                || (ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED);;
        if (permission) {
            ActivityCompat.requestPermissions(activity, PERMISSION_ALL,
                    GET_RECODE_AUDIO);
        }
    }


    private void initializeCameraLauncher() {
        takePictureLauncher = registerForActivityResult(new ActivityResultContracts.TakePicture(), isSuccess -> {
            if (isSuccess) {
                Toast.makeText(getContext(), "Photo saved successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Failed to save photo", Toast.LENGTH_SHORT).show();
                // If failed, delete the empty image file
                if (imageUri != null) {
                    getContext().getContentResolver().delete(imageUri, null, null);
                }
            }
        });
    }

    private void launchCamera() {
        String fileName = "HCI_Project_" + System.currentTimeMillis() + ".jpg";

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, fileName);
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
        // This saves the image to the "Pictures/HCI_Project" folder
        values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/HCI_Project");

        imageUri = getContext().getContentResolver().insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
        );

        if (imageUri != null) {
            takePictureLauncher.launch(imageUri);
        } else {
            Toast.makeText(getContext(), "Failed to create image file", Toast.LENGTH_SHORT).show();
        }
    }
}