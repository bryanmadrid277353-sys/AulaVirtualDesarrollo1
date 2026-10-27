package com.aula.virtual.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.aula.virtual.R;
import com.aula.virtual.data.CommentInfo;
import com.aula.virtual.data.entity.BlogComment;
import com.aula.virtual.data.entity.DirectMessage;
import com.aula.virtual.data.entity.User;
import com.aula.virtual.databinding.FragmentDirectChatBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class DirectChatFragment extends Fragment {
    private FragmentDirectChatBinding binding;
    private MainViewModel viewModel;
    private BlogCommentAdapter adapter;
    private User currentUser;
    private int contactId;
    private String contactName;
    private String chatRoomId;
    private List<DirectMessage> rawHistory = new ArrayList<>();
    private String currentSearchQuery = "";

    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private static final int POLL_INTERVAL = 3000;

    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            if (viewModel != null && isResumed() && currentUser != null) {
                viewModel.fetchDirectMessagesHistory(currentUser.id, contactId);
                pollHandler.postDelayed(this, POLL_INTERVAL);
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDirectChatBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        currentUser = viewModel.getCurrentUser().getValue();
        if (getArguments() != null) {
            chatRoomId = getArguments().getString("chatRoomId");
            contactId = getArguments().getInt("contactId");
            contactName = getArguments().getString("contactName", "Contacto");
        }

        binding.tvChatContactName.setText(contactName);
        binding.btnBackChat.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        boolean isAdmin = currentUser != null && "ADMIN".equals(currentUser.role);
        int currentUserId = currentUser != null ? currentUser.id : -1;

        adapter = new BlogCommentAdapter(currentUserId, isAdmin);
        binding.rvChatMessages.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvChatMessages.setAdapter(adapter);

        binding.btnDirectChatMenu.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(requireContext(), v);
            popup.getMenu().add(0, 1, 0, "🔍 Buscar mensaje");
            popup.getMenu().add(0, 2, 1, "🔕 Silenciar Notificaciones");
            if (isAdmin) {
                popup.getMenu().add(0, 3, 2, "🗑️ Eliminar chat permanentemente");
            }
            popup.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == 1) {
                    boolean isVis = binding.layoutSearchContainer.getVisibility() == View.VISIBLE;
                    binding.layoutSearchContainer.setVisibility(isVis ? View.GONE : View.VISIBLE);
                    if (!isVis) {
                        binding.etSearchChat.requestFocus();
                    } else {
                        currentSearchQuery = "";
                        binding.etSearchChat.setText("");
                        applyChatSearch();
                    }
                    return true;
                } else if (id == 2) {
                    Toast.makeText(getContext(), "Notificaciones silenciadas para esta conversación", Toast.LENGTH_SHORT).show();
                    return true;
                } else if (id == 3 && isAdmin) {
                    showDeleteChatRoomConfirmation();
                    return true;
                }
                return false;
            });
            popup.show();
        });

        binding.btnCloseSearch.setOnClickListener(v -> {
            binding.layoutSearchContainer.setVisibility(View.GONE);
            currentSearchQuery = "";
            binding.etSearchChat.setText("");
            applyChatSearch();
        });

        binding.etSearchChat.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s != null ? s.toString().toLowerCase().trim() : "";
                applyChatSearch();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        binding.btnSendDirectMsg.setOnClickListener(v -> {
            String msgText = binding.etChatMessage.getText().toString().trim();
            if (!msgText.isEmpty() && currentUser != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                String timestamp = sdf.format(new Date());

                String roomId = chatRoomId != null ? chatRoomId : ("chat_" + Math.min(currentUser.id, contactId) + "_" + Math.max(currentUser.id, contactId));
                DirectMessage msg = new DirectMessage(roomId, currentUser.id, contactId, msgText, timestamp, 0, currentUser.id);

                viewModel.sendDirectMessage(msg);
                binding.etChatMessage.setText("");
            }
        });

        viewModel.getChatHistory().observe(getViewLifecycleOwner(), messages -> {
            if (messages != null) {
                rawHistory = new ArrayList<>(messages);
                applyChatSearch();
            }
        });

        viewModel.fetchDirectMessagesHistory(currentUserId, contactId);
        pollHandler.post(pollRunnable);
    }

    private void applyChatSearch() {
        if (binding == null) return;
        List<CommentInfo> commentInfos = new ArrayList<>();
        for (DirectMessage msg : rawHistory) {
            if (msg == null) continue;
            boolean matchesSearch = currentSearchQuery.isEmpty() || (msg.message != null && msg.message.toLowerCase().contains(currentSearchQuery));
            if (matchesSearch) {
                CommentInfo ci = new CommentInfo();
                ci.comment = new BlogComment();
                ci.comment.id = msg.id;
                ci.comment.userId = msg.senderId;
                ci.comment.content = msg.message;
                ci.comment.timestamp = msg.timestamp;
                ci.userName = msg.senderId == currentUser.id ? currentUser.name : contactName;
                ci.userRole = msg.senderId == currentUser.id ? currentUser.role : "USER";
                commentInfos.add(ci);
            }
        }

        int oldSize = adapter.getItemCount();
        adapter.setComments(commentInfos);
        int newSize = adapter.getItemCount();
        if (newSize > oldSize && newSize > 0 && currentSearchQuery.isEmpty()) {
            binding.rvChatMessages.scrollToPosition(newSize - 1);
        }
    }

    private void showDeleteChatRoomConfirmation() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Eliminar Chat Permanentemente")
                .setMessage("¿Estás seguro de que deseas eliminar este chat privado? La conversación se cerrará y borrará para ambos usuarios.")
                .setPositiveButton("Eliminar Chat", (dialog, which) -> {
                    if (chatRoomId != null && currentUser != null) {
                        viewModel.deleteChatRoom(chatRoomId, currentUser.id);
                        Toast.makeText(getContext(), "Chat eliminado exitosamente", Toast.LENGTH_SHORT).show();
                        Navigation.findNavController(requireView()).popBackStack();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    @Override
    public void onPause() {
        super.onPause();
        pollHandler.removeCallbacks(pollRunnable);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (currentUser != null) {
            pollHandler.postDelayed(pollRunnable, POLL_INTERVAL);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        pollHandler.removeCallbacks(pollRunnable);
        binding = null;
    }
}
