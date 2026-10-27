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
import com.aula.virtual.data.entity.AttendanceRecord;
import com.aula.virtual.data.entity.AttendanceState;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AttendanceDateAdapter extends RecyclerView.Adapter<AttendanceDateAdapter.ViewHolder> {
    private List<AttendanceRecord> records = new ArrayList<>();
    private final SimpleDateFormat inSdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private final SimpleDateFormat outSdf = new SimpleDateFormat("EEEE dd/MM/yyyy", new Locale("es", "ES"));

    public void setRecords(List<AttendanceRecord> records) {
        this.records = records != null ? records : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_attendance_date_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AttendanceRecord record = records.get(position);
        if (record == null) return;

        String formattedDate = record.classDate;
        if (record.classDate != null) {
            try {
                Date d = inSdf.parse(record.classDate);
                if (d != null) {
                    String formatted = outSdf.format(d);
                    formattedDate = formatted.substring(0, 1).toUpperCase(Locale.getDefault()) + formatted.substring(1);
                }
            } catch (Exception ignored) {}
        }

        holder.tvClassDate.setText(formattedDate != null ? formattedDate : "-");

        setupBadgeStyle(holder.badgeStatus, record.status);

        if (record.status == 3 || (record.justification != null && !record.justification.trim().isEmpty())) {
            String text = (record.justification != null && !record.justification.trim().isEmpty())
                    ? record.justification.trim()
                    : "Ausencia justificada";
            holder.tvJustificationReason.setText(text);
            holder.layoutJustificationBox.setVisibility(View.VISIBLE);
        } else {
            holder.layoutJustificationBox.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return records.size();
    }

    private void setupBadgeStyle(TextView tv, int statusVal) {
        String label = AttendanceState.getLabelForValue(statusVal);
        tv.setText(label);

        boolean isDark = ThemeHelper.isDarkMode(tv.getContext());

        int textColor;
        int bgColor;
        int borderColor;

        switch (statusVal) {
            case 1: // Asistió
                textColor = isDark ? Color.parseColor("#4ADE80") : Color.parseColor("#15803D");
                bgColor = isDark ? Color.parseColor("#14532D") : Color.parseColor("#DCFCE7");
                borderColor = isDark ? Color.parseColor("#22C55E") : Color.parseColor("#86EFAC");
                break;
            case 2: // No asistió
                textColor = isDark ? Color.parseColor("#FCA5A5") : Color.parseColor("#B91C1C");
                bgColor = isDark ? Color.parseColor("#7F1D1D") : Color.parseColor("#FEE2E2");
                borderColor = isDark ? Color.parseColor("#EF4444") : Color.parseColor("#FCA5A5");
                break;
            case 3: // Justificado
                textColor = isDark ? Color.parseColor("#67E8F9") : Color.parseColor("#0E7490");
                bgColor = isDark ? Color.parseColor("#164E63") : Color.parseColor("#CFFAFE");
                borderColor = isDark ? Color.parseColor("#06B6D4") : Color.parseColor("#67E8F9");
                break;
            case 0: // Sin evaluar
            default:
                textColor = isDark ? Color.parseColor("#94A3B8") : Color.parseColor("#64748B");
                bgColor = isDark ? Color.parseColor("#1E293B") : Color.parseColor("#F1F5F9");
                borderColor = isDark ? Color.parseColor("#334155") : Color.parseColor("#CBD5E1");
                break;
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
        TextView tvClassDate, tvDayLabel, badgeStatus, tvJustificationReason;
        View layoutJustificationBox;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvClassDate = itemView.findViewById(R.id.tvAttendanceClassDate);
            tvDayLabel = itemView.findViewById(R.id.tvAttendanceDayLabel);
            badgeStatus = itemView.findViewById(R.id.badgeAttendanceStatus);
            layoutJustificationBox = itemView.findViewById(R.id.layoutJustificationBox);
            tvJustificationReason = itemView.findViewById(R.id.tvJustificationReason);
        }
    }
}
