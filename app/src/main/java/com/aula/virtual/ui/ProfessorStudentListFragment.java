package com.aula.virtual.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.aula.virtual.R;
import com.aula.virtual.data.StudentGradeInfo;
import com.aula.virtual.databinding.FragmentAdminDashboardBinding;

public class ProfessorStudentListFragment extends Fragment {
    private FragmentAdminDashboardBinding binding;
    private MainViewModel viewModel;
    private GradeAdapter adapter;
    private int subjectId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        subjectId = getArguments().getInt("subjectId");

        viewModel.getSubjectById(subjectId, sub -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (binding != null && sub != null) {
                        binding.tvTitle.setText(sub.name);
                        binding.tvSubtitle.setText(sub.description);
                        binding.tvSubtitle.setVisibility(View.VISIBLE);
                    }
                });
            }
        });

        binding.btnAdd.setText("Foro de Discusión");
        binding.btnAdd.setVisibility(View.VISIBLE);
        binding.spinnerFilter.setVisibility(View.GONE);
        binding.etSearch.setHint("Buscar alumno...");

        adapter = new GradeAdapter(true, true); // Professor viewing student list
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerView.setAdapter(adapter);

        viewModel.getStudentsBySubject(subjectId).observe(getViewLifecycleOwner(), studentInfos -> {
            adapter.setGradeInfos(studentInfos); 
            if (studentInfos != null) {
                binding.tvRecordCount.setText(studentInfos.size() + (studentInfos.size() == 1 ? " alumno" : " alumnos"));
            } else {
                binding.tvRecordCount.setText("0 alumnos");
            }
        });

        adapter.setOnGradeActionListener(new GradeAdapter.OnGradeActionListener() {
            @Override
            public void onEditGrades(StudentGradeInfo info) {
                Bundle args = new Bundle();
                args.putInt("enrollmentId", info.enrollment.id);
                Navigation.findNavController(requireView()).navigate(R.id.action_professorStudentListFragment_to_adminEditGradeFragment, args);
            }

            @Override
            public void onViewBlog(StudentGradeInfo info) {
                Bundle args = new Bundle();
                args.putInt("studentId", info.enrollment.studentId);
                Navigation.findNavController(requireView()).navigate(R.id.action_professorStudentListFragment_to_studentProfileFragment, args);
            }

            @Override
            public void onStudentClick(StudentGradeInfo info) {
                Bundle args = new Bundle();
                args.putInt("studentId", info.enrollment.studentId);
                Navigation.findNavController(requireView()).navigate(R.id.action_professorStudentListFragment_to_studentProfileFragment, args);
            }
        });

        binding.btnAdd.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putInt("subjectId", subjectId);
            Navigation.findNavController(view).navigate(R.id.action_professorStudentListFragment_to_subjectBlogFragment, args);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
