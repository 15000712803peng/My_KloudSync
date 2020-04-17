package com.kloudsync.techexcel.dialog;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.kloudsync.techexcel.R;
import com.kloudsync.techexcel.bean.EventKickOffMember;
import com.kloudsync.techexcel.bean.MeetingConfig;
import com.kloudsync.techexcel.bean.MeetingMember;
import com.kloudsync.techexcel.config.AppConfig;
import com.ub.techexcel.tools.Tools;

import org.greenrobot.eventbus.EventBus;

/**
 * Created by tonyan on 2019/12/20.
 */

public class PopMeetingHandsMemberSetting extends PopupWindow implements View.OnClickListener {

    private Context context;

    private MeetingMember meetingMember;
    private TextView mAllowSpeak, mHandDown, mSetMainMembers,kickOffMember;
    private MeetingConfig meetingConfig;
    private View mView;

    public interface OnHandsMemberSettingChanged{
        void setHandsAllowSpeak(MeetingMember meetingMember);
        void setHandsDown(MeetingMember meetingMember);
        void setHandsMember(MeetingMember meetingMember);
    }

    private OnHandsMemberSettingChanged onMemberSettingChanged;

    public void setOnMemberSettingChanged(OnHandsMemberSettingChanged onMemberSettingChanged) {
        this.onMemberSettingChanged = onMemberSettingChanged;
    }

    public PopMeetingHandsMemberSetting(Context context) {
        super(context);
        this.context = context;
        initalize();
    }

    private void initalize() {
        LayoutInflater inflater = LayoutInflater.from(context);
        mView = inflater.inflate(R.layout.pop_meeting_hands_member_options, null);
        mAllowSpeak = mView.findViewById(R.id.ppw_tv_speak);
        mHandDown = mView.findViewById(R.id.ppw_tv_hand_down);
        mSetMainMembers = mView.findViewById(R.id.ppw_tv_main_members);
        kickOffMember = mView.findViewById(R.id.txt_kick_off);
        kickOffMember.setOnClickListener(this);
        mSetMainMembers.setOnClickListener(this);
        mAllowSpeak.setOnClickListener(this);
        mHandDown.setOnClickListener(this);
        setContentView(mView);
        initWindow();
    }

    private void initWindow() {
        this.setWidth(context.getResources().getDimensionPixelOffset(R.dimen.meeting_members_setting_width));
        this.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        this.setFocusable(true);
        this.setOutsideTouchable(true);
        this.setBackgroundDrawable(new BitmapDrawable());
        this.update();

    }

    public void showAtBottom(MeetingMember meetingMember,View view,MeetingConfig meetingConfig) {
        this.meetingMember = meetingMember;
        this.meetingConfig = meetingConfig;
        if(meetingMember.getPresenter() == 1){
            mAllowSpeak.setVisibility(View.GONE);
        }
        if(meetingMember.getPresenter() == 1 || meetingMember.getRole() == 2){
            mHandDown.setVisibility(View.GONE);
        }
        if(meetingConfig.getMeetingHostId().equals(meetingMember.getUserId()+"")){
            // 操作的成员是HOST
            kickOffMember.setVisibility(View.GONE);
        }else {
            // 不是HOST，如果自己是HOST
            if(AppConfig.UserID.equals(meetingConfig.getMeetingHostId())){
                kickOffMember.setVisibility(View.VISIBLE);
            }else {
                kickOffMember.setVisibility(View.GONE);
            }
        }
      mView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int popupHeight = mView.getMeasuredHeight();
        int xoff = -context.getResources().getDimensionPixelOffset(R.dimen.dp_160);
        showAsDropDown(view,xoff,-popupHeight);

    }



    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.ppw_tv_speak://允许发言
                if(meetingMember != null && onMemberSettingChanged != null){
                    onMemberSettingChanged.setHandsAllowSpeak(meetingMember);

                }
                dismiss();
                break;
            case R.id.ppw_tv_hand_down://把手放下
                if(meetingMember != null && onMemberSettingChanged != null){
                    onMemberSettingChanged.setHandsDown(meetingMember);
                }
                dismiss();
                break;
            case R.id.ppw_tv_main_members://成为主讲人
                if(meetingMember != null && onMemberSettingChanged != null){
                    onMemberSettingChanged.setHandsMember(meetingMember);

                }
                dismiss();
                break;
            case R.id.txt_kick_off:
                if(meetingMember != null && onMemberSettingChanged != null){
                    Log.e("check_post_kick_off","post_2");
                    EventKickOffMember kickOffMember = new EventKickOffMember();
                    kickOffMember.setMeetingMember(meetingMember);
                    EventBus.getDefault().post(kickOffMember);
                }
                dismiss();
            default:
                break;
        }
    }


}