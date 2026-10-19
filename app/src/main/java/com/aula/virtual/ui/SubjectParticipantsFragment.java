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
import com.aula.virtual.databinding.FragmentAdminDashboardBinding;

public class SubjectParticipantsFragment extends Fragment {
    private FragmentAdminDashboardBinding binding;
    private MainViewModel viewModel;
    private ParticipantAdapter adapter;
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

        binding.tvTitle.setText("Integrantes de la Materia");
        binding.btnAdd.setVisibility(View.GONE);
        binding.spinnerFilter.setVisibility(View.GONE);
        binding.etSearch.setHint("Buscar compañero...");

        adapter = new ParticipantAdapter(user -> {
            Bundle args = new Bundle();
            if ("STUDENT".equals(user.role)) {
                args.putInt("studentId", user.id);
                Navigation.findNavController(view).navigate(R.id.action_subjectParticipantsFragment_to_studentDetailFragment, args);
            } else {
                args.putInt("adminId", user.id);
                Navigation.findNavController(view).navigate(R.id.action_subjectParticipantsFragment_to_adminDetailFragment, args);
            }
        });
        
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerView.setAdapter(adapter);

        viewModel.getSubjectParticipants(subjectId).observe(getViewLifecycleOwner(), participants -> {
            adapter.setParticipants(participants);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
