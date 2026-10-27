package com.aula.virtual.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import com.aula.virtual.R;
import com.aula.virtual.data.entity.Facility;
import com.aula.virtual.data.entity.User;
import com.aula.virtual.databinding.FragmentAdminFacilityDetailBinding;

public class AdminFacilityDetailFragment extends Fragment {
    private FragmentAdminFacilityDetailBinding binding;
    private MainViewModel viewModel;
    private int facilityId;
    private Facility facility;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminFacilityDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        facilityId = getArguments().getInt("facilityId");

        setupUI();
        observeViewModel();
        
        viewModel.getModificationError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                viewModel.clearModificationError();
            }
        });

        DialogUtils.showLoadingOverlay(binding.getRoot(), requireContext());
        viewModel.refreshData();
    }

    private void setupUI() {
        binding.btnBackDetail.setOnClickListener(v -> Navigation.findNavController(v).popBackStack());
        binding.btnSave.setEnabled(false);
        binding.btnDelete.setEnabled(false);

        binding.btnSave.setOnClickListener(v -> saveChanges());
        binding.btnDelete.setOnClickListener(v -> showDeleteConfirmation());
    }

    private void observeViewModel() {
        viewModel.getAllFacilities().observe(getViewLifecycleOwner(), facilities -> {
            for (Facility f : facilities) {
                if (f.id == facilityId) {
                    facility = f;
                    binding.etField1.setText(f.name != null ? f.name : "");

                    String codeStr = (f.code != null && !f.code.isEmpty()) ? f.code : ("INSTA" + (10000 + f.id));
                    if (codeStr.startsWith("INSTA")) {
                        codeStr = codeStr.substring(5);
                    }
                    binding.etField2.setText(codeStr);

                    binding.etType.setText(f.type != null ? f.type : "");
                    binding.etDesc.setText(f.description != null ? f.description : "");

                    binding.btnCopyField1.setOnClickListener(v -> 
                        ClipboardUtils.copyToClipboard(getContext(), "Nombre de la instalación", binding.etField1.getText().toString()));
                    binding.btnCopyField2.setOnClickListener(v -> 
                        ClipboardUtils.copyToClipboard(getContext(), "ID Instalación", "INSTA" + binding.etField2.getText().toString().trim()));
                    binding.btnCopyType.setOnClickListener(v -> 
                        ClipboardUtils.copyToClipboard(getContext(), "Tipo de instalación", binding.etType.getText().toString()));
                    binding.btnCopyDesc.setOnClickListener(v -> 
                        ClipboardUtils.copyToClipboard(getContext(), "Descripción de instalación", binding.etDesc.getText().toString()));

                    User currentUser = viewModel.getCurrentUser().getValue();
                    String modUser = (currentUser != null && currentUser.name != null) ? currentUser.name : "Admin Maestro";
                    binding.tvLastModifiedFooter.setText("Última modificación: Hoy por " + modUser);

                    binding.btnSave.setEnabled(true);
                    binding.btnDelete.setEnabled(true);

                    DialogUtils.hideLoadingOverlay(binding.getRoot());
                    break;
                }
            }
        });
    }

    private void saveChanges() {
        if (facility != null) {
            String name = binding.etField1.getText().toString().trim();
            if (name.isEmpty()) {
                binding.etField1.setError("El nombre es obligatorio");
                return;
            }

            String codeInput = binding.etField2.getText().toString().trim();
            String codeDigits = codeInput;
            if (codeDigits.startsWith("INSTA")) codeDigits = codeDigits.substring(5);

            if (!codeDigits.matches("\\d{5}")) {
                binding.etField2.setError("ID Instalación inválido (debe ingresar exactamente 5 dígitos numéricos)");
                return;
            }

            String type = binding.etType.getText().toString().trim();
            if (type.isEmpty()) {
                binding.etType.setError("El tipo de instalación es obligatorio");
                return;
            }

            facility.name = name;
            facility.code = "INSTA" + codeDigits;
            facility.type = type;
            facility.description = binding.etDesc.getText().toString().trim();

            viewModel.performOnlineAction(() -> {
                viewModel.updateFacility(facility);
                Toast.makeText(getContext(), "Instalación actualizada exitosamente", Toast.LENGTH_SHORT).show();
                Navigation.findNavController(requireView()).popBackStack();
            });
        }
    }

    private void popBackToFacilityManagement() {
        NavController navController = Navigation.findNavController(requireView());
        if (navController.popBackStack(R.id.adminFacilityListFragment, false)) {
            return;
        }
        navController.popBackStack();
    }

    private void showDeleteConfirmation() {
        if (facility == null) return;
        String typeStr = (facility.type != null && !facility.type.isEmpty()) ? facility.type : "Instalación";
        String badge = facility.code != null ? facility.code : ("INSTA" + (10000 + facility.id));
        DialogUtils.showConfirmDeleteDialog(
            requireContext(),
            "Confirmar Eliminación",
            "¿Estás seguro de que deseas eliminar esta instalación? Se borrarán todos los horarios asociados de forma permanente.",
            facility.name,
            typeStr + " • Espacio Físico",
            badge,
            null,
            R.drawable.ic_building,
            () -> {
                viewModel.performOnlineAction(() -> {
                    viewModel.deleteFacility(facility.id);
                    Toast.makeText(getContext(), "Instalación eliminada", Toast.LENGTH_SHORT).show();
                    popBackToFacilityManagement();
                });
            }
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
