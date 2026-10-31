package com.aula.virtual.ui;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.aula.virtual.R;
import com.aula.virtual.data.StudentGradeInfo;
import com.aula.virtual.data.entity.Enrollment;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GradeAdapter extends RecyclerView.Adapter<GradeAdapter.ViewHolder> {
    private List<StudentGradeInfo> gradeInfos = new ArrayList<>();
    private List<com.aula.virtual.data.entity.AcademicPeriod> activePeriods = null;
    private final boolean isAdmin;
    private boolean isStudentList = false;
    private OnGradeActionListener listener;

    public interface OnGradeActionListener {
        void onEditGrades(StudentGradeInfo info);
        void onViewBlog(StudentGradeInfo info);
        default void onDeleteEnrollment(StudentGradeInfo info) {}
        default void onStudentClick(StudentGradeInfo info) {}
        default void onAddPeriodClick(StudentGradeInfo info) {}
        default void onViewAttendance(StudentGradeInfo info) {}
        default void onViewConduct(StudentGradeInfo info) {}
    }

    private Map<Integer, String> facilityMap = new HashMap<>();
    private Map<Integer, String> professorMap = new HashMap<>();

    public GradeAdapter(boolean isAdmin) {
        this(isAdmin, false);
    }

    public GradeAdapter(boolean isAdmin, boolean isStudentList) {
        this.isAdmin = isAdmin;
        this.isStudentList = isStudentList;
    }

    public void setOnGradeActionListener(OnGradeActionListener listener) {
        this.listener = listener;
    }

    public void setGradeInfos(List<StudentGradeInfo> gradeInfos) {
        this.gradeInfos = gradeInfos;
        notifyDataSetChanged();
    }

    public void setActivePeriods(List<com.aula.virtual.data.entity.AcademicPeriod> periods) {
        this.activePeriods = periods;
        notifyDataSetChanged();
    }

    public void setFacilityMap(Map<Integer, String> map) {
        if (map != null) {
            this.facilityMap = map;
            notifyDataSetChanged();
        }
    }

    public void setProfessorMap(Map<Integer, String> map) {
        if (map != null) {
            this.professorMap = map;
            notifyDataSetChanged();
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_grade, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StudentGradeInfo info = gradeInfos.get(position);
        if (info == null) return;

        holder.tvName.setText(info.subject != null ? info.subject.name : "Materia Desconocida");

        if (holder.tvSection != null) {
            if (info.subject != null) {
                String sec = (info.subject.section != null && !info.subject.section.isEmpty()) ? info.subject.section : "01";
                holder.tvSection.setText("Sección: " + sec);
                holder.tvSection.setVisibility(View.VISIBLE);
            } else {
                holder.tvSection.setVisibility(View.GONE);
            }
        }

        if (holder.tvProfessor != null && info.subject != null) {
            String profName = professorMap.get(info.subject.id);
            if (profName != null && !profName.isEmpty()) {
                holder.tvProfessor.setText("Prof. " + profName);
                holder.tvProfessor.setVisibility(View.VISIBLE);
            } else {
                holder.tvProfessor.setText("Sin profesor asignado");
                holder.tvProfessor.setVisibility(View.VISIBLE);
            }
        }

        if (holder.tvFacility != null && info.subject != null) {
            String facName = facilityMap.get(info.subject.id);
            if (facName != null && !facName.isEmpty()) {
                holder.tvFacility.setText("🏢 Instalación: " + facName);
                holder.tvFacility.setVisibility(View.VISIBLE);
            } else {
                holder.tvFacility.setVisibility(View.GONE);
            }
        }

        // Entire Card Click -> Open Subject Blog (Gestión de la materia)
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onViewBlog(info);
        });

        if (holder.btnChevronRight != null) {
            holder.btnChevronRight.setOnClickListener(v -> {
                if (listener != null) listener.onViewBlog(info);
            });
        }

        // Modificar Notas button click
        if (holder.btnAddPeriod != null) {
            holder.btnAddPeriod.setOnClickListener(v -> {
                if (listener != null) listener.onEditGrades(info);
            });
        }

        // Asistencia button click
        if (holder.btnAttendance != null) {
            holder.btnAttendance.setOnClickListener(v -> {
                if (listener != null) listener.onViewAttendance(info);
            });
        }

        // Conducta button click
        if (holder.btnConduct != null) {
            holder.btnConduct.setOnClickListener(v -> {
                if (listener != null) listener.onViewConduct(info);
            });
        }

        // Render Periods Dynamically
        renderPeriodsAndAverage(holder, info);
    }

    private void renderPeriodsAndAverage(ViewHolder holder, StudentGradeInfo info) {
        if (holder.layoutPeriodsContainer == null) return;
        holder.layoutPeriodsContainer.removeAllViews();

        Enrollment e = info.enrollment;

        // If Academic Calendar is not set up in DB, do NOT display grades by period!
        if (activePeriods == null || activePeriods.isEmpty()) {
            int density = (int) holder.itemView.getContext().getResources().getDisplayMetrics().density;
            TextView tvWarning = new TextView(holder.itemView.getContext());
            tvWarning.setText("⚠️ Calendario Académico no configurado (Obligatorio para mostrar notas)");
            tvWarning.setTextSize(12);
            tvWarning.setTypeface(null, Typeface.BOLD);
            tvWarning.setTextColor(Color.parseColor("#F59E0B"));
            tvWarning.setPadding(4 * density, 8 * density, 4 * density, 8 * density);
            holder.layoutPeriodsContainer.addView(tvWarning);

            if (holder.tvStatusTag != null) {
                String cycleStr = (e != null && e.cycle != null) ? e.cycle : "Ciclo 2025";
                holder.tvStatusTag.setText("⚠️ Sin Calendario • " + cycleStr);
                holder.tvStatusTag.setTextColor(Color.parseColor("#F59E0B"));
            }
            return;
        }

        int periodsCount = activePeriods.size();

        double sum = 0;
        int filledCount = 0;

        for (int i = 1; i <= periodsCount; i++) {
            Double val = getGradeForPeriod(e, i);
            if (val != null) {
                sum += val;
                filledCount++;
            }

            View periodBox = createPeriodBox(holder.itemView.getContext(), "P" + i, formatGrade(val), getGradeColor(holder.itemView.getContext(), val));
            holder.layoutPeriodsContainer.addView(periodBox);
        }

        // Real Average calculated across active periods
        double avgReal = sum / periodsCount;
        String avgStr = String.format(Locale.getDefault(), "%.1f", avgReal);
        View avgBox = createAvgBox(holder.itemView.getContext(), "AVG", avgStr);
        holder.layoutPeriodsContainer.addView(avgBox);

        // Status Tag with Cycle Year
        if (holder.tvStatusTag != null) {
            String cycleStr = (e != null && e.cycle != null) ? e.cycle : "Ciclo 2025";
            boolean isPastCycle = !cycleStr.contains("2025");
            boolean hasPending = filledCount < periodsCount;

            if (isPastCycle) {
                if (avgReal >= 6.0) {
                    holder.tvStatusTag.setText("🟢 Aprobado • " + cycleStr);
                    holder.tvStatusTag.setTextColor(Color.parseColor("#10B981"));
                } else if (!hasPending && avgReal < 6.0) {
                    holder.tvStatusTag.setText("🔴 Reprobado • " + cycleStr);
                    holder.tvStatusTag.setTextColor(Color.parseColor("#EF4444"));
                } else {
                    holder.tvStatusTag.setText("📁 Finalizado • " + cycleStr);
                    holder.tvStatusTag.setTextColor(Color.parseColor("#94A3B8"));
                }
            } else {
                if (hasPending) {
                    holder.tvStatusTag.setText("🟡 En curso • " + cycleStr);
                    holder.tvStatusTag.setTextColor(Color.parseColor("#F59E0B"));
                } else if (avgReal >= 6.0) {
                    holder.tvStatusTag.setText("🟢 Aprobado • " + cycleStr);
                    holder.tvStatusTag.setTextColor(Color.parseColor("#10B981"));
                } else {
                    holder.tvStatusTag.setText("🔴 Reprobado • " + cycleStr);
                    holder.tvStatusTag.setTextColor(Color.parseColor("#EF4444"));
                }
            }
        }
    }

    private Double getGradeForPeriod(Enrollment e, int index) {
        if (e == null) return null;
        switch (index) {
            case 1: return e.grade1;
            case 2: return e.grade2;
            case 3: return e.grade3;
            case 4: return e.grade4;
            case 5: return e.grade5;
            case 6: return e.grade6;
            case 7: return e.grade7;
            case 8: return e.grade8;
            default: return null;
        }
    }

    private View createPeriodBox(android.content.Context ctx, String label, String val, int color) {
        MaterialCardView card = new MaterialCardView(ctx);
        int density = (int) ctx.getResources().getDisplayMetrics().density;

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(48 * density, 48 * density);
        lp.setMargins(0, 0, 8 * density, 0);
        card.setLayoutParams(lp);

        card.setRadius(10 * density);
        card.setCardElevation(0f);
        card.setCardBackgroundColor(ColorStateList.valueOf(Color.parseColor("#162032")));
        card.setStrokeWidth(1 * density);
        card.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#23334F")));

        LinearLayout layout = new LinearLayout(ctx);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));

        TextView tvLabel = new TextView(ctx);
        tvLabel.setText(label);
        tvLabel.setTextSize(10);
        tvLabel.setGravity(Gravity.CENTER);
        tvLabel.setTextColor(Color.parseColor("#8E9BB0"));

        TextView tvVal = new TextView(ctx);
        tvVal.setText(val);
        tvVal.setTextSize(14);
        tvVal.setTypeface(null, Typeface.BOLD);
        tvVal.setGravity(Gravity.CENTER);
        tvVal.setTextColor(color);

        layout.addView(tvLabel);
        layout.addView(tvVal);
        card.addView(layout);

        return card;
    }

    private View createAvgBox(android.content.Context ctx, String label, String val) {
        MaterialCardView card = new MaterialCardView(ctx);
        int density = (int) ctx.getResources().getDisplayMetrics().density;

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(58 * density, 48 * density);
        lp.setMargins(14 * density, 0, 0, 0);
        card.setLayoutParams(lp);

        card.setRadius(10 * density);
        card.setCardElevation(0f);
        card.setCardBackgroundColor(ColorStateList.valueOf(Color.parseColor("#132540")));
        card.setStrokeWidth((int) (1.5f * density));
        card.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#2563EB")));

        LinearLayout layout = new LinearLayout(ctx);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));

        TextView tvLabel = new TextView(ctx);
        tvLabel.setText(label);
        tvLabel.setTextSize(10);
        tvLabel.setTypeface(null, Typeface.BOLD);
        tvLabel.setGravity(Gravity.CENTER);
        tvLabel.setTextColor(Color.parseColor("#38BDF8"));

        TextView tvVal = new TextView(ctx);
        tvVal.setText(val);
        tvVal.setTextSize(18);
        tvVal.setTypeface(null, Typeface.BOLD);
        tvVal.setGravity(Gravity.CENTER);
        tvVal.setTextColor(Color.parseColor("#38BDF8"));

        layout.addView(tvLabel);
        layout.addView(tvVal);
        card.addView(layout);

        return card;
    }

    private int getGradeColor(android.content.Context ctx, Double grade) {
        if (grade == null) {
            return Color.parseColor("#8E9BB0");
        }
        if (grade <= 3.0) {
            return ContextCompat.getColor(ctx, R.color.bs_danger);
        } else if (grade <= 6.0) {
            return ContextCompat.getColor(ctx, R.color.bs_warning);
        } else {
            return ContextCompat.getColor(ctx, R.color.bs_success);
        }
    }

    private String formatGrade(Double grade) {
        if (grade == null) return "-";
        return String.format(Locale.getDefault(), "%.1f", grade);
    }

    @Override
    public int getItemCount() {
        return gradeInfos.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvSection, tvFacility, tvProfessor, tvStatusTag, btnAddPeriod, btnAttendance, btnConduct;
        ImageView btnChevronRight;
        LinearLayout layoutPeriodsContainer;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvSubjectName);
            tvSection = itemView.findViewById(R.id.tvSection);
            tvFacility = itemView.findViewById(R.id.tvFacility);
            tvProfessor = itemView.findViewById(R.id.tvProfessor);
            tvStatusTag = itemView.findViewById(R.id.tvStatusTag);
            btnAddPeriod = itemView.findViewById(R.id.btnAddPeriod);
            btnAttendance = itemView.findViewById(R.id.btnAttendance);
            btnConduct = itemView.findViewById(R.id.btnConduct);
            btnChevronRight = itemView.findViewById(R.id.btnChevronRight);
            layoutPeriodsContainer = itemView.findViewById(R.id.layoutPeriodsContainer);
        }
    }
}
