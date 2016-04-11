package com.silent_disco;

import android.app.Activity;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.freedesktop.gstreamer.GStreamer;

import java.net.Inet4Address;
import java.net.InetAddress;

public class SilentDisco extends Activity implements NsdHelper.HelperListener {
    private static final String TAG = "SilentDisco";
    private NsdHelper mNsdHelper;

    private native void nativeInit(); // Initialize native code, build pipeline, etc
    private native void nativeFinalize(); // Destroy pipeline and shutdown native code
    private native void nativePlay(); // Set pipeline to PLAYING
    private native void nativePause(); // Set pipeline to PAUSED
    private native void nativeSetUri(String mediaUri); // Set the URI for playbin
    private static native boolean nativeClassInit(); // Initialize native class: cache Method IDs for callbacks
    private long native_custom_data; // Native code will use this to keep private data

    private boolean is_playing_desired; // Whether the user asked to go to PLAYING

    // Called when the activity is first created.
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Initialize GStreamer and warn if it fails
        try {
            GStreamer.init(this);
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setContentView(com.silent_disco.R.layout.main);

        ImageButton play = (ImageButton) this.findViewById(com.silent_disco.R.id.button_play);
        play.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                is_playing_desired = true;
                nativePlay();
            }
        });

        ImageButton pause = (ImageButton) this.findViewById(com.silent_disco.R.id.button_stop);
        pause.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                is_playing_desired = false;
                nativePause();
            }
        });

        if (savedInstanceState != null) {
            is_playing_desired = savedInstanceState.getBoolean("playing");
            Log.i ("GStreamer", "Activity created. Saved state is playing:" + is_playing_desired);
        } else {
            is_playing_desired = false;
            Log.i ("GStreamer", "Activity created. There is no saved state, playing: false");
        }

        // Start with disabled buttons, until native code is initialized
        this.findViewById(com.silent_disco.R.id.button_play).setEnabled(false);
        this.findViewById(com.silent_disco.R.id.button_stop).setEnabled(false);

        nativeInit();

        mNsdHelper = new NsdHelper(this);
        mNsdHelper.initializeNsd();
        mNsdHelper.registerListener(this);
    }

    protected void onSaveInstanceState(Bundle outState) {
        Log.d ("GStreamer", "Saving state, playing:" + is_playing_desired);
        outState.putBoolean("playing", is_playing_desired);
    }

    // Called from native code. This sets the content of the TextView from the UI thread.
    private void setMessage(final String message) {
        final TextView tv = (TextView) this.findViewById(com.silent_disco.R.id.textview_message);
        runOnUiThread(new Runnable() {
            public void run() {
                tv.setText(message);
            }
        });
    }

    private void setMediaUri(String mediaUri) {
        nativeSetUri (mediaUri);
    }

    // Called from native code. Native code calls this once it has created its pipeline and
    // the main loop is running, so it is ready to accept commands.
    private void onGStreamerInitialized () {
        Log.i("GStreamer", "Gst initialized. Restoring state, playing:" + is_playing_desired);
        // Restore previous playing state
        if (is_playing_desired) {
            nativePlay();
        } else {
            nativePause();
        }

        // Re-enable buttons, now that GStreamer is initialized
        final Activity activity = this;
        runOnUiThread(new Runnable() {
            public void run() {
                activity.findViewById(com.silent_disco.R.id.button_play).setEnabled(true);
                activity.findViewById(com.silent_disco.R.id.button_stop).setEnabled(true);
            }
        });
    }

    static {
        System.loadLibrary("gstreamer_android");
        System.loadLibrary("silent-disco");
        nativeClassInit();
    }

    @Override
    protected void onPause() {
        if (mNsdHelper != null) {
            mNsdHelper.stopDiscovery();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mNsdHelper != null) {
            mNsdHelper.discoverServices();
        }
    }

    @Override
    protected void onDestroy() {
        nativeFinalize();
        mNsdHelper.tearDown();
        super.onDestroy();
    }


    /**
     * HelperListener methods so that we can receive messages from the NsdHelper and tell Gstreamer
     * the uri of the service
     */

    @Override
    public void notifyOnResolved(InetAddress inetAddress, int port) {
        String uri = "rtsp://" + inetAddress.getHostName() + ":" + String.valueOf(port) + "/disco";
        setMediaUri(uri);
    }
}