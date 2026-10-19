package com.aula.virtual.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.aula.virtual.MainActivity;
import com.aula.virtual.R;
import com.aula.virtual.data.entity.Subject;
import com.aula.virtual.data.entity.User;
import com.aula.virtual.databinding.FragmentAdminProfessorDetailBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ProfessorSubjectListFragment extends Fragment {
    private FragmentAdminProfessorDetailBinding binding;
    private MainViewModel viewModel;
    private SubjectAdapter adapter;
    private List<Subject> allProfSubjects = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminProfessorDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        binding.btnBackList.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToHome();
            } else {
                Navigation.findNavController(v).navigateUp();
            }
        });

        binding.tvHeaderTitle.setText("Gestión del profesor");
        binding.tvTitle.setText("Materias asignadas");
        binding.tvSubtitle.setText("Consulta y gestiona las materias del profesor.");

        adapter = new SubjectAdapter();
        adapter.setManageButtonText("Gestionar");
        adapter.setShowDeleteButton(false);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerView.setAdapter(adapter);

        int targetProfId = -1;
        if (getArguments() != null && getArguments().containsKey("professorId")) {
            targetProfId = getArguments().getInt("professorId");
        } else if (getArguments() != null && getArguments().containsKey("adminId")) {
            targetProfId = getArguments().getInt("adminId");
        } else {
            User prof = viewModel.getCurrentUser().getValue();
            if (prof != null) targetProfId = prof.id;
        }

        if (targetProfId != -1) {
            final int profId = targetProfId;

            viewModel.getUserById(profId, user -> {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (binding != null && user != null) {
                            binding.tvProfessorName.setText(user.name);
                            binding.tvProfessorCarnet.setText("Carnet: " + (user.carnet != null ? user.carnet : "-"));
                            binding.tvProfessorInitials.setText(getInitials(user.name));
                        }
                    });
                }
            });

            // Horario Button
            binding.btnHeaderAction.setOnClickListener(v -> {
                Bundle args = new Bundle();
                args.putInt("professorId", profId);
                Navigation.findNavController(view).navigate(R.id.action_professorSubjectListFragment_to_adminFacilityTimetableFragment, args);
            });

            // Detalle Button
            binding.btnHeaderDetail.setOnClickListener(v -> {
                Bundle args = new Bundle();
                args.putInt("adminId", profId);
                Navigation.findNavController(view).navigate(R.id.action_professorSubjectListFragment_to_adminDetailFragment, args);
            });

            binding.btnAdd.setText("+ Inscribir en materia");
            binding.btnAdd.setOnClickListener(v -> showAssignSubjectDialog(profId));

            viewModel.getSubjectsByProfessor(profId).observe(getViewLifecycleOwner(), subjects -> {
                allProfSubjects = subjects != null ? subjects : new ArrayList<>();
                applyFilter();
            });

            viewModel.fetchSubjectFacilityMap(map -> {
                if (adapter != null && map != null) {
                    adapter.setFacilityMap(map);
                }
            });
        }

        setupSearch();

        adapter.setOnItemClickListener(subject -> {
            Bundle args = new Bundle();
            args.putInt("subjectId", subject.id);
            Navigation.findNavController(view).navigate(R.id.action_professorSubjectListFragment_to_subjectBlogFragment, args);
        });
    }

    private String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) return "AV";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        } else {
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
        }
    }

    private void setupSearch() {
        String[] options = {"Todo", "Materia"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, options);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerFilter.setAdapter(spinnerAdapter);

        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applyFilter(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        binding.spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { applyFilter(); }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void applyFilter() {
        String query = binding.etSearch.getText().toString().toLowerCase().trim();
        if (query.isEmpty()) {
            adapter.setSubjects(allProfSubjects);
            binding.tvRecordCount.setText(allProfSubjects.size() + (allProfSubjects.size() == 1 ? " materia" : " materias"));
            binding.tvNoSubjectsMessage.setVisibility(allProfSubjects.isEmpty() ? View.VISIBLE : View.GONE);
            return;
        }

        List<Subject> filtered = allProfSubjects.stream().filter(s -> {
            if (s == null) return false;
            String name = s.name != null ? s.name.toLowerCase() : "";
            String desc = s.description != null ? s.description.toLowerCase() : "";
            return name.contains(query) || desc.contains(query);
        }).collect(Collectors.toList());

        adapter.setSubjects(filtered);
        binding.tvRecordCount.setText(filtered.size() + (filtered.size() == 1 ? " materia" : " materias"));
        binding.tvNoSubjectsMessage.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private <T> void observeOnce(androidx.lifecycle.LiveData<T> liveData, androidx.lifecycle.Observer<T> observer) {
        liveData.observe(getViewLifecycleOwner(), new androidx.lifecycle.Observer<T>() {
            @Override
            public void onChanged(T t) {
                if (t != null) {
                    liveData.removeObserver(this);
                    observer.onChanged(t);
                }
            }
        });
    }

    private void showAssignSubjectDialog(int profId) {
        observeOnce(viewModel.getAllSubjects(), allSubjects -> {
            if (allSubjects == null || allSubjects.isEmpty()) {
                Toast.makeText(getContext(), "No hay materias registradas", Toast.LENGTH_SHORT).show();
                return;
            }
            observeOnce(viewModel.getSubjectsByProfessor(profId), assignedSubjects -> {
                List<Integer> assignedSubjectIds = assignedSubjects != null ? 
                    assignedSubjects.stream().map(s -> s.id).collect(Collectors.toList()) : 
                    new ArrayList<>();

                List<Subject> availableSubjects = allSubjects.stream()
                    .filter(s -> !assignedSubjectIds.contains(s.id))
                    .collect(Collectors.toList());

                if (availableSubjects.isEmpty()) {
                    Toast.makeText(getContext(), "El profesor ya está inscrito en todas las materias disponibles.", Toast.LENGTH_SHORT).show();
                    return;
                }

                String[] names = new String[availableSubjects.size()];
                for (int i = 0; i < availableSubjects.size(); i++) {
                    Subject s = availableSubjects.get(i);
                    String sec = s.section != null ? s.section : "01";
                    names[i] = s.name + " (Sección: " + sec + ")";
                }

                new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Inscribir Materia a Profesor")
                    .setItems(names, (dialog, which) -> {
                        Subject selected = availableSubjects.get(which);
                        if (selected.professorId != null && selected.professorId != 0 && selected.professorId != profId) {
                            viewModel.getUserById(selected.professorId, existingProf -> {
                                String profName = existingProf != null ? existingProf.name : "otro profesor";
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() -> 
                                        new MaterialAlertDialogBuilder(requireContext())
                                            .setTitle("⚠️ Asignación Restringida")
                                            .setMessage("La materia \"" + selected.name + "\" ya está asignada al profesor " + profName + ".\n\nSolo se permite UN Profesor por Materia.")
                                            .setPositiveButton("Entendido", null)
                                            .show()
                                    );
                                }
                            });
                            return;
                        }

                        selected.professorId = profId;
                        viewModel.performOnlineAction(() -> {
                            viewModel.updateSubject(selected);
                            viewModel.updateSchedulesProfessorBySubject(selected.id, profId);
                            Toast.makeText(getContext(), "¡Profesor inscrito/asignado a la materia!", Toast.LENGTH_SHORT).show();
                            viewModel.getSubjectsByProfessor(profId);
                            viewModel.refreshData();
                        });
                    })
                    .show();
            });
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
