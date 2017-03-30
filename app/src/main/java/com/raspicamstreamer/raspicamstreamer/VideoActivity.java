package com.raspicamstreamer.raspicamstreamer;

import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.TextView;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;



public class VideoActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener{

    public final static String CAMERA = "camera";
    private DecoderThread decoderThread = null;
    private TextView messageView;
    private String m_strCamera;

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
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (decoderThread != null)
            decoderThread.setUnpaused();
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height)
    {
        if (decoderThread == null)
        {
            decoderThread = new DecoderThread();
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
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
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

