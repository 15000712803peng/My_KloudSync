package com.kloudsync.techexcel.frgment;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.kloudsync.techexcel.R;
import com.kloudsync.techexcel.bean.EventJoinMeeting;
import com.kloudsync.techexcel.bean.MeetingConfig;
import com.kloudsync.techexcel.config.AppConfig;
import com.kloudsync.techexcel.help.ApiTask;
import com.kloudsync.techexcel.help.ShowMyMeetingIdDialog;
import com.kloudsync.techexcel.help.SpaceMemberOperationDialog;
import com.kloudsync.techexcel.help.ThreadManager;
import com.kloudsync.techexcel.school.SelectSchoolActivity;
import com.kloudsync.techexcel.school.SwitchOrganizationActivity;
import com.kloudsync.techexcel.service.ConnectService;
import com.kloudsync.techexcel.ui.DocAndMeetingActivity;
import com.mining.app.zxing.MipcaActivityCapture;
import com.ub.service.activity.MeetingPropertyActivity;
import com.ub.service.activity.MeetingSearchResultsActivity;
import com.ub.service.activity.MeetingShareActivity;
import com.ub.service.activity.MyKlassroomActivity;
import com.ub.service.activity.NewMeetingActivity;
import com.ub.service.activity.NewPublicLessonActivity;
import com.ub.service.activity.SelectCourseActivity;
import com.ub.service.activity.WatchCourseActivity2;
import com.ub.service.activity.WatchCourseActivity3;
import com.ub.techexcel.adapter.ServiceAdapter2;
import com.ub.techexcel.bean.ServiceBean;
import com.ub.techexcel.tools.EventSchoolPopup;
import com.ub.techexcel.tools.JoinMeetingPopup;
import com.ub.techexcel.tools.MeetingMoreOperationPopup;
import com.ub.techexcel.tools.MenuEventPopup;
import com.ub.techexcel.tools.ServiceInterfaceListener;
import com.ub.techexcel.tools.ServiceInterfaceTools;
import com.ub.techexcel.tools.ServiceTool;
import com.ub.techexcel.tools.Tools;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.reactivex.Observable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class ServiceFragment extends MyFragment implements View.OnClickListener {
    private boolean isPrepared = false;

    private ServiceAdapter2 serviceAdapter1;
    private ListView serviceListView1;
    private ServiceAdapter2 serviceAdapter2;
    private ListView serviceListView2;
    private ServiceAdapter2 serviceAdapter3;
    private ListView serviceListView3;
    private List<ServiceBean> mList1 = new ArrayList<>();
    private List<ServiceBean> mList2 = new ArrayList<>();
    private List<ServiceBean> mList3 = new ArrayList<>();
    private List<List<ServiceBean>> mlist = new ArrayList<>();  //所有课程的list集合

    private ViewPager mViewPager;
    private List<View> viewList = new ArrayList<>();
    private TextView inprogressunderline;
    private int currIndex = 0;
    private int width;

    private ImageView addService;
    private ImageView img_notice;
    private TextView underline;
    private TextView tv_ns;
    private BroadcastReceiver broadcastReceiver;

    private BroadcastReceiver broadcastReceiver_finish;
    private EventSchoolPopup eventSchoolPopup;
    private MenuEventPopup menuPopupWindow;

    private RelativeLayout lin_myroom, lin_join, lin_schedule;
    private TextView tv_title;
    private TextView xxschool;
    private ImageView changeschool;
    private int lessionid;
    private TextView upcoming, pastdue, finished;
    private RelativeLayout search_layout;
    private ImageView switchCompanyImage;

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case AppConfig.LOAD_FINISH:
                    serviceAdapter1 = new ServiceAdapter2(getActivity(), mList1, true, 0);
                    serviceListView1.setAdapter(serviceAdapter1);

                    serviceAdapter2 = new ServiceAdapter2(getActivity(), mList2, true, 1);
                    serviceAdapter2.setOnModifyServiceListener(new ServiceAdapter2.OnModifyServiceListener() {
                        @Override
                        public void select(final ServiceBean bean) {
                            MeetingMoreOperationPopup meetingMoreOperationPopup = new MeetingMoreOperationPopup();
                            meetingMoreOperationPopup.getPopwindow(getActivity());
                            meetingMoreOperationPopup.setFavoritePoPListener(new MeetingMoreOperationPopup.FavoritePoPListener() {

                                @Override
                                public void delete() {
                                    deleteMeeting(bean);
                                }

                                @Override
                                public void view() {
                                    Intent intent = new Intent(getActivity(), WatchCourseActivity2.class);
                                    intent.putExtra("userid", bean.getUserId());
                                    intent.putExtra("meetingId", bean.getId() + "");
                                    intent.putExtra("teacherid", bean.getTeacherId());
                                    intent.putExtra("identity", bean.getRoleinlesson());
                                    intent.putExtra("isInstantMeeting", 0);
                                    intent.putExtra("isStartCourse", true);
                                    intent.putExtra("isPrepare", true);
                                    intent.putExtra("yinxiangmode", 1);
                                    startActivity(intent);
                                }

                                @Override
                                public void edit() {

                                }

                                @Override
                                public void startMeeting() {

                                }

                                @Override
                                public void dismiss() {
                                }

                                @Override
                                public void open() {
                                }

                                @Override
                                public void property() {

                                }

                            });
                            meetingMoreOperationPopup.StartPop(mViewPager, bean, 1);
                        }
                    });
                    serviceListView2.setAdapter(serviceAdapter2);

                    serviceAdapter3 = new ServiceAdapter2(getActivity(), mList3, true, 2);
                    serviceAdapter3.setOnModifyServiceListener(new ServiceAdapter2.OnModifyServiceListener() {
                        @Override
                        public void select(final ServiceBean bean) {
                            MeetingMoreOperationPopup meetingMoreOperationPopup = new MeetingMoreOperationPopup();
                            meetingMoreOperationPopup.getPopwindow(getActivity());
                            meetingMoreOperationPopup.setFavoritePoPListener(new MeetingMoreOperationPopup.FavoritePoPListener() {

                                @Override
                                public void delete() {
                                    deleteMeeting(bean);
                                }

                                @Override
                                public void view() {
                                    Intent intent = new Intent(getActivity(), WatchCourseActivity2.class);
                                    intent.putExtra("userid", bean.getUserId());
                                    intent.putExtra("meetingId", bean.getId() + "");
                                    intent.putExtra("teacherid", bean.getTeacherId());
                                    intent.putExtra("identity", bean.getRoleinlesson());
                                    intent.putExtra("isInstantMeeting", 0);
                                    intent.putExtra("isStartCourse", true);
                                    intent.putExtra("isPrepare", true);
                                    intent.putExtra("filemeetingId", bean.getId() + "");
                                    intent.putExtra("isFinished", true);
                                    intent.putExtra("yinxiangmode", 1);
                                    startActivity(intent);
                                }

                                @Override
                                public void edit() {  //SHARE

                                    Intent shareintent = new Intent(getActivity(), MeetingShareActivity.class);
                                    shareintent.putExtra("lesson", bean);
                                    startActivity(shareintent);

                                }

                                @Override
                                public void startMeeting() {

                                }

                                @Override
                                public void dismiss() {
                                }

                                @Override
                                public void open() {
                                }

                                @Override
                                public void property() {
                                    Intent intent = new Intent(getActivity(), MeetingPropertyActivity.class);
                                    intent.putExtra("servicebean", bean);
                                    getActivity().startActivity(intent);
                                }

                            });
                            meetingMoreOperationPopup.StartPop(mViewPager, bean, 2);
                        }
                    });
                    serviceListView3.setAdapter(serviceAdapter3);
                    break;
                case AppConfig.CONFIRM_SERVICE:  //结束课程
                    break;
                case 0x1102:
                    Intent intent = new Intent(getActivity(), SelectCourseActivity.class);
                    intent.putExtra("service", (ServiceBean) msg.obj);
                    AppConfig.ISMODIFY_SERVICE = true;
                    startActivity(intent);
                    break;
                case 0x1104:  //加入会议室
                    ServiceBean serviceBean = (ServiceBean) msg.obj;
                    Intent intent3 = new Intent(getActivity(), WatchCourseActivity2.class);
                    intent3.putExtra("isHtml", serviceBean.getLineItems().get(0).isHtml5());
                    intent3.putExtra("url", serviceBean.getLineItems().get(0).getUrl());
                    intent3.putExtra("CustomerRongCloudID", serviceBean.getCustomerRongCloudId());
                    intent3.putExtra("attachmentid", serviceBean.getLineItems().get(0).getAttachmentID());
                    intent3.putExtra("userid", serviceBean.getUserId());
                    intent3.putExtra("meetingId", serviceBean.getId() + "");
                    intent3.putExtra("teacherid", serviceBean.getTeacherId());
                    intent3.putExtra("isInstantMeeting", 0);
                    if (serviceBean.getTeacherId().equals((AppConfig.UserID + "").replace("-", ""))) {
                        intent3.putExtra("identity", 2);
                    } else if (serviceBean.getUserId().equals((AppConfig.UserID + "").replace("-", ""))) {
                        intent3.putExtra("identity", 1);
                    } else {
                        intent3.putExtra("identity", 3);
                    }
                    intent3.putExtra("lineItems", (Serializable) serviceBean.getLineItems());
                    startActivity(intent3);
                    break;
                case 0x1001:   // start meeting
                    lessionid = (int) msg.obj;
                    if (lessionid == -1) {
                        addInstantLesson(AppConfig.ClassRoomID);
                    } else {
                        Intent intent2 = new Intent(getActivity(), WatchCourseActivity3.class);
                        intent2.putExtra("meetingId", AppConfig.ClassRoomID + "");
                        intent2.putExtra("identity", 2);
                        intent2.putExtra("lessionId", lessionid + "");
                        intent2.putExtra("ishavedefaultpage", true);
                        intent2.putExtra("isInstantMeeting", 1);
                        intent2.putExtra("teacherid", AppConfig.UserID.replace("-", ""));
                        intent2.putExtra("isStartCourse", true);
                        intent2.putExtra("isTeamspace", false);
                        startActivity(intent2);
                    }
                    break;
                case 0x1002:  // addInstantLesson
                    Log.e("getClassRoomLessonID", "加入课程成功");
                    int lessionid2 = (int) msg.obj;
                    Intent intent5 = new Intent(getActivity(), WatchCourseActivity3.class);
                    intent5.putExtra("meetingId", AppConfig.ClassRoomID + "");
                    intent5.putExtra("identity", 2);
                    intent5.putExtra("ishavedefaultpage", true);
                    intent5.putExtra("lessionId", lessionid2 + "");
                    intent5.putExtra("isInstantMeeting", 1);
                    intent5.putExtra("isTeamspace", false);
                    intent5.putExtra("teacherid", AppConfig.UserID.replace("-", ""));
                    intent5.putExtra("isStartCourse", true);
                    startActivity(intent5);
                    break;
                default:
                    break;
            }
        }
    };


    /**
     * 确认结束课程
     */
    private Dialog dialog;

    private void deleteMeeting(final ServiceBean bean) {
        final LayoutInflater inflater = LayoutInflater
                .from(getActivity());
        View windov = inflater.inflate(R.layout.confirmservice, null);
        windov.findViewById(R.id.no).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                dialog.dismiss();
            }
        });
        windov.findViewById(R.id.yes).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                dialog.dismiss();
                new ApiTask(new Runnable() {
                    @Override
                    public void run() {
                        JSONObject jsonObject = ConnectService.getIncidentDataattachment(AppConfig.URL_PUBLIC + "Lesson/Delete?lessonID=" + bean.getId());
                        try {
                            int retcode = jsonObject.getInt("RetCode");
                            if (retcode == 0) {

                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }).start(ThreadManager.getManager());

            }
        });
//        dialog = new AlertDialog.Builder(getActivity()).show();
//        Window dialogWindow = dialog.getWindow();
//        WindowManager m = getActivity().getWindowManager();
//        Display d = m.getDefaultDisplay();
//        WindowManager.LayoutParams p = dialogWindow.getAttributes();
//        p.width = (int) (d.getWidth() * 0.8);
//        dialogWindow.setAttributes(p);
//        dialog.setCanceledOnTouchOutside(true);
//        dialog.setContentView(windov);


        dialog = new Dialog(getActivity(), R.style.bottom_dialog);

        dialog.setContentView(windov);
        dialog.getWindow().setWindowAnimations(R.style.dialogwindowAnim);
        dialog.getWindow().setGravity(Gravity.BOTTOM);
        WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
        params.width = getActivity().getResources().getDisplayMetrics().widthPixels;
        dialog.getWindow().setAttributes(params);
        dialog.show();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
    }


    View view;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (view == null) {
            view = inflater.inflate(R.layout.service, container, false);
            initView(view);
        }
//        GetSchoolInfo();
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        broadcastReceiver_finish = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

            }
        };
        IntentFilter filter_finish = new IntentFilter();
        filter_finish.addAction("com.ubao.techexcel.frgment");
        getActivity().registerReceiver(broadcastReceiver_finish, filter_finish);
    }

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        getActivity().unregisterReceiver(broadcastReceiver_finish);
        broadcastReceiver_finish = null;
        if (broadcastReceiver != null && getActivity() != null) {
            getActivity().unregisterReceiver(broadcastReceiver);
            broadcastReceiver = null;
        }
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }


    @Override
    protected void lazyLoad() {
        if (isPrepared && isVisible) {

        }
    }


    @Override
    public void onResume() {
        super.onResume();
        if (AppConfig.newlesson) {  //新建课程刷新
            AppConfig.newlesson = false;
        }
        if (tv_title != null) {  //切换schoolId刷新
//            GetSchoolInfo();
        }
    }


    private int schoolid = -1;


    private void initView(View view) {
        Fresco.initialize(getActivity());
        lin_myroom = (RelativeLayout) view.findViewById(R.id.lin_myroom);
        lin_join = (RelativeLayout) view.findViewById(R.id.lin_join);
        lin_schedule = (RelativeLayout) view.findViewById(R.id.lin_schedule);
        lin_myroom.setOnClickListener(this);
        lin_join.setOnClickListener(this);
        lin_schedule.setOnClickListener(this);
        search_layout = (RelativeLayout) view.findViewById(R.id.search_layout);
        search_layout.setOnClickListener(this);
        switchCompanyImage = (ImageView) view.findViewById(R.id.image_switch_company);
        switchCompanyImage.setOnClickListener(this);
        inprogressunderline = (TextView) view.findViewById(R.id.inprogressunderline);
        DisplayMetrics dm = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
        int screenW = dm.widthPixels;
        width = screenW / 3;
        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) inprogressunderline.getLayoutParams();
        lp.width = width / 2; //设置滑动条的宽度为屏幕的1/6
        lp.leftMargin = width / 4;
        inprogressunderline.setLayoutParams(lp);

        upcoming = (TextView) view.findViewById(R.id.upcoming);
        upcoming.setOnClickListener(this);
        pastdue = (TextView) view.findViewById(R.id.pastdue);
        pastdue.setOnClickListener(this);
        finished = (TextView) view.findViewById(R.id.finished);
        finished.setOnClickListener(this);


        changeschool = (ImageView) view.findViewById(R.id.changeschool);
        changeschool.setOnClickListener(this);
        tv_title = (TextView) view.findViewById(R.id.tv_title);
        underline = (TextView) view.findViewById(R.id.underline);
        xxschool = (TextView) view.findViewById(R.id.xxschool);

        tv_ns = (TextView) view.findViewById(R.id.tv_ns);

        mViewPager = (ViewPager) view.findViewById(R.id.viewpager);

        LayoutInflater layoutInflater = getActivity().getLayoutInflater();
        View allView1 = layoutInflater.inflate(R.layout.tabone, null);
        View allView2 = layoutInflater.inflate(R.layout.tabtwo, null);
        View allView3 = layoutInflater.inflate(R.layout.tabthree, null);

//        serviceListView1 = (ListView) allView1.findViewById(R.id.serviceList);
//        serviceListView2 = (ListView) allView2.findViewById(R.id.serviceList);
//        serviceListView3 = (ListView) allView3.findViewById(R.id.serviceList);
//        viewList.add(allView1);
//        viewList.add(allView2);
//        viewList.add(allView3);

        mViewPager.setAdapter(new MeetingAdapter(getChildFragmentManager()));
//        mViewPager.setCurrentItem(0);

        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                Log.e("positionOffset", position + "  " + positionOffset);
                float currMarginLeft = width * positionOffset + position * width + width / 4;
                RelativeLayout.LayoutParams redLp = (RelativeLayout.LayoutParams) inprogressunderline.getLayoutParams();
                redLp.leftMargin = (int) currMarginLeft;
                inprogressunderline.setLayoutParams(redLp);
            }

            @Override
            public void onPageSelected(int position) {
                switch (position) {
                    case 0:
                        setDefault();
                        upcoming.setTextColor(getResources().getColor(R.color.skyblue));
                        break;
                    case 1:
                        setDefault();
                        pastdue.setTextColor(getResources().getColor(R.color.skyblue));
                        break;
                    case 2:
                        setDefault();
                        finished.setTextColor(getResources().getColor(R.color.skyblue));
                    default:
                        break;
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        GetCourseBroad();

    }


    class MeetingAdapter extends FragmentPagerAdapter {

        public MeetingAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            Bundle bundle = new Bundle();
            bundle.putInt("type", position + 1);
            MeetingFragment fragment = new MeetingFragment();
            fragment.setArguments(bundle);
            return fragment;
        }

        @Override
        public int getCount() {
            return 3;
        }
    }


    private void GetCourseBroad() {
        RefreshNotify();
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                RefreshNotify();
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(getString(R.string.Receive_Course));
        getActivity().registerReceiver(broadcastReceiver, filter);
    }


    private void RefreshNotify() {
        int sum = 0;
        for (int i = 0; i < AppConfig.progressCourse.size(); i++) {
            if (!AppConfig.progressCourse.get(i).isStatus()) {
                sum++;
            }
        }
        if (tv_ns != null) {
            tv_ns.setText(sum + "");
            tv_ns.setVisibility(sum == 0 ? View.GONE : View.VISIBLE);
        }

    }

    private ShowMyMeetingIdDialog showMyMeetingIdDialog;
    private JoinMeetingPopup joinMeetingPopup;

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.addService:
                menuPopupWindow = new MenuEventPopup();
                menuPopupWindow.getPopwindow(getActivity());
                menuPopupWindow.StartPop(addService);
                menuPopupWindow.setWebCamPopupListener(new MenuEventPopup.WebCamPopupListener() {
                    @Override
                    public void changeOptions(int position) {
                        Intent i;
                        switch (position) {
                            case 0: //new private lesson
                                i = new Intent(getActivity(), SelectCourseActivity.class);
                                startActivity(i);
                                getActivity().overridePendingTransition(R.anim.tran_in5, R.anim.tran_out5);
                                break;
                            case 1:
                                i = new Intent(getActivity(), NewPublicLessonActivity.class);
                                startActivity(i);
                                getActivity().overridePendingTransition(R.anim.tran_in5, R.anim.tran_out5);
                                break;
                            case 2:
                                i = new Intent(getActivity(), MyKlassroomActivity.class);
                                startActivity(i);
                                getActivity().overridePendingTransition(R.anim.tran_in5, R.anim.tran_out5);
                                break;
                            case 3:
                                i = new Intent(getActivity(), MipcaActivityCapture.class);
                                startActivity(i);
                                getActivity().overridePendingTransition(R.anim.tran_in5, R.anim.tran_out5);
                                break;
                            case 4:
                                i = new Intent(getActivity(), SelectSchoolActivity.class);
                                startActivity(i);
                                getActivity().overridePendingTransition(R.anim.tran_in5, R.anim.tran_out5);
                                break;
                        }
                    }
                });
                break;
            case R.id.img_notice:
                startActivity(new Intent(getActivity(), MeetingSearchResultsActivity.class));
                break;
            case R.id.search_layout:
                Intent searchIntnt = new Intent(getActivity(), MeetingSearchResultsActivity.class);
                searchIntnt.putExtra("type", mViewPager.getCurrentItem());
                startActivity(searchIntnt);
                break;
            case R.id.lin_myroom:
                if (showMyMeetingIdDialog == null) {
                    showMyMeetingIdDialog = new ShowMyMeetingIdDialog(getActivity(), AppConfig.ClassRoomID.replaceAll("-", ""));
                    showMyMeetingIdDialog.setOptionsLinstener(new ShowMyMeetingIdDialog.InviteOptionsLinstener() {
                        @Override
                        public void enter() {
                            if (!Tools.isFastClick()) {
                                if (TextUtils.isEmpty(AppConfig.ClassRoomID)) {
                                    Toast.makeText(getActivity(), "你加入的课堂不存在!", Toast.LENGTH_LONG).show();
                                } else {
//                                    getClassRoomLessonID(AppConfig.ClassRoomID);
                                    doStartMeeting(AppConfig.ClassRoomID);
                                }
                            }
                        }
                    });
                }
                showMyMeetingIdDialog.show();
                break;
            case R.id.lin_join: // join meeting
                if (joinMeetingPopup == null) {
                    joinMeetingPopup = new JoinMeetingPopup();
                    joinMeetingPopup.getPopwindow(getActivity());
                    joinMeetingPopup.setFavoritePoPListener(new JoinMeetingPopup.FavoritePoPListener() {
                        @Override
                        public void dismiss() {

                        }

                        @Override
                        public void open() {

                        }
                    });
                }
                joinMeetingPopup.StartPop(lin_schedule);
                break;
            case R.id.lin_schedule:
                Intent schintent = new Intent(getActivity(), NewMeetingActivity.class);
                startActivity(schintent);
                getActivity().overridePendingTransition(R.anim.tran_in5, R.anim.tran_out5);
                break;
            case R.id.changeschool:
                eventSchoolPopup = new EventSchoolPopup();
                eventSchoolPopup.getPopwindow(getActivity());
                eventSchoolPopup.StartPop(changeschool);
                eventSchoolPopup.setWebCamPopupListener(new EventSchoolPopup.WebCamPopupListener() {
                    @Override
                    public void changeOptions(int position) {
                        if (position == 0) {

                        } else if (position == 1) {

                        }
                    }
                });
                break;
            case R.id.upcoming:
                mViewPager.setCurrentItem(0);
                setDefault();
                upcoming.setTextColor(getResources().getColor(R.color.skyblue));
                break;
            case R.id.pastdue:
                mViewPager.setCurrentItem(1);
                setDefault();
                pastdue.setTextColor(getResources().getColor(R.color.skyblue));
                break;
            case R.id.finished:
                mViewPager.setCurrentItem(2);
                setDefault();
                finished.setTextColor(getResources().getColor(R.color.skyblue));
                break;
            default:
                break;
            case R.id.image_switch_company:
                goToSwitchCompany();
                break;
        }

    }

    private void goToSwitchCompany() {
        Intent intent = new Intent(getActivity(), SwitchOrganizationActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }


    private void setDefault() {
        upcoming.setTextColor(getResources().getColor(R.color.c5));
        pastdue.setTextColor(getResources().getColor(R.color.c5));
        finished.setTextColor(getResources().getColor(R.color.c5));
    }

    private void doStartMeeting(final String meetingRoom) {
        final EventJoinMeeting joinMeeting = new EventJoinMeeting();
        Observable.just(joinMeeting).observeOn(Schedulers.io()).doOnNext(new Consumer<EventJoinMeeting>() {
            @Override
            public void accept(EventJoinMeeting eventJoinMeeting) throws Exception {
                JSONObject params = new JSONObject();
                params.put("classroomID", meetingRoom.toUpperCase());
                params.put("addBlankPage", 0);
                JSONObject result = ConnectService.submitDataByJson(AppConfig.URL_PUBLIC + "Lesson/AddInstantLesson?classRoomID=" + meetingRoom + "&addBlankPage=0", params);
                Log.e("GetClassRoomLessonID", "meetingRoom:" + meetingRoom + ",result:" + result);
                if (result.has("RetCode")) {
                    int retCode = result.getInt("RetCode");
                    if (retCode == 0) {
                        joinMeeting.setLessionId(result.getInt("RetData"));
                        joinMeeting.setMeetingId(meetingRoom.toUpperCase());
                        joinMeeting.setRole(MeetingConfig.MeetingRole.HOST);
                    }
                }

                EventBus.getDefault().post(joinMeeting);
            }
        }).subscribe();
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

    private void addInstantLesson(final String classRoomId) {
        new ApiTask(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject js = new JSONObject();
                    js.put("classroomID", classRoomId);
                    js.put("addBlankPage", 0);
                    JSONObject jsonObject = ConnectService.submitDataByJson(AppConfig.URL_PUBLIC + "Lesson/AddInstantLesson?classroomID=" + classRoomId + "&addBlankPage=0", js);
                    Log.e("getClassRoomLessonID2", jsonObject.toString()); // {"RetCode":0,"ErrorMessage":null,"DetailMessage":null,"RetData":2477}
                    int retCode = jsonObject.getInt("RetCode");
                    switch (retCode) {
                        case 0:
                            int retdate = jsonObject.getInt("RetData");
                            Message msg = Message.obtain();
                            msg.what = 0x1002;
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void joinMeeting(EventJoinMeeting eventJoinMeeting) {
        Log.e("check_event_join_meeting","eventJoinMeeting:" + eventJoinMeeting);
        if (eventJoinMeeting.getLessionId() <= 0) {
            Toast.makeText(getActivity(), "加入的meeting不存在或没有开始", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(getActivity(), DocAndMeetingActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        //-----
        intent.putExtra("meeting_id", eventJoinMeeting.getMeetingId());
        intent.putExtra("meeting_type", 0);
        intent.putExtra("lession_id", eventJoinMeeting.getLessionId());
        intent.putExtra("meeting_role", eventJoinMeeting.getRole());
//        intent.putExtra("meeting_role", 3);
        intent.putExtra("from_meeting", true);
        startActivity(intent);
    }
}