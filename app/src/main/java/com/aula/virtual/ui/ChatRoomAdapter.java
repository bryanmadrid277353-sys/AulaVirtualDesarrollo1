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
import com.aula.virtual.data.ChatRoomInfo;
import java.util.ArrayList;
import java.util.List;

public class ChatRoomAdapter extends RecyclerView.Adapter<ChatRoomAdapter.ViewHolder> {
    private List<ChatRoomInfo> rooms = new ArrayList<>();
    private OnRoomClickListener listener;

    public interface OnRoomClickListener {
        void onRoomClick(ChatRoomInfo room);
    }

    public void setOnRoomClickListener(OnRoomClickListener listener) {
        this.listener = listener;
    }

    public void setRooms(List<ChatRoomInfo> newRooms) {
        this.rooms = newRooms != null ? newRooms : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_room, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatRoomInfo room = rooms.get(position);
        holder.tvName.setText(room.contactName != null ? room.contactName : "Contacto");
        holder.tvLastMsg.setText(room.lastMessage != null ? room.lastMessage : "");
        holder.tvTimestamp.setText(room.lastMessageTimestamp != null ? room.lastMessageTimestamp : "");

        String badge = room.contactSubjectOrGrade != null ? room.contactSubjectOrGrade : getRoleLabel(room.contactRole);
        holder.tvRoleBadge.setText(badge);
        setRoleBadgeStyle(holder.tvRoleBadge, room.contactRole);

        holder.tvInitials.setText(getInitials(room.contactName));

        if (room.contactPhoto != null && !room.contactPhoto.isEmpty()) {
            Bitmap b = ImageUtils.base64ToBitmap(room.contactPhoto);
            if (b != null) {
                holder.ivPhoto.setImageBitmap(b);
                holder.cardPhoto.setVisibility(View.VISIBLE);
            } else {
                holder.cardPhoto.setVisibility(View.GONE);
            }
        } else {
            holder.cardPhoto.setVisibility(View.GONE);
        }

        if (room.unreadCount > 0) {
            holder.tvUnread.setText(String.valueOf(room.unreadCount));
            holder.tvUnread.setVisibility(View.VISIBLE);
            holder.tvReadCheckmark.setVisibility(View.GONE);
        } else {
            holder.tvUnread.setVisibility(View.GONE);
            holder.tvReadCheckmark.setVisibility(View.VISIBLE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onRoomClick(room);
        });
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

    private void setRoleBadgeStyle(TextView tv, String role) {
        tv.setBackground(null);
        tv.setTextColor(0xFF8E9BB0);
    }

    @Override
    public int getItemCount() {
        return rooms.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvLastMsg, tvTimestamp, tvRoleBadge, tvInitials, tvUnread, tvReadCheckmark;
        ImageView ivPhoto;
        View cardPhoto;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvContactName);
            tvLastMsg = itemView.findViewById(R.id.tvLastMessage);
            tvTimestamp = itemView.findViewById(R.id.tvChatTimestamp);
            tvRoleBadge = itemView.findViewById(R.id.tvContactRoleBadge);
            tvInitials = itemView.findViewById(R.id.tvChatInitials);
            tvUnread = itemView.findViewById(R.id.tvUnreadBadge);
            tvReadCheckmark = itemView.findViewById(R.id.tvReadCheckmark);
            ivPhoto = itemView.findViewById(R.id.ivChatPhoto);
            cardPhoto = itemView.findViewById(R.id.cardChatPhoto);
        }
    }
}
