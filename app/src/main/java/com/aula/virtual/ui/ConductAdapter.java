package com.aula.virtual.ui;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.aula.virtual.R;
import com.aula.virtual.data.entity.ConductLevel;
import com.aula.virtual.data.entity.ConductRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ConductAdapter extends RecyclerView.Adapter<ConductAdapter.ViewHolder> {
    private List<ConductRecord> records = new ArrayList<>();

    public void setRecords(List<ConductRecord> records) {
        this.records = records != null ? records : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_conduct_period, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ConductRecord record = records.get(position);
        if (record == null) return;

        holder.tvPeriodTitle.setText("Período " + record.period);

        Double avg = record.average;
        if (avg == null) avg = record.calculateAverage();

        if (avg != null) {
            holder.tvPeriodAverage.setText(String.format(Locale.US, "Promedio: %.1f / 5.0", avg));
            holder.tvPeriodAverage.setVisibility(View.VISIBLE);
        } else {
            holder.tvPeriodAverage.setText("Sin evaluar");
            holder.tvPeriodAverage.setVisibility(View.VISIBLE);
        }

        setupBadgeStyle(holder.badgeAttention, record.attention_participation);
        setupBadgeStyle(holder.badgeRules, record.rules_routines);
        setupBadgeStyle(holder.badgeSelfReg, record.self_regulation_effort);
        setupBadgeStyle(holder.badgeRespect, record.respect_collaboration);
        setupBadgeStyle(holder.badgeResponsibility, record.responsibility);

        if (record.observations != null && !record.observations.trim().isEmpty()) {
            holder.tvObservations.setText(record.observations.trim());
            holder.layoutObservations.setVisibility(View.VISIBLE);
        } else {
            holder.layoutObservations.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return records.size();
    }

    private void setupBadgeStyle(TextView tv, Integer levelVal) {
        String label = ConductLevel.getLabelForValue(levelVal);
        tv.setText(label);

        boolean isDark = ThemeHelper.isDarkMode(tv.getContext());

        int textColor;
        int bgColor;
        int borderColor;

        if (levelVal == null) {
            textColor = isDark ? Color.parseColor("#94A3B8") : Color.parseColor("#64748B");
            bgColor = isDark ? Color.parseColor("#1E293B") : Color.parseColor("#F1F5F9");
            borderColor = isDark ? Color.parseColor("#334155") : Color.parseColor("#CBD5E1");
        } else {
            switch (levelVal) {
                case 5: // Excelente
                    textColor = isDark ? Color.parseColor("#4ADE80") : Color.parseColor("#15803D");
                    bgColor = isDark ? Color.parseColor("#14532D") : Color.parseColor("#DCFCE7");
                    borderColor = isDark ? Color.parseColor("#22C55E") : Color.parseColor("#86EFAC");
                    break;
                case 4: // Muy bueno
                    textColor = isDark ? Color.parseColor("#86EFAC") : Color.parseColor("#16A34A");
                    bgColor = isDark ? Color.parseColor("#166534") : Color.parseColor("#F0FDF4");
                    borderColor = isDark ? Color.parseColor("#4ADE80") : Color.parseColor("#BBF7D0");
                    break;
                case 3: // Bueno
                    textColor = isDark ? Color.parseColor("#FDE047") : Color.parseColor("#A16207");
                    bgColor = isDark ? Color.parseColor("#713F12") : Color.parseColor("#FEF9C3");
                    borderColor = isDark ? Color.parseColor("#EAB308") : Color.parseColor("#FDE047");
                    break;
                case 2: // Regular
                    textColor = isDark ? Color.parseColor("#FDBA74") : Color.parseColor("#C2410C");
                    bgColor = isDark ? Color.parseColor("#7C2D12") : Color.parseColor("#FFEDD5");
                    borderColor = isDark ? Color.parseColor("#F97316") : Color.parseColor("#FDBA74");
                    break;
                case 1: // Malo
                    textColor = isDark ? Color.parseColor("#FCA5A5") : Color.parseColor("#B91C1C");
                    bgColor = isDark ? Color.parseColor("#7F1D1D") : Color.parseColor("#FEE2E2");
                    borderColor = isDark ? Color.parseColor("#EF4444") : Color.parseColor("#FCA5A5");
                    break;
                default:
                    textColor = isDark ? Color.parseColor("#94A3B8") : Color.parseColor("#64748B");
                    bgColor = isDark ? Color.parseColor("#1E293B") : Color.parseColor("#F1F5F9");
                    borderColor = isDark ? Color.parseColor("#334155") : Color.parseColor("#CBD5E1");
                    break;
            }
        }

        tv.setTextColor(textColor);
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setCornerRadius(12f);
        shape.setColor(bgColor);
        shape.setStroke(2, borderColor);
        tv.setBackground(shape);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPeriodTitle, tvPeriodAverage;
        TextView badgeAttention, badgeRules, badgeSelfReg, badgeRespect, badgeResponsibility;
        View layoutObservations;
        TextView tvObservations;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPeriodTitle = itemView.findViewById(R.id.tvPeriodTitle);
            tvPeriodAverage = itemView.findViewById(R.id.tvPeriodAverage);

            badgeAttention = itemView.findViewById(R.id.badgeAttention);
            badgeRules = itemView.findViewById(R.id.badgeRules);
            badgeSelfReg = itemView.findViewById(R.id.badgeSelfReg);
            badgeRespect = itemView.findViewById(R.id.badgeRespect);
            badgeResponsibility = itemView.findViewById(R.id.badgeResponsibility);

            layoutObservations = itemView.findViewById(R.id.layoutObservations);
            tvObservations = itemView.findViewById(R.id.tvObservations);
        }
    }
}
