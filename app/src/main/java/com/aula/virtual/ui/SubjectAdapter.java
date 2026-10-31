package com.aula.virtual.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.aula.virtual.R;
import com.aula.virtual.data.entity.Subject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SubjectAdapter extends RecyclerView.Adapter<SubjectAdapter.ViewHolder> {
    private List<Subject> subjects = new ArrayList<>();
    private Map<Integer, String> facilityMap = new HashMap<>();
    private OnItemClickListener listener;
    private OnDeleteClickListener deleteListener;
    private boolean showDeleteButton = false;

    public interface OnItemClickListener {
        void onItemClick(Subject subject);
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(Subject subject);
    }

    private String manageButtonText = "Gestionar";
    private String deleteButtonText = "Borrar";

    public void setManageButtonText(String text) {
        this.manageButtonText = text;
        notifyDataSetChanged();
    }

    public void setDeleteButtonText(String text) {
        this.deleteButtonText = text;
        notifyDataSetChanged();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setOnDeleteClickListener(OnDeleteClickListener deleteListener) {
        this.deleteListener = deleteListener;
    }

    public void setShowDeleteButton(boolean show) {
        this.showDeleteButton = show;
        notifyDataSetChanged();
    }

    public void setSubjects(List<Subject> subjects) {
        this.subjects = subjects;
        notifyDataSetChanged();
    }

    public void setFacilityMap(Map<Integer, String> map) {
        if (map != null) {
            this.facilityMap = map;
            notifyDataSetChanged();
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_subject, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Subject subject = subjects.get(position);
        if (subject == null) return;
        
        holder.tvName.setText(subject.name != null ? subject.name : "Sin nombre");

        String secRaw = (subject.section != null && !subject.section.isEmpty()) ? subject.section : "1";
        String secNum = secRaw;
        try {
            secNum = String.valueOf(Integer.parseInt(secRaw));
        } catch (Exception ignored) {}

        holder.tvSection.setText("Sección: " + secNum);
        holder.tvSection.setTextColor(holder.itemView.getContext().getColor(R.color.accent_cyan));

        if (subject.description != null && !subject.description.isEmpty()) {
            holder.tvDescription.setText(subject.description);
            holder.tvDescription.setVisibility(View.VISIBLE);
        } else {
            holder.tvDescription.setVisibility(View.GONE);
        }

        if (holder.tvFacility != null) {
            String facName = facilityMap.get(subject.id);
            if (facName != null && !facName.isEmpty()) {
                holder.tvFacility.setText("🏢 Instalación: " + facName);
                holder.tvFacility.setVisibility(View.VISIBLE);
            } else {
                holder.tvFacility.setVisibility(View.GONE);
            }
        }
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(subject);
        });
        if (holder.btnManage != null) {
            holder.btnManage.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(subject);
            });
        }

        if (holder.btnDelete != null) {
            holder.btnDelete.setVisibility(showDeleteButton ? View.VISIBLE : View.GONE);
            holder.btnDelete.setOnClickListener(v -> {
                if (deleteListener != null) deleteListener.onDeleteClick(subject);
            });
        }
    }

    @Override
    public int getItemCount() {
        return subjects.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvFaculty, tvDescription, tvSection, tvFacility;
        View btnManage;
        Button btnDelete;
        View layoutButtons;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvSubjectName);
            tvFaculty = itemView.findViewById(R.id.tvFaculty);
            tvSection = itemView.findViewById(R.id.tvSection);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvFacility = itemView.findViewById(R.id.tvFacility);
            btnManage = itemView.findViewById(R.id.btnManage);
            btnDelete = itemView.findViewById(R.id.btnDeleteSubject);
            layoutButtons = itemView.findViewById(R.id.layoutButtons);
        }
    }
}
