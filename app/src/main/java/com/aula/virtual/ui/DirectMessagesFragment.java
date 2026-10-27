package com.aula.virtual.ui;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.aula.virtual.MainActivity;
import com.aula.virtual.R;
import com.aula.virtual.data.ChatRoomInfo;
import com.aula.virtual.data.entity.User;
import com.aula.virtual.databinding.FragmentDirectMessagesBinding;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DirectMessagesFragment extends Fragment {
    private FragmentDirectMessagesBinding binding;
    private MainViewModel viewModel;
    private ChatRoomAdapter adapter;
    private User currentUser;
    private List<ChatRoomInfo> allChatRooms = new ArrayList<>();
    private String selectedFilterRole = "ALL";
    private String searchQuery = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDirectMessagesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        currentUser = viewModel.getCurrentUser().getValue();
        if (currentUser == null) {
            binding.tvEmptyChats.setText("Debe iniciar sesión para ver sus mensajes directos.");
            binding.tvEmptyChats.setVisibility(View.VISIBLE);
            binding.rvChatRooms.setVisibility(View.GONE);
            return;
        }

        binding.btnBackDirectMessages.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToHome();
            } else {
                Navigation.findNavController(v).navigateUp();
            }
        });

        adapter = new ChatRoomAdapter();
        binding.rvChatRooms.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvChatRooms.setAdapter(adapter);

        adapter.setOnRoomClickListener(room -> {
            Bundle args = new Bundle();
            args.putString("chatRoomId", room.chatRoomId);
            args.putInt("contactId", room.contactId);
            args.putString("contactName", room.contactName);
            Navigation.findNavController(view).navigate(R.id.action_directMessagesFragment_to_directChatFragment, args);
        });

        binding.btnNewChat.setOnClickListener(v -> showNewChatContactSelector());

        binding.etSearchChatRooms.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s != null ? s.toString().toLowerCase().trim() : "";
                applyFilter();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        setupFilterChips();

        viewModel.getUserChatRooms().observe(getViewLifecycleOwner(), rooms -> {
            allChatRooms = rooms != null ? rooms : new ArrayList<>();
            applyFilter();
        });

        viewModel.fetchUserChatRooms(currentUser.id);
    }

    private void setupFilterChips() {
        binding.chipsLayout.removeAllViews();
        String[] filters = {"ALL", "PROFESSOR", "ENCARGADO", "ADMIN"};
        String[] labels = {"Todos", "Profesores", "Encargados", "Admins"};

        int primaryColor = ContextCompat.getColor(requireContext(), R.color.bs_primary);
        int inputBg = ContextCompat.getColor(requireContext(), R.color.input_bg);
        int border = ContextCompat.getColor(requireContext(), R.color.input_border);

        for (int i = 0; i < filters.length; i++) {
            String filterKey = filters[i];
            String label = labels[i];

            MaterialButton chip = new MaterialButton(requireContext(), null, com.google.android.material.R.style.Widget_Material3_Button_OutlinedButton);
            chip.setText(label);
            chip.setTextSize(12);
            chip.setAllCaps(false);
            chip.setCornerRadius(10);
            chip.setStrokeWidth(1);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    (int) (36 * getResources().getDisplayMetrics().density)
            );
            params.setMargins(0, 0, (int) (8 * getResources().getDisplayMetrics().density), 0);
            chip.setLayoutParams(params);

            boolean isSelected = filterKey.equals(selectedFilterRole);
            if (isSelected) {
                chip.setBackgroundTintList(ColorStateList.valueOf(primaryColor));
                chip.setTextColor(0xFFFFFFFF);
                chip.setStrokeColor(ColorStateList.valueOf(primaryColor));
            } else {
                chip.setBackgroundTintList(ColorStateList.valueOf(inputBg));
                chip.setTextColor(0xFF8E9BB0);
                chip.setStrokeColor(ColorStateList.valueOf(border));
            }

            chip.setOnClickListener(v -> {
                selectedFilterRole = filterKey;
                setupFilterChips();
                applyFilter();
            });

            binding.chipsLayout.addView(chip);
        }
    }

    private void applyFilter() {
        if (binding == null) return;
        List<ChatRoomInfo> filtered = allChatRooms.stream().filter(room -> {
            if (room == null) return false;

            if (!"ALL".equals(selectedFilterRole) && !selectedFilterRole.equals(room.contactRole)) {
                return false;
            }

            if (!searchQuery.isEmpty()) {
                String name = room.contactName != null ? room.contactName.toLowerCase() : "";
                String lastMsg = room.lastMessage != null ? room.lastMessage.toLowerCase() : "";
                return name.contains(searchQuery) || lastMsg.contains(searchQuery);
            }
            return true;
        }).collect(Collectors.toList());

        adapter.setRooms(filtered);
        if (filtered.isEmpty()) {
            binding.tvEmptyChats.setVisibility(View.VISIBLE);
            binding.rvChatRooms.setVisibility(View.GONE);
        } else {
            binding.tvEmptyChats.setVisibility(View.GONE);
            binding.rvChatRooms.setVisibility(View.VISIBLE);
        }
    }

    private void showNewChatContactSelector() {
        if (currentUser == null) return;

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_new_chat_selector, null);
        com.google.android.material.dialog.MaterialAlertDialogBuilder builder = new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext(), R.style.Theme_MyApplication_Dialog);
        builder.setView(dialogView);

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        View btnClose = dialogView.findViewById(R.id.btnCloseNewChatDialog);
        if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());

        android.widget.EditText etSearch = dialogView.findViewById(R.id.etSearchNewChat);
        LinearLayout chipsLayout = dialogView.findViewById(R.id.chipsNewChatLayout);
        androidx.recyclerview.widget.RecyclerView rvContacts = dialogView.findViewById(R.id.rvNewChatContacts);
        TextView tvEmpty = dialogView.findViewById(R.id.tvEmptyNewChatContacts);

        NewChatContactAdapter contactAdapter = new NewChatContactAdapter();
        rvContacts.setLayoutManager(new LinearLayoutManager(getContext()));
        rvContacts.setAdapter(contactAdapter);

        contactAdapter.setOnContactClickListener(selectedContact -> {
            dialog.dismiss();
            String roomId = "chat_" + Math.min(currentUser.id, selectedContact.id) + "_" + Math.max(currentUser.id, selectedContact.id);

            Bundle args = new Bundle();
            args.putString("chatRoomId", roomId);
            args.putInt("contactId", selectedContact.id);
            args.putString("contactName", selectedContact.name);
            Navigation.findNavController(requireView()).navigate(R.id.action_directMessagesFragment_to_directChatFragment, args);
        });

        final List<User>[] allEligible = new List[]{new ArrayList<>()};
        final String[] activeRoleFilter = {"ALL"};
        final String[] dialogSearchQuery = {""};

        Runnable applyDialogFilter = () -> {
            List<User> list = allEligible[0];
            if (list == null) list = new ArrayList<>();

            List<User> filtered = list.stream().filter(u -> {
                if (u == null) return false;
                if (!"ALL".equals(activeRoleFilter[0]) && !activeRoleFilter[0].equals(u.role)) {
                    return false;
                }
                if (!dialogSearchQuery[0].isEmpty()) {
                    String q = dialogSearchQuery[0].toLowerCase();
                    String name = u.name != null ? u.name.toLowerCase() : "";
                    String carnet = u.carnet != null ? u.carnet.toLowerCase() : "";
                    String role = u.role != null ? u.role.toLowerCase() : "";
                    return name.contains(q) || carnet.contains(q) || role.contains(q);
                }
                return true;
            }).collect(Collectors.toList());

            contactAdapter.setContacts(filtered);
            if (filtered.isEmpty()) {
                if (tvEmpty != null) tvEmpty.setVisibility(View.VISIBLE);
                rvContacts.setVisibility(View.GONE);
            } else {
                if (tvEmpty != null) tvEmpty.setVisibility(View.GONE);
                rvContacts.setVisibility(View.VISIBLE);
            }
        };

        Runnable setupDialogChips = new Runnable() {
            @Override
            public void run() {
                if (chipsLayout == null) return;
                chipsLayout.removeAllViews();

                List<User> fullList = allEligible[0];
                int total = fullList != null ? fullList.size() : 0;

                String[] filters = {"ALL", "PROFESSOR", "ENCARGADO", "ADMIN"};
                String[] labels = {"Todos " + (total > 0 ? total : ""), "Profesores", "Encargados", "Administradores"};

                int primaryColor = ContextCompat.getColor(requireContext(), R.color.bs_primary);
                int inputBg = ContextCompat.getColor(requireContext(), R.color.input_bg);
                int border = ContextCompat.getColor(requireContext(), R.color.input_border);

                for (int i = 0; i < filters.length; i++) {
                    String filterKey = filters[i];
                    String label = labels[i];

                    MaterialButton chip = new MaterialButton(requireContext(), null, com.google.android.material.R.style.Widget_Material3_Button_OutlinedButton);
                    chip.setText(label);
                    chip.setTextSize(12);
                    chip.setAllCaps(false);
                    chip.setCornerRadius(10);
                    chip.setStrokeWidth(1);

                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            (int) (36 * getResources().getDisplayMetrics().density)
                    );
                    params.setMargins(0, 0, (int) (8 * getResources().getDisplayMetrics().density), 0);
                    chip.setLayoutParams(params);

                    boolean isSelected = filterKey.equals(activeRoleFilter[0]);
                    if (isSelected) {
                        chip.setBackgroundTintList(ColorStateList.valueOf(primaryColor));
                        chip.setTextColor(0xFFFFFFFF);
                        chip.setStrokeColor(ColorStateList.valueOf(primaryColor));
                    } else {
                        chip.setBackgroundTintList(ColorStateList.valueOf(inputBg));
                        chip.setTextColor(0xFF8E9BB0);
                        chip.setStrokeColor(ColorStateList.valueOf(border));
                    }

                    chip.setOnClickListener(v -> {
                        activeRoleFilter[0] = filterKey;
                        run();
                        applyDialogFilter.run();
                    });

                    chipsLayout.addView(chip);
                }
            }
        };

        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    dialogSearchQuery[0] = s != null ? s.toString().trim() : "";
                    applyDialogFilter.run();
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        viewModel.getAllEligibleChatContacts(new retrofit2.Callback<List<User>>() {
            @Override
            public void onResponse(@NonNull Call<List<User>> call, @NonNull Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<User> users = response.body();
                    List<User> eligibleContacts = new ArrayList<>();
                    for (User u : users) {
                        if (u == null || u.id == currentUser.id) continue;
                        if ("STUDENT".equals(u.role)) continue; // Skip students

                        // ROLE PERMISSION RULES:
                        // Encargado/Professor CANNOT initiate chat with Admin unless Admin writes first!
                        if ("ADMIN".equals(u.role) && !"ADMIN".equals(currentUser.role)) {
                            continue; // Skip Admin for Non-Admins
                        }
                        eligibleContacts.add(u);
                    }

                    allEligible[0] = eligibleContacts;
                    setupDialogChips.run();
                    applyDialogFilter.run();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<User>> call, @NonNull Throwable t) {}
        });
        dialog.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
