package com.kloudsync.techexcel.adapter.expand;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import static com.kloudsync.techexcel.adapter.expand.BaseViewHolder.VIEW_TYPE_CHILD;
import static com.kloudsync.techexcel.adapter.expand.BaseViewHolder.VIEW_TYPE_PARENT;


/**
 * T :group  data
 * S :child  data
 * VH :ViewHolder
 */

public abstract class BaseRecyclerViewAdapter<T, S, VH extends BaseViewHolder> extends RecyclerView.Adapter<VH> {

    public static final String TAG = BaseRecyclerViewAdapter.class.getSimpleName();

    private Context ctx;
    /**
     * all data
     */
    private List<RecyclerViewData> allDatas;
    /**
     * showing datas
     */
    private List showingDatas = new ArrayList<>();

    /**
     * child datas
     */
    private List<List<S>> childDatas;

    private OnRecyclerViewListener.OnItemClickListener itemClickListener;
    private OnRecyclerViewListener.OnItemLongClickListener itemLongClickListener;

    public void setOnItemClickListener(OnRecyclerViewListener.OnItemClickListener listener) {
        this.itemClickListener = listener;
    }

    public void setOnItemLongClickListener(OnRecyclerViewListener.OnItemLongClickListener longClickListener) {
        this.itemLongClickListener = longClickListener;
    }

    public BaseRecyclerViewAdapter(Context ctx, List<RecyclerViewData> datas) {
        this.ctx = ctx;
        this.allDatas = datas;
        setShowingDatas();
        this.notifyDataSetChanged();
    }

    public void setAllDatas(List<RecyclerViewData> allDatas) {
        this.allDatas = allDatas;
        setShowingDatas();
        this.notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return null == showingDatas ? 0 : showingDatas.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (showingDatas.get(position) instanceof GroupItem) {
            return VIEW_TYPE_PARENT;
        } else {
            return VIEW_TYPE_CHILD;
        }
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = null;
        switch (viewType) {
            case VIEW_TYPE_PARENT:
                view = getGroupView(parent);
                break;
            case VIEW_TYPE_CHILD:
                view = getChildView(parent);
                break;
        }
        return createRealViewHolder(ctx, view, viewType);
    }


    @Override
    public void onBindViewHolder(final VH holder, final int position) {
        final Object item = showingDatas.get(position);
        final int gp = getGroupPosition(position);
        final int cp = getChildPosition(gp, position);
        if (item != null && item instanceof GroupItem) {
            onBindGroupHolder(holder, gp, position, (T) ((GroupItem) item).getGroupData());
            holder.groupView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (null != itemClickListener) {
                        itemClickListener.onGroupItemClick(position, gp, holder.groupView);
                    }
                    if (item instanceof GroupItem && ((GroupItem) item).isExpand()) {
                        collapseGroup(position);
                    } else {
                        expandGroup(position);
                    }
                }
            });
            holder.groupView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (null != itemLongClickListener) {
                        itemLongClickListener.onGroupItemLongClick(position, gp, holder.groupView);
                    }
                    return true;
                }
            });
        } else {
            onBindChildpHolder(holder, gp, cp, position, (S) item);
            holder.childView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (null != itemClickListener) {
                        itemClickListener.onChildItemClick(position, gp, cp, holder.childView);
                    }
                }
            });
            holder.childView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (null != itemLongClickListener) {
                        int gp = getGroupPosition(position);
                        itemLongClickListener.onChildItemLongClick(position, gp, cp, holder.childView);
                    }
                    return true;
                }
            });
        }
    }


    /**
     * setup showing datas
     */
    private void setShowingDatas() {
        if (null != showingDatas) {
            showingDatas.clear();
        }
        if (this.childDatas == null) {
            this.childDatas = new ArrayList<>();
        }
        childDatas.clear();
        GroupItem groupItem;
        for (int i = 0; i < allDatas.size(); i++) {
            if (allDatas.get(i).getGroupItem() instanceof GroupItem) {
                groupItem = allDatas.get(i).getGroupItem();
            } else {
                break;
            }
            childDatas.add(i, groupItem.getChildDatas());
            showingDatas.add(groupItem);
            if (null != groupItem && groupItem.hasChilds() && groupItem.isExpand()) {
                showingDatas.addAll(groupItem.getChildDatas());
            }
        }
    }

    /**
     * expandGroup
     *
     * @param position showingDatas position
     */
    private void expandGroup(int position) {
        Object item = showingDatas.get(position);
        if (null == item) {
            return;
        }
        if (!(item instanceof GroupItem)) {
            return;
        }
        if (((GroupItem) item).isExpand()) {
            return;
        }
        if (!canExpandAll()) {
            for (int i = 0; i < showingDatas.size(); i++) {
                if (i != position) {
                    int tempPositino = collapseGroup(i);
                    if (tempPositino != -1) {
                        position = tempPositino;
                    }
                }
            }
        }

        List<BaseItem> tempChilds;
        if (((GroupItem) item).hasChilds()) {
            tempChilds = ((GroupItem) item).getChildDatas();
            ((GroupItem) item).onExpand();
            if (canExpandAll()) {
                showingDatas.addAll(position + 1, tempChilds);
                notifyItemRangeInserted(position + 1, tempChilds.size());
                notifyItemRangeChanged(position + 1, showingDatas.size() - (position + 1));
            } else {
                int tempPsi = showingDatas.indexOf(item);
                showingDatas.addAll(tempPsi + 1, tempChilds);
                notifyItemRangeInserted(tempPsi + 1, tempChilds.size());
                notifyItemRangeChanged(tempPsi + 1, showingDatas.size() - (tempPsi + 1));
            }
        }
    }

    /**
     * collapseGroup
     *
     * @param position showingDatas position
     */
    private int collapseGroup(int position) {
        Object item = showingDatas.get(position);
        if (null == item) {
            return -1;
        }
        if (!(item instanceof GroupItem)) {
            return -1;
        }
        if (!((GroupItem) item).isExpand()) {
            return -1;
        }
        int tempSize = showingDatas.size();
        List<BaseItem> tempChilds;
        if (((GroupItem) item).hasChilds()) {
            tempChilds = ((GroupItem) item).getChildDatas();
            ((GroupItem) item).onExpand();
            showingDatas.removeAll(tempChilds);
            notifyItemRangeRemoved(position + 1, tempChilds.size());
            notifyItemRangeChanged(position + 1, tempSize - (position + 1));
            return position;
        }
        return -1;
    }

    /**
     * @param position showingDatas position
     * @return GroupPosition
     */
    private int getGroupPosition(int position) {
        Object item = showingDatas.get(position);
        if (item instanceof GroupItem) {
            for (int j = 0; j < allDatas.size(); j++) {
                if (allDatas.get(j).getGroupItem().equals(item)) {
                    return j;
                }
            }
        }
        for (int i = 0; i < childDatas.size(); i++) {
            if (childDatas.get(i).contains(item)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * @param groupPosition
     * @param showDataPosition
     * @return ChildPosition
     */
    private int getChildPosition(int groupPosition, int showDataPosition) {
        Object item = showingDatas.get(showDataPosition);
        try {
            return childDatas.get(groupPosition).indexOf(item);
        } catch (IndexOutOfBoundsException ex) {
            Log.e(TAG, ex.getMessage());
        }
        return 0;
    }

    /**
     * return groupView
     */
    public abstract View getGroupView(ViewGroup parent);

    /**
     * return childView
     */
    public abstract View getChildView(ViewGroup parent);

    /**
     * return <VH extends BaseViewHolder> instance
     */
    public abstract VH createRealViewHolder(Context ctx, View view, int viewType);

    /**
     * onBind groupData to groupView
     *
     * @param holder
     * @param position
     */
    public abstract void onBindGroupHolder(VH holder, int groupPos, int position, T groupData);

    /**
     * onBind childData to childView
     *
     * @param holder
     * @param position
     */
    public abstract void onBindChildpHolder(VH holder, int groupPos, int childPos, int position, S childData);

    /**
     * if return true Allow all expand otherwise Only one can be expand at the same time
     */
    public boolean canExpandAll() {
        return true;
    }

    /**
     * 对原数据进行增加删除，调用此方法进行notify
     */
    public void notifyRecyclerViewData() {
        notifyDataSetChanged();
        setShowingDatas();
    }


}