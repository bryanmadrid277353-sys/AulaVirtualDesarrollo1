package com.aula.virtual.ui;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.aula.virtual.R;
import com.aula.virtual.data.entity.AcademicPeriod;
import com.aula.virtual.data.entity.NonSchoolDay;
import com.aula.virtual.data.local.AppDatabase;
import com.aula.virtual.databinding.FragmentAcademicCalendarBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class AcademicCalendarFragment extends Fragment {
    private FragmentAcademicCalendarBinding binding;
    private MainViewModel viewModel;

    private final EditText[][] periodInputs = new EditText[5][2];
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAcademicCalendarBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        setupUI();
        loadData();
    }

    private void setupUI() {
        binding.btnBackCalendar.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        periodInputs[0][0] = binding.etP1Start;
        periodInputs[0][1] = binding.etP1End;
        periodInputs[1][0] = binding.etP2Start;
        periodInputs[1][1] = binding.etP2End;
        periodInputs[2][0] = binding.etP3Start;
        periodInputs[2][1] = binding.etP3End;
        periodInputs[3][0] = binding.etP4Start;
        periodInputs[3][1] = binding.etP4End;
        periodInputs[4][0] = binding.etP5Start;
        periodInputs[4][1] = binding.etP5End;

        for (int p = 0; p < 5; p++) {
            final EditText etStart = periodInputs[p][0];
            final EditText etEnd = periodInputs[p][1];

            etStart.setOnClickListener(v -> showDatePicker(etStart));
            etEnd.setOnClickListener(v -> showDatePicker(etEnd));
        }

        binding.etNonSchoolDate.setOnClickListener(v -> showDatePicker(binding.etNonSchoolDate));

        binding.btnSavePeriods.setOnClickListener(v -> savePeriods());
        binding.btnAddNonSchoolDay.setOnClickListener(v -> addNonSchoolDay());
    }

    private void showDatePicker(EditText target) {
        Calendar cal = Calendar.getInstance();
        String current = target.getText().toString().trim();
        if (!current.isEmpty()) {
            try {
                Date d = sdf.parse(current);
                if (d != null) cal.setTime(d);
            } catch (Exception ignored) {}
        }

        DatePickerDialog dialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(year, month, dayOfMonth);
                    target.setText(sdf.format(selected.getTime()));
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }

    private void loadData() {
        if (getContext() == null) return;
        DialogUtils.showLoadingOverlay(binding.getRoot(), requireContext());

        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            List<AcademicPeriod> periods = db.academicCalendarDao().getAllPeriods();
            List<NonSchoolDay> nonSchoolDays = db.academicCalendarDao().getAllNonSchoolDays();

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    DialogUtils.hideLoadingOverlay(binding.getRoot());

                    if (periods != null) {
                        for (AcademicPeriod ap : periods) {
                            if (ap.period >= 1 && ap.period <= 5) {
                                int idx = ap.period - 1;
                                periodInputs[idx][0].setText(ap.startDate != null ? ap.startDate : "");
                                periodInputs[idx][1].setText(ap.endDate != null ? ap.endDate : "");
                            }
                        }
                    }

                    renderNonSchoolDaysList(nonSchoolDays);
                });
            }
        });
    }

    private void renderNonSchoolDaysList(List<NonSchoolDay> days) {
        if (binding == null) return;
        binding.layoutNonSchoolDaysList.removeAllViews();

        if (days == null || days.isEmpty()) {
            TextView tvEmpty = new TextView(requireContext());
            tvEmpty.setText("No hay días asuetos o festivos registrados.");
            tvEmpty.setTextSize(13);
            tvEmpty.setTextColor(android.graphics.Color.parseColor("#9E9E9E"));
            tvEmpty.setPadding(0, 8, 0, 8);
            binding.layoutNonSchoolDaysList.addView(tvEmpty);
            return;
        }

        for (NonSchoolDay nsd : days) {
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setPadding(8, 8, 8, 8);

            TextView tvInfo = new TextView(requireContext());
            tvInfo.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
            tvInfo.setText("📅 " + nsd.day + " - " + nsd.reason);
            tvInfo.setTextSize(13);
            tvInfo.setTextColor(android.graphics.Color.parseColor("#E0E0E0"));

            ImageView btnDel = new ImageView(requireContext());
            btnDel.setImageResource(R.drawable.ic_close);
            btnDel.setColorFilter(android.graphics.Color.parseColor("#EF4444"));
            btnDel.setPadding(8, 8, 8, 8);
            btnDel.setOnClickListener(v -> deleteNonSchoolDay(nsd.id));

            row.addView(tvInfo);
            row.addView(btnDel);
            binding.layoutNonSchoolDaysList.addView(row);
        }
    }

    private void savePeriods() {
        List<AcademicPeriod> list = new ArrayList<>();

        try {
            Date lastEndDate = null;

            for (int p = 0; p < 5; p++) {
                int periodNum = p + 1;
                String startStr = periodInputs[p][0].getText().toString().trim();
                String endStr = periodInputs[p][1].getText().toString().trim();

                if (startStr.isEmpty() || endStr.isEmpty()) {
                    Toast.makeText(getContext(), "Complete la fecha de inicio y fin para el Período " + periodNum, Toast.LENGTH_LONG).show();
                    return;
                }

                Date startDate = sdf.parse(startStr);
                Date endDate = sdf.parse(endStr);

                if (startDate == null || endDate == null) {
                    Toast.makeText(getContext(), "Formato de fecha inválido en el Período " + periodNum, Toast.LENGTH_LONG).show();
                    return;
                }

                if (startDate.after(endDate)) {
                    Toast.makeText(getContext(), "Período " + periodNum + ": La fecha de inicio debe ser menor o igual a la fecha de fin.", Toast.LENGTH_LONG).show();
                    return;
                }

                if (lastEndDate != null && !startDate.after(lastEndDate)) {
                    Toast.makeText(getContext(), "Error de traslape: La fecha de inicio del Período " + periodNum + " (" + startStr + ") debe ser posterior al fin del Período " + p + " (" + sdf.format(lastEndDate) + ").", Toast.LENGTH_LONG).show();
                    return;
                }

                lastEndDate = endDate;
                list.add(new AcademicPeriod(periodNum, startStr, endStr));
            }

            Executors.newSingleThreadExecutor().execute(() -> {
                AppDatabase db = AppDatabase.getInstance(requireContext());
                db.academicCalendarDao().insertPeriods(list);

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Calendario académico (Períodos 1-5) guardado exitosamente.", Toast.LENGTH_SHORT).show();
                    });
                }
            });

        } catch (Exception e) {
            Toast.makeText(getContext(), "Error al validar fechas: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void addNonSchoolDay() {
        String dateStr = binding.etNonSchoolDate.getText().toString().trim();
        String reasonStr = binding.etNonSchoolReason.getText().toString().trim();

        if (dateStr.isEmpty() || reasonStr.isEmpty()) {
            Toast.makeText(getContext(), "Ingrese la fecha y el motivo del asueto/día sin clases.", Toast.LENGTH_SHORT).show();
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            db.academicCalendarDao().insertNonSchoolDay(new NonSchoolDay(dateStr, reasonStr));

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    binding.etNonSchoolDate.setText("");
                    binding.etNonSchoolReason.setText("");
                    Toast.makeText(getContext(), "Día no lectivo agregado.", Toast.LENGTH_SHORT).show();
                    loadData();
                });
            }
        });
    }

    private void deleteNonSchoolDay(int id) {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            db.academicCalendarDao().deleteNonSchoolDay(id);

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Día no lectivo eliminado.", Toast.LENGTH_SHORT).show();
                    loadData();
                });
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
