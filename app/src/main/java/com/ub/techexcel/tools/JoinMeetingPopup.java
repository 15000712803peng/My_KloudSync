package com.ub.techexcel.tools;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.kloudsync.techexcel.R;
import com.kloudsync.techexcel.config.AppConfig;
import com.kloudsync.techexcel.help.ApiTask;
import com.kloudsync.techexcel.help.ThreadManager;
import com.kloudsync.techexcel.service.ConnectService;
import com.ub.service.activity.WatchCourseActivity2;
import com.ub.service.activity.WatchCourseActivity3;
import com.ub.techexcel.bean.UpcomingLesson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wang on 2017/9/18.
 */

public class JoinMeetingPopup implements View.OnClickListener {

    public Context mContext;
    public int width;
    public Dialog mPopupWindow;
    private View view;
    private TextView joinroom2;
    private EditText roomet;

    private String roomid;
    private int lessionid;
    private int teacherid;
    private UpcomingLesson lesson = null;

    private static FavoritePoPListener mFavoritePoPListener;

    public interface FavoritePoPListener {

        void open();

        void dismiss();
    }

    public void setFavoritePoPListener(FavoritePoPListener documentPoPListener) {
        this.mFavoritePoPListener = documentPoPListener;
    }


    public void getPopwindow(Context context) {
        this.mContext = context;
        width = mContext.getResources().getDisplayMetrics().widthPixels;
        getPopupWindowInstance();
        mPopupWindow.getWindow().setWindowAnimations(R.style.PopupAnimation5);
    }

    public void getPopupWindowInstance() {
        if (null != mPopupWindow) {
            mPopupWindow.dismiss();
            return;
        } else {
            initPopuptWindow();
        }
    }


    @SuppressLint("WrongConstant")
    public void initPopuptWindow() {
        LayoutInflater layoutInflater = LayoutInflater.from(mContext);
        view = layoutInflater.inflate(R.layout.newmeeting, null);
        joinroom2 = (TextView) view.findViewById(R.id.joinroom2);
        roomet = (EditText) view.findViewById(R.id.roomet);
        joinroom2.setOnClickListener(this);

        mPopupWindow = new Dialog(mContext, R.style.bottom_dialog);
        mPopupWindow.setContentView(view);
        mPopupWindow.getWindow().setGravity(Gravity.BOTTOM);
        WindowManager.LayoutParams lp = mPopupWindow.getWindow().getAttributes();
        lp.width = mContext.getResources().getDisplayMetrics().widthPixels;
        mPopupWindow.getWindow().setAttributes(lp);

    }


    @SuppressLint("NewApi")
    public void StartPop(View v) {
        if (mPopupWindow != null) {
            mPopupWindow.show();
            mFavoritePoPListener.open();
        }
    }


    public boolean isShowing() {
        return mPopupWindow.isShowing();
    }

    public void dismiss() {
        if (mPopupWindow != null) {
            mPopupWindow.dismiss();
        }
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.joinroom2:
                if (!Tools.isFastClick()) {
                    InputMethodManager imm = (InputMethodManager)
                            mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(roomet.getWindowToken(), 0);
                    roomid = roomet.getText().toString();
                    if (!TextUtils.isEmpty(roomid)) {
                        checkClassRoomExist(roomid);
                    } else {
                        Toast.makeText(mContext, mContext.getString(R.string.joinroom), Toast.LENGTH_LONG).show();
                    }
                }
                dismiss();
                break;

            default:
                break;
        }
    }


    private void checkClassRoomExist(final String classRoomId) {
        new ApiTask(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject jsonObject = ConnectService.getIncidentbyHttpGet(AppConfig.URL_PUBLIC + "Lesson/CheckClassRoomExist?classroomID=" + classRoomId);
                    Log.e("getClassRoomLessonID3", jsonObject.toString()); // {"RetCode":0,"ErrorMessage":null,"DetailMessage":null,"RetData":-1}
                    int retCode = jsonObject.getInt("RetCode");
                    switch (retCode) {
                        case 0:
                            int retdate = jsonObject.getInt("RetData");
                            Message msg = Message.obtain();
                            msg.what = 0x1003;
                            msg.obj = retdate;
                            handler.sendMessage(msg);
                            break;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }).start(ThreadManager.getManager());

    }


    private void getClassRoomLessonID(final String classRoomId) {
        new ApiTask(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject jsonObject = ConnectService.getIncidentbyHttpGet(AppConfig.URL_PUBLIC + "Lesson/GetClassRoomLessonID?classRoomID=" + classRoomId);
                    Log.e("getClassRoomLessonID", jsonObject.toString()); // {"RetCode":0,"ErrorMessage":null,"DetailMessage":null,"RetData":-1}
                    int retCode = jsonObject.getInt("RetCode");
                    switch (retCode) {
                        case 0:
                            int retdate = jsonObject.getInt("RetData");
                            Message msg = Message.obtain();
                            msg.what = 0x1001;
                            msg.obj = retdate;
                            handler.sendMessage(msg);
                            break;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }).start(ThreadManager.getManager());

    }


    private void getClassRoomTeacherID(final String classRoomId) {
        new ApiTask(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject jsonObject = ConnectService.getIncidentbyHttpGet(AppConfig.URL_PUBLIC + "Lesson/GetClassRoomTeacherID?classroomID=" + classRoomId);
                    Log.e("getClassRoomLessonID4", jsonObject.toString()); // {"RetCode":0,"ErrorMessage":null,"DetailMessage":null,"RetData":-1}
                    int retCode = jsonObject.getInt("RetCode");
                    switch (retCode) {
                        case 0:
                            int retdate = jsonObject.getInt("RetData");
                            Message msg = Message.obtain();
                            msg.what = 0x1004;
                            msg.obj = retdate;
                            handler.sendMessage(msg);
                            break;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }).start(ThreadManager.getManager());
    }


    private List<UpcomingLesson> upcomingLessonList = new ArrayList<>();

    private void getUpcomingLessonList(final String teacherid) {
        new ApiTask(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject jsonObject = ConnectService.getIncidentbyHttpGet(AppConfig.URL_PUBLIC + "Lesson/UpcomingLessonList?teacherID=" + teacherid);
                    Log.e("upcoming", teacherid + "   " + jsonObject.toString());
                    int retCode = jsonObject.getInt("RetCode");
                    switch (retCode) {
                        case 0:
                            JSONArray jsonArray = jsonObject.getJSONArray("RetData");
                            upcomingLessonList.clear();
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject js = jsonArray.getJSONObject(i);
                                UpcomingLesson upcomingLesson = new UpcomingLesson();
                                upcomingLesson.setLessonID(js.getString("LessonID"));
                                upcomingLesson.setTitle(js.getString("Title"));
                                upcomingLesson.setStartDate(js.getString("StartDate"));
                                upcomingLesson.setTeacherID(js.getString("TeacherID"));
                                upcomingLesson.setStudentID(js.getString("StudentID"));
                                upcomingLesson.setCourseID(js.getString("CourseID"));
                                upcomingLesson.setCourseName(js.getString("CourseName"));
                                upcomingLesson.setLectureIDs(js.getString("LectureIDs"));
                                upcomingLesson.setIsInClassroom(js.getInt("IsInClassroom"));
                                upcomingLesson.setIsOnGoing(js.getInt("IsOnGoing"));
                                upcomingLessonList.add(upcomingLesson);
                            }
                            Message message = Message.obtain();
                            message.what = 0x1305;
                            handler.sendMessage(message);
                            break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start(ThreadManager.getManager());

    }


    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 0x1003:  // CheckClassRoomExist
                    int retdata2 = (int) msg.obj;
                    if (retdata2 == 0) { //不存在
                        Toast.makeText(mContext, "你加入的课堂不存在!", Toast.LENGTH_LONG).show();
                    } else if (retdata2 == 1) { // 存在
                        getClassRoomLessonID(roomid);
                    }
                    break;
                case 0x1001:   // getClassRoomLessonID
                    lessionid = (int) msg.obj;
                    getClassRoomTeacherID(roomid);
                    break;
                case 0x1004:  // getClassRoomTeacherID
                    teacherid = (int) msg.obj;
                    if (lessionid == -1) {     //看看老师是否正在上课
                        getUpcomingLessonList(teacherid + "");
                    } else {
                        Intent ii = new Intent(mContext, WatchCourseActivity3.class);
                        ii.putExtra("meetingId", roomid + "");
                        ii.putExtra("identity", 1);  // 学生
                        ii.putExtra("ishavedefaultpage", true);
                        ii.putExtra("lessionId", lessionid + "");
                        ii.putExtra("isInstantMeeting", 1);
                        ii.putExtra("teacherid", teacherid + "");
                        mContext.startActivity(ii);
                    }
                    break;
                case 0x1305:
                    for (int i = 0; i < upcomingLessonList.size(); i++) {
                        if (upcomingLessonList.get(i).getIsOnGoing() == 1) {
                            lesson = upcomingLessonList.get(i);
                        }
                    }
                    if (null == lesson) {  // 进去等待
                        Intent ii = new Intent(mContext, WatchCourseActivity3.class);
                        ii.putExtra("meetingId", roomid + "");
                        ii.putExtra("identity", 1);  // 学生
                        ii.putExtra("lessionId", lessionid + "");
                        ii.putExtra("ishavedefaultpage", true);
                        ii.putExtra("isInstantMeeting", 1);
                        ii.putExtra("teacherid", teacherid + "");
                        mContext.startActivity(ii);
                    } else {
                        if (lesson.getIsInClassroom() == 1) {
                            Intent ii = new Intent(mContext, WatchCourseActivity3.class);
                            ii.putExtra("meetingId", roomid + "");
                            ii.putExtra("identity", 1);  // 学生
                            ii.putExtra("ishavedefaultpage", true);
                            ii.putExtra("lessionId", lessionid + "");
                            ii.putExtra("isInstantMeeting", 1);
                            ii.putExtra("teacherid", teacherid + "");
                            mContext.startActivity(ii);
                        } else {
                            Intent ii = new Intent(mContext, WatchCourseActivity2.class);
                            ii.putExtra("meetingId", lesson.getLessonID() + "");
                            ii.putExtra("identity", 1);  // 学生
                            ii.putExtra("lessionId", lessionid + "");
                            ii.putExtra("isInstantMeeting", 0);
                            ii.putExtra("teacherid", teacherid + "");
                            mContext.startActivity(ii);
                        }
                    }
                    break;
            }
        }
    };

}