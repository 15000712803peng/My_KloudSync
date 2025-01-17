package com.kloudsync.techexcel.personal;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.kloudsync.techexcel.R;

import me.imid.swipebacklayout.lib.app.SwipeBackActivity;

public class SchoolProfileActivity extends Activity {

    private TextView tv_back;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_school_profile);

        findView();
        initView();
    }

    private void findView() {
        tv_back = (TextView) findViewById(R.id.tv_back);
    }

    private void initView() {
        tv_back.setOnClickListener(new myOnClick());
    }

    protected class myOnClick implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.tv_back:
                    finish();
                    break;
                default:
                    break;
            }
        }
    }


}
