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

import com.kloudsync.techexcel.R;
import com.kloudsync.techexcel.bean.DeviceType;
import com.kloudsync.techexcel.bean.EventRefreshMembers;
import com.kloudsync.techexcel.bean.EventSetPresenter;
import com.kloudsync.techexcel.bean.MeetingConfig;
import com.kloudsync.techexcel.bean.MeetingMember;
import com.kloudsync.techexcel.bean.MeetingType;
import com.kloudsync.techexcel.config.AppConfig;
import com.kloudsync.techexcel.dialog.PopMeetingMemberSetting;
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
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by tonyan on 2019/11/9.
 */

public class MeetingMembersFragment extends MyFragment implements PopMeetingMemberSetting.OnMemberSettingChanged {

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
        if (type == 1) {
            meetingMembers.addAll(meetingConfig.getMeetingMembers());
            Collections.sort(meetingMembers);
        } else if (type == 2) {
            meetingMembers.addAll(meetingConfig.getMeetingAuditor());
        } else if (type == 3) {
            meetingMembers.addAll(meetingConfig.getMeetingInvitors());
        }

        membersList.setLayoutManager(new LinearLayoutManager(getActivity(), RecyclerView.VERTICAL, false));

        if (membersAdapter == null) {
            membersAdapter = new MeetingMembersAdapter(getActivity(), meetingMembers);
            membersList.setAdapter(membersAdapter);
        } else {
            membersAdapter.updateMembers(meetingMembers);
        }

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
        }

        public CircleImageView icon;
        public TextView name;
        public TextView presenter;
        public TextView me;
        public TextView type;
        public ImageView settingImage;
        public TextView host;
        public TextView changeToMember;

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
            holder.name.setText(member.getUserName());
            if (member.getPresenter() == 1) {
                holder.settingImage.setVisibility(View.GONE);
            } else {
                holder.settingImage.setVisibility(View.VISIBLE);

            }

            if (meetingConfig.getMeetingHostId().equals(member.getUserId() + "")) {
                holder.host.setVisibility(View.VISIBLE);
            } else {
                holder.host.setVisibility(View.GONE);
            }

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

            fillDevictType(member.getDeviceType(), holder.type);
            fillViewByRole(member, holder);

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                }
            });

        }
    }

    private void fillDevictType(int deviceType, TextView typeText) {
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

    private void fillViewByRole(final MeetingMember meetingMember, final ViewHolder holder) {

        int role = meetingMember.getRole();

        if (meetingMember.getPresenter() == 1) {
            holder.presenter.setVisibility(View.VISIBLE);

        } else {
            holder.presenter.setVisibility(View.GONE);
        }


        holder.settingImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMemberSetting(meetingMember, holder.settingImage);
            }
        });


        if (role == MeetingConfig.MeetingRole.MEMBER || role == MeetingConfig.MeetingRole.HOST) {
            holder.changeToMember.setVisibility(View.GONE);

        } else if (role == MeetingConfig.MeetingRole.AUDIENCE) {
            holder.settingImage.setVisibility(View.GONE);
            holder.changeToMember.setVisibility(View.VISIBLE);
            holder.changeToMember.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setMember(meetingMember);
                }
            });

        } else if (role == MeetingConfig.MeetingRole.BE_INVITED) {
            holder.settingImage.setVisibility(View.GONE);
            holder.changeToMember.setVisibility(View.GONE);
        }
    }

    @Override
    public void setPresenter(MeetingMember meetingMember) {
        EventSetPresenter setPresenter = new EventSetPresenter();
        setPresenter.setMeetingMember(meetingMember);
        EventBus.getDefault().post(setPresenter);
    }

    @Override
    public void setAuditor(MeetingMember meetingMember) {
        Observable.just(meetingMember).observeOn(Schedulers.io()).doOnNext(new Consumer<MeetingMember>() {
            @Override
            public void accept(MeetingMember meetingMember) throws Exception {
                JSONObject response = ServiceInterfaceTools.getinstance().
                        syncMakeUserUpAndDown(meetingMember.getUserId() + "",0);
                if (response.has("code")) {
                    if (response.getInt("code") == 0) {
                        MeetingKit.getInstance().requestMeetingMembers(meetingConfig);
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
                        syncMakeUserUpAndDown(meetingMember.getUserId() + "",1);
                if (response.has("code")) {
                    if (response.getInt("code") == 0) {
                        MeetingKit.getInstance().requestMeetingMembers(meetingConfig);
                    }
                }
            }
        }).subscribe();
    }


}
