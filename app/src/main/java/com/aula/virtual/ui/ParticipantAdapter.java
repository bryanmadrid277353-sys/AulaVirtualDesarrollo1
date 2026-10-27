package com.aula.virtual.ui;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.aula.virtual.R;
import com.aula.virtual.data.entity.User;
import java.util.ArrayList;
import java.util.List;

public class ParticipantAdapter extends RecyclerView.Adapter<ParticipantAdapter.ViewHolder> {
    private List<User> participants = new ArrayList<>();
    private final OnParticipantClickListener listener;

    public interface OnParticipantClickListener {
        void onProfileClick(User user);
    }

    public ParticipantAdapter(OnParticipantClickListener listener) {
        this.listener = listener;
    }

    public void setParticipants(List<User> participants) {
        this.participants = participants != null ? participants : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_participant, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = participants.get(position);
        holder.tvName.setText(user.name != null ? user.name : "-");
        
        String roleText = "Estudiante";
        if ("PROFESSOR".equals(user.role)) {
            roleText = "Profesor";
        } else if ("ADMIN".equals(user.role)) {
            roleText = "Administrador";
        } else if ("ENCARGADO".equals(user.role)) {
            roleText = "Encargado";
        }
        holder.tvRole.setText(roleText);

        if (holder.tvInitials != null) {
            holder.tvInitials.setText(getInitials(user.name));
        }

        if (user.profile_image != null && !user.profile_image.isEmpty()) {
            Bitmap b = ImageUtils.base64ToBitmap(user.profile_image);
            if (b != null) {
                holder.ivPhoto.setImageBitmap(b);
                if (holder.cardPhoto != null) holder.cardPhoto.setVisibility(View.VISIBLE);
            } else {
                if (holder.cardPhoto != null) holder.cardPhoto.setVisibility(View.GONE);
            }
        } else {
            if (holder.cardPhoto != null) holder.cardPhoto.setVisibility(View.GONE);
        }

        holder.btnProfile.setOnClickListener(v -> listener.onProfileClick(user));
        holder.itemView.setOnClickListener(v -> listener.onProfileClick(user));
    }

    private String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) return "U";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        } else {
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
        }
    }

    @Override
    public int getItemCount() {
        return participants.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvRole, tvInitials;
        ImageView ivPhoto;
        View cardPhoto, btnProfile;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvParticipantName);
            tvRole = itemView.findViewById(R.id.tvParticipantRole);
            tvInitials = itemView.findViewById(R.id.tvParticipantInitials);
            ivPhoto = itemView.findViewById(R.id.ivParticipantPhoto);
            cardPhoto = itemView.findViewById(R.id.cardParticipantPhoto);
            btnProfile = itemView.findViewById(R.id.btnViewProfile);
        }
    }
}
