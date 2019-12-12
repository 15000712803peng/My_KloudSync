package com.ub.techexcel.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.kloudsync.techexcel.R;
import com.ub.techexcel.bean.AgoraBean;
import com.ub.techexcel.bean.AgoraMember;
import com.ub.techexcel.bean.AgoraUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.agora.openlive.ui.VideoViewEventListener;

/**
 * Created by wang on 2018/8/8.
 */

public class AgoraCameraAdapter extends RecyclerView.Adapter<AgoraCameraAdapter.ViewHolder> {

    private LayoutInflater inflater;
    private List<AgoraMember> users;
    private Context mContext;

    public List<AgoraMember> getUsers() {
        return users;
    }

    public AgoraCameraAdapter(Context context) {
        this.mContext = context;
        inflater = LayoutInflater.from(mContext);
        users = new ArrayList<>();
    }

    private OnCameraOptionsListener onCameraOptionsListener;

    public void setOnCameraOptionsListener(OnCameraOptionsListener onCameraOptionsListener) {
        this.onCameraOptionsListener = onCameraOptionsListener;
    }

    public interface OnCameraOptionsListener {
        void onCameraFrameClick(AgoraMember member);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.video_view_container, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ViewHolder myHolder = holder;
        final AgoraMember user = users.get(position);
        FrameLayout holderView = (FrameLayout) myHolder.itemView;
        holderView.removeAllViews();
        if (holderView.getChildCount() == 0) {
            View d = inflater.inflate(R.layout.framelayout_head, null);
            TextView videoname = (TextView) d.findViewById(R.id.videoname);
            ViewParent parent = videoname.getParent();
            if (parent != null) {
                ((RelativeLayout) parent).removeView(videoname);
            }
            holderView.addView(videoname);
            SurfaceView target = user.getSurfaceView();
            if (!user.isMuteVideo()) {
                target.setVisibility(View.VISIBLE);
                stripSurfaceView(target);
                holderView.addView(target, 0, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            } else {
                Log.e("onBindViewHolder","target_gone");
                stripSurfaceView(target);
                target.setVisibility(View.GONE);
            }
            myHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (onCameraOptionsListener != null) {
                        onCameraOptionsListener.onCameraFrameClick(user);
                    }
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    public AgoraMember getItem(int position) {
        return users.get(position);
    }

    protected final void stripSurfaceView(SurfaceView view) {
        ViewParent parent = view.getParent();
        if (parent != null) {
            ((FrameLayout) parent).removeView(view);
        }
    }

    public void muteOrOpenCamera(int myId, boolean isMute) {
        int index = users.indexOf(new AgoraMember(myId));
        if (index >= 0 && index < users.size()) {
            users.get(index).setMuteVideo(isMute);
            Log.e("muteOrOpenCamera","isMute:" + isMute);
            notifyDataSetChanged();
        }
    }

    public void setMembers(List<AgoraMember> users) {
        if (users != null) {
            this.users.clear();
            this.users.addAll(users);
            Collections.sort(users);
        }
        notifyDataSetChanged();
    }

    public void addUser(AgoraMember user) {

        if (users.contains(user)) {
            return;
        }
        this.users.add(user);
        Collections.sort(users);
        notifyItemRangeChanged(getItemCount() - 2, 2);
    }

    public void removeUser(AgoraMember user) {

        if (users.contains(user)) {
            this.users.remove(user);
        }
        Collections.sort(users);
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View a) {
            super(a);
        }
    }

   public void reset(){
       for (AgoraMember member : this.users) {
           SurfaceView surfaceView = member.getSurfaceView();
           stripSurfaceView(surfaceView);
       }
       this.users.clear();
       notifyDataSetChanged();
   }

   public void muteVideo(AgoraMember member,boolean isMute){
       int index = this.users.indexOf(member);
       if(index >= 0){
           this.users.get(index).setMuteVideo(isMute);
           notifyItemChanged(index);
       }

   }

}
