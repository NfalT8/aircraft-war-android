package edu.hitsz.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;

import java.util.ArrayList;
import java.util.List;

import edu.hitsz.R;
import edu.hitsz.dao.RankRecord;

/**
 * 排行榜自定义 Adapter：基于 BaseAdapter + ViewHolder 复用行视图，
 * 负责第一名高亮渲染及行末删除按钮交互绑定。
 */
public class RankAdapter extends BaseAdapter {

    public interface OnDeleteClickListener {
        void onDeleteClick(RankRecord record, int position);
    }

    private static final int COLOR_GOLD = 0xFFFFD54F;
    private static final int COLOR_SILVER = 0xFFD7E2F0;
    private static final int COLOR_BRONZE = 0xFFE6A56E;
    private static final int COLOR_NORMAL_BADGE = 0x33FFFFFF;
    private static final int COLOR_DARK_TEXT = 0xFF0B1220;
    private static final int COLOR_LIGHT_TEXT = 0xFFE8F0FC;

    private final LayoutInflater inflater;
    private final List<RankRecord> records = new ArrayList<>();
    private final OnDeleteClickListener deleteClickListener;

    public RankAdapter(@NonNull Context context,
                       List<RankRecord> initialRecords,
                       OnDeleteClickListener deleteClickListener) {
        this.inflater = LayoutInflater.from(context);
        this.deleteClickListener = deleteClickListener;
        if (initialRecords != null) {
            this.records.addAll(initialRecords);
        }
    }

    public void setRecords(List<RankRecord> newRecords) {
        records.clear();
        if (newRecords != null) {
            records.addAll(newRecords);
        }
        notifyDataSetChanged();
    }

    public void removeAt(int position) {
        if (position >= 0 && position < records.size()) {
            records.remove(position);
            notifyDataSetChanged();
        }
    }

    @Override
    public int getCount() {
        return records.size();
    }

    @Override
    public RankRecord getItem(int position) {
        return records.get(position);
    }

    @Override
    public long getItemId(int position) {
        RankRecord record = records.get(position);
        return record.getId() >= 0 ? record.getId() : position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_rank, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        RankRecord record = records.get(position);
        int rank = position + 1;

        holder.tvRank.setText(String.valueOf(rank));
        holder.tvName.setText(record.getName());
        holder.tvTime.setText(record.getTime());
        holder.tvScore.setText(String.valueOf(record.getScore()));

        if (rank == 1) {
            holder.rankItemRoot.setBackgroundResource(R.drawable.bg_rank_item_top1);
            ViewCompat.setBackgroundTintList(holder.tvRank, ColorStateList.valueOf(COLOR_GOLD));
            holder.tvRank.setTextColor(COLOR_DARK_TEXT);
            holder.tvName.setTextColor(0xFFFFF4CC);
            holder.tvScore.setTextColor(COLOR_GOLD);
        } else {
            holder.rankItemRoot.setBackgroundResource(R.drawable.bg_rank_item);
            if (rank == 2) {
                ViewCompat.setBackgroundTintList(holder.tvRank, ColorStateList.valueOf(COLOR_SILVER));
                holder.tvRank.setTextColor(COLOR_DARK_TEXT);
            } else if (rank == 3) {
                ViewCompat.setBackgroundTintList(holder.tvRank, ColorStateList.valueOf(COLOR_BRONZE));
                holder.tvRank.setTextColor(COLOR_DARK_TEXT);
            } else {
                ViewCompat.setBackgroundTintList(holder.tvRank, ColorStateList.valueOf(COLOR_NORMAL_BADGE));
                holder.tvRank.setTextColor(COLOR_LIGHT_TEXT);
            }
            holder.tvName.setTextColor(0xFFFFFFFF);
            holder.tvScore.setTextColor(0xFF4DD0E1);
        }

        holder.btnDelete.setOnClickListener(v -> {
            if (deleteClickListener != null) {
                deleteClickListener.onDeleteClick(record, position);
            }
        });

        return convertView;
    }

    private static final class ViewHolder {
        final View rankItemRoot;
        final TextView tvRank;
        final TextView tvName;
        final TextView tvTime;
        final TextView tvScore;
        final ImageButton btnDelete;

        ViewHolder(View itemView) {
            this.rankItemRoot = itemView.findViewById(R.id.rankItemRoot);
            this.tvRank = itemView.findViewById(R.id.tvRank);
            this.tvName = itemView.findViewById(R.id.tvName);
            this.tvTime = itemView.findViewById(R.id.tvTime);
            this.tvScore = itemView.findViewById(R.id.tvScore);
            this.btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
