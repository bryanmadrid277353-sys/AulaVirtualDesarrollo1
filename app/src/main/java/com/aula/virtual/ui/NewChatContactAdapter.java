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

public class NewChatContactAdapter extends RecyclerView.Adapter<NewChatContactAdapter.ViewHolder> {
    private List<User> contacts = new ArrayList<>();
    private OnContactClickListener listener;

    public interface OnContactClickListener {
        void onContactClick(User contact);
    }

    public void setOnContactClickListener(OnContactClickListener listener) {
        this.listener = listener;
    }

    public void setContacts(List<User> newContacts) {
        this.contacts = newContacts != null ? newContacts : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_new_chat_contact, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = contacts.get(position);
        holder.tvName.setText(user.name != null ? user.name : "Usuario");

        String carnetStr = user.carnet != null ? user.carnet : "GEN10001";
        String roleLabel = getRoleLabel(user.role);
        holder.tvDetails.setText("CARNET: " + carnetStr + " • " + roleLabel);

        holder.tvInitials.setText(getInitials(user.name));

        if (user.profile_image != null && !user.profile_image.isEmpty()) {
            Bitmap b = ImageUtils.base64ToBitmap(user.profile_image);
            if (b != null) {
                holder.ivPhoto.setImageBitmap(b);
                holder.cardPhoto.setVisibility(View.VISIBLE);
            } else {
                holder.cardPhoto.setVisibility(View.GONE);
            }
        } else {
            holder.cardPhoto.setVisibility(View.GONE);
        }

        View.OnClickListener click = v -> {
            if (listener != null) listener.onContactClick(user);
        };
        holder.itemView.setOnClickListener(click);
        if (holder.btnStartChat != null) holder.btnStartChat.setOnClickListener(click);
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

    private String getRoleLabel(String role) {
        if ("ADMIN".equals(role)) return "Administrador";
        if ("PROFESSOR".equals(role)) return "Profesor";
        if ("ENCARGADO".equals(role)) return "Encargado";
        return "Estudiante";
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDetails, tvInitials;
        ImageView ivPhoto;
        View cardPhoto, btnStartChat;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvContactName);
            tvDetails = itemView.findViewById(R.id.tvContactDetails);
            tvInitials = itemView.findViewById(R.id.tvContactInitials);
            ivPhoto = itemView.findViewById(R.id.ivContactPhoto);
            cardPhoto = itemView.findViewById(R.id.cardContactPhoto);
            btnStartChat = itemView.findViewById(R.id.btnActionStartChat);
        }
    }
}
