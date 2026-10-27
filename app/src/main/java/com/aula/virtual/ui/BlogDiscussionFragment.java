package com.aula.virtual.ui;

import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.aula.virtual.R;
import com.aula.virtual.data.CommentInfo;
import com.aula.virtual.data.entity.BlogComment;
import com.aula.virtual.data.entity.User;
import com.aula.virtual.databinding.FragmentBlogDiscussionBinding;

import android.text.Editable;
import android.text.TextWatcher;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class BlogDiscussionFragment extends Fragment {
    private FragmentBlogDiscussionBinding binding;
    private MainViewModel viewModel;
    private BlogCommentAdapter adapter;
    private int blogEntryId;
    private List<CommentInfo> rawComments = new ArrayList<>();
    private String currentSearchQuery = "";
    
    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private static final int POLL_INTERVAL = 4000; // Poll every 4 seconds

    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            if (viewModel != null && isResumed()) {
                viewModel.fetchBlogComments(blogEntryId);
                pollHandler.postDelayed(this, POLL_INTERVAL);
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentBlogDiscussionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        
        blogEntryId = getArguments().getInt("blogEntryId");
        String entryTitle = getArguments().getString("blogTitle");
        int subjectId = getArguments().getInt("subjectId");
        binding.tvEntryTitle.setText(entryTitle);

        User currentUser = viewModel.getCurrentUser().getValue();
        int currentUserId = currentUser != null ? currentUser.id : -1;
        boolean isAdmin = currentUser != null && "ADMIN".equals(currentUser.role);

        adapter = new BlogCommentAdapter(currentUserId, isAdmin);
        binding.rvComments.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvComments.setAdapter(adapter);

        ViewCompat.setOnApplyWindowInsetsListener(binding.layoutInput, (v, insets) -> {
            Insets imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime());
            Insets systemBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            
            int bottomPadding = Math.max(imeInsets.bottom, systemBarInsets.bottom);
            int defaultPadding = (int) (8 * getResources().getDisplayMetrics().density);

            v.setPadding(
                    v.getPaddingLeft(),
                    v.getPaddingTop(),
                    v.getPaddingRight(),
                    defaultPadding + bottomPadding
            );
            return insets;
        });

        if (binding.btnBackDiscussion != null) {
            binding.btnBackDiscussion.setOnClickListener(v -> Navigation.findNavController(view).navigateUp());
        }

        if (binding.btnChatMenu != null) {
            binding.btnChatMenu.setOnClickListener(v -> {
                android.widget.PopupMenu popup = new android.widget.PopupMenu(requireContext(), v);
                popup.getMenu().add(0, 1, 0, "🔍 Buscar mensaje");
                popup.getMenu().add(0, 2, 1, "🔕 Silenciar Notificaciones");
                popup.getMenu().add(0, 3, 2, "🗑️ Vaciar chat");
                popup.setOnMenuItemClickListener(item -> {
                    int id = item.getItemId();
                    if (id == 1) {
                        if (binding.layoutSearchContainer != null) {
                            boolean isVis = binding.layoutSearchContainer.getVisibility() == View.VISIBLE;
                            binding.layoutSearchContainer.setVisibility(isVis ? View.GONE : View.VISIBLE);
                            if (!isVis) {
                                binding.etSearchChat.requestFocus();
                            } else {
                                currentSearchQuery = "";
                                binding.etSearchChat.setText("");
                                applyChatSearch();
                            }
                        }
                        return true;
                    } else if (id == 2) {
                        Toast.makeText(getContext(), "Notificaciones del chat silenciadas", Toast.LENGTH_SHORT).show();
                        return true;
                    } else if (id == 3) {
                        showClearChatConfirmation();
                        return true;
                    }
                    return false;
                });
                popup.show();
            });
        }



        if (binding.btnCloseSearch != null) {
            binding.btnCloseSearch.setOnClickListener(v -> {
                if (binding.layoutSearchContainer != null) {
                    binding.layoutSearchContainer.setVisibility(View.GONE);
                    currentSearchQuery = "";
                    binding.etSearchChat.setText("");
                    applyChatSearch();
                }
            });
        }

        if (binding.etSearchChat != null) {
            binding.etSearchChat.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    currentSearchQuery = s != null ? s.toString().toLowerCase().trim() : "";
                    applyChatSearch();
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        adapter.setOnCommentLongClickListener(comment -> showMessageOptionsDialog(comment, currentUserId, isAdmin));

        viewModel.getModificationError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                viewModel.clearModificationError();
            }
        });

        viewModel.getBlogComments().observe(getViewLifecycleOwner(), comments -> {
            if (comments != null) {
                rawComments = new ArrayList<>(comments);
                applyChatSearch();
            }
        });

        // Start polling
        pollHandler.post(pollRunnable);

        binding.btnSend.setOnClickListener(v -> {
            String content = binding.etComment.getText().toString().trim();
            if (!content.isEmpty()) {
                User user = viewModel.getCurrentUser().getValue();
                if (user != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault());
                    String timestamp = sdf.format(new Date());
                    BlogComment newComment = new BlogComment(blogEntryId, user.id, content);
                    newComment.timestamp = timestamp;
                    viewModel.performOnlineAction(() -> {
                        viewModel.insertBlogComment(newComment);
                        binding.etComment.setText("");
                    });
                }
            }
        });
    }

    private void showMessageOptionsDialog(BlogComment comment, int currentUserId, boolean isAdmin) {
        boolean canDelete = (comment.userId == currentUserId || isAdmin);
        CharSequence[] options = canDelete ? new CharSequence[]{"Copiar mensaje", "Eliminar"} : new CharSequence[]{"Copiar mensaje"};

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Opciones de mensaje")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                        android.content.ClipData clip = android.content.ClipData.newPlainText("Mensaje del foro", comment.content);
                        if (clipboard != null) {
                            clipboard.setPrimaryClip(clip);
                            Toast.makeText(getContext(), "Mensaje copiado al portapapeles", Toast.LENGTH_SHORT).show();
                        }
                    } else if (which == 1 && canDelete) {
                        viewModel.performOnlineAction(() -> viewModel.deleteBlogComment(comment));
                        Toast.makeText(getContext(), "Mensaje eliminado", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    private void showClearChatConfirmation() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Vaciar conversación")
                .setMessage("¿Deseas eliminar TODOS los mensajes de esta discusión? Esta acción no se puede deshacer.")
                .setPositiveButton("Vaciar todo", (dialog, which) -> {
                    viewModel.performOnlineAction(() -> viewModel.clearBlogDiscussion(blogEntryId));
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void applyChatSearch() {
        if (binding == null) return;
        List<CommentInfo> filtered;
        if (currentSearchQuery.isEmpty()) {
            filtered = rawComments;
        } else {
            filtered = rawComments.stream().filter(info -> {
                if (info == null || info.comment == null) return false;
                String content = info.comment.content != null ? info.comment.content.toLowerCase() : "";
                String author = info.userName != null ? info.userName.toLowerCase() : "";
                return content.contains(currentSearchQuery) || author.contains(currentSearchQuery);
            }).collect(Collectors.toList());
        }

        int oldSize = adapter.getItemCount();
        adapter.setComments(filtered);
        int newSize = adapter.getItemCount();
        if (newSize > oldSize && newSize > 0 && currentSearchQuery.isEmpty()) {
            binding.rvComments.scrollToPosition(newSize - 1);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        pollHandler.removeCallbacks(pollRunnable);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (blogEntryId != 0) {
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
