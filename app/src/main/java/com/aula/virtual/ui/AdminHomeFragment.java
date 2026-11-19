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
import com.aula.virtual.R;
import com.aula.virtual.databinding.FragmentAdminHomeBinding;

public class AdminHomeFragment extends Fragment {
    private FragmentAdminHomeBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        MainViewModel viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        updateHomeLayout();

        View.OnClickListener openEncargados = v -> {
            Bundle args = new Bundle();
            args.putString("filterRole", "ENCARGADO");
            Navigation.findNavController(v).navigate(R.id.action_adminHomeFragment_to_adminStudentListFragment, args);
        };
        binding.cardEncargados.setOnClickListener(openEncargados);
        if (binding.cardEncargadosGrid != null) binding.cardEncargadosGrid.setOnClickListener(openEncargados);

        View.OnClickListener openStudents = v -> {
            Bundle args = new Bundle();
            args.putString("filterRole", "STUDENT");
            Navigation.findNavController(v).navigate(R.id.action_adminHomeFragment_to_adminStudentListFragment, args);
        };
        binding.cardStudents.setOnClickListener(openStudents);
        if (binding.cardStudentsGrid != null) binding.cardStudentsGrid.setOnClickListener(openStudents);

        View.OnClickListener openSubjects = v ->
            Navigation.findNavController(v).navigate(R.id.action_adminHomeFragment_to_adminSubjectListFragment);
        binding.cardSubjects.setOnClickListener(openSubjects);
        if (binding.cardSubjectsGrid != null) binding.cardSubjectsGrid.setOnClickListener(openSubjects);

        View.OnClickListener openAdmins = v ->
            Navigation.findNavController(v).navigate(R.id.action_adminHomeFragment_to_adminAdminListFragment);
        binding.cardAdmins.setOnClickListener(openAdmins);
        if (binding.cardAdminsGrid != null) binding.cardAdminsGrid.setOnClickListener(openAdmins);

        View.OnClickListener openProfessors = v ->
            Navigation.findNavController(v).navigate(R.id.action_adminHomeFragment_to_adminProfessorListFragment);
        binding.cardProfessors.setOnClickListener(openProfessors);
        if (binding.cardProfessorsGrid != null) binding.cardProfessorsGrid.setOnClickListener(openProfessors);

        View.OnClickListener openFacilities = v ->
            Navigation.findNavController(v).navigate(R.id.action_adminHomeFragment_to_adminFacilityListFragment);
        binding.cardFacilities.setOnClickListener(openFacilities);
        if (binding.cardFacilitiesGrid != null) binding.cardFacilitiesGrid.setOnClickListener(openFacilities);

        viewModel.getCurrentUser().observe(getViewLifecycleOwner(), user -> {
            if (binding == null || !isAdded()) return;
            if (user != null) {
                binding.tvAdminName.setText(user.name);
                ImageUtils.setProfileImage(binding.ivProfileImageAdmin, user.profile_image, user.name);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        updateHomeLayout();
    }

    private void updateHomeLayout() {
        if (binding == null || getContext() == null) return;
        boolean isGrid = ThemeHelper.isGridHomeLayout(requireContext());
        if (binding.layoutAdminHomeGrid != null && binding.layoutAdminHomeList != null) {
            binding.layoutAdminHomeGrid.setVisibility(isGrid ? View.VISIBLE : View.GONE);
            binding.layoutAdminHomeList.setVisibility(isGrid ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
