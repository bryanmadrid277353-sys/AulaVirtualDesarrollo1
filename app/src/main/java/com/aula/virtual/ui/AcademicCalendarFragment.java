package com.aula.virtual.ui;

import android.app.DatePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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
import com.aula.virtual.data.RetrofitClient;
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
    private final LinearLayout[] periodLayouts = new LinearLayout[5];
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

        periodLayouts[0] = binding.layoutPeriod1;
        periodLayouts[1] = binding.layoutPeriod2;
        periodLayouts[2] = binding.layoutPeriod3;
        periodLayouts[3] = binding.layoutPeriod4;
        periodLayouts[4] = binding.layoutPeriod5;

        String[] options = new String[]{"1 Período", "2 Períodos", "3 Períodos", "4 Períodos", "5 Períodos"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spPeriodCount.setAdapter(adapter);
        binding.spPeriodCount.setSelection(3); // Por defecto 4 Períodos (índice 3)

        binding.spPeriodCount.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updatePeriodLayoutsVisibility(position + 1);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

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

    private void updatePeriodLayoutsVisibility(int activeCount) {
        if (binding == null) return;
        for (int p = 0; p < 5; p++) {
            if (p < activeCount) {
                if (periodLayouts[p] != null) periodLayouts[p].setVisibility(View.VISIBLE);
            } else {
                if (periodLayouts[p] != null) periodLayouts[p].setVisibility(View.GONE);
                periodInputs[p][0].setText("");
                periodInputs[p][1].setText("");
            }
        }
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

        if (binding != null && target == binding.etNonSchoolDate) {
            Long minTime = null;
            Long maxTime = null;
            int activeCount = binding.spPeriodCount.getSelectedItemPosition() + 1;
            for (int p = 0; p < activeCount; p++) {
                String startStr = periodInputs[p][0].getText().toString().trim();
                String endStr = periodInputs[p][1].getText().toString().trim();
                try {
                    if (!startStr.isEmpty()) {
                        Date d = sdf.parse(startStr);
                        if (d != null && (minTime == null || d.getTime() < minTime)) minTime = d.getTime();
                    }
                    if (!endStr.isEmpty()) {
                        Date d = sdf.parse(endStr);
                        if (d != null && (maxTime == null || d.getTime() > maxTime)) maxTime = d.getTime();
                    }
                } catch (Exception ignored) {}
            }
            if (minTime != null) dialog.getDatePicker().setMinDate(minTime);
            if (maxTime != null) dialog.getDatePicker().setMaxDate(maxTime);
        }

        dialog.show();
    }

    private void loadData() {
        Context ctx = getContext();
        if (ctx == null) return;
        boolean isLocal = ThemeHelper.isLocalMode(ctx);

        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(ctx);
            List<AcademicPeriod> localPeriods = db.academicCalendarDao().getAllPeriods();
            List<NonSchoolDay> localNsd = db.academicCalendarDao().getAllNonSchoolDays();

            // Cargar inmediatamente los datos locales en caché (0ms de espera)
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (binding == null) return;
                    if (localPeriods != null && !localPeriods.isEmpty()) {
                        displayPeriods(localPeriods);
                    }
                    if (localNsd != null && !localNsd.isEmpty()) {
                        renderNonSchoolDaysList(localNsd);
                    }
                });
            }

            // Si está en Modo Servidor, solicitar datos actualizados en PARALELO
            if (!isLocal) {
                boolean isCacheEmpty = (localPeriods == null || localPeriods.isEmpty()) && (localNsd == null || localNsd.isEmpty());
                if (isCacheEmpty && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (binding != null) DialogUtils.showLoadingOverlay(binding.getRoot(), ctx);
                    });
                }

                java.util.concurrent.ExecutorService parallelExecutor = Executors.newFixedThreadPool(2);

                java.util.concurrent.Future<List<AcademicPeriod>> periodsFuture = parallelExecutor.submit(() -> {
                    try {
                        retrofit2.Response<List<AcademicPeriod>> resp =
                                RetrofitClient.getApiService(ctx).getAcademicPeriods().execute();
                        if (resp.isSuccessful() && resp.body() != null && !resp.body().isEmpty()) {
                            return resp.body();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return null;
                });

                java.util.concurrent.Future<List<NonSchoolDay>> nsdFuture = parallelExecutor.submit(() -> {
                    try {
                        retrofit2.Response<List<NonSchoolDay>> resp =
                                RetrofitClient.getApiService(ctx).getNonSchoolDays().execute();
                        if (resp.isSuccessful() && resp.body() != null && !resp.body().isEmpty()) {
                            return resp.body();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return null;
                });

                try {
                    List<AcademicPeriod> freshPeriods = periodsFuture.get();
                    List<NonSchoolDay> freshNsd = nsdFuture.get();

                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (binding == null) return;
                            DialogUtils.hideLoadingOverlay(binding.getRoot());
                            if (freshPeriods != null && !freshPeriods.isEmpty()) {
                                displayPeriods(freshPeriods);
                            }
                            if (freshNsd != null) {
                                renderNonSchoolDaysList(freshNsd);
                            }
                        });
                    }
                } catch (Exception e) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (binding != null) DialogUtils.hideLoadingOverlay(binding.getRoot());
                        });
                    }
                } finally {
                    parallelExecutor.shutdown();
                }
            }
        });
    }

    private void displayPeriods(List<AcademicPeriod> periods) {
        if (binding == null || periods == null) return;
        int activeCount = periods.size();
        if (activeCount < 1) activeCount = 4;
        if (activeCount > 5) activeCount = 5;

        binding.spPeriodCount.setSelection(activeCount - 1);
        updatePeriodLayoutsVisibility(activeCount);

        for (AcademicPeriod ap : periods) {
            if (ap.period >= 1 && ap.period <= activeCount) {
                int idx = ap.period - 1;
                periodInputs[idx][0].setText(ap.startDate != null ? ap.startDate : "");
                periodInputs[idx][1].setText(ap.endDate != null ? ap.endDate : "");
            }
        }
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
        if (binding == null) return;
        int activeCount = binding.spPeriodCount.getSelectedItemPosition() + 1;
        List<AcademicPeriod> list = new ArrayList<>();

        try {
            Date lastEndDate = null;

            for (int p = 0; p < activeCount; p++) {
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

            Context ctx = getContext();
            if (ctx == null) return;
            DialogUtils.showLoadingOverlay(binding.getRoot(), ctx);
            boolean isLocal = ThemeHelper.isLocalMode(ctx);

            Executors.newSingleThreadExecutor().execute(() -> {
                boolean success = false;
                String errorMsg = null;

                if (!isLocal) {
                    try {
                        retrofit2.Response<List<AcademicPeriod>> resp =
                                RetrofitClient.getApiService(ctx).saveAcademicPeriods(list).execute();
                        if (resp.isSuccessful() && resp.body() != null) {
                            success = true;
                            try {
                                AppDatabase.getInstance(ctx).academicCalendarDao().deleteAllPeriods();
                                AppDatabase.getInstance(ctx).academicCalendarDao().insertPeriods(list);
                            } catch (Exception ignored) {}
                        } else {
                            errorMsg = "Error en el servidor al guardar períodos (" + resp.code() + ")";
                        }
                    } catch (Exception e) {
                        errorMsg = "Error de conexión al servidor: " + e.getMessage();
                    }
                } else {
                    AppDatabase.getInstance(ctx).academicCalendarDao().deleteAllPeriods();
                    AppDatabase.getInstance(ctx).academicCalendarDao().insertPeriods(list);
                    success = true;
                }

                final boolean finalSuccess = success;
                final String finalMsg = errorMsg;

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        DialogUtils.hideLoadingOverlay(binding.getRoot());
                        if (finalSuccess) {
                            Toast.makeText(getContext(), isLocal ? "Calendario académico guardado localmente." : "Calendario académico guardado en el servidor.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), finalMsg != null ? finalMsg : "Error al guardar períodos.", Toast.LENGTH_LONG).show();
                        }
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

        try {
            Date targetDate = sdf.parse(dateStr);
            if (targetDate == null) {
                Toast.makeText(getContext(), "Formato de fecha inválido.", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean isWithinCalendar = false;
            boolean hasAnyPeriod = false;

            int activeCount = binding.spPeriodCount.getSelectedItemPosition() + 1;

            for (int p = 0; p < activeCount; p++) {
                String startStr = periodInputs[p][0].getText().toString().trim();
                String endStr = periodInputs[p][1].getText().toString().trim();
                if (!startStr.isEmpty() && !endStr.isEmpty()) {
                    hasAnyPeriod = true;
                    try {
                        Date startDate = sdf.parse(startStr);
                        Date endDate = sdf.parse(endStr);
                        if (startDate != null && endDate != null) {
                            if (!targetDate.before(startDate) && !targetDate.after(endDate)) {
                                isWithinCalendar = true;
                                break;
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }

            if (!hasAnyPeriod) {
                Toast.makeText(getContext(), "No hay Períodos Lectivos configurados. Establezca y guarde las fechas del Calendario Académico primero.", Toast.LENGTH_LONG).show();
                return;
            }

            if (!isWithinCalendar) {
                Toast.makeText(getContext(), "La fecha (" + dateStr + ") no pertenece a ningún Período Lectivo establecido en el Calendario Académico.", Toast.LENGTH_LONG).show();
                return;
            }

        } catch (Exception e) {
            Toast.makeText(getContext(), "Error al validar la fecha del asueto: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        Context ctx = getContext();
        if (ctx == null) return;
        DialogUtils.showLoadingOverlay(binding.getRoot(), ctx);
        boolean isLocal = ThemeHelper.isLocalMode(ctx);

        NonSchoolDay nsdNew = new NonSchoolDay(dateStr, reasonStr);

        Executors.newSingleThreadExecutor().execute(() -> {
            boolean success = false;
            String errorMsg = null;

            if (!isLocal) {
                try {
                    retrofit2.Response<NonSchoolDay> resp =
                            RetrofitClient.getApiService(ctx).createNonSchoolDay(nsdNew).execute();
                    if (resp.isSuccessful() && resp.body() != null) {
                        success = true;
                    } else {
                        errorMsg = "Error en el servidor (" + resp.code() + ")";
                    }
                } catch (Exception e) {
                    errorMsg = "Error de red: " + e.getMessage();
                }
            } else {
                AppDatabase.getInstance(ctx).academicCalendarDao().insertNonSchoolDay(nsdNew);
                success = true;
            }

            final boolean finalSuccess = success;
            final String finalMsg = errorMsg;

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    DialogUtils.hideLoadingOverlay(binding.getRoot());
                    if (finalSuccess) {
                        binding.etNonSchoolDate.setText("");
                        binding.etNonSchoolReason.setText("");
                        Toast.makeText(getContext(), isLocal ? "Día no lectivo agregado localmente." : "Día no lectivo agregado en el servidor.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), finalMsg != null ? finalMsg : "Error al guardar en el servidor.", Toast.LENGTH_LONG).show();
                    }
                    loadData();
                });
            }
        });
    }

    private void deleteNonSchoolDay(int id) {
        Context ctx = getContext();
        if (ctx == null) return;
        DialogUtils.showLoadingOverlay(binding.getRoot(), ctx);
        boolean isLocal = ThemeHelper.isLocalMode(ctx);

        Executors.newSingleThreadExecutor().execute(() -> {
            boolean success = false;

            if (!isLocal) {
                try {
                    retrofit2.Response<Void> resp = RetrofitClient.getApiService(ctx).deleteNonSchoolDay(id).execute();
                    if (resp.isSuccessful()) {
                        success = true;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                AppDatabase.getInstance(ctx).academicCalendarDao().deleteNonSchoolDay(id);
                success = true;
            }

            final boolean finalSuccess = success;

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    DialogUtils.hideLoadingOverlay(binding.getRoot());
                    if (finalSuccess) {
                        Toast.makeText(getContext(), isLocal ? "Día no lectivo eliminado localmente." : "Día no lectivo eliminado en el servidor.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Error al eliminar en el servidor.", Toast.LENGTH_SHORT).show();
                    }
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
