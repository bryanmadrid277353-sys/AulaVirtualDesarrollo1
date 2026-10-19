package com.aula.virtual.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.aula.virtual.R;
import com.aula.virtual.data.entity.Facility;
import java.util.ArrayList;
import java.util.List;

public class FacilityAdapter extends RecyclerView.Adapter<FacilityAdapter.ViewHolder> {
    private List<Facility> facilities = new ArrayList<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Facility facility);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setFacilities(List<Facility> facilities) {
        this.facilities = facilities;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_facility, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Facility facility = facilities.get(position);
        if (facility == null) return;
        
        holder.tvName.setText(facility.name);
        holder.tvType.setText(facility.type);
        holder.tvDesc.setText(facility.description);
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(facility);
        });
        if (holder.btnManage != null) {
            holder.btnManage.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(facility);
            });
        }
    }

    @Override
    public int getItemCount() {
        return facilities.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvType, tvDesc;
        View btnManage;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvFacilityName);
            tvType = itemView.findViewById(R.id.tvFacilityType);
            tvDesc = itemView.findViewById(R.id.tvFacilityDescription);
            btnManage = itemView.findViewById(R.id.btnManage);
        }
    }
}
