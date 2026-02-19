package com.example.edulock;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AppAdapter extends RecyclerView.Adapter<AppAdapter.ViewHolder> {

    public interface OnCheckedChangeListener {
        void onChanged(AppModel app, boolean isChecked);
    }

    private List<AppModel> appList;
    private OnCheckedChangeListener listener;
    private boolean isSelectionMode;

    public AppAdapter(List<AppModel> appList, boolean isSelectionMode, OnCheckedChangeListener listener) {
        this.appList = appList;
        this.isSelectionMode = isSelectionMode;
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAppIcon;
        TextView tvAppName;
        TextView tvPackageName;
        CheckBox cbBlock;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAppIcon = itemView.findViewById(R.id.ivAppIcon);
            tvAppName = itemView.findViewById(R.id.tvAppName);
            tvPackageName = itemView.findViewById(R.id.tvPackageName);
            cbBlock = itemView.findViewById(R.id.cbBlock);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppModel app = appList.get(position);
        holder.ivAppIcon.setImageDrawable(app.getIcon());
        holder.tvAppName.setText(app.getAppName());
        holder.tvPackageName.setText(app.getPackageName());

        if (isSelectionMode) {
            holder.cbBlock.setVisibility(View.VISIBLE);
            holder.cbBlock.setOnCheckedChangeListener(null);
            holder.cbBlock.setChecked(app.isChecked());
            holder.cbBlock.setOnCheckedChangeListener((buttonView, isChecked) -> {
                app.setChecked(isChecked);
                if (listener != null) listener.onChanged(app, isChecked);
            });
        } else {
            holder.cbBlock.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return appList.size();
    }
}
