package main.java.com.reactlibrary;

import android.content.Context;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Message;
import android.support.v7.appcompat.R;
import android.text.Html;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;

public class SnowboyModule extends ReactContextBaseJavaModule {

    //snowboy
    private Button record_button;
    private Button play_button;
    private TextView log;
    private ScrollView logView;
    static String strLog = null;

    private Callback callbackInvoker;
    
    private int preVolume = -1;
    private static long activeTimes = 0;

    private main.java.com.reactlibrary.audio.RecordingThread recordingThread;
    private main.java.com.reactlibrary.audio.PlaybackThread playbackThread;
    //snowboy
    private final ReactApplicationContext reactContext;

    public SnowboyModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;

        setProperVolume();
        main.java.com.reactlibrary.AppResCopy.copyResFromAssetsToSD(getReactApplicationContext());
    }

    @Override
    public String getName() {
        return "Snowboy";
    }
    boolean recording =  false;
    @ReactMethod
    public void start(Callback callback) {
        // TODO: Implement some actually useful functionality
        Toast.makeText(getReactApplicationContext(), "Testing...", Toast.LENGTH_LONG).show();
        callbackInvoker = callback;
        if(recording) {
            stopPlayback();
            sleep();
            startRecording();
            recording = !recording;
        } else {
            stopRecording();
            sleep();
            recording = !recording;
        }
    }

    private void setMaxVolume() {
        AudioManager audioManager = (AudioManager) reactContext.getSystemService(Context.AUDIO_SERVICE);
        preVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        updateLog(" ----> preVolume = "+preVolume, "green");
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        updateLog(" ----> maxVolume = "+maxVolume, "green");
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0);
        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        updateLog(" ----> currentVolume = "+currentVolume, "green");
    }

    private void setProperVolume() {
        AudioManager audioManager = (AudioManager) reactContext.getSystemService(Context.AUDIO_SERVICE);
        preVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        updateLog(" ----> preVolume = "+preVolume, "green");
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        updateLog(" ----> maxVolume = "+maxVolume, "green");
        int properVolume = (int) ((float) maxVolume * 0.2);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, properVolume, 0);
        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        updateLog(" ----> currentVolume = "+currentVolume, "green");
    }
    private void restoreVolume() {
        if(preVolume>=0) {
            AudioManager audioManager = (AudioManager) reactContext.getSystemService(Context.AUDIO_SERVICE);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, preVolume, 0);
            updateLog(" ----> set preVolume = "+preVolume, "green");
            int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            updateLog(" ----> currentVolume = "+currentVolume, "green");
        }
    }

    private void startRecording() {
        recordingThread.startRecording();
        updateLog(" ----> recording started ...", "green");
    }

    private void stopRecording() {
        recordingThread.stopRecording();
        updateLog(" ----> recording stopped ", "green");
    }

    private void startPlayback() {
        updateLog(" ----> playback started ...", "green");
        // (new PcmPlayer()).playPCM();
        playbackThread.startPlayback();
    }

    private void stopPlayback() {
        updateLog(" ----> playback stopped ", "green");
        playbackThread.stopPlayback();
    }

    private void sleep() {
        try { Thread.sleep(500);
        } catch (Exception e) {}
    }

    void showToast(CharSequence msg) {
        Toast.makeText(getReactApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    public Handler handle = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            MsgEnum message = MsgEnum.getMsgEnum(msg.what);
            switch(message) {
                case MSG_ACTIVE:
                    activeTimes++;
                    updateLog(" ----> Detected " + activeTimes + " times", "green");
                    // Toast.makeText(Demo.this, "Active "+activeTimes, Toast.LENGTH_SHORT).show();
                    callbackInvoker.invoke(" ----> Detected " + activeTimes + " times", "green");
                    showToast("Active "+activeTimes);
                    break;
                case MSG_INFO:
                    updateLog(" ----> "+message);
                    break;
                case MSG_VAD_SPEECH:
                    updateLog(" ----> normal voice", "blue");
                    break;
                case MSG_VAD_NOSPEECH:
                    updateLog(" ----> no speech", "blue");
                    break;
                case MSG_ERROR:
                    updateLog(" ----> " + msg.toString(), "red");
                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    };

    public void updateLog(final String text) {

        log.post(new Runnable() {
            @Override
            public void run() {
                if (currLogLineNum >= MAX_LOG_LINE_NUM) {
                    int st = strLog.indexOf("<br>");
                    strLog = strLog.substring(st+4);
                } else {
                    currLogLineNum++;
                }
                String str = "<font color='white'>"+text+"</font>"+"<br>";
                strLog = (strLog == null || strLog.length() == 0) ? str : strLog + str;
                log.setText(Html.fromHtml(strLog));
            }
        });
        logView.post(new Runnable() {
            @Override
            public void run() {
                logView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }

    static int MAX_LOG_LINE_NUM = 200;
    static int currLogLineNum = 0;

    public void updateLog(final String text, final String color) {
        log.post(new Runnable() {
            @Override
            public void run() {
                if(currLogLineNum>=MAX_LOG_LINE_NUM) {
                    int st = strLog.indexOf("<br>");
                    strLog = strLog.substring(st+4);
                } else {
                    currLogLineNum++;
                }
                String str = "<font color='"+color+"'>"+text+"</font>"+"<br>";
                strLog = (strLog == null || strLog.length() == 0) ? str : strLog + str;
                log.setText(Html.fromHtml(strLog));
            }
        });
        logView.post(new Runnable() {
            @Override
            public void run() {
                logView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }

    private void emptyLog() {
        strLog = null;
        log.setText("");
    }

}
