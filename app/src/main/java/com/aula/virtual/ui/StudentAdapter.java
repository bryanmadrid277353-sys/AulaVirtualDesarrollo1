package com.aula.virtual.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.aula.virtual.R;
import com.aula.virtual.data.entity.User;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.List;

public class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.ViewHolder> {
    private List<User> students = new ArrayList<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(User user);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setStudents(List<User> students) {
        this.students = students != null ? students : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_student, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = students.get(position);
        if (user == null) return;
        
        holder.tvName.setText(user.name != null ? user.name : "Sin nombre");

        String carnetDisplay = user.carnet != null ? user.carnet : "-";
        if ("ENCARGADO".equals(user.role) || "GUARDIAN".equals(user.role)) {
            if (!carnetDisplay.startsWith("ENC")) {
                carnetDisplay = "ENC" + carnetDisplay;
            }
        } else if ("PROFESSOR".equals(user.role)) {
            if (!carnetDisplay.startsWith("PROF")) {
                carnetDisplay = "PROF" + carnetDisplay;
            }
        } else if ("ADMIN".equals(user.role)) {
            if (!carnetDisplay.startsWith("ADMIN")) {
                carnetDisplay = "ADMIN" + carnetDisplay;
            }
        }

        holder.tvCarnet.setText("Carnet: " + carnetDisplay);

        if (holder.tvAvatarInitials != null) {
            holder.tvAvatarInitials.setText(getInitials(user.name));
        }
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(user);
        });
        if (holder.btnManage != null) {
            holder.btnManage.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(user);
            });
        }
    }

    private String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) return "AV";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        } else {
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
        }
    }

    @Override
    public int getItemCount() {
        return students.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        TextView tvCarnet;
        TextView tvAvatarInitials;
        View btnManage;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvCarnet = itemView.findViewById(R.id.tvCarnet);
            tvAvatarInitials = itemView.findViewById(R.id.tvAvatarInitials);
            btnManage = itemView.findViewById(R.id.btnManage);
        }
    }
}
