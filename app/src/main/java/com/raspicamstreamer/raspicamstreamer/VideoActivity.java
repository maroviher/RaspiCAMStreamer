package com.raspicamstreamer.raspicamstreamer;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import java.io.InputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.util.ArrayList;
import android.view.WindowManager;


public class VideoActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener, SeekBar.OnSeekBarChangeListener{

    public final static String CAMERA = "camera";
    private DecoderThread decoderThread = null;
    private String m_strCamera;
    private ArrayList<View> views_to_fade = new ArrayList<>();
    private TextView textViewSS, messageView, textViewISO, textViewFPS;
    private SeekBar seekBar_iso, seekBar_ss;
    private Button button_SS, button_move_up, button_move_down, button_zoom_reset, button_move_left,
            button_move_right, button_mot, button_zoom_in, button_zoom_out;
    private int[] iso_map = {0, 100, 200, 400, 800, 1600};
    private boolean m_bRunning = true;
    private Runnable fpsRunnable;

    @Override
    public void onStart()
    {
        super.onStart();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (decoderThread != null)
            decoderThread.setPaused();
        m_bRunning = false;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (decoderThread != null)
            decoderThread.setUnpaused();
        m_bRunning = true;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height)
    {
        if (decoderThread == null)
        {
            decoderThread = new DecoderThread(this);
            decoderThread.setSurface(new Surface(surfaceTexture));
            decoderThread.start();
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height)
    {
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture)
    {
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture)
    {
    }

    protected String getStringExtra(Bundle savedInstanceState, String id) {
        String l = (savedInstanceState == null) ? null : (String) savedInstanceState.getSerializable(id);
        if (l == null) {
            Bundle extras = getIntent().getExtras();
            l = extras != null ? extras.getString(id) : null;
        }
        return l;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_video);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        messageView = (TextView) findViewById(R.id.video_message);
        m_strCamera = getStringExtra(savedInstanceState, CAMERA);

        TextureView textureView = (TextureView)findViewById(R.id.video_surface);
        textureView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        textureView.setSurfaceTextureListener(this);

        views_to_fade.add(button_zoom_in = (Button)findViewById(R.id.button_zoom_in));
        button_zoom_in.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                decoderThread.ZoomIN();
            }
        });
        views_to_fade.add(button_zoom_out = (Button)findViewById(R.id.button_zoom_out));
        button_zoom_out.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                decoderThread.ZoomOUT();
            }
        });
        views_to_fade.add(button_zoom_reset = (Button)findViewById(R.id.button_zoom_reset));
        button_zoom_reset.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                decoderThread.Move('R');
            }
        });
        views_to_fade.add(button_move_left = (Button)findViewById(R.id.button_move_left));
        button_move_left.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                decoderThread.Move('l');
            }
        });
        views_to_fade.add(button_move_right = (Button)findViewById(R.id.button_move_right));
        button_move_right.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                decoderThread.Move('r');
            }
        });
        views_to_fade.add(button_move_up = (Button)findViewById(R.id.button_move_up));
        button_move_up.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                decoderThread.Move('u');
            }
        });
        views_to_fade.add(button_move_down = (Button)findViewById(R.id.button_move_down));
        button_move_down.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                decoderThread.Move('d');
            }
        });

        views_to_fade.add(textViewISO = (TextView)findViewById(R.id.iso));
        textViewISO.setVisibility(View.VISIBLE);

        views_to_fade.add(seekBar_iso = (SeekBar)findViewById(R.id.seekBar_iso));
        seekBar_iso.setOnSeekBarChangeListener(this);

        views_to_fade.add(seekBar_ss = (SeekBar)findViewById(R.id.seekBar_ss));
        seekBar_ss.setOnSeekBarChangeListener(this);

        views_to_fade.add(textViewSS = (TextView)findViewById(R.id.shutter_speed));
        views_to_fade.add(textViewFPS = (TextView)findViewById(R.id.frames_cnt));

        views_to_fade.add(button_SS = (Button)findViewById(R.id.button_ss));
        button_SS.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String bCap = button_SS.getText().toString();
                if(bCap.equals("1"))
                    button_SS.setText("10");
                else if(bCap.equals("10"))
                    button_SS.setText("100");
                else
                    button_SS.setText("1");
            }
        });
        views_to_fade.add(button_mot = (Button)findViewById(R.id.button_motion));
        button_mot.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String bCap = button_mot.getText().toString();
                if(bCap.equals("M=0")) {
                    button_mot.setText("M=1");
                    decoderThread.SetMotion(1);
                }
                else {
                    button_mot.setText("M=0");
                    decoderThread.SetMotion(0);
                }
            }
        });

        textureView = (TextureView)findViewById(R.id.video_surface);

        //example how to determine screen size
        /*DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager windowmanager = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        windowmanager.getDefaultDisplay().getMetrics(displayMetrics);
        Log.d("ads", String.format("%d:%d", displayMetrics.widthPixels, displayMetrics.heightPixels));*/

        //textureView.setLayoutParams(new FrameLayout.LayoutParams(1440, 1080, Gravity.CENTER));//fullscreen 4:3 for -w 1296 -h 972, full FOV of raspi camera
        textureView.setLayoutParams(new FrameLayout.LayoutParams(1920, 1080, Gravity.CENTER));//fullscreen 16:9
        textureView.setSurfaceTextureListener(this);
        //textureView.setZoomRange(MIN_ZOOM, MAX_ZOOM);
        textureView.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent e)
            {
                switch (e.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        int iAction;
                        if(View.VISIBLE == views_to_fade.get(0).getVisibility()) {
                            iAction = View.GONE;
                            if (decoderThread != null)
                                decoderThread.ShowFps(false);
                        }
                        else {
                            if (decoderThread != null)
                                decoderThread.ShowFps(true);
                            iAction = View.VISIBLE;
                        }

                        for (View view : views_to_fade) {
                            view.setVisibility(iAction);
                        }
                        break;
                }
                return false;
            }
        });
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
    }

    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
    {
        switch(seekBar.getId())
        {
            case R.id.seekBar_iso:
                final int iso_seek = progress;
                runOnUiThread(new Runnable()
                {
                    public void run()
                    {
                        textViewISO.setText(String.format("ISO=%d", iso_map[iso_seek]));
                        if(decoderThread != null)
                        {
                            decoderThread.SetISO(iso_map[iso_seek]);
                        }
                    }
                });
                break;
            case R.id.seekBar_ss:
                final int ss_seek = progress * Integer.parseInt(button_SS.getText().toString());
                runOnUiThread(new Runnable()
                {
                    public void run()
                    {
                        textViewSS.setText(String.format("ss=%d", ss_seek));
                        if(decoderThread != null)
                        {
                            decoderThread.SetSS(ss_seek);
                        }
                    }
                });
                break;
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    private class DecoderThread extends Thread
    {
        private final static int BUFFER_TIMEOUT = 100000;
        private final static int TCPIP_BUFFER_SIZE = 2000000;

        private MediaCodec mediaCodec = null;
        private MediaFormat outputFormat;
        private Surface surface;
        private byte[] buffer = null;

        private Socket socket = null;
        private InputStream inputStream = null;

        private final Object mPauseLock = new Object();
        private boolean mPaused = false;

        private AppCompatActivity m_act;

        private PrintWriter sockPrintWriter = null;

        DecoderThread(AppCompatActivity act)
        {
            m_act = act;
        }
        private int m_iFramesCnt = 0;
        private boolean m_bShowFps = true;

        public void ShowFps(boolean bShow)
        {
            m_bShowFps = bShow;
        }

        public void SetISO(int iISO)
        {
            if(socket != null && socket.isConnected())
                sockPrintWriter.println(String.format("iso=%d", iISO));
        }

        public void SetSS(int iSS)
        {
            if(socket != null && socket.isConnected())
                sockPrintWriter.println(String.format("ss=%d", iSS));
        }

        public void ZoomIN()
        {
            if(socket != null && socket.isConnected())
                sockPrintWriter.println("move=i");
        }

        public void ZoomOUT()
        {
            if(socket != null && socket.isConnected())
                sockPrintWriter.println("move=o");
        }

        public void SetMotion(int i)
        {
            if(socket != null && socket.isConnected())
                sockPrintWriter.println(String.format("motion=%d", i));
        }

        public void Move(char ch)
        {
            if(socket != null && socket.isConnected())
                sockPrintWriter.println(String.format("move=%c", ch));
        }

        public void setPaused() {
            synchronized (mPauseLock) {
                mPaused = true;
            }
        }

        public void setUnpaused() {
            synchronized (mPauseLock) {
                mPaused = false;
                mPauseLock.notifyAll();
            }
        }

        public void setSurface(Surface surface)
        {
            this.surface = surface;
        }

        private int byteArrayToInt(byte[] b)
        {
            return   b[0] & 0xFF |
                    (b[1] & 0xFF) << 8 |
                    (b[2] & 0xFF) << 16 |
                    (b[3] & 0xFF) << 24;
        }

        private Boolean read_exact(byte[] buffer, int cnt)
        {
            if(inputStream == null)
                return false;
            int iToRead = cnt;
            int offset = 0;
            try
            {
                int read;
                while(iToRead > 0)
                {
                    read = inputStream.read(buffer, offset, iToRead);
                    if(read < 1)
                        throw new Exception("qwer");
                    iToRead -= read;
                    offset += read;
                }
                return true;
            }
            catch (Exception ex)
            {
                setMessage(String.format("read_exact(cnt=%d, offset=%d, iToRead=%d)", cnt, offset, iToRead));
                return false;
            }
        }

        @Override
        public void run() {
            int iSemPos = m_strCamera.lastIndexOf(":");
            if(-1 == iSemPos) {
                setMessage(String.format("'%s' is a bad camera name. Use something like 'mycam.com:1234'", m_strCamera));
                return;
            }
            String strHost = m_strCamera.substring(0, iSemPos);
            int iPort = 0;

            String strPort = m_strCamera.substring(iSemPos+1);
            try {
                iPort = Integer.parseInt(strPort);
            }
            catch(java.lang.NumberFormatException ex)
            {
                setMessage(String.format("'%s' is an invalid port", strPort));
                return;
            }

            boolean bInterrupted = false;
            while (!bInterrupted) {
                long presentationTime = System.nanoTime() / 1000;
                try {
                    InetSocketAddress socketAddress = new InetSocketAddress(strHost, iPort);
                    boolean bConnected = false;
                    int attempts = 0;
                    while (!bConnected) {
                        try {
                            synchronized (mPauseLock) {
                                while (mPaused) {
                                    try {
                                        mPauseLock.wait();
                                    } catch (InterruptedException e) {
                                    }
                                }
                            }
                            setMessage(String.format("Connecting(%d) to %s:%d...", attempts++, strHost, iPort));
                            socket = new Socket();
                            socket.connect(socketAddress, 3000);
                            inputStream = socket.getInputStream();
                            sockPrintWriter = new PrintWriter(socket.getOutputStream(), true);
                            bConnected = true;
                        } catch (Exception ex) {
                            sleep(3000, 0);
                            socket = null;
                            if (Thread.interrupted())
                                return;
                        }
                    }

                    buffer = new byte[TCPIP_BUFFER_SIZE];

                    outputFormat = MediaFormat.createVideoFormat("video/avc", 1920, 1080);
                    mediaCodec = MediaCodec.createDecoderByType("video/avc");
                    mediaCodec.configure(outputFormat, surface, null, 0);
                    mediaCodec.start();

                    //read and set SPS, PPS
                    for (int i = 0; i < 2; i++) {
                        if (!read_exact(buffer, 4))
                            throw new Exception("read_exact");

                        int sps_or_pps_len = byteArrayToInt(buffer);
                        if (!read_exact(buffer, sps_or_pps_len))
                            throw new Exception("read_exact");

                        int inputBufferId = mediaCodec.dequeueInputBuffer(BUFFER_TIMEOUT);
                        if (inputBufferId >= 0) {
                            ByteBuffer inputBuffer = mediaCodec.getInputBuffer(inputBufferId);
                            inputBuffer.put(buffer, 0, sps_or_pps_len);
                            mediaCodec.queueInputBuffer(inputBufferId, 0, sps_or_pps_len, presentationTime, MediaCodec.BUFFER_FLAG_CODEC_CONFIG);
                            presentationTime += 666666;
                        } else {
                            setMessage(String.format("codec.dequeueInputBuffer=%d", inputBufferId));
                            return;
                        }
                    }

                    MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                    int outputBufferId = mediaCodec.dequeueOutputBuffer(info, BUFFER_TIMEOUT);
                    if (outputBufferId >= 0) {
                        mediaCodec.releaseOutputBuffer(outputBufferId, true);
                    } else if (outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        // Subsequent data will conform to new format.
                        // Can ignore if using getOutputFormat(outputBufferId)
                        outputFormat = mediaCodec.getOutputFormat(); // option B
                    }

                    hideMessage();
                    m_iFramesCnt = 0;
                    long timePrevFpsShow = System.currentTimeMillis();
                    int iFramesSinceLastFpsShow = 1;
                    while (!(bInterrupted = Thread.interrupted())) {
                        if (!read_exact(buffer, 4))
                            throw new Exception("read_exact");

                        int iFrameLen = byteArrayToInt(buffer);
                        if (iFrameLen > TCPIP_BUFFER_SIZE) {
                            setMessage(String.format("iFrameLen=%d", iFrameLen));
                            return;
                        }

                        int inputBufferId = mediaCodec.dequeueInputBuffer(BUFFER_TIMEOUT);
                        if (inputBufferId >= 0) {
                            ByteBuffer inputBuffer = mediaCodec.getInputBuffer(inputBufferId);
                            if (!read_exact(buffer, iFrameLen))
                                throw new Exception("read_exact");
                            inputBuffer.put(buffer, 0, iFrameLen);
                            mediaCodec.queueInputBuffer(inputBufferId, 0, iFrameLen, presentationTime, 0);
                            presentationTime += 666666;
                        } else {
                            setMessage(String.format("codec.dequeueInputBuffer=%d", inputBufferId));
                            return;
                        }
                        outputBufferId = mediaCodec.dequeueOutputBuffer(info, BUFFER_TIMEOUT);
                        if (outputBufferId >= 0) {
                            mediaCodec.releaseOutputBuffer(outputBufferId, true);
                        } else if (outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                            // Subsequent data will conform to new format.
                            // Can ignore if using getOutputFormat(outputBufferId)
                            outputFormat = mediaCodec.getOutputFormat(); // option B
                        }
                        m_iFramesCnt++;

                        if(m_bShowFps) {
                            long timeNow = System.currentTimeMillis();
                            final long timeDelta = timeNow - timePrevFpsShow;
                            if (timeDelta > 500) {
                                final double fFPS = iFramesSinceLastFpsShow * 1000.0 / timeDelta;
                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        String strTmp;
                                        if (fFPS < 5)
                                            strTmp = String.format("%d, %.1f", m_iFramesCnt, fFPS);
                                        else
                                            strTmp = String.format("%d, %d", m_iFramesCnt, Math.round(fFPS));

                                        textViewFPS.setText(strTmp);
                                    }
                                });
                                timePrevFpsShow = timeNow;
                                iFramesSinceLastFpsShow = 1;
                            } else {
                                iFramesSinceLastFpsShow++;
                            }
                        }

                    }
                } catch (Exception ex) {
                }

                try {
                    if (inputStream != null) {
                        inputStream.close();
                        inputStream = null;
                    }
                    if (socket != null) {
                        socket.close();
                        socket = null;
                    }
                } catch (Exception ex) { }

                if (mediaCodec != null) {
                    try {
                        mediaCodec.release();
                    } catch (Exception ex) {
                    }
                    mediaCodec = null;
                }
            }//while (!bInterrupted) {
        }//public void run() {

        private void hideMessage()
        {
            runOnUiThread(new Runnable()
            {
                public void run()
                {
                    messageView.setVisibility(View.GONE);
                }
            });
        }
        private void setMessage(final String str)
        {
            runOnUiThread(new Runnable()
            {
                public void run()
                {
                    messageView.setText(str);
                    messageView.setVisibility(View.VISIBLE);
                }
            });
        }
    }
}

