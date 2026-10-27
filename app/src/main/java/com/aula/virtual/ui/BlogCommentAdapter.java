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
import com.aula.virtual.data.CommentInfo;
import com.aula.virtual.data.entity.BlogComment;
import java.util.ArrayList;
import java.util.List;

public class BlogCommentAdapter extends RecyclerView.Adapter<BlogCommentAdapter.ViewHolder> {
    private static final int TYPE_ME = 1;
    private static final int TYPE_OTHERS = 2;

    private List<CommentInfo> comments = new ArrayList<>();
    private final int currentUserId;
    private final boolean isAdmin;
    private OnCommentLongClickListener listener;

    public interface OnCommentLongClickListener {
        void onLongClick(BlogComment comment);
    }

    public BlogCommentAdapter(int currentUserId, boolean isAdmin) {
        this.currentUserId = currentUserId;
        this.isAdmin = isAdmin;
    }

    public void setOnCommentLongClickListener(OnCommentLongClickListener listener) {
        this.listener = listener;
    }

    public void setComments(List<CommentInfo> newComments) {
        if (newComments == null) return;
        if (isSameCommentList(this.comments, newComments)) {
            return;
        }
        this.comments = new ArrayList<>(newComments);
        notifyDataSetChanged();
    }

    private boolean isSameCommentList(List<CommentInfo> oldList, List<CommentInfo> newList) {
        if (oldList == newList) return true;
        if (oldList == null || newList == null) return false;
        if (oldList.size() != newList.size()) return false;
        for (int i = 0; i < oldList.size(); i++) {
            CommentInfo o = oldList.get(i);
            CommentInfo n = newList.get(i);
            if (o == null || n == null || o.comment == null || n.comment == null) return false;
            if (o.comment.id != n.comment.id && o.comment.id != 0 && n.comment.id != 0) return false;
            if (o.comment.userId != n.comment.userId) return false;
            if (!equalsNullSafe(o.comment.content, n.comment.content)) return false;
            if (!equalsNullSafe(o.comment.timestamp, n.comment.timestamp)) return false;
            if (!equalsNullSafe(o.userName, n.userName)) return false;
            if (!equalsNullSafe(o.userPhoto, n.userPhoto)) return false;
        }
        return true;
    }

    private boolean equalsNullSafe(String a, String b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    @Override
    public int getItemViewType(int position) {
        if (comments.get(position).comment.userId == currentUserId) {
            return TYPE_ME;
        }
        return TYPE_OTHERS;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = (viewType == TYPE_ME) ? R.layout.item_blog_comment_me : R.layout.item_blog_comment;
        View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new ViewHolder(view, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CommentInfo info = comments.get(position);
        holder.tvContent.setText(info.comment.content);
        
        if (holder.tvTimestamp != null) {
            if (info.comment != null && info.comment.timestamp != null && !info.comment.timestamp.isEmpty()) {
                holder.tvTimestamp.setText(info.comment.timestamp);
                holder.tvTimestamp.setVisibility(View.VISIBLE);
            } else {
                holder.tvTimestamp.setVisibility(View.GONE);
            }
        }

        // Long click for message options
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onLongClick(info.comment);
                return true;
            }
            return false;
        });

        if (getItemViewType(position) == TYPE_OTHERS) {
            String roleLabel = getRoleLabel(info.userRole);
            holder.tvName.setText(info.userName != null ? info.userName : "Usuario");
            holder.tvRole.setText(roleLabel);
            setRoleBadgeStyle(holder.tvRole, info.userRole);

            if (holder.tvInitials != null) {
                holder.tvInitials.setText(getInitials(info.userName));
            }

            if (info.userPhoto != null && !info.userPhoto.isEmpty()) {
                Bitmap b = ImageUtils.base64ToBitmap(info.userPhoto);
                if (b != null) {
                    holder.ivPhoto.setImageBitmap(b);
                    if (holder.cardPhoto != null) holder.cardPhoto.setVisibility(View.VISIBLE);
                } else {
                    if (holder.cardPhoto != null) holder.cardPhoto.setVisibility(View.GONE);
                }
            } else {
                if (holder.cardPhoto != null) holder.cardPhoto.setVisibility(View.GONE);
            }
        }
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
        if ("ADMIN".equals(role)) return "Admin";
        if ("PROFESSOR".equals(role)) return "Profesor";
        return "Estudiante";
    }

    private void setRoleBadgeStyle(TextView tv, String role) {
        if ("ADMIN".equals(role)) {
            tv.setBackgroundResource(R.drawable.bg_badge_danger);
        } else if ("PROFESSOR".equals(role)) {
            tv.setBackgroundResource(R.drawable.bg_badge_success);
        } else {
            tv.setBackgroundResource(R.drawable.bg_badge_primary);
        }
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvContent, tvRole, tvTimestamp, tvInitials;
        ImageView ivPhoto;
        View cardPhoto;

        ViewHolder(View itemView, int viewType) {
            super(itemView);
            tvContent = itemView.findViewById(R.id.tvCommentContent);
            tvTimestamp = itemView.findViewById(R.id.tvCommentTimestamp);
            if (viewType == TYPE_OTHERS) {
                tvName = itemView.findViewById(R.id.tvUserName);
                tvRole = itemView.findViewById(R.id.tvUserRole);
                tvInitials = itemView.findViewById(R.id.tvCommentInitials);
                ivPhoto = itemView.findViewById(R.id.ivUserPhoto);
                cardPhoto = itemView.findViewById(R.id.cardCommentPhoto);
            }
        }
    }
}
