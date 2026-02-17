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
import java.util.Set;

public class AppAdapter extends RecyclerView.Adapter<AppAdapter.ViewHolder> {

    private List<AppModel> appList;
    private Set<String> blockedPackages;
    private OnAppClickListener listener;

    public interface OnAppClickListener {
        void onAppClick(AppModel app, boolean isChecked);
    }

    public AppAdapter(List<AppModel> appList, Set<String> blockedPackages, OnAppClickListener listener) {
        this.appList = appList;
        this.blockedPackages = blockedPackages;
        this.listener = listener;
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
        holder.tvAppName.setText(app.getAppName());
        holder.tvPackageName.setText(app.getPackageName());
        holder.ivAppIcon.setImageDrawable(app.getIcon());
        
        holder.cbBlock.setOnCheckedChangeListener(null);
        
        if (app.isDefault()) {
            holder.cbBlock.setChecked(true);
            holder.cbBlock.setEnabled(false); // User cannot unblock default apps
            holder.cbBlock.setAlpha(0.5f);
        } else {
            holder.cbBlock.setChecked(blockedPackages.contains(app.getPackageName()));
            holder.cbBlock.setEnabled(true);
            holder.cbBlock.setAlpha(1.0f);
        }

        holder.cbBlock.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) {
                listener.onAppClick(app, isChecked);
            }
        });
    }

    @Override
    public int getItemCount() {
        return appList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAppIcon;
        TextView tvAppName, tvPackageName;
        CheckBox cbBlock;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAppIcon = itemView.findViewById(R.id.ivAppIcon);
            tvAppName = itemView.findViewById(R.id.tvAppName);
            tvPackageName = itemView.findViewById(R.id.tvPackageName);
            cbBlock = itemView.findViewById(R.id.cbBlock);
        }
    }
}
