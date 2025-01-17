package io.agora.service;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceView;

import com.kloudsync.techexcel.R;
import com.kloudsync.techexcel.config.AppConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import io.agora.commom.Constant;
import io.agora.openlive.model.EngineConfig;
import io.agora.rtc.Constants;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.video.VideoCanvas;
import io.agora.videoprp.AgoraYuvEnhancer;

public class AgoraWorkerThread extends Thread {
    private final static Logger log = LoggerFactory.getLogger(AgoraWorkerThread.class);

    private final Context mContext;

    private static final int ACTION_WORKER_THREAD_QUIT = 0X1010; // quit this thread

    private static final int ACTION_WORKER_JOIN_CHANNEL = 0X2010;

    private static final int ACTION_WORKER_LEAVE_CHANNEL = 0X2011;

    private static final int ACTION_WORKER_CONFIG_ENGINE = 0X2012;

    private static final int ACTION_WORKER_PREVIEW = 0X2014;

    private static final class WorkerThreadHandler extends Handler {

        private AgoraWorkerThread mWorkerThread;

        WorkerThreadHandler(AgoraWorkerThread thread) {
            this.mWorkerThread = thread;
        }

        public void release() {
            mWorkerThread = null;
        }

        @Override
        public void handleMessage(Message msg) {
            if (this.mWorkerThread == null) {
                log.warn("handler is already released! " + msg.what);
                return;
            }

            switch (msg.what) {
                case ACTION_WORKER_THREAD_QUIT:
                    mWorkerThread.exit();
                    break;
                case ACTION_WORKER_JOIN_CHANNEL:
                    String[] data = (String[]) msg.obj;
                    mWorkerThread.joinChannel(data[0], msg.arg1);
                    break;
                case ACTION_WORKER_LEAVE_CHANNEL:
                    String channel = (String) msg.obj;
                    mWorkerThread.leaveChannel(channel);
                    break;
                case ACTION_WORKER_CONFIG_ENGINE:
                    Object[] configData = (Object[]) msg.obj;
                    mWorkerThread.configEngine((int) configData[0], (int) configData[1]);
                    break;
                case ACTION_WORKER_PREVIEW:
                    Object[] previewData = (Object[]) msg.obj;
                    mWorkerThread.preview((boolean) previewData[0], (SurfaceView) previewData[1], (int) previewData[2]);
                    break;
            }
        }
    }

    private WorkerThreadHandler mWorkerHandler;

    private boolean mReady;

    public final void waitForReady() {
        while (!mReady) {
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            log.debug("wait for " + AgoraWorkerThread.class.getSimpleName());
        }
    }

    @Override
    public void run() {
        log.trace("start to run");
        Looper.prepare();
        mWorkerHandler = new WorkerThreadHandler(this);
        ensureRtcEngineReadyLock();
        mReady = true;
        // enter thread looper
        Looper.loop();
    }

    private RtcEngine mRtcEngine;

    private AgoraYuvEnhancer mVideoEnhancer = null;

    public final void enablePreProcessor() {
        if (mEngineConfig.mClientRole == Constants.CLIENT_ROLE_BROADCASTER) {
            if (Constant.PRP_ENABLED) {
                if (mVideoEnhancer == null) {
                    mVideoEnhancer = new AgoraYuvEnhancer(mContext);
                    mVideoEnhancer.SetLighteningFactor(Constant.PRP_DEFAULT_LIGHTNESS);
                    mVideoEnhancer.SetSmoothnessFactor(Constant.PRP_DEFAULT_SMOOTHNESS);
                    mVideoEnhancer.StartPreProcess();
                }
            }
        }
    }

    public final void setPreParameters(float lightness, float smoothness) {
        if (mEngineConfig.mClientRole == Constants.CLIENT_ROLE_BROADCASTER) {
            if (Constant.PRP_ENABLED) {
                if (mVideoEnhancer == null) {
                    mVideoEnhancer = new AgoraYuvEnhancer(mContext);
                }
                mVideoEnhancer.StartPreProcess();
            }
        }
        Constant.PRP_DEFAULT_LIGHTNESS = lightness;
        Constant.PRP_DEFAULT_SMOOTHNESS = smoothness;

        if (mVideoEnhancer != null) {
            mVideoEnhancer.SetLighteningFactor(Constant.PRP_DEFAULT_LIGHTNESS);
            mVideoEnhancer.SetSmoothnessFactor(Constant.PRP_DEFAULT_SMOOTHNESS);
        }
    }

    public final void disablePreProcessor() {
        if (mVideoEnhancer != null) {
            mVideoEnhancer.StopPreProcess();
            mVideoEnhancer = null;
        }
    }

    public final void joinChannel(final String channel, int uid) {
        if (Thread.currentThread() != this) {
            log.warn("joinChannel() - worker thread asynchronously " + channel + " " + uid);
            Message envelop = new Message();
            envelop.what = ACTION_WORKER_JOIN_CHANNEL;
            envelop.obj = new String[]{channel};
            envelop.arg1 = uid;
            mWorkerHandler.sendMessage(envelop);
            return;
        }

        ensureRtcEngineReadyLock();
        mRtcEngine.joinChannel(null, channel, "OpenLive", uid);

        mEngineConfig.mChannel = channel;

        enablePreProcessor();
        log.debug("joinChannel " + channel + " " + uid);
    }

    public final void joinChannel(final String channel) {
        if (Thread.currentThread() != this) {
            log.warn("joinChannel() - worker thread asynchronously " + channel + " " + mEngineConfig.mUid);
            Message envelop = new Message();
            envelop.what = ACTION_WORKER_JOIN_CHANNEL;
            envelop.obj = new String[]{channel};
            envelop.arg1 = mEngineConfig.mUid;
            mWorkerHandler.sendMessage(envelop);
            return;
        }

        ensureRtcEngineReadyLock();
        mRtcEngine.joinChannel(null, channel, "OpenLive", mEngineConfig.mUid);
        mEngineConfig.mChannel = channel;
        enablePreProcessor();
        log.debug("joinChannel " + channel + " " + mEngineConfig.mUid);
    }

    public final void leaveChannel(String channel) {

        if (Thread.currentThread() != this) {
            log.warn("leaveChannel() - worker thread asynchronously " + channel);
            Message envelop = new Message();
            envelop.what = ACTION_WORKER_LEAVE_CHANNEL;
            envelop.obj = channel;
            mWorkerHandler.sendMessage(envelop);
            return;
        }

        if (mRtcEngine != null) {
            mRtcEngine.leaveChannel();
        }
        disablePreProcessor();
        int clientRole = mEngineConfig.mClientRole;
        mEngineConfig.reset();
        log.debug("leaveChannel " + channel + " " + clientRole);
    }

    private EngineConfig mEngineConfig;

    public final EngineConfig getEngineConfig() {
        return mEngineConfig;
    }


    public final void configEngine(int cRole, int vProfile) {
        if (Thread.currentThread() != this) {
            log.warn("configEngine() - worker thread asynchronously " + cRole + " " + vProfile);
            Message envelop = new Message();
            envelop.what = ACTION_WORKER_CONFIG_ENGINE;
            envelop.obj = new Object[]{cRole, vProfile};
            mWorkerHandler.sendMessage(envelop);
            return;
        }

        ensureRtcEngineReadyLock();
        mEngineConfig.mClientRole = cRole;
        mEngineConfig.mVideoProfile = vProfile;

        mRtcEngine.setVideoProfile(mEngineConfig.mVideoProfile, false);

        mRtcEngine.setClientRole(cRole);

        log.debug("configEngine " + cRole + " " + mEngineConfig.mVideoProfile);
    }

    public final void preview(boolean start, SurfaceView view, int uid) {
        if (Thread.currentThread() != this) {
            log.warn("preview() - worker thread asynchronously " + start + " " + view + " " + (uid & 0XFFFFFFFFL));
            Message envelop = new Message();
            envelop.what = ACTION_WORKER_PREVIEW;
            envelop.obj = new Object[]{start, view, uid};
            mWorkerHandler.sendMessage(envelop);
            return;
        }
        ensureRtcEngineReadyLock();
        if (start) {
            mRtcEngine.setupLocalVideo(new VideoCanvas(view, VideoCanvas.RENDER_MODE_HIDDEN, uid));
            mRtcEngine.startPreview();
        } else {
            mRtcEngine.stopPreview();
        }
    }

    public static String getDeviceID(Context context) {
        // XXX according to the API docs, this value may change after factory reset
        // use Android id as device id
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    IRtcEngineEventHandler eventHandler;

    private RtcEngine ensureRtcEngineReadyLock() {
        if (mRtcEngine == null) {
            String appId = mContext.getString(R.string.private_app_id);
            if (TextUtils.isEmpty(appId)) {
                throw new RuntimeException("NEED TO use your App ID, get your own ID at https://dashboard.agora.io/");
            }
            try {
                mRtcEngine = RtcEngine.create(mContext, appId, eventHandler);
            } catch (Exception e) {
                log.error(Log.getStackTraceString(e));
                throw new RuntimeException("NEED TO check rtc sdk init fatal error\n" + Log.getStackTraceString(e));
            }
            mRtcEngine.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING);
            mRtcEngine.enableVideo();
            mRtcEngine.setLogFile(Environment.getExternalStorageDirectory()
                    + File.separator + mContext.getPackageName() + "/log/agora-rtc.log");
            mRtcEngine.enableDualStreamMode(true);
        }
        return mRtcEngine;
    }


    public RtcEngine getRtcEngine() {
        return mRtcEngine;
    }

    /**
     * call this method to exit
     * should ONLY call this method when this thread is running
     */
    public final void exit() {
        if (Thread.currentThread() != this) {
            log.warn("exit() - exit app thread asynchronously");
            mWorkerHandler.sendEmptyMessage(ACTION_WORKER_THREAD_QUIT);
            return;
        }
        mReady = false;
        // TODO should remove all pending(read) messages

        mVideoEnhancer = null;

        log.debug("exit() > start");

        // exit thread looper
        Looper.myLooper().quit();

        mWorkerHandler.release();

        log.debug("exit() > end");
    }

    public AgoraWorkerThread(Context context, IRtcEngineEventHandler eventHandler) {
        this.mContext = context;
        this.mEngineConfig = new EngineConfig();
        this.mEngineConfig.mUid = Integer.parseInt(AppConfig.UserID);
        this.eventHandler = eventHandler;


    }

}
