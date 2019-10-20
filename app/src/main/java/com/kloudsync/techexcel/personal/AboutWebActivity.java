package com.kloudsync.techexcel.personal;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.transition.Slide;
import android.transition.Transition;
import android.transition.Visibility;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.kloudsync.techexcel.R;
import com.kloudsync.techexcel.config.AppConfig;
import com.kloudsync.techexcel.pc.ui.ChangePasswordActivity;

public class AboutWebActivity extends AppCompatActivity {

    private ImageView img_notice;
    private WebView about_webview;
    private SharedPreferences sharedPreferences;
    int language;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_web);
        initView();
    }

    private void initView() {
        img_notice = (ImageView) findViewById(R.id.img_notice);
        about_webview = (WebView) findViewById(R.id.about_webview);

        //获取当前语言
        sharedPreferences = getSharedPreferences(AppConfig.LOGININFO,
                MODE_PRIVATE);
        language = sharedPreferences.getInt("language",1);


        img_notice.setOnClickListener(new AboutWebActivity.MyOnClick());

        OpenWeb();
    }

    protected class MyOnClick implements View.OnClickListener{

        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.img_notice:
//                    finish();
                    ActivityCompat.finishAfterTransition(AboutWebActivity.this);
                    break;
                default:
                    break;
            }
        }
    }

    private void OpenWeb() {
     //   Uri uri;
        if(language == 1){
            //uri = Uri.parse("https://kloudsync.peertime.cn/privacy.html");
            about_webview.loadUrl ("https://kloudsync.peertime.cn/privacy.html");
            //Uri uri = Uri.parse("http://http;//www.peertime.com/privacy-statement");

        }else {
           // uri = Uri.parse("https://kloudsync.peertime.cn/privacy-cn.html");
            about_webview.loadUrl ("https://kloudsync.peertime.cn/privacy-cn.html");
        }

       // startActivity(new Intent(Intent.ACTION_VIEW,uri));
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        ActivityCompat.finishAfterTransition(AboutWebActivity.this);
    }
}