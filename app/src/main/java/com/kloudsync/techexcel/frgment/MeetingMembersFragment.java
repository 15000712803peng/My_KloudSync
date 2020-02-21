package com.kloudsync.techexcel.frgment;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.kloudsync.techexcel.R;
import com.kloudsync.techexcel.bean.DeviceType;
import com.kloudsync.techexcel.bean.EventRefreshMembers;
import com.kloudsync.techexcel.bean.EventSetPresenter;
import com.kloudsync.techexcel.bean.MeetingConfig;
import com.kloudsync.techexcel.bean.MeetingMember;
import com.kloudsync.techexcel.config.AppConfig;
import com.kloudsync.techexcel.dialog.PopMeetingHandsMemberSetting;
import com.kloudsync.techexcel.dialog.PopMeetingMemberSetting;
import com.kloudsync.techexcel.dialog.PopMeetingSpeakMemberSetting;
import com.kloudsync.techexcel.help.MeetingKit;
import com.kloudsync.techexcel.httpgetimage.ImageLoader;
import com.kloudsync.techexcel.ui.DocAndMeetingActivity;
import com.kloudsync.techexcel.view.CircleImageView;
import com.ub.techexcel.tools.ServiceInterfaceTools;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by tonyan on 2019/11/9.
 */

public class MeetingMembersFragment extends MyFragment implements PopMeetingMemberSetting.OnMemberSettingChanged,PopMeetingSpeakMemberSetting.OnSpeakMemberSettingChanged, PopMeetingHandsMemberSetting.OnHandsMemberSettingChanged {

    private RecyclerView membersList;
    int type;
    private MeetingConfig meetingConfig;
    private MeetingMembersAdapter membersAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        type = getArguments().getInt("type");
        meetingConfig = DocAndMeetingActivity.meetingConfig;
        EventBus.getDefault().register(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);

    }


    @Override
    protected void lazyLoad() {
        Log.e("MeetingMembersFragment", "lazyLoad");
    }

    private View view;

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void refreshMeetingMembers(EventRefreshMembers refreshMembers) {
        this.meetingConfig = refreshMembers.getMeetingConfig();
        if(popMeetingMemberSetting != null && popMeetingMemberSetting.isShowing()){
            popMeetingMemberSetting.dismiss();
        }
        if(popMeetingSpeakMemberSetting != null && popMeetingSpeakMemberSetting.isShowing()){
            popMeetingMemberSetting.dismiss();
        }
        loadMembers();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (view == null) {
            view = inflater.inflate(R.layout.fragment_meeting_members, container, false);
            membersList = view.findViewById(R.id.list_members);
            loadMembers();
        }
        return view;
    }

    List<MeetingMember> meetingMembers = new ArrayList<>();

    private void loadMembers() {
        meetingMembers.clear();
        if (meetingConfig.getMeetingMembers() == null || meetingConfig.getMeetingMembers().size() <= 0) {
            return;
        }

        membersList.setLayoutManager(new LinearLayoutManager(getActivity(), RecyclerView.VERTICAL, false));
        if (type == 1) {
            meetingMembers.addAll(meetingConfig.getMeetingMembers());
            Collections.sort(meetingMembers);
            fetchCategoryDataForSpeakerTab(meetingConfig);
            if (mainSpeakersAdapter == null) {
                mainSpeakersAdapter = new MeetingMainSpeakersAdapter(getActivity(), tabSpeakersMembers);
                membersList.setAdapter(mainSpeakersAdapter);
            } else {
                mainSpeakersAdapter.updateMembers(tabSpeakersMembers);
            }

        } else if (type == 2) {
            meetingMembers.addAll(meetingConfig.getMeetingAuditor());
            if (membersAdapter == null) {
                membersAdapter = new MeetingMembersAdapter(getActivity(), meetingMembers);
                membersList.setAdapter(membersAdapter);
            } else {
                membersAdapter.updateMembers(meetingMembers);
            }
        } else if (type == 3) {
            meetingMembers.addAll(meetingConfig.getMeetingInvitors());
            if (membersAdapter == null) {
                membersAdapter = new MeetingMembersAdapter(getActivity(), meetingMembers);
                membersList.setAdapter(membersAdapter);
            } else {
                membersAdapter.updateMembers(meetingMembers);
            }
        }
    }

    List<MeetingMember> tabSpeakersMembers = new ArrayList<>();

    private void fetchCategoryDataForSpeakerTab(MeetingConfig meetingConfig) {
        tabSpeakersMembers.clear();
        if (meetingConfig.getMeetingMembers() != null && meetingConfig.getMeetingMembers().size() > 0) {
            List<MeetingMember> notTempStageMembers = new ArrayList<>();
            List<MeetingMember> tempStageMembers = new ArrayList<>();
            for (MeetingMember member : meetingConfig.getMeetingMembers()) {

                if (member.getTempOnStage() == 0) {
                    member.setViewType(MeetingMember.TYPE_ITEM_MAIN_SPEAKER);
                    notTempStageMembers.add(member);
                } else {
                    tempStageMembers.add(member);
                    member.setViewType(MeetingMember.TYPE_ITEM_SPEAKING_SPEAKER);
                }
            }

            tabSpeakersMembers.addAll(notTempStageMembers);
            if (tempStageMembers.size() > 0) {
                MeetingMember title = new MeetingMember();
                title.setViewType(MeetingMember.TYPE_SPARKER_TITLE);
                title.setTitle("可讲话参会者");
                tabSpeakersMembers.add(title);
                tabSpeakersMembers.addAll(tempStageMembers);

            }
        }

        if (meetingConfig.getMeetingAuditor() != null && meetingConfig.getMeetingAuditor().size() > 0) {
            List<MeetingMember> handsUpMembers = new ArrayList<>();
            for (MeetingMember auditor : meetingConfig.getMeetingAuditor()) {
                if (auditor.getHandStatus() == 1) {
                    auditor.setViewType(MeetingMember.TYPE_ITEM_HANDSUP_MEMBER);
                    handsUpMembers.add(auditor);
                }
            }

            if (handsUpMembers.size() > 0) {
                MeetingMember title = new MeetingMember();
                title.setViewType(MeetingMember.TYPE_HANDSUP_TITLE);
                title.setTitle("已举手参会者");
                tabSpeakersMembers.add(title);
                tabSpeakersMembers.addAll(handsUpMembers);
            }
        }

    }

    @Override
    public void setSpeakToAuditor(MeetingMember meetingMember) {
        Observable.just(meetingMember).observeOn(Schedulers.io()).doOnNext(new Consumer<MeetingMember>() {
            @Override
            public void accept(MeetingMember meetingMember) throws Exception {
                JSONObject response = ServiceInterfaceTools.getinstance().
                        syncMakeUserUpAndDown(meetingMember.getUserId() + "", 0);
                if (response.has("code")) {
                    if (response.getInt("code") == 0) {
                        MeetingKit.getInstance().requestMeetingMembers(meetingConfig);
                    } else if (response.getInt("code") == 22) {
                        Observable.just("toast_main_thread").observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer<String>() {
                            @Override
                            public void accept(String s) throws Exception {
                                Toast.makeText(getActivity(), "没有权限进行此操作", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }
        }).subscribe();
    }

    @Override
    public void setSpeakToMember(MeetingMember meetingMember) {
        Observable.just(meetingMember).observeOn(Schedulers.io()).doOnNext(new Consumer<MeetingMember>() {
            @Override
            public void accept(MeetingMember meetingMember) throws Exception {
                JSONObject response = ServiceInterfaceTools.getinstance().
                        syncChangeTemStatus(meetingMember.getUserId() + "", 0);
                if (response.has("code")) {
                    if (response.getInt("code") == 0) {
                        MeetingKit.getInstance().requestMeetingMembers(meetingConfig);
                    } else if (response.getInt("code") == 22) {
                        Observable.just("toast_main_thread").observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer<String>() {
                            @Override
                            public void accept(String s) throws Exception {
                                Toast.makeText(getActivity(), "没有权限进行此操作", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }
        }).subscribe();
    }

    @Override
    public void setHandsAllowSpeak(MeetingMember meetingMember) {
        Observable.just(meetingMember).observeOn(Schedulers.io()).doOnNext(new Consumer<MeetingMember>() {
            @Override
            public void accept(MeetingMember meetingMember) throws Exception {
                JSONObject response = ServiceInterfaceTools.getinstance().
                        syncMakeUserUpAndDownHands(meetingMember.getUserId() + "", 1,1);
                if (response.has("code")) {
                    if (response.getInt("code") == 0) {
                        MeetingKit.getInstance().requestMeetingMembers(meetingConfig);
                    } else if (response.getInt("code") == 22) {
                        Observable.just("toast_main_thread").observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer<String>() {
                            @Override
                            public void accept(String s) throws Exception {
                                Toast.makeText(getActivity(), "没有权限进行此操作", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }
        }).subscribe();
    }

    @Override
    public void setHandsDown(final MeetingMember meetingMember) {
        Observable.just("hands_up").observeOn(Schedulers.io()).doOnNext(new Consumer<String>() {
            @Override
            public void accept(String str) throws Exception {
                JSONObject response = ServiceInterfaceTools.getinstance().
                        syncHandUpOrDown(0,meetingMember.getUserId()+"");
                if (response.has("code")) {
                    if (response.getInt("code") == 0) {
                        MeetingKit.getInstance().requestMeetingMembers(meetingConfig);
                    }
                }
            }
        }).subscribe();
    }

    @Override
    public void setHandsMember(MeetingMember meetingMember) {
        Observable.just(meetingMember).observeOn(Schedulers.io()).doOnNext(new Consumer<MeetingMember>() {
            @Override
            public void accept(MeetingMember meetingMember) throws Exception {
                JSONObject response = ServiceInterfaceTools.getinstance().
                        syncMakeUserUpAndDownHands(meetingMember.getUserId() + "", 1,0);
                if (response.has("code")) {
                    if (response.getInt("code") == 0) {
                        MeetingKit.getInstance().requestMeetingMembers(meetingConfig);
                    } else if (response.getInt("code") == 22) {
                        Observable.just("toast_main_thread").observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer<String>() {
                            @Override
                            public void accept(String s) throws Exception {
                                Toast.makeText(getActivity(), "没有权限进行此操作", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }
        }).subscribe();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View view) {
            super(view);
            icon = (CircleImageView) view.findViewById(R.id.member_icon);
            name = (TextView) view.findViewById(R.id.name);
            presenter = view.findViewById(R.id.txt_presenter);
            me = view.findViewById(R.id.txt_is_me);
            type = view.findViewById(R.id.txt_type);
            settingImage = view.findViewById(R.id.image_setting);
            host = view.findViewById(R.id.txt_host);
            changeToMember = view.findViewById(R.id.txt_change_to_member);
            handsUpText = view.findViewById(R.id.txt_hands_up);
        }

        public CircleImageView icon;
        public TextView name;
        public TextView presenter;
        public TextView me;
        public TextView type;
        public ImageView settingImage;
        public TextView host;
        public TextView changeToMember;
        public TextView handsUpText;

    }

    public class MainSpeakerViewHolder extends RecyclerView.ViewHolder {
        public MainSpeakerViewHolder(View view) {
            super(view);
            icon = (CircleImageView) view.findViewById(R.id.member_icon);
            name = (TextView) view.findViewById(R.id.name);
            presenter = view.findViewById(R.id.txt_presenter);
            me = view.findViewById(R.id.txt_is_me);
            type = view.findViewById(R.id.txt_type);
            settingImage = view.findViewById(R.id.image_setting);
            host = view.findViewById(R.id.txt_host);
            changeToMember = view.findViewById(R.id.txt_change_to_member);
            handsUpText = view.findViewById(R.id.txt_hands_up);
        }

        public CircleImageView icon;
        public TextView name;
        public TextView presenter;
        public TextView me;
        public TextView type;
        public ImageView settingImage;
        public TextView host;
        public TextView changeToMember;
        public TextView handsUpText;

    }

    public class SpeakerViewHolder extends RecyclerView.ViewHolder {
        public SpeakerViewHolder(View view) {
            super(view);
            icon = (CircleImageView) view.findViewById(R.id.member_icon);
            name = (TextView) view.findViewById(R.id.name);
            presenter = view.findViewById(R.id.txt_presenter);
            me = view.findViewById(R.id.txt_is_me);
            type = view.findViewById(R.id.txt_type);
            settingImage = view.findViewById(R.id.image_setting);
            host = view.findViewById(R.id.txt_host);
        }

        public CircleImageView icon;
        public TextView name;
        public TextView presenter;
        public TextView me;
        public TextView type;
        public ImageView settingImage;
        public TextView host;

    }

    public class HandsUpViewHolder extends RecyclerView.ViewHolder {
        public HandsUpViewHolder(View view) {
            super(view);
            icon = (CircleImageView) view.findViewById(R.id.member_icon);
            name = (TextView) view.findViewById(R.id.name);
            presenter = view.findViewById(R.id.txt_presenter);
            me = view.findViewById(R.id.txt_is_me);
            type = view.findViewById(R.id.txt_type);
            settingImage = view.findViewById(R.id.image_setting);
            host = view.findViewById(R.id.txt_host);
        }

        public CircleImageView icon;
        public TextView name;
        public TextView presenter;
        public TextView me;
        public TextView type;
        public ImageView settingImage;
        public TextView host;

    }


    public class SpeakerTitleViewHolder extends RecyclerView.ViewHolder {
        public SpeakerTitleViewHolder(View view) {
            super(view);
            title = (TextView) view.findViewById(R.id.title);
        }

        public TextView title;
    }

    public class HandsupTitleViewHolder extends RecyclerView.ViewHolder {
        public HandsupTitleViewHolder(View view) {
            super(view);
            title = (TextView) view.findViewById(R.id.title);
        }

        public TextView title;
    }


    public class MeetingMembersAdapter extends RecyclerView.Adapter<ViewHolder> {
        private LayoutInflater inflater;
        private List<MeetingMember> meetingMembers = new ArrayList<>();
        public ImageLoader imageLoader;

        public MeetingMembersAdapter(Context context, List<MeetingMember> members) {
            inflater = LayoutInflater.from(context);
            meetingMembers.clear();
            meetingMembers.addAll(members);
            imageLoader = new ImageLoader(context);
        }

        public List<MeetingMember> getmDatas() {
            return meetingMembers;
        }

        public void updateMembers(List<MeetingMember> members) {
            Log.e("MeetingMembersAdapter", "updateMembers:" + members);
            meetingMembers.clear();
            meetingMembers.addAll(members);
            notifyDataSetChanged();
        }

        @Override
        public int getItemCount() {
            return meetingMembers.size();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = inflater.inflate(R.layout.meeting_member, parent, false);
            ViewHolder viewHolder = new ViewHolder(view);
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, final int position) {
            final MeetingMember member = meetingMembers.get(position);
            if(member == null){
                return;
            }
            holder.name.setText(member.getUserName());
            String url = member.getAvatarUrl();
            if (null == url || url.length() < 1) {
                holder.icon.setImageResource(R.drawable.hello);
            } else {
                imageLoader.DisplayImage(url, holder.icon);
            }

            if ((member.getUserId() + "").equals(AppConfig.UserID)) {
                holder.me.setVisibility(View.VISIBLE);
            } else {
                holder.me.setVisibility(View.GONE);
            }

            fillDeviceType(member.getDeviceType(), holder.type);

            if (type == 1) {
                if (meetingConfig.getMeetingHostId().equals(member.getUserId() + "")) {
                    holder.host.setVisibility(View.VISIBLE);
                } else {
                    holder.host.setVisibility(View.GONE);
                }
                if (member.getPresenter() == 1) {
                    holder.settingImage.setVisibility(View.GONE);
                    holder.presenter.setVisibility(View.VISIBLE);
                } else {
                    holder.settingImage.setVisibility(View.VISIBLE);
                    holder.presenter.setVisibility(View.GONE);
                }
                fillViewByRoleForMembers(member, holder);
                //------
            } else if (type == 2) {
                fillViewByRoleForAuditors(member, holder);

            } else if (type == 3) {

            }


            //----------

//            fillViewByRole(member, holder);

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                }
            });

        }
    }


    MeetingMainSpeakersAdapter mainSpeakersAdapter;

    public class MeetingMainSpeakersAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private LayoutInflater inflater;
        private List<MeetingMember> meetingMembers = new ArrayList<>();
        public ImageLoader imageLoader;

        @Override
        public int getItemViewType(int position) {
            return meetingMembers.get(position).getViewType();
        }


        public MeetingMainSpeakersAdapter(Context context, List<MeetingMember> members) {
            inflater = LayoutInflater.from(context);
            meetingMembers.clear();
            meetingMembers.addAll(members);
            imageLoader = new ImageLoader(context);
        }


        public List<MeetingMember> getmDatas() {
            return meetingMembers;
        }

        public void updateMembers(List<MeetingMember> members) {
            Log.e("MeetingMembersAdapter", "updateMembers:" + members);
            meetingMembers.clear();
            meetingMembers.addAll(members);
            notifyDataSetChanged();
        }

        @Override
        public int getItemCount() {
            return meetingMembers.size();
        }


        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = null;
            switch (viewType) {
                case MeetingMember.TYPE_ITEM_MAIN_SPEAKER:
                    view = inflater.inflate(R.layout.meeting_member, parent, false);
                    return new MainSpeakerViewHolder(view);
                case MeetingMember.TYPE_SPARKER_TITLE:
                    view = inflater.inflate(R.layout.meeting_speaking_member_title, parent, false);
                    return new SpeakerTitleViewHolder(view);
                case MeetingMember.TYPE_ITEM_SPEAKING_SPEAKER:
                    view = inflater.inflate(R.layout.meeting_speaking_member, parent, false);
                    return new SpeakerViewHolder(view);
                case MeetingMember.TYPE_HANDSUP_TITLE:
                    view = inflater.inflate(R.layout.meeting_handsup_member_title, parent, false);
                    return new HandsupTitleViewHolder(view);
                case MeetingMember.TYPE_ITEM_HANDSUP_MEMBER:
                    view = inflater.inflate(R.layout.meeting_hands_up_member, parent, false);
                    return new HandsUpViewHolder(view);
            }

            ViewHolder viewHolder = new ViewHolder(view);
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
            if (holder instanceof MainSpeakerViewHolder) {
                MainSpeakerViewHolder mainSpeakerViewHolder = (MainSpeakerViewHolder) holder;
                final MeetingMember member = meetingMembers.get(position);
                mainSpeakerViewHolder.name.setText(member.getUserName());
                String url = member.getAvatarUrl();
                if (null == url || url.length() < 1) {
                    mainSpeakerViewHolder.icon.setImageResource(R.drawable.hello);
                } else {
                    imageLoader.DisplayImage(url, mainSpeakerViewHolder.icon);
                }

                if ((member.getUserId() + "").equals(AppConfig.UserID)) {
                    mainSpeakerViewHolder.me.setVisibility(View.VISIBLE);
                } else {
                    mainSpeakerViewHolder.me.setVisibility(View.GONE);
                }

                fillDeviceType(member.getDeviceType(), mainSpeakerViewHolder.type);

                if (meetingConfig.getMeetingHostId().equals(member.getUserId() + "")) {
                    mainSpeakerViewHolder.host.setVisibility(View.VISIBLE);
                } else {
                    mainSpeakerViewHolder.host.setVisibility(View.GONE);
                }
                if (member.getPresenter() == 1) {
                    mainSpeakerViewHolder.settingImage.setVisibility(View.GONE);
                    mainSpeakerViewHolder.presenter.setVisibility(View.VISIBLE);
                } else {
                    mainSpeakerViewHolder.settingImage.setVisibility(View.VISIBLE);
                    mainSpeakerViewHolder.presenter.setVisibility(View.GONE);
                }
                fillViewByRoleForMainSpeakingMembers(member, mainSpeakerViewHolder);
                //------

            } else if (holder instanceof SpeakerViewHolder) {
                SpeakerViewHolder speakerViewHolder = (SpeakerViewHolder) holder;
                final MeetingMember member = meetingMembers.get(position);
                speakerViewHolder.name.setText(member.getUserName());
                String url = member.getAvatarUrl();
                if (null == url || url.length() < 1) {
                    speakerViewHolder.icon.setImageResource(R.drawable.hello);
                } else {
                    imageLoader.DisplayImage(url, speakerViewHolder.icon);
                }

                if ((member.getUserId() + "").equals(AppConfig.UserID)) {
                    speakerViewHolder.me.setVisibility(View.VISIBLE);
                } else {
                    speakerViewHolder.me.setVisibility(View.GONE);
                }

                fillDeviceType(member.getDeviceType(), speakerViewHolder.type);
                fillViewByRoleForSpeakingMembers(member,speakerViewHolder);
            } else if (holder instanceof HandsupTitleViewHolder) {

            } else if (holder instanceof HandsUpViewHolder) {
                HandsUpViewHolder handsUpViewHolder = (HandsUpViewHolder) holder;
                final MeetingMember member = meetingMembers.get(position);
                handsUpViewHolder.name.setText(member.getUserName());
                String url = member.getAvatarUrl();
                if (null == url || url.length() < 1) {
                    handsUpViewHolder.icon.setImageResource(R.drawable.hello);
                } else {
                    imageLoader.DisplayImage(url, handsUpViewHolder.icon);
                }

                if ((member.getUserId() + "").equals(AppConfig.UserID)) {
                    handsUpViewHolder.me.setVisibility(View.VISIBLE);
                } else {
                    handsUpViewHolder.me.setVisibility(View.GONE);
                }

                fillDeviceType(member.getDeviceType(), handsUpViewHolder.type);
                fillViewByRoleForHandsMembers(member,handsUpViewHolder);
            }
        }
    }

    private void handleMemeber() {

    }

    private void handleAuditors() {

    }

    private void handleInvitors() {

    }

    private void fillDeviceType(int deviceType, TextView typeText) {
        switch (deviceType) {
            case DeviceType.WEB:
                typeText.setVisibility(View.VISIBLE);
                typeText.setBackgroundResource(R.drawable.bg_web);
                typeText.setTextColor(Color.parseColor("#6A6DEB"));
                typeText.setText("Web");
                break;
            case DeviceType.ANDROID:
                typeText.setVisibility(View.VISIBLE);
                typeText.setBackgroundResource(R.drawable.bg_android);
                typeText.setTextColor(Color.parseColor("#26C184"));
                typeText.setText("Android");

                break;
            case DeviceType.IPHONE:
                typeText.setVisibility(View.VISIBLE);
                typeText.setBackgroundResource(R.drawable.bg_iphone);
                typeText.setTextColor(Color.parseColor("#999999"));
                typeText.setText("IOS");
                break;
            case DeviceType.TV:
                typeText.setVisibility(View.VISIBLE);
                typeText.setBackgroundResource(R.drawable.bg_tv);
                typeText.setTextColor(getActivity().getResources().getColor(R.color.darkblack2));
                typeText.setText("TV");
                break;
            default:
                typeText.setText("");
                typeText.setVisibility(View.GONE);
                break;
        }
    }

    PopMeetingMemberSetting popMeetingMemberSetting;

    private void showMemberSetting(MeetingMember member, View view) {
        if (popMeetingMemberSetting != null) {
            if (popMeetingMemberSetting.isShowing()) {
                popMeetingMemberSetting.dismiss();
            }
            popMeetingMemberSetting = null;
        }

        popMeetingMemberSetting = new PopMeetingMemberSetting(getActivity());
        popMeetingMemberSetting.setOnMemberSettingChanged(this);
        popMeetingMemberSetting.showAtBottom(member, view, meetingConfig);
    }

    PopMeetingSpeakMemberSetting popMeetingSpeakMemberSetting;

    private void showSpeakMemberSetting(MeetingMember member, View view) {
        if (popMeetingSpeakMemberSetting != null) {
            if (popMeetingSpeakMemberSetting.isShowing()) {
                popMeetingSpeakMemberSetting.dismiss();
            }
            popMeetingSpeakMemberSetting = null;
        }

        popMeetingSpeakMemberSetting = new PopMeetingSpeakMemberSetting(getActivity());
        popMeetingSpeakMemberSetting.setOnMemberSettingChanged(this);
        popMeetingSpeakMemberSetting.showAtBottom(member, view, meetingConfig);
    }

    PopMeetingHandsMemberSetting mPopMeetingHandsMemberSetting;
    private void showHandsMemberSetting(MeetingMember member, View view) {
        if (mPopMeetingHandsMemberSetting != null) {
            if (mPopMeetingHandsMemberSetting.isShowing()) {
                mPopMeetingHandsMemberSetting.dismiss();
            }
            mPopMeetingHandsMemberSetting = null;
        }

        mPopMeetingHandsMemberSetting = new PopMeetingHandsMemberSetting(getActivity());
        mPopMeetingHandsMemberSetting.setOnMemberSettingChanged(this);
        mPopMeetingHandsMemberSetting.showAtBottom(member, view, meetingConfig);
    }



    private void fillViewByRoleForMainSpeakingMembers(final MeetingMember meetingMember, final MainSpeakerViewHolder holder) {

        int role = meetingMember.getRole();
        holder.settingImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMemberSetting(meetingMember, holder.settingImage);
            }
        });

//        meetingConfig.getMeetingHostId().equals(member.getUserId() + "")  是否是host
        //

        if (!(meetingMember.getUserId() + "").equals(AppConfig.UserID)) {
            // 当前的member不是自己
            if (meetingConfig.getPresenterId().equals(AppConfig.UserID)) {
                // 如果自己是presenter
                holder.settingImage.setVisibility(View.VISIBLE);
            } else {
                holder.settingImage.setVisibility(View.GONE);
            }

        } else {
            if (meetingConfig.getPresenterId().equals(AppConfig.UserID)) {
                holder.settingImage.setVisibility(View.GONE);
            } else {
                holder.settingImage.setVisibility(View.VISIBLE);
            }
        }

//        if (role == MeetingConfig.MeetingRole.MEMBER || ) {
//            holder.changeToMember.setVisibility(View.GONE);
//
//        } else if (role == MeetingConfig.MeetingRole.AUDIENCE) {
//            holder.settingImage.setVisibility(View.GONE);
//            holder.changeToMember.setVisibility(View.VISIBLE);

//
//        } else if (role == MeetingConfig.MeetingRole.BE_INVITED) {
//            holder.settingImage.setVisibility(View.GONE);
//            holder.changeToMember.setVisibility(View.GONE);
//        }
    }


    private void fillViewByRoleForMembers(final MeetingMember meetingMember, final ViewHolder holder) {

        int role = meetingMember.getRole();
        holder.settingImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMemberSetting(meetingMember, holder.settingImage);
            }
        });

//        meetingConfig.getMeetingHostId().equals(member.getUserId() + "")  是否是host
        //

        if (!(meetingMember.getUserId() + "").equals(AppConfig.UserID)) {
            // 当前的member不是自己
            if (meetingConfig.getPresenterId().equals(AppConfig.UserID)) {
                // 如果自己是presenter
                holder.settingImage.setVisibility(View.VISIBLE);
            } else {
                holder.settingImage.setVisibility(View.GONE);
            }

        } else {
            if (meetingConfig.getPresenterId().equals(AppConfig.UserID)) {
                holder.settingImage.setVisibility(View.GONE);
            } else {
                holder.settingImage.setVisibility(View.VISIBLE);
            }
        }

//        if (role == MeetingConfig.MeetingRole.MEMBER || ) {
//            holder.changeToMember.setVisibility(View.GONE);
//
//        } else if (role == MeetingConfig.MeetingRole.AUDIENCE) {
//            holder.settingImage.setVisibility(View.GONE);
//            holder.changeToMember.setVisibility(View.VISIBLE);

//
//        } else if (role == MeetingConfig.MeetingRole.BE_INVITED) {
//            holder.settingImage.setVisibility(View.GONE);
//            holder.changeToMember.setVisibility(View.GONE);
//        }
    }

    private void fillViewByRoleForAuditors(final MeetingMember meetingMember, final ViewHolder holder) {

        if (!(meetingMember.getUserId() + "").equals(AppConfig.UserID)) {
            // 当前的member不是自己
            if (meetingConfig.getPresenterId().equals(AppConfig.UserID) || (meetingConfig.getMeetingHostId() + "").equals(AppConfig.UserID)) {
                // 如果自己是presenter
                holder.handsUpText.setVisibility(View.GONE);
                holder.changeToMember.setVisibility(View.VISIBLE);
                holder.changeToMember.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        setMember(meetingMember);
                    }
                });
            } else {
                holder.handsUpText.setVisibility(View.GONE);
                holder.changeToMember.setVisibility(View.GONE);
                holder.changeToMember.setOnClickListener(null);
            }

        } else {
            holder.handsUpText.setVisibility(View.VISIBLE);
        }

        if (meetingMember.getHandStatus() == 0) {
            holder.handsUpText.setText("举手");
            holder.handsUpText.setBackground(getResources().getDrawable(R.drawable.change_auditor_bg));
            holder.handsUpText.setTextColor(Color.parseColor("#ff999999"));
            holder.handsUpText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    handsUp();
                }
            });

        } else {
            holder.handsUpText.setOnClickListener(null);
            holder.handsUpText.setText("已举手");
            holder.handsUpText.setTextColor(getResources().getColor(R.color.white));
            holder.handsUpText.setBackground(getResources().getDrawable(R.drawable.has_handed_up_bg));
        }
    }

    private void fillViewByRoleForSpeakingMembers(final MeetingMember meetingMember, final SpeakerViewHolder holder) {

        int role = meetingMember.getRole();
        holder.settingImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSpeakMemberSetting(meetingMember, holder.settingImage);
            }
        });

//        meetingConfig.getMeetingHostId().equals(member.getUserId() + "")  是否是host
        //

        if (!(meetingMember.getUserId() + "").equals(AppConfig.UserID)) {
            // 当前的member不是自己
            if (meetingConfig.getPresenterId().equals(AppConfig.UserID) || meetingConfig.getMeetingHostId().equals(AppConfig.UserID)) {
                // 如果自己是presenter
                holder.settingImage.setVisibility(View.VISIBLE);
            } else {
                holder.settingImage.setVisibility(View.GONE);
            }

        } else {
            if (meetingConfig.getPresenterId().equals(AppConfig.UserID)) {
                holder.settingImage.setVisibility(View.GONE);
            } else {
                holder.settingImage.setVisibility(View.VISIBLE);
            }
        }

    }

    private void fillViewByRoleForHandsMembers(final MeetingMember meetingMember, final HandsUpViewHolder holder) {

        int role = meetingMember.getRole();
        holder.settingImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showHandsMemberSetting(meetingMember, holder.settingImage);
            }
        });

//        meetingConfig.getMeetingHostId().equals(member.getUserId() + "")  是否是host
        //

        if (!(meetingMember.getUserId() + "").equals(AppConfig.UserID)) {
            // 当前的member不是自己
            if (meetingConfig.getPresenterId().equals(AppConfig.UserID) || meetingConfig.getPresenterId().equals(AppConfig.UserID)) {
                // 如果自己是presenter
                holder.settingImage.setVisibility(View.VISIBLE);
            } else {
                holder.settingImage.setVisibility(View.GONE);
            }

        } else {
            if (meetingConfig.getPresenterId().equals(AppConfig.UserID)) {
                holder.settingImage.setVisibility(View.GONE);
            } else {
                holder.settingImage.setVisibility(View.VISIBLE);
            }
        }

    }


    @Override
    public void setPresenter(MeetingMember meetingMember) {
        Observable.just(meetingMember).observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer<MeetingMember>() {
            @Override
            public void accept(MeetingMember meetingMember) throws Exception {
                if (!meetingConfig.getPresenterId().equals(AppConfig.UserID)) {
                    //不是presenter
                    if (!(meetingMember.getUserId() + "").equals(AppConfig.UserID)) {
                        Toast.makeText(getActivity(), "没有权限进行此操作", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                EventSetPresenter setPresenter = new EventSetPresenter();
                setPresenter.setMeetingMember(meetingMember);
                EventBus.getDefault().post(setPresenter);
            }
        });
    }

    @Override
    public void setAuditor(MeetingMember meetingMember) {
        Observable.just(meetingMember).observeOn(Schedulers.io()).doOnNext(new Consumer<MeetingMember>() {
            @Override
            public void accept(MeetingMember meetingMember) throws Exception {
                JSONObject response = ServiceInterfaceTools.getinstance().
                        syncMakeUserUpAndDown(meetingMember.getUserId() + "", 0);
                if (response.has("code")) {
                    if (response.getInt("code") == 0) {
                        MeetingKit.getInstance().requestMeetingMembers(meetingConfig);
                    } else if (response.getInt("code") == 22) {
                        Observable.just("toast_main_thread").observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer<String>() {
                            @Override
                            public void accept(String s) throws Exception {
                                Toast.makeText(getActivity(), "没有权限进行此操作", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }
        }).subscribe();
    }

    @Override
    public void setSpeakMember(MeetingMember meetingMember) {
        Observable.just(meetingMember).observeOn(Schedulers.io()).doOnNext(new Consumer<MeetingMember>() {
            @Override
            public void accept(MeetingMember meetingMember) throws Exception {
                JSONObject response = ServiceInterfaceTools.getinstance().
                        syncChangeTemStatus(meetingMember.getUserId() + "", 1);
                if (response.has("code")) {
                    if (response.getInt("code") == 0) {
                        MeetingKit.getInstance().requestMeetingMembers(meetingConfig);
                    } else if (response.getInt("code") == 22) {
                        Observable.just("toast_main_thread").observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer<String>() {
                            @Override
                            public void accept(String s) throws Exception {
                                Toast.makeText(getActivity(), "没有权限进行此操作", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }
        }).subscribe();
    }

    public void setMember(MeetingMember meetingMember) {
        Observable.just(meetingMember).observeOn(Schedulers.io()).doOnNext(new Consumer<MeetingMember>() {
            @Override
            public void accept(MeetingMember meetingMember) throws Exception {
                JSONObject response = ServiceInterfaceTools.getinstance().
                        syncMakeUserUpAndDown(meetingMember.getUserId() + "", 1);
                if (response.has("code")) {
                    if (response.getInt("code") == 0) {
                        MeetingKit.getInstance().requestMeetingMembers(meetingConfig);
                    }
                }
            }
        }).subscribe();
    }

    public void handsUp() {
        Observable.just("hands_up").observeOn(Schedulers.io()).doOnNext(new Consumer<String>() {
            @Override
            public void accept(String str) throws Exception {
                JSONObject response = ServiceInterfaceTools.getinstance().
                        syncHandUpOrDown(1,null);
                if (response.has("code")) {
                    if (response.getInt("code") == 0) {
                        MeetingKit.getInstance().requestMeetingMembers(meetingConfig);
                    }
                }
            }
        }).subscribe();
    }


}
