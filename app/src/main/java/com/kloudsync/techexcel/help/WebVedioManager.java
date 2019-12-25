package com.kloudsync.techexcel.help;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import com.kloudsync.techexcel.bean.WebVedio;
import com.ub.techexcel.bean.SectionVO;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by tonyan on 2019/11/21.
 */

public class WebVedioManager implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {

    private static WebVedioManager instance;
    private MediaPlayer vedioPlayer;
    private Context context;
    private WebVedio currentWebVedio;
    private SurfaceView surfaceView;
    //
    private WebVedioManager(Context context) {
        this.context = context;
        vedioPlayer = new MediaPlayer();
    }

    public static WebVedioManager getInstance(Context context) {
        if (instance == null) {
            synchronized (WebVedioManager.class) {
                if (instance == null) {
                    instance = new WebVedioManager(context);
                }
            }
        }
        return instance;
    }

    public void execute(WebVedio webVedio){

        if(currentWebVedio == null || webVedio == null){
            return;
        }
        switch (webVedio.getStat()){
            case 1:
                if(!currentWebVedio.equals(webVedio)){
                    return;
                }
                //播放
                if(currentWebVedio.isPrepared()){
                    if(surfaceView.getVisibility() != View.VISIBLE){
                        surfaceView.setVisibility(View.VISIBLE);
                    }
                    vedioPlayer.seekTo((int)(webVedio.getTime() * 1000));
                    vedioPlayer.start();
                }
                break;

            case 2:
                //close
                if(surfaceView != null){
                    surfaceView.setVisibility(View.GONE);
                    if(vedioPlayer.isPlaying()){
                        vedioPlayer.stop();
                    }
                    vedioPlayer.reset();
                }
//                release();
                break;

            case 0:

                if(!currentWebVedio.equals(webVedio)){
                    return;
                }
                // pause:
                if(vedioPlayer.isPlaying()){
//                    vedioPlayer.seekTo((int)(webVedio.getTime() * 1000));
                    vedioPlayer.pause();
                }
        }
    }


    public void safePrepare(WebVedio webVedio) {

        if(webVedio == null || webVedio.getStat() != 1){
            return;
        }

        if(this.currentWebVedio != null){
            if(webVedio.equals(currentWebVedio)){
                return;
            }else {
                this.currentWebVedio.setPreparing(false);
            }
        }

        try {

            if(vedioPlayer == null){
                vedioPlayer = new MediaPlayer();
                initSurface(surfaceView);
            }

            if (webVedio.isPreparing() || vedioPlayer.isPlaying() || webVedio.isPrepared()) {
                return;
            }
            this.currentWebVedio = webVedio;
            webVedio.setPreparing(true);
            vedioPlayer.reset();
            vedioPlayer.setDataSource(context, Uri.parse(webVedio.getUrl()));
            vedioPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            try {
                vedioPlayer.prepareAsync();
            }catch (IllegalStateException e){

                reinit(webVedio);
            }

            vedioPlayer.setOnPreparedListener(this);
            vedioPlayer.setOnCompletionListener(this);
            vedioPlayer.setOnErrorListener(this);
        } catch (IOException e) {
            e.printStackTrace();
            webVedio.setPreparing(false);
        }

    }

    private void reinit(WebVedio webVedio){
        vedioPlayer = null;
        vedioPlayer = new MediaPlayer();
        refreshSurface();
        try {
            vedioPlayer.setDataSource(context, Uri.parse(webVedio.getUrl()));
            vedioPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            vedioPlayer.prepareAsync();

        } catch (IOException e) {
            e.printStackTrace();
            webVedio.setPreparing(false);
        }


    }

    public void refresh(WebVedio webVedio){

    }


    @Override
    public void onPrepared(MediaPlayer mp) {
        if(currentWebVedio != null){
            currentWebVedio.setPrepared(true);
            currentWebVedio.setPreparing(false);
            vedioPlayer.seekTo((int)(currentWebVedio.getTime() * 1000));
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        mp.reset();

    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return false;
    }



    public void release(){
        if (vedioPlayer != null) {
            vedioPlayer.stop();
            vedioPlayer.reset();
        }
        if(this.currentWebVedio != null){
            this.currentWebVedio.setPreparing(false);
            this.currentWebVedio.setPrepared(false);
        }
        this.currentWebVedio = null;
    }

    public void initSurface(SurfaceView surfaceView){
        //给surfaceHolder设置一个callback
        this.surfaceView = surfaceView;
        surfaceView.getHolder().addCallback(new SurfaceCallBack());
    }

    private void refreshSurface(){
        if(this.surfaceView != null){
            this.surfaceView.getHolder().addCallback(new SurfaceCallBack());
        }
    }

    private class SurfaceCallBack implements SurfaceHolder.Callback {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            //调用MediaPlayer.setDisplay(holder)设置surfaceHolder，surfaceHolder可以通过surfaceview的getHolder()方法获得

            Log.e("WebVedioManager", "surfaceCreated");
            if(vedioPlayer != null){
                vedioPlayer.setDisplay(holder);
            }
        }

        /**
         * 当SurfaceHolder的尺寸发生变化的时候被回调
         *
         * @param holder
         * @param format
         * @param width
         * @param height
         */
        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            release();
        }
    }

}
