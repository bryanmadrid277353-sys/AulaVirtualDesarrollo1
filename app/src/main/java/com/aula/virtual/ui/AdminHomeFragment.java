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

        binding.cardEncargados.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("filterRole", "ENCARGADO");
            Navigation.findNavController(view).navigate(R.id.action_adminHomeFragment_to_adminStudentListFragment, args);
        });

        binding.cardStudents.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("filterRole", "STUDENT");
            Navigation.findNavController(view).navigate(R.id.action_adminHomeFragment_to_adminStudentListFragment, args);
        });
        
        binding.cardSubjects.setOnClickListener(v -> 
            Navigation.findNavController(view).navigate(R.id.action_adminHomeFragment_to_adminSubjectListFragment));

        binding.cardAdmins.setOnClickListener(v -> 
            Navigation.findNavController(view).navigate(R.id.action_adminHomeFragment_to_adminAdminListFragment));

        binding.cardProfessors.setOnClickListener(v -> 
            Navigation.findNavController(view).navigate(R.id.action_adminHomeFragment_to_adminProfessorListFragment));

        binding.cardFacilities.setOnClickListener(v -> 
            Navigation.findNavController(view).navigate(R.id.action_adminHomeFragment_to_adminFacilityListFragment));



        viewModel.getCurrentUser().observe(getViewLifecycleOwner(), user -> {
            if (binding == null || !isAdded()) return;
            if (user != null) {
                binding.tvAdminName.setText(user.name);
                ImageUtils.setProfileImage(binding.ivProfileImageAdmin, user.profile_image, user.name);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
