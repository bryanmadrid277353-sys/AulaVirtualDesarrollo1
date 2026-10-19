package com.aula.virtual.ui;

import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.util.TypedValue;
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
        this.participants = participants;
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
        holder.tvName.setText(user.name);
        holder.tvRole.setText("PROFESSOR".equals(user.role) ? "Profesor" : "Estudiante");

        ImageUtils.setProfileImage(holder.ivPhoto, user.profile_image, user.name);

        holder.btnProfile.setOnClickListener(v -> listener.onProfileClick(user));
    }

    @Override
    public int getItemCount() {
        return participants.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvRole;
        ImageView ivPhoto;
        View btnProfile;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvParticipantName);
            tvRole = itemView.findViewById(R.id.tvParticipantRole);
            ivPhoto = itemView.findViewById(R.id.ivParticipantPhoto);
            btnProfile = itemView.findViewById(R.id.btnViewProfile);
        }
    }
}
