package com.ub.techexcel.tools;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.support.v4.util.LruCache;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.*;
import com.google.gson.Gson;
import com.kloudsync.techexcel.R;
import com.kloudsync.techexcel.bean.EventCreateSync;
import com.kloudsync.techexcel.bean.EventPlaySoundtrack;
import com.kloudsync.techexcel.bean.EventSoundtrackList;
import com.kloudsync.techexcel.bean.MeetingConfig;
import com.kloudsync.techexcel.bean.SoundTrack;
import com.kloudsync.techexcel.bean.SoundtrackDetail;
import com.kloudsync.techexcel.bean.SoundtrackDetailData;
import com.kloudsync.techexcel.config.AppConfig;
import com.kloudsync.techexcel.dialog.ShareSyncDialog;
import com.kloudsync.techexcel.dialog.ShareSyncInAppDialog;
import com.kloudsync.techexcel.dialog.SoundtrackPlayDialog;
import com.kloudsync.techexcel.docment.WeiXinApi;
import com.kloudsync.techexcel.help.SoundtrackManager;
import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.SendMessageToWX;
import com.tencent.mm.sdk.openapi.WXMediaMessage;
import com.tencent.mm.sdk.openapi.WXWebpageObject;
import com.ub.techexcel.adapter.SoundtrackAdapter;
import com.ub.techexcel.bean.SoundtrackBean;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;


public class UserSoundtrackDialog implements View.OnClickListener, DialogInterface.OnDismissListener, SoundtrackManager.OnSoundtrackResponse, PopSoundtrackOperations.OnSoundtrackOperationListener, SoundtrackAdapter.OnSoundtrackClickedListener, SoundtrackShareDialog.OnSoundtrackShareOptionsClickListener {

    public Activity host;
    public int width;
    public Dialog dialog;
    private View view;
    private ImageView close;
    private MeetingConfig meetingConfig;
    private RecyclerView soundtrackListView;
    private SoundtrackAdapter soundtrackAdapter;
    private LinearLayout createLayout;

    private void init() {
        if (null != dialog) {
            dialog.dismiss();
            return;
        } else {
            initPopuptWindow();
        }
    }

    public UserSoundtrackDialog(Activity host) {
        this.host = host;
        init();
    }

    public void initPopuptWindow() {
        LayoutInflater layoutInflater = LayoutInflater.from(host);
        view = layoutInflater.inflate(R.layout.dialog_soundtrack, null);
        close = (ImageView) view.findViewById(R.id.close);
        close.setOnClickListener(this);
        createLayout = view.findViewById(R.id.layout_create);
        createLayout.setOnClickListener(this);
        soundtrackListView = view.findViewById(R.id.list_soundtrack);
        soundtrackListView.setLayoutManager(new LinearLayoutManager(host, RecyclerView.VERTICAL, false));
        dialog = new Dialog(host, R.style.my_dialog);
        dialog.setContentView(view);
        dialog.getWindow().setGravity(Gravity.RIGHT);
        dialog.setOnDismissListener(this);
        WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
        View root = host.getWindow().getDecorView();
        params.height = root.getMeasuredHeight();
        dialog.getWindow().setAttributes(params);
        dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        dialog.getWindow().setWindowAnimations(R.style.anination3);
    }

    @SuppressLint("NewApi")
    public void show(MeetingConfig meetingConfig) {
        this.meetingConfig = meetingConfig;
        SoundtrackManager.getInstance().requestSoundtrackList(meetingConfig, this);
        if (dialog != null) {
            dialog.show();
        }
    }

    public boolean isShowing() {
        if (dialog != null) {
            return dialog.isShowing();
        }
        return false;
    }

    public void dismiss() {
        if (dialog != null) {
            dialog.dismiss();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.close:
                dismiss();
                break;

            case R.id.layout_create:
                EventBus.getDefault().post(new EventCreateSync());
                dismiss();
                break;

            default:
                break;
        }
    }


    @Override
    public void onDismiss(DialogInterface dialog) {

    }

    @Override
    public void soundtrackList(EventSoundtrackList soundtrackList) {
        Observable.just(soundtrackList).observeOn(AndroidSchedulers.mainThread()).doOnNext(new Consumer<EventSoundtrackList>() {
            @Override
            public void accept(EventSoundtrackList soundtrackList) throws Exception {
                if (soundtrackList.getSoundTracks() != null && soundtrackList.getSoundTracks().size() >= 0) {
                    if (soundtrackAdapter == null) {
                        soundtrackAdapter = new SoundtrackAdapter(host);
                        soundtrackAdapter.setSoundTracks(soundtrackList.getSoundTracks());
                        soundtrackListView.setAdapter(soundtrackAdapter);
                    } else {
                        soundtrackAdapter.setSoundTracks(soundtrackList.getSoundTracks());
                    }
                    soundtrackAdapter.setOnSoundtrackClickedListener(UserSoundtrackDialog.this);
                }
            }
        }).subscribe();

    }

    @Override
    public void editSoundTrack(SoundTrack soundTrack) {

    }

    @Override
    public void deleteSoundTrack(SoundTrack soundTrack) {
        Observable.just(soundTrack).observeOn(Schedulers.io()).map(new Function<SoundTrack, Integer>() {
            @Override
            public Integer apply(SoundTrack soundTrack) throws Exception {

                JSONObject response = ServiceInterfaceTools.getinstance().syncDeleteSoundtrack(soundTrack);
                int recode = -1;
                if(response.has("RetCode")){
                    recode = response.getInt("RetCode");
                    if(recode == 0){
                        SoundtrackManager.getInstance().requestSoundtrackList(meetingConfig, UserSoundtrackDialog.this);
                    }
                }
                return recode;
            }
        }).observeOn(AndroidSchedulers.mainThread()).doOnNext(new Consumer<Integer>() {
            @Override
            public void accept(Integer integer) throws Exception {
                if(integer == 0){
                    Toast.makeText(host,R.string.operate_success,Toast.LENGTH_SHORT).show();
                }
            }
        }).subscribe();
    }

    @Override
    public void playSoundTrack(SoundTrack soundTrack) {
        requestDetailAndPlay(soundTrack);
    }

    @Override
    public void sharePopup(SoundTrack soundTrack) {
        showShareSoundtrackDialog(soundTrack);
    }

    private SoundtrackShareDialog soundtrackShareDialog;

    private void showShareSoundtrackDialog(SoundTrack soundTrack) {
        if (soundtrackShareDialog != null) {
            if (soundtrackShareDialog.isShowing()) {
                soundtrackShareDialog.dismiss();
                soundtrackShareDialog = null;
            }
        }
        soundtrackShareDialog = new SoundtrackShareDialog(host);
        soundtrackShareDialog.setSoundtrackShareOptionsClickListener(this);
        soundtrackShareDialog.show(soundTrack);
    }

    @Override
    public void onSoundtrackClicked(View itemView, SoundTrack soundTrack) {
        showOperationsPop(itemView, soundTrack);
    }

    private PopSoundtrackOperations soundtrackOperationsPop;

    private void showOperationsPop(View itemView, SoundTrack soundTrack) {
        if (soundtrackOperationsPop != null) {
            if (soundtrackOperationsPop.isShowing()) {
                soundtrackOperationsPop.dismiss();
                soundtrackOperationsPop = null;
            }
        }
        soundtrackOperationsPop = new PopSoundtrackOperations(host);
        soundtrackOperationsPop.setSoundtrackOperationListener(this);
        soundtrackOperationsPop.show(itemView, soundTrack);
    }

    private ShareSyncInAppDialog shareSyncInAppDialog;

    private void showShareInAppDialog(SoundTrack soundTrack) {
        if (shareSyncInAppDialog != null) {
            if (shareSyncInAppDialog.isShowing()) {
                shareSyncInAppDialog.dismiss();
                shareSyncInAppDialog = null;
            }
        }
        shareSyncInAppDialog = new ShareSyncInAppDialog(host);

        shareSyncInAppDialog.setDocument(meetingConfig.getDocument());
        shareSyncInAppDialog.setSync(soundTrack);
        shareSyncInAppDialog.show();
    }


    private void shareSoundtrackOnWechat(final MeetingConfig meetingConfig, final int id, final boolean st) {

        RequestQueue requestQueue = Volley.newRequestQueue(host);
        final LruCache<String, Bitmap> lruCache = new LruCache<String, Bitmap>(
                20);
        com.android.volley.toolbox.ImageLoader.ImageCache imageCache = new com.android.volley.toolbox.ImageLoader.ImageCache() {
            @Override
            public void putBitmap(String key, Bitmap value) {
                lruCache.put(key, value);
            }

            @Override
            public Bitmap getBitmap(String key) {
                return lruCache.get(key);
            }
        };
        com.android.volley.toolbox.ImageLoader imageLoader = new com.android.volley.toolbox.ImageLoader(requestQueue, imageCache);
        String url = meetingConfig.getDocument().getAttachmentUrl();
        if (url.contains("<") && url.contains(">")) {
            url = url.substring(0, url.lastIndexOf("<")) + "1" + url.substring(url.lastIndexOf("."), url.length());
        }
        imageLoader.get(url, new com.android.volley.toolbox.ImageLoader.ImageListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                doShareWechat(meetingConfig, null, id, st);
            }

            @Override
            public void onResponse(com.android.volley.toolbox.ImageLoader.ImageContainer response, boolean isImmediate) {
                if (response.getBitmap() != null) {
                    doShareWechat(meetingConfig, response.getBitmap(), id, st);
                }
            }
        });

    }

    private void doShareWechat(MeetingConfig meetingConfig, Bitmap b, int id, final boolean st) {

        String url = AppConfig.SHARE_DOCUMENT + meetingConfig.getDocument().getItemID();
        if (id > 0) {
            url = AppConfig.SHARE_SYNC + id;
        }
        if (isWXAppInstalledAndSupported(WeiXinApi.getInstance().GetApi())) {
            WXWebpageObject webpage = new WXWebpageObject();
            webpage.webpageUrl = url;
            final WXMediaMessage msg = new WXMediaMessage(webpage);
            msg.title = meetingConfig.getDocument().getFileName();
            msg.description = "请点击此框跳转至" + url;
            Bitmap thumb = b;
            if (null == thumb) {
                thumb = BitmapFactory.decodeResource(host.getResources(),
                        R.drawable.logo);
            }
            final Bitmap finalThumb = thumb;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    msg.thumbData = bitmapToByteArray(finalThumb, true);
                    SendMessageToWX.Req req = new SendMessageToWX.Req();
                    req.transaction = buildTransaction("webpage");
                    req.message = msg;
                    req.scene = st ? SendMessageToWX.Req.WXSceneSession : SendMessageToWX.Req.WXSceneTimeline;
                    WeiXinApi.getInstance().GetApi().sendReq(req);
                }
            }).start();
        } else {
            Toast.makeText(host, "微信客户端未安装，请确认",
                    Toast.LENGTH_LONG).show();
        }
    }

    private boolean isWXAppInstalledAndSupported(IWXAPI api) {
        return api.isWXAppInstalled() && api.isWXAppSupportAPI();
    }

    private byte[] bitmapToByteArray(Bitmap bitmap, final boolean needRecycle) {

        bitmap = Bitmap.createScaledBitmap(bitmap, 150, 150, true);

        int i;
        int j;
        if (bitmap.getHeight() > bitmap.getWidth()) {
            i = bitmap.getWidth();
            j = bitmap.getWidth();
        } else {
            i = bitmap.getHeight();
            j = bitmap.getHeight();
        }

        Bitmap localBitmap = Bitmap.createBitmap(i, j, Bitmap.Config.RGB_565);
        Canvas localCanvas = new Canvas(localBitmap);

        while (true) {
            localCanvas.drawBitmap(bitmap, new Rect(0, 0, i, j), new Rect(0, 0, i, j), null);
            if (needRecycle)
                bitmap.recycle();
            ByteArrayOutputStream localByteArrayOutputStream = new ByteArrayOutputStream();
            localBitmap.compress(Bitmap.CompressFormat.JPEG, 100,
                    localByteArrayOutputStream);
            localBitmap.recycle();
            byte[] arrayOfByte = localByteArrayOutputStream.toByteArray();
            try {
                localByteArrayOutputStream.close();
                Log.e("hahahaTT", "  " + arrayOfByte.length);
                return arrayOfByte;
            } catch (Exception e) {
                //F.out(e);
            }
            i = bitmap.getHeight();
            j = bitmap.getHeight();
        }
    }

    private String buildTransaction(final String type) {
        return (type == null) ? String.valueOf(System.currentTimeMillis())
                : type + System.currentTimeMillis();
    }

    // do share
    @Override
    public void wechatShareSoundtrack(SoundTrack soundTrack) {
        shareSoundtrackOnWechat(meetingConfig, soundTrack.getSoundtrackID(), true);

    }

    @Override
    public void appShareSoundtrack(SoundTrack soundTrack) {
        showShareInAppDialog(soundTrack);
    }

    @Override
    public void copySoundtrackLink(SoundTrack soundTrack) {
        String url = AppConfig.SHARE_DOCUMENT + soundTrack.getSoundtrackID();
        if (soundTrack.getSoundtrackID() > 0) {
            url = AppConfig.SHARE_SYNC + soundTrack.getSoundtrackID();
        }
        ClipboardManager clipboardManager = (ClipboardManager) host.getSystemService(Context.CLIPBOARD_SERVICE);
        clipboardManager.setPrimaryClip(ClipData.newPlainText(null, url));
        Toast.makeText(host, "Copy link success!", Toast.LENGTH_SHORT).show();
    }

    //-----
    private void requestDetailAndEdit(final SoundTrack soundTrack) {
        Observable.just(soundTrack).observeOn(Schedulers.io()).map(new Function<SoundTrack, SoundtrackDetailData>() {
            @Override
            public SoundtrackDetailData apply(SoundTrack soundtrack) throws Exception {
                SoundtrackDetailData soundtrackDetailData = new SoundtrackDetailData();
                JSONObject response = ServiceInterfaceTools.getinstance().syncGetSoundtrackDetail(soundTrack);
                if(response.has("RetCode")){
                    if(response.getInt("RetCode") == 0){
                        SoundtrackDetail soundtrackDetail = new Gson().fromJson(response.getJSONObject("RetData").toString(),SoundtrackDetail.class);
                        soundtrackDetailData.setSoundtrackDetail(soundtrackDetail);

                    }
                }

                return soundtrackDetailData;
            }
        }).doOnNext(new Consumer<SoundtrackDetailData>() {
            @Override
            public void accept(SoundtrackDetailData soundtrackDetailData) throws Exception {
                if(soundtrackDetailData.getSoundtrackDetail() != null){

                }
            }
        }).subscribe();

    }

    private void requestDetailAndPlay(final SoundTrack soundTrack) {
        Observable.just(soundTrack).observeOn(Schedulers.io()).map(new Function<SoundTrack, SoundtrackDetailData>() {
            @Override
            public SoundtrackDetailData apply(SoundTrack soundtrack) throws Exception {
                SoundtrackDetailData soundtrackDetailData = new SoundtrackDetailData();
                JSONObject response = ServiceInterfaceTools.getinstance().syncGetSoundtrackDetail(soundTrack);
                if(response.has("RetCode")){
                    if(response.getInt("RetCode") == 0){
                        SoundtrackDetail soundtrackDetail = new Gson().fromJson(response.getJSONObject("RetData").toString(),SoundtrackDetail.class);
                        soundtrackDetailData.setSoundtrackDetail(soundtrackDetail);

                    }
                }
                return soundtrackDetailData;
            }
        }).observeOn(AndroidSchedulers.mainThread()).doOnNext(new Consumer<SoundtrackDetailData>() {
            @Override
            public void accept(SoundtrackDetailData soundtrackDetailData) throws Exception {
                if(soundtrackDetailData.getSoundtrackDetail() != null){
                    EventPlaySoundtrack soundtrack = new EventPlaySoundtrack();
                    soundtrack.setSoundtrackDetail(soundtrackDetailData.getSoundtrackDetail());
                    EventBus.getDefault().post(soundtrack);
                    dismiss();
                }
            }
        }).subscribe();
    }

}
