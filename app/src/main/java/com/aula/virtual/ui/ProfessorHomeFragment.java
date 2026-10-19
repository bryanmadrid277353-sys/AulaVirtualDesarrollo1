package com.aula.virtual.ui;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.aula.virtual.R;
import com.aula.virtual.databinding.FragmentProfessorHomeBinding;

public class ProfessorHomeFragment extends Fragment {
    private FragmentProfessorHomeBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProfessorHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        MainViewModel viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        binding.cardMySubjects.setOnClickListener(v -> 
            Navigation.findNavController(view).navigate(R.id.action_professorHomeFragment_to_professorSubjectListFragment));
            
        binding.cardSettings.setOnClickListener(v -> 
            Navigation.findNavController(view).navigate(R.id.action_professorHomeFragment_to_settingsFragment));

        binding.cardLogout.setOnClickListener(v -> {
            viewModel.logout();
            Navigation.findNavController(view).navigate(R.id.action_professorHomeFragment_to_loginFragment);
        });

        binding.btnGoToProfileProf.setOnClickListener(v -> 
            Navigation.findNavController(view).navigate(R.id.action_professorHomeFragment_to_studentProfileFragment));

        if (viewModel.isImpersonating()) {
            binding.cardReturnToAdmin.setVisibility(View.VISIBLE);
            binding.cardReturnToAdmin.setOnClickListener(v -> {
                viewModel.stopImpersonation();
                Navigation.findNavController(view).popBackStack();
            });
        }

        viewModel.getCurrentUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                binding.tvProfessorName.setText("Bienvenido Prof. " + user.name);
                ImageUtils.setProfileImage(binding.ivProfileImageProf, user.profile_image, user.name);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
