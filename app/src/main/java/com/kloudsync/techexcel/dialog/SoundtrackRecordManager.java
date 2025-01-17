package com.kloudsync.techexcel.dialog;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.kloudsync.techexcel.R;
import com.kloudsync.techexcel.bean.EventCreateSync;
import com.kloudsync.techexcel.bean.MeetingConfig;
import com.kloudsync.techexcel.bean.params.EventSoundSync;
import com.kloudsync.techexcel.tool.SocketMessageManager;
import com.ub.kloudsync.activity.Document;
import com.ub.service.audiorecord.AudioRecorder;
import com.ub.service.audiorecord.RecordEndListener;
import com.ub.techexcel.bean.SoundtrackBean;
import com.ub.techexcel.tools.UploadAudioTool;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;



public class SoundtrackRecordManager implements View.OnClickListener {

    private Context mContext;
    private static Handler recordHandler;
    private SoundtrackRecordManager(Context context) {
        this.mContext = context;
        recordHandler=new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(Message msg) {
                if (recordHandler == null) {
                    return;
                }
                handlePlayMessage(msg);
                super.handleMessage(msg);
            }
        };
    }

    private static final int MESSAGE_PLAY_TIME_REFRESHED = 1;

    private void handlePlayMessage(Message message) {
        switch (message.what) {
            case MESSAGE_PLAY_TIME_REFRESHED:
                String time= (String) message.obj;
                audiotime.setText(time);
//              timeShow.setText(time);
                break;
        }

    }

    static volatile SoundtrackRecordManager instance;

    public static SoundtrackRecordManager getManager(Context context) {
        if (instance == null) {
            synchronized (SocketMessageManager.class) {
                if (instance == null) {
                    instance = new SoundtrackRecordManager(context);
                }
            }
        }
        return instance;
    }

    private LinearLayout audiosyncll;
    private boolean isrecordvoice;
    private int soundtrackID;
    private int fieldId;
    private String fieldNewPath;
    private MeetingConfig meetingConfig;

    /**
     *
     * @param isrecordvoice 是否開啟錄製音頻  目前一直為true
     * @param soundtrackBean
     * @param audiosyncll
     */
    public void setInitParams(boolean isrecordvoice, SoundtrackBean soundtrackBean, LinearLayout audiosyncll, MeetingConfig meetingConfig) {
        this.audiosyncll=audiosyncll;
        this.isrecordvoice=isrecordvoice;
        this.meetingConfig=meetingConfig;
        soundtrackID = soundtrackBean.getSoundtrackID();
        fieldId = soundtrackBean.getFileId();
        fieldNewPath = soundtrackBean.getPath();
        Document backgroudMusicInfo = soundtrackBean.getBackgroudMusicInfo();
        String url="";
        if (backgroudMusicInfo == null || backgroudMusicInfo.getAttachmentID().equals("0")) {
        } else {
            url=backgroudMusicInfo.getFileDownloadURL();
        }
        String url1="";
        if (soundtrackBean.getNewAudioAttachmentID() != 0) {
            url1=soundtrackBean.getNewAudioInfo().getFileDownloadURL();
        } else if (soundtrackBean.getSelectedAudioAttachmentID() != 0) {
            url1= soundtrackBean.getSelectedAudioInfo().getFileDownloadURL();
        }
        initPlayMusic(isrecordvoice,url,url1);

    }

    private MediaPlayer mediaPlayer;
    private MediaPlayer mediaPlayer2;

    private void  initPlayMusic(final boolean isrecordvoice, String url,String url2){
        Log.e("syncing---", isrecordvoice+"  "+url+"  "+url2);
        if(isrecordvoice){
            startSync();  //开始录音
        }
        //显示进度条
        displayLayout();
        if(!TextUtils.isEmpty(url)) {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.reset();
                mediaPlayer.release();
                mediaPlayer = null;
            }else{
                mediaPlayer = new MediaPlayer();
            }
            try {
                mediaPlayer.setDataSource(url);
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mediaPlayer.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.start();
                }
            });
        }

        if (!TextUtils.isEmpty(url2)) {
            if (mediaPlayer2 != null) {
                mediaPlayer2.stop();
                mediaPlayer2.reset();
                mediaPlayer2.release();
                mediaPlayer2 = null;
            }
            mediaPlayer2 = new MediaPlayer();
            try {
                mediaPlayer2.setDataSource(url2);
                mediaPlayer2.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mediaPlayer2.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mediaPlayer2.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.start();
                }
            });
        }
    }


    private void  startSync(){
        EventSoundSync soundSync=new EventSoundSync();
        soundSync.setSoundtrackID(soundtrackID);
        soundSync.setStatus(1);
        soundSync.setTime(tttime);
        EventBus.getDefault().post(soundSync);
        //启动录音程序
        startAudioRecord();
    }

    private ImageView playstop,syncicon,close;
    private  TextView audiotime,isStatus;
    private SeekBar mSeekBar;
    private boolean isPause=false;

    public void displayLayout() {
        audiosyncll.setVisibility(View.VISIBLE);
        playstop = audiosyncll.findViewById(R.id.playstop);
        playstop.setOnClickListener(this);
        playstop.setImageResource(R.drawable.video_stop);
        syncicon =  audiosyncll.findViewById(R.id.syncicon);
        close = audiosyncll.findViewById(R.id.close);
        close.setOnClickListener(this);
        isStatus =  audiosyncll.findViewById(R.id.isStatus);
        audiotime =  audiosyncll.findViewById(R.id.audiotime);
        mSeekBar = audiosyncll.findViewById(R.id.seekBar);
        mSeekBar.setVisibility(View.GONE  );
        if (isrecordvoice) {
            isStatus.setText("Recording");
            syncicon.setVisibility(View.VISIBLE);
        } else {
            isStatus.setText("Syncing");  //只同步  不錄音
            syncicon.setVisibility(View.GONE);
        }
        refreshRecord();
    }

    private int tttime=0;
    private Timer audioplaytimer;
    /**
     * 每隔100毫秒拿录制进度
     */
    private void refreshRecord() {
        tttime = 0;
        audiotime.setText("00:00");
        if (audioplaytimer != null) {
            audioplaytimer.cancel();
            audioplaytimer = null;
        }
        audioplaytimer = new Timer();
        audioplaytimer.schedule(new TimerTask() {

            @Override
            public void run() {
                Log.e("refreshTime", isPause + "");
                if (!isPause) {
                    tttime = tttime + 100;
                    Log.e("refreshTime", " " + tttime);
                    if (audiotime != null) {
                        final String time = new SimpleDateFormat("mm:ss").format(tttime);
                        Message ms=Message.obtain();
                        ms.what=MESSAGE_PLAY_TIME_REFRESHED;
                        ms.obj=time;
                        recordHandler.sendMessage(ms);
                    }
                }
            }
        }, 0, 100);
    }


    private AudioRecorder audioRecorder;

    private void startAudioRecord() {
        if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
        } else {
            ActivityCompat.requestPermissions((Activity) mContext, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
        }
        if (audioRecorder != null) {
            audioRecorder.canel();  //取消录音
        }
        audioRecorder = AudioRecorder.getInstance();
        try {
            if (audioRecorder.getStatus() == AudioRecorder.Status.STATUS_NO_READY) {
                Log.e("syncing---", "startAudioRecord");
                String fileName = new SimpleDateFormat("yyyyMMddhhmmss").format(new Date());
                audioRecorder.createDefaultAudio(fileName);
                audioRecorder.startRecord(null);
            }
        } catch (IllegalStateException e) {
            Toast.makeText(mContext, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }


    private void stopAudioRecord(final int soundtrackID) {
        if (audioRecorder != null) {
            audioRecorder.stopRecord(new RecordEndListener() {
                @Override
                public void endRecord(String fileName) {
                    Log.e("syncing---", "录音结束，开始上传 " + fileName);
                    File file = com.ub.service.audiorecord.FileUtils.getWavFile(fileName);
                    if (file != null) {
                        Log.e("syncing---", file.getAbsolutePath() + "   " + file.getName());
//                        uploadAudioFile(file, soundtrackID, false, false);
                        UploadAudioTool.getManager(mContext).uploadAudio(file,soundtrackID,fieldId,fieldNewPath,audiosyncll,meetingConfig);
                    }
                }
            });
            audioRecorder = null;
        }
    }

    private void pauseOrStartAudioRecord() {
        if (audioRecorder != null) {
            if (audioRecorder.getStatus() == AudioRecorder.Status.STATUS_START) {
                audioRecorder.pauseRecord();
                Log.e("syncing---", "false");
            } else {
                audioRecorder.startRecord(null);
                Log.e("syncing---", "true");
            }
        }
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.playstop: //暂停或录音
                if (isPause) {
                    isPause=false;
                    resumeMedia();
                } else {
                    isPause=true;
                    pauseMedia();
                }
                if (isrecordvoice) {
                    pauseOrStartAudioRecord();
                }
                break;
            case R.id.close: //结束录音
                closeAudioSync();
                StopMedia();
                if (isrecordvoice) {    // 完成录音
                    stopAudioRecord(soundtrackID);
                }
                break;
        }

    }


    public void release(){

    }

    private void closeAudioSync() {

        EventSoundSync soundSync=new EventSoundSync();
        soundSync.setSoundtrackID(soundtrackID);
        soundSync.setStatus(0);
        soundSync.setTime(tttime);
        EventBus.getDefault().post(soundSync);

        if (audioplaytimer != null) {
            audioplaytimer.cancel();
            audioplaytimer = null;
            isPause=false;
            tttime=0;
        }
        audiosyncll.setVisibility(View.GONE);
//        timeShow.setVisibility(View.GONE);
//        getPageObjectsAfterChange(currentAttachmentPage);

    }


    private void resumeMedia() {
        playstop.setImageResource(R.drawable.video_stop);
        if (mediaPlayer != null) {
            mediaPlayer.start();
        }
        if (mediaPlayer2 != null) {
            mediaPlayer2.start();
        }
    }
    private void pauseMedia() {
        playstop.setImageResource(R.drawable.video_play);
        if (mediaPlayer != null) {
            mediaPlayer.pause();
        }
        if (mediaPlayer2 != null) {
            mediaPlayer2.pause();
        }
    }

    private void StopMedia() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.reset();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (mediaPlayer2 != null) {
            mediaPlayer2.stop();
            mediaPlayer2.reset();
            mediaPlayer2.release();
            mediaPlayer2 = null;
        }
    }

    public int  getCurrentTime() {
        return  tttime;
    }
}
