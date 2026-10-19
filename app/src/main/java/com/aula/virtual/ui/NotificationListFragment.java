package com.aula.virtual.ui;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.aula.virtual.MainActivity;
import com.aula.virtual.R;
import com.aula.virtual.data.ScheduleInfo;
import com.aula.virtual.data.StudentGradeInfo;
import com.aula.virtual.data.entity.Facility;
import com.aula.virtual.data.entity.Notification;
import com.aula.virtual.data.entity.Subject;
import com.aula.virtual.data.entity.User;
import com.aula.virtual.databinding.FragmentNotificationListBinding;
import com.aula.virtual.service.VirtualFirebaseMessagingService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NotificationListFragment extends Fragment {
    private FragmentNotificationListBinding binding;
    private MainViewModel viewModel;
    private User currentUser;
    private List<Notification> allUserNotifications = new ArrayList<>();
    private List<Notification> rawServerNotifications = new ArrayList<>();
    private final List<String> userSubjectNames = new ArrayList<>();
    private final List<String> userFacilityNames = new ArrayList<>();
    private final Map<Integer, String> facilityMap = new HashMap<>();
    private final String[] selectedChannelFilter = {"ALL"};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentNotificationListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        currentUser = viewModel.getCurrentUser().getValue();

        if (currentUser == null) {
            binding.cardLoginNotice.setVisibility(View.VISIBLE);
            binding.cardDirectMessagingContainer.setVisibility(View.GONE);
            binding.chipsScrollView.setVisibility(View.GONE);
            binding.etSearchNotification.setVisibility(View.GONE);

            binding.btnGoLogin.setOnClickListener(v -> 
                Navigation.findNavController(view).navigate(R.id.action_global_loginFragment));
            return;
        }

        binding.cardLoginNotice.setVisibility(View.GONE);

        binding.btnBackNotifications.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToHome();
            } else {
                Navigation.findNavController(v).navigateUp();
            }
        });

        binding.btnConfigurePush.setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_notificationListFragment_to_notificationSettingsFragment));

        boolean isAdmin = "ADMIN".equals(currentUser.role);
        binding.btnCreateNotification.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
        binding.cardDirectMessagingContainer.setVisibility(isAdmin ? View.VISIBLE : View.GONE);

        binding.btnCreateNotification.setOnClickListener(v -> showCreateNotificationDialog(null, "ALL"));
        binding.btnComposeEncargados.setOnClickListener(v -> showCreateNotificationDialog(null, "ROLE_GUARDIANS"));
        binding.btnComposeProfessors.setOnClickListener(v -> showCreateNotificationDialog(null, "ROLE_PROFESSORS"));
        binding.btnComposeAdmins.setOnClickListener(v -> showCreateNotificationDialog(null, "ROLE_ADMINS"));
        binding.btnViewAllChannels.setOnClickListener(v -> {
            selectedChannelFilter[0] = "ALL";
            renderNotificationsAndChips();
        });

        setupSearch();

        viewModel.getAllFacilities().observe(getViewLifecycleOwner(), facilities -> {
            if (facilities != null) {
                for (Facility f : facilities) {
                    facilityMap.put(f.id, f.name);
                }
                reFilterAndRender();
            }
        });

        if ("STUDENT".equals(currentUser.role)) {
            viewModel.getStudentSchedules(currentUser.id).observe(getViewLifecycleOwner(), schedules -> {
                if (schedules != null) {
                    for (ScheduleInfo info : schedules) {
                        if (info.subjectName != null && !userSubjectNames.contains(info.subjectName)) {
                            userSubjectNames.add(info.subjectName);
                        }
                        if (info.schedule != null) {
                            String facName = facilityMap.get(info.schedule.facilityId);
                            if (facName != null && !userFacilityNames.contains(facName)) {
                                userFacilityNames.add(facName);
                            }
                        }
                    }
                    reFilterAndRender();
                }
            });

            viewModel.getGradeInfoForStudent(currentUser.id).observe(getViewLifecycleOwner(), gradeInfos -> {
                if (gradeInfos != null) {
                    for (StudentGradeInfo info : gradeInfos) {
                        if (info.subject != null && info.subject.name != null && !userSubjectNames.contains(info.subject.name)) {
                            userSubjectNames.add(info.subject.name);
                        }
                    }
                    reFilterAndRender();
                }
            });
        } else if ("PROFESSOR".equals(currentUser.role)) {
            viewModel.getProfessorSchedules(currentUser.id).observe(getViewLifecycleOwner(), schedules -> {
                if (schedules != null) {
                    for (ScheduleInfo info : schedules) {
                        if (info.subjectName != null && !userSubjectNames.contains(info.subjectName)) {
                            userSubjectNames.add(info.subjectName);
                        }
                        if (info.schedule != null) {
                            String facName = facilityMap.get(info.schedule.facilityId);
                            if (facName != null && !userFacilityNames.contains(facName)) {
                                userFacilityNames.add(facName);
                            }
                        }
                    }
                    reFilterAndRender();
                }
            });

            viewModel.getSubjectsByProfessor(currentUser.id).observe(getViewLifecycleOwner(), subjects -> {
                if (subjects != null) {
                    for (Subject sub : subjects) {
                        if (sub.name != null && !userSubjectNames.contains(sub.name)) {
                            userSubjectNames.add(sub.name);
                        }
                    }
                    reFilterAndRender();
                }
            });
        }

        viewModel.getAllNotifications().observe(getViewLifecycleOwner(), notifications -> {
            if (notifications != null) {
                rawServerNotifications = notifications;
                reFilterAndRender();
            }
        });

        viewModel.fetchNotifications();
    }

    private void setupSearch() {
        binding.etSearchNotification.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                renderNotificationsAndChips();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        reFilterAndRender();
    }

    private void reFilterAndRender() {
        if (currentUser == null) return;
        if (binding != null && getContext() != null) {
            boolean isPushEnabled = NotificationPrefs.isPushEnabled(requireContext());
            binding.cardPushDisabledNotice.setVisibility(isPushEnabled ? View.GONE : View.VISIBLE);
        }

        allUserNotifications.clear();

        for (Notification n : rawServerNotifications) {
            if (n == null) continue;
            if (isNotificationForUser(n)) {
                allUserNotifications.add(n);
            }
        }
        renderNotificationsAndChips();
    }

    private boolean isNotificationForUser(Notification n) {
        if (currentUser == null) return false;

        // Si la notificación fue creada por el usuario actual, permitir siempre que se cargue para él (comprobación de efectividad)
        if (n.senderName != null && currentUser.name != null && n.senderName.trim().equalsIgnoreCase(currentUser.name.trim())) {
            return true;
        }

        // Check priority alert exception
        boolean isPriorityAlert = n.iconType != null && 
                ("ALERT".equalsIgnoreCase(n.iconType) || "SYSTEM".equalsIgnoreCase(n.iconType)) &&
                getContext() != null && NotificationPrefs.isPriorityAlertsEnabled(requireContext());

        // Check channel preferences unless it's a priority alert
        if (!isPriorityAlert && getContext() != null) {
            String targetType = n.targetType != null ? n.targetType.toUpperCase() : "ALL";
            if ("ALL".equals(targetType) && !NotificationPrefs.isChannelGeneralEnabled(requireContext())) {
                return false;
            }
            if ("SUBJECT".equals(targetType) && !NotificationPrefs.isChannelSubjectsEnabled(requireContext())) {
                return false;
            }
            if ("FACILITY".equals(targetType) && !NotificationPrefs.isChannelFacilitiesEnabled(requireContext())) {
                return false;
            }
            if (targetType.startsWith("ROLE_") && !NotificationPrefs.isChannelInstitutionalEnabled(requireContext())) {
                return false;
            }
        }

        if ("ADMIN".equals(currentUser.role)) return true;
        if (n.targetType == null || "ALL".equalsIgnoreCase(n.targetType)) return true;

        String type = n.targetType.toUpperCase();
        if ("ROLE_STUDENTS".equals(type)) {
            if ("STUDENT".equals(currentUser.role) || "ENCARGADO".equals(currentUser.role) || "GUARDIAN".equals(currentUser.role)) return true;
        }
        if ("ROLE_PROFESSORS".equals(type) && "PROFESSOR".equals(currentUser.role)) return true;
        if ("ROLE_GUARDIANS".equals(type) || "ROLE_ENCARGADOS".equals(type)) {
            if ("ENCARGADO".equals(currentUser.role) || "GUARDIAN".equals(currentUser.role)) return true;
        }
        if ("ROLE_ADMINS".equals(type) && "ADMIN".equals(currentUser.role)) return true;
        if ("SUPPORT".equals(type) && "ADMIN".equals(currentUser.role)) return true;

        if ("SUBJECT".equals(type) && n.targetValue != null) {
            return userSubjectNames.contains(n.targetValue);
        }
        if ("FACILITY".equals(type) && n.targetValue != null) {
            return userFacilityNames.contains(n.targetValue);
        }
        return false;
    }

    private void renderNotificationsAndChips() {
        if (binding == null) return;

        String query = binding.etSearchNotification.getText().toString().toLowerCase().trim();

        List<Notification> queryFiltered = new ArrayList<>();
        for (Notification n : allUserNotifications) {
            if (n == null) continue;
            if (query.isEmpty()) {
                queryFiltered.add(n);
            } else {
                String title = n.title != null ? n.title.toLowerCase() : "";
                String msg = n.message != null ? n.message.toLowerCase() : "";
                String target = getNotificationTargetLabel(n).toLowerCase();
                if (title.contains(query) || msg.contains(query) || target.contains(query)) {
                    queryFiltered.add(n);
                }
            }
        }

        List<Notification> generalNotifs = new ArrayList<>();
        List<Notification> supportNotifs = new ArrayList<>();
        List<Notification> studentNotifs = new ArrayList<>();
        List<Notification> professorNotifs = new ArrayList<>();
        List<Notification> adminNotifs = new ArrayList<>();
        List<Notification> subjectNotifs = new ArrayList<>();
        List<Notification> facilityNotifs = new ArrayList<>();

        for (Notification n : queryFiltered) {
            if (n == null || n.targetType == null) {
                generalNotifs.add(n);
                continue;
            }
            String type = n.targetType.toUpperCase();
            if ("SUPPORT".equals(type)) {
                supportNotifs.add(n);
            } else if ("ROLE_STUDENTS".equals(type)) {
                studentNotifs.add(n);
            } else if ("ROLE_PROFESSORS".equals(type)) {
                professorNotifs.add(n);
            } else if ("ROLE_ADMINS".equals(type)) {
                adminNotifs.add(n);
            } else if ("SUBJECT".equals(type)) {
                subjectNotifs.add(n);
            } else if ("FACILITY".equals(type)) {
                facilityNotifs.add(n);
            } else {
                generalNotifs.add(n);
            }
        }

        if (binding != null && binding.tvRecordCount != null) {
            int total = queryFiltered.size();
            binding.tvRecordCount.setText("Avisos publicados (" + total + (total == 1 ? " registro)" : " registros)"));
        }

        int activeThemeColor = ThemeHelper.getSubjectColor(requireContext(), ThemeHelper.getAccentColorName(requireContext()));
        renderChannelChips(generalNotifs, supportNotifs, studentNotifs, professorNotifs, adminNotifs, subjectNotifs, facilityNotifs, activeThemeColor);

        binding.notificationsContainer.removeAllViews();
        boolean isDark = ThemeHelper.isDarkMode(requireContext());
        TypedValue typedValue = new TypedValue();
        requireContext().getTheme().resolveAttribute(android.R.attr.textColorPrimary, typedValue, true);
        int primaryTextColor = typedValue.data;
        requireContext().getTheme().resolveAttribute(android.R.attr.textColorSecondary, typedValue, true);
        int secondaryTextColor = typedValue.data;

        String filter = selectedChannelFilter[0];
        boolean hasContent = false;

        if ("ALL".equals(filter) || "GENERAL".equals(filter)) {
            if (!generalNotifs.isEmpty()) {
                renderNotificationCards(binding.notificationsContainer, generalNotifs, primaryTextColor, secondaryTextColor, isDark);
                hasContent = true;
            }
        }
        if ("ALL".equals(filter) || "SUPPORT".equals(filter)) {
            if (!supportNotifs.isEmpty()) {
                renderNotificationCards(binding.notificationsContainer, supportNotifs, primaryTextColor, secondaryTextColor, isDark);
                hasContent = true;
            }
        }
        if ("ALL".equals(filter) || "ROLE_STUDENTS".equals(filter)) {
            if (!studentNotifs.isEmpty()) {
                renderNotificationCards(binding.notificationsContainer, studentNotifs, primaryTextColor, secondaryTextColor, isDark);
                hasContent = true;
            }
        }
        if ("ALL".equals(filter) || "ROLE_PROFESSORS".equals(filter)) {
            if (!professorNotifs.isEmpty()) {
                renderNotificationCards(binding.notificationsContainer, professorNotifs, primaryTextColor, secondaryTextColor, isDark);
                hasContent = true;
            }
        }
        if ("ALL".equals(filter) || "ROLE_ADMINS".equals(filter)) {
            if (!adminNotifs.isEmpty()) {
                renderNotificationCards(binding.notificationsContainer, adminNotifs, primaryTextColor, secondaryTextColor, isDark);
                hasContent = true;
            }
        }
        if ("ALL".equals(filter) || "SUBJECT".equals(filter)) {
            if (!subjectNotifs.isEmpty()) {
                renderNotificationCards(binding.notificationsContainer, subjectNotifs, primaryTextColor, secondaryTextColor, isDark);
                hasContent = true;
            }
        }
        if ("ALL".equals(filter) || "FACILITY".equals(filter)) {
            if (!facilityNotifs.isEmpty()) {
                renderNotificationCards(binding.notificationsContainer, facilityNotifs, primaryTextColor, secondaryTextColor, isDark);
                hasContent = true;
            }
        }

        if (!hasContent) {
            TextView tvEmpty = new TextView(requireContext());
            tvEmpty.setText("\nNo hay avisos o notificaciones para esta categoría.\n");
            tvEmpty.setGravity(Gravity.CENTER);
            tvEmpty.setTextColor(0xFF757575);
            binding.notificationsContainer.addView(tvEmpty);
        }
    }

    private void renderChannelChips(List<Notification> gen, List<Notification> sup, List<Notification> stu, List<Notification> prof, List<Notification> adm, List<Notification> sub, List<Notification> fcl, int activeColor) {
        binding.chipsLayout.removeAllViews();
        List<MaterialButton> chipButtons = new ArrayList<>();

        addChipButton(binding.chipsLayout, chipButtons, "📢 Canal General", "GENERAL", activeColor);
        addChipButton(binding.chipsLayout, chipButtons, "👨‍🎓 Estudiantes", "ROLE_STUDENTS", activeColor);
        addChipButton(binding.chipsLayout, chipButtons, "👨‍🏫 Profesores", "ROLE_PROFESSORS", activeColor);
        addChipButton(binding.chipsLayout, chipButtons, "⚙️ Administración", "ROLE_ADMINS", activeColor);
        addChipButton(binding.chipsLayout, chipButtons, "📚 Por Materias", "SUBJECT", activeColor);
    }

    private void addChipButton(LinearLayout layout, List<MaterialButton> allChips, String label, String channelKey, int activeColor) {
        MaterialButton chip = new MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        chip.setText(label);
        chip.setTextSize(12);
        chip.setAllCaps(false);
        chip.setCornerRadius(24);
        chip.setInsetTop(0);
        chip.setInsetBottom(0);

        boolean isSelected = channelKey.equals(selectedChannelFilter[0]);
        updateChipStyle(chip, isSelected, activeColor);

        chip.setOnClickListener(v -> {
            selectedChannelFilter[0] = channelKey;
            for (MaterialButton btn : allChips) {
                boolean active = channelKey.equals(btn.getTag());
                updateChipStyle(btn, active, activeColor);
            }
            renderNotificationsAndChips();
        });

        chip.setTag(channelKey);
        allChips.add(chip);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 10, 0);
        chip.setLayoutParams(lp);

        layout.addView(chip);
    }

    private void updateChipStyle(MaterialButton chip, boolean isSelected, int activeColor) {
        if (isSelected) {
            chip.setBackgroundColor(0xFF2563EB);
            chip.setTextColor(0xFFFFFFFF);
            chip.setStrokeWidth(0);
        } else {
            chip.setBackgroundColor(0xFF131B2E);
            chip.setTextColor(0xFF8E9BB0);
            chip.setStrokeColor(ColorStateList.valueOf(0xFF1E293B));
            chip.setStrokeWidth((int) (1f * getResources().getDisplayMetrics().density));
        }
    }

    private void addChannelHeaderView(LinearLayout container, String channelName, int accentColor) {
        TextView header = new TextView(requireContext());
        header.setText(channelName);
        header.setTextSize(13);
        header.setTypeface(null, Typeface.BOLD);
        header.setTextColor(0xFFFFFFFF);
        header.setPadding(24, 10, 24, 10);
        
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(accentColor);
        gd.setCornerRadius(16f);
        header.setBackground(gd);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 16, 0, 8);
        header.setLayoutParams(lp);

        container.addView(header);
    }

    private void renderNotificationCards(LinearLayout container, List<Notification> notifs, int primaryTextColor, int secondaryTextColor, boolean isDark) {
        for (Notification n : notifs) {
            MaterialCardView card = new MaterialCardView(requireContext());
            card.setCardElevation(0f);
            card.setRadius(24f);
            card.setStrokeWidth(1);
            card.setStrokeColor(ColorStateList.valueOf(0xFF1E293B));
            card.setCardBackgroundColor(ColorStateList.valueOf(0xFF121929));
            
            LinearLayout cardLayout = new LinearLayout(requireContext());
            cardLayout.setOrientation(LinearLayout.VERTICAL);
            cardLayout.setPadding(24, 24, 24, 24);

            LinearLayout topRow = new LinearLayout(requireContext());
            topRow.setOrientation(LinearLayout.HORIZONTAL);
            topRow.setGravity(Gravity.TOP);

            // Icon Frame Capsule
            LinearLayout iconCapsule = new LinearLayout(requireContext());
            LinearLayout.LayoutParams capsuleLp = new LinearLayout.LayoutParams(
                    (int) (48 * getResources().getDisplayMetrics().density),
                    (int) (48 * getResources().getDisplayMetrics().density));
            capsuleLp.setMargins(0, 0, 16, 0);
            iconCapsule.setLayoutParams(capsuleLp);
            iconCapsule.setGravity(Gravity.CENTER);

            GradientDrawable capsuleBg = new GradientDrawable();
            capsuleBg.setCornerRadius(24f);

            ImageView ivIcon = new ImageView(requireContext());
            LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(
                    (int) (24 * getResources().getDisplayMetrics().density),
                    (int) (24 * getResources().getDisplayMetrics().density));
            ivIcon.setLayoutParams(iconLp);

            String iconKind = n.iconType != null ? n.iconType.toUpperCase() : "GENERAL";
            if ("ACADEMIC".equals(iconKind)) {
                capsuleBg.setColor(0xFF291F15);
                capsuleBg.setStroke((int) (1f * getResources().getDisplayMetrics().density), 0x66D97706);
                ivIcon.setImageResource(R.drawable.ic_person);
                ivIcon.setImageTintList(ColorStateList.valueOf(0xFFF59E0B));
            } else if ("ALERT".equals(iconKind)) {
                capsuleBg.setColor(0xFF0E242D);
                capsuleBg.setStroke((int) (1f * getResources().getDisplayMetrics().density), 0x6606B6D4);
                ivIcon.setImageResource(R.drawable.ic_notification);
                ivIcon.setImageTintList(ColorStateList.valueOf(0xFF06B6D4));
            } else if ("SYSTEM".equals(iconKind)) {
                capsuleBg.setColor(0xFF1E1B2E);
                capsuleBg.setStroke((int) (1f * getResources().getDisplayMetrics().density), 0x668B5CF6);
                ivIcon.setImageResource(R.drawable.ic_settings);
                ivIcon.setImageTintList(ColorStateList.valueOf(0xFF8B5CF6));
            } else if ("SUBJECT".equals(iconKind)) {
                capsuleBg.setColor(0xFF0D281E);
                capsuleBg.setStroke((int) (1f * getResources().getDisplayMetrics().density), 0x6610B981);
                ivIcon.setImageResource(R.drawable.ic_open_book);
                ivIcon.setImageTintList(ColorStateList.valueOf(0xFF10B981));
            } else {
                capsuleBg.setColor(0xFF131D33);
                capsuleBg.setStroke((int) (1f * getResources().getDisplayMetrics().density), 0x6638BDF8);
                ivIcon.setImageResource(R.drawable.ic_megaphone);
                ivIcon.setImageTintList(ColorStateList.valueOf(0xFF38BDF8));
            }
            iconCapsule.setBackground(capsuleBg);
            iconCapsule.addView(ivIcon);
            topRow.addView(iconCapsule);

            // Details Column
            LinearLayout contentCol = new LinearLayout(requireContext());
            contentCol.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams contentLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            contentCol.setLayoutParams(contentLp);

            TextView tvTag = new TextView(requireContext());
            tvTag.setTextSize(11);
            tvTag.setTypeface(null, Typeface.BOLD);
            tvTag.setSingleLine(true);

            String targetLabel = getNotificationTargetLabel(n);
            tvTag.setText(targetLabel);

            GradientDrawable tagBg = new GradientDrawable();
            tagBg.setCornerRadius(100f);

            int tagColor = 0xFF38BDF8;
            int tagBgColor = 0xFF131D33;
            int tagBorderColor = 0x8038BDF8;

            String type = n.targetType != null ? n.targetType.toUpperCase() : "ALL";
            if ("ROLE_PROFESSORS".equals(type)) {
                tagColor = 0xFFF59E0B;
                tagBgColor = 0xFF291F15;
                tagBorderColor = 0x80D97706;
            } else if ("ROLE_STUDENTS".equals(type)) {
                tagColor = 0xFF06B6D4;
                tagBgColor = 0xFF0E242D;
                tagBorderColor = 0x8006B6D4;
            } else if ("ROLE_ADMINS".equals(type)) {
                tagColor = 0xFF8B5CF6;
                tagBgColor = 0xFF1E1B2E;
                tagBorderColor = 0x808B5CF6;
            } else if ("SUBJECT".equals(type)) {
                tagColor = 0xFF10B981;
                tagBgColor = 0xFF0D281E;
                tagBorderColor = 0x8010B981;
            }

            tagBg.setColor(tagBgColor);
            tagBg.setStroke((int) (1f * getResources().getDisplayMetrics().density), tagBorderColor);
            tvTag.setBackground(tagBg);
            tvTag.setTextColor(tagColor);
            tvTag.setPadding((int) (10 * getResources().getDisplayMetrics().density),
                             (int) (3 * getResources().getDisplayMetrics().density),
                             (int) (10 * getResources().getDisplayMetrics().density),
                             (int) (3 * getResources().getDisplayMetrics().density));

            LinearLayout.LayoutParams tagLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            tagLp.setMargins(0, 0, 0, (int) (6 * getResources().getDisplayMetrics().density));
            tvTag.setLayoutParams(tagLp);

            TextView tvTitle = new TextView(requireContext());
            tvTitle.setText(n.title != null ? n.title : "Notificación");
            tvTitle.setTextSize(15);
            tvTitle.setTypeface(null, Typeface.BOLD);
            tvTitle.setTextColor(0xFFFFFFFF);

            TextView tvMsg = new TextView(requireContext());
            tvMsg.setText(n.message != null ? n.message : "");
            tvMsg.setTextSize(13);
            tvMsg.setTextColor(0xFF8E9BB0);
            tvMsg.setPadding(0, 4, 0, 0);

            contentCol.addView(tvTag);
            contentCol.addView(tvTitle);
            contentCol.addView(tvMsg);

            topRow.addView(contentCol);
            cardLayout.addView(topRow);

            // Bottom Divider & Metadata Row
            View divider = new View(requireContext());
            LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, (int) (1 * getResources().getDisplayMetrics().density));
            divLp.setMargins(0, 16, 0, 12);
            divider.setLayoutParams(divLp);
            divider.setBackgroundColor(0xFF1E293B);
            cardLayout.addView(divider);

            LinearLayout bottomRow = new LinearLayout(requireContext());
            bottomRow.setOrientation(LinearLayout.HORIZONTAL);
            bottomRow.setGravity(Gravity.CENTER_VERTICAL);

            TextView tvSender = new TextView(requireContext());
            LinearLayout.LayoutParams senderLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            tvSender.setLayoutParams(senderLp);
            String senderStr = (n.senderName != null && !n.senderName.isEmpty() ? n.senderName + " • " : "") + 
                               (n.timestamp != null ? n.timestamp : "");
            tvSender.setText(senderStr);
            tvSender.setTextSize(12);
            tvSender.setTextColor(0xFF64748B);
            bottomRow.addView(tvSender);

            if (currentUser != null && "ADMIN".equals(currentUser.role)) {
                LinearLayout actionLayout = new LinearLayout(requireContext());
                actionLayout.setOrientation(LinearLayout.HORIZONTAL);
                actionLayout.setGravity(Gravity.END);

                boolean isSupport = n.targetType != null && "SUPPORT".equalsIgnoreCase(n.targetType.trim());

                if (isSupport) {
                    MaterialButton btnProfile = new MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
                    btnProfile.setText("👤 Ver Perfil");
                    btnProfile.setTextSize(12);
                    btnProfile.setAllCaps(false);
                    btnProfile.setTextColor(0xFF38BDF8);
                    btnProfile.setCornerRadius((int) (10 * getResources().getDisplayMetrics().density));
                    btnProfile.setStrokeColor(ColorStateList.valueOf(0xFF1E293B));
                    btnProfile.setStrokeWidth((int) (1 * getResources().getDisplayMetrics().density));
                    btnProfile.setBackgroundTintList(ColorStateList.valueOf(0xFF131B2E));
                    btnProfile.setInsetTop(0);
                    btnProfile.setInsetBottom(0);
                    btnProfile.setOnClickListener(v -> navigateToUserProfile(n.senderName));
                    actionLayout.addView(btnProfile);
                } else {
                    MaterialButton btnEdit = new MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
                    btnEdit.setText("Editar");
                    btnEdit.setTextSize(12);
                    btnEdit.setTextColor(0xFF8E9BB0);
                    btnEdit.setAllCaps(false);
                    btnEdit.setIconResource(R.drawable.ic_edit);
                    btnEdit.setIconTint(ColorStateList.valueOf(0xFF8E9BB0));
                    btnEdit.setIconSize((int) (15 * getResources().getDisplayMetrics().density));
                    btnEdit.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
                    btnEdit.setIconPadding((int) (6 * getResources().getDisplayMetrics().density));
                    btnEdit.setCornerRadius((int) (10 * getResources().getDisplayMetrics().density));
                    btnEdit.setStrokeColor(ColorStateList.valueOf(0xFF1E293B));
                    btnEdit.setStrokeWidth((int) (1 * getResources().getDisplayMetrics().density));
                    btnEdit.setBackgroundTintList(ColorStateList.valueOf(0xFF131B2E));
                    btnEdit.setInsetTop(0);
                    btnEdit.setInsetBottom(0);
                    btnEdit.setPadding((int) (12 * getResources().getDisplayMetrics().density), 0, (int) (12 * getResources().getDisplayMetrics().density), 0);

                    LinearLayout.LayoutParams editLp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            (int) (36 * getResources().getDisplayMetrics().density));
                    editLp.setMargins(0, 0, 8, 0);
                    btnEdit.setLayoutParams(editLp);
                    btnEdit.setOnClickListener(v -> showCreateNotificationDialog(n, "ALL"));
                    actionLayout.addView(btnEdit);
                }

                MaterialButton btnDelete = new MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
                btnDelete.setText("");
                btnDelete.setIconResource(R.drawable.ic_delete);
                btnDelete.setIconTint(ColorStateList.valueOf(0xFFFFB4AB));
                btnDelete.setIconSize((int) (18 * getResources().getDisplayMetrics().density));
                btnDelete.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
                btnDelete.setIconPadding(0);
                btnDelete.setCornerRadius((int) (10 * getResources().getDisplayMetrics().density));
                btnDelete.setStrokeColor(ColorStateList.valueOf(0xFF1E293B));
                btnDelete.setStrokeWidth((int) (1 * getResources().getDisplayMetrics().density));
                btnDelete.setBackgroundTintList(ColorStateList.valueOf(0xFF131B2E));
                btnDelete.setInsetTop(0);
                btnDelete.setInsetBottom(0);
                btnDelete.setPadding(0, 0, 0, 0);

                LinearLayout.LayoutParams deleteLp = new LinearLayout.LayoutParams(
                        (int) (36 * getResources().getDisplayMetrics().density),
                        (int) (36 * getResources().getDisplayMetrics().density));
                btnDelete.setLayoutParams(deleteLp);
                btnDelete.setOnClickListener(v -> showDeleteNotificationConfirmation(n));

                actionLayout.addView(btnDelete);
                bottomRow.addView(actionLayout);
            }

            cardLayout.addView(bottomRow);
            card.addView(cardLayout);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, 16);
            card.setLayoutParams(lp);

            container.addView(card);
        }
    }

    private void navigateToUserProfile(String senderName) {
        if (senderName == null || senderName.trim().isEmpty() || getView() == null) return;

        String extractedCarnet = "";
        int start = senderName.lastIndexOf('(');
        int end = senderName.lastIndexOf(')');
        if (start != -1 && end != -1 && end > start) {
            extractedCarnet = senderName.substring(start + 1, end).trim();
        } else {
            extractedCarnet = senderName.trim();
        }

        final String searchCarnet = extractedCarnet;
        final String searchName = senderName.trim().toLowerCase();

        boolean[] found = {false};

        List<User> students = viewModel.getAllStudents().getValue();
        if (students != null) {
            for (User s : students) {
                if (s != null && ((s.carnet != null && s.carnet.equalsIgnoreCase(searchCarnet)) || (s.name != null && searchName.contains(s.name.toLowerCase())))) {
                    Bundle args = new Bundle();
                    args.putInt("studentId", s.id);
                    Navigation.findNavController(requireView()).navigate(R.id.action_notificationListFragment_to_studentProfileFragment, args);
                    found[0] = true;
                    return;
                }
            }
        }

        List<User> profs = viewModel.getAllProfessors().getValue();
        if (!found[0] && profs != null) {
            for (User p : profs) {
                if (p != null && ((p.carnet != null && p.carnet.equalsIgnoreCase(searchCarnet)) || (p.name != null && searchName.contains(p.name.toLowerCase())))) {
                    Bundle args = new Bundle();
                    args.putInt("studentId", p.id);
                    Navigation.findNavController(requireView()).navigate(R.id.action_notificationListFragment_to_studentProfileFragment, args);
                    found[0] = true;
                    return;
                }
            }
        }

        List<User> admins = viewModel.getAllAdmins().getValue();
        if (!found[0] && admins != null) {
            for (User a : admins) {
                if (a != null && ((a.carnet != null && a.carnet.equalsIgnoreCase(searchCarnet)) || (a.name != null && searchName.contains(a.name.toLowerCase())))) {
                    Bundle args = new Bundle();
                    args.putInt("adminId", a.id);
                    Navigation.findNavController(requireView()).navigate(R.id.adminDetailFragment, args);
                    found[0] = true;
                    return;
                }
            }
        }

        if (!found[0]) {
            Toast.makeText(getContext(), "Perfil del emisor no encontrado (" + searchCarnet + ")", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDeleteNotificationConfirmation(Notification n) {
        if (n == null) return;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Eliminar Notificación")
                .setMessage("¿Estás seguro de que deseas eliminar esta notificación?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    viewModel.deleteNotification(n.id, () -> {
                        Toast.makeText(getContext(), "Notificación eliminada", Toast.LENGTH_SHORT).show();
                        viewModel.fetchNotifications();
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private <T> void observeOnce(LiveData<T> liveData, Observer<T> observer) {
        liveData.observe(getViewLifecycleOwner(), new Observer<T>() {
            @Override
            public void onChanged(T t) {
                if (t != null) {
                    liveData.removeObserver(this);
                    observer.onChanged(t);
                }
            }
        });
    }

    private void showCreateNotificationDialog(Notification existingNotif, String initialTargetType) {
        if (currentUser == null) return;

        boolean isEditing = existingNotif != null;
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_notification, null);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext(), R.style.Theme_MyApplication_Dialog);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvDialogTitle = dialogView.findViewById(R.id.tvDialogTitle);
        if (isEditing) tvDialogTitle.setText("Editar Notificación");

        View btnClose = dialogView.findViewById(R.id.btnCloseDialog);
        btnClose.setOnClickListener(v -> dialog.dismiss());

        EditText etTitle = dialogView.findViewById(R.id.etTitle);
        EditText etMessage = dialogView.findViewById(R.id.etMessage);
        TextView tvSelectedIconLabel = dialogView.findViewById(R.id.tvSelectedIconLabel);

        MaterialCardView btnIconGeneral = dialogView.findViewById(R.id.btnIconGeneral);
        MaterialCardView btnIconAcademic = dialogView.findViewById(R.id.btnIconAcademic);
        MaterialCardView btnIconAlert = dialogView.findViewById(R.id.btnIconAlert);
        MaterialCardView btnIconSystem = dialogView.findViewById(R.id.btnIconSystem);
        MaterialCardView btnIconSubject = dialogView.findViewById(R.id.btnIconSubject);

        final String[] selectedIconType = {isEditing && existingNotif.iconType != null ? existingNotif.iconType : "GENERAL"};

        Runnable updateIconSelectionUI = () -> {
            int activeBorder = Color.parseColor("#38BDF8");
            int inactiveBorder = Color.parseColor("#1E293B");

            boolean isGen = "GENERAL".equals(selectedIconType[0]);
            boolean isAcad = "ACADEMIC".equals(selectedIconType[0]);
            boolean isAlr = "ALERT".equals(selectedIconType[0]);
            boolean isSys = "SYSTEM".equals(selectedIconType[0]);
            boolean isSub = "SUBJECT".equals(selectedIconType[0]);

            btnIconGeneral.setStrokeWidth(isGen ? (int) (2 * getResources().getDisplayMetrics().density) : (int) (1 * getResources().getDisplayMetrics().density));
            btnIconGeneral.setStrokeColor(isGen ? activeBorder : inactiveBorder);

            btnIconAcademic.setStrokeWidth(isAcad ? (int) (2 * getResources().getDisplayMetrics().density) : (int) (1 * getResources().getDisplayMetrics().density));
            btnIconAcademic.setStrokeColor(isAcad ? Color.parseColor("#F59E0B") : inactiveBorder);

            btnIconAlert.setStrokeWidth(isAlr ? (int) (2 * getResources().getDisplayMetrics().density) : (int) (1 * getResources().getDisplayMetrics().density));
            btnIconAlert.setStrokeColor(isAlr ? Color.parseColor("#06B6D4") : inactiveBorder);

            btnIconSystem.setStrokeWidth(isSys ? (int) (2 * getResources().getDisplayMetrics().density) : (int) (1 * getResources().getDisplayMetrics().density));
            btnIconSystem.setStrokeColor(isSys ? Color.parseColor("#8B5CF6") : inactiveBorder);

            btnIconSubject.setStrokeWidth(isSub ? (int) (2 * getResources().getDisplayMetrics().density) : (int) (1 * getResources().getDisplayMetrics().density));
            btnIconSubject.setStrokeColor(isSub ? Color.parseColor("#10B981") : inactiveBorder);

            if (isGen) tvSelectedIconLabel.setText("Aviso general");
            else if (isAcad) tvSelectedIconLabel.setText("Académico");
            else if (isAlr) tvSelectedIconLabel.setText("Alerta");
            else if (isSys) tvSelectedIconLabel.setText("Sistema");
            else if (isSub) tvSelectedIconLabel.setText("Materias");
        };

        btnIconGeneral.setOnClickListener(v -> { selectedIconType[0] = "GENERAL"; updateIconSelectionUI.run(); });
        btnIconAcademic.setOnClickListener(v -> { selectedIconType[0] = "ACADEMIC"; updateIconSelectionUI.run(); });
        btnIconAlert.setOnClickListener(v -> { selectedIconType[0] = "ALERT"; updateIconSelectionUI.run(); });
        btnIconSystem.setOnClickListener(v -> { selectedIconType[0] = "SYSTEM"; updateIconSelectionUI.run(); });
        btnIconSubject.setOnClickListener(v -> { selectedIconType[0] = "SUBJECT"; updateIconSelectionUI.run(); });

        updateIconSelectionUI.run();

        if (isEditing && existingNotif.title != null) etTitle.setText(existingNotif.title);
        if (isEditing && existingNotif.message != null) etMessage.setText(existingNotif.message);

        final String defaultTarget = (isEditing && existingNotif.targetType != null) ? existingNotif.targetType : (initialTargetType != null ? initialTargetType : "ALL");
        final String[] targetType = {defaultTarget};
        final String[] targetValue = {isEditing ? existingNotif.targetValue : null};

        TextView tvSelectedTarget = dialogView.findViewById(R.id.tvSelectedTarget);
        View btnSelectTarget = dialogView.findViewById(R.id.btnSelectTarget);

        String targetLabel = "Destinatarios: [📢 General]";
        if ("ROLE_STUDENTS".equals(defaultTarget)) targetLabel = "Destinatarios: [🎓 Estudiantes]";
        else if ("ROLE_PROFESSORS".equals(defaultTarget)) targetLabel = "Destinatarios: [👨‍🏫 Profesores]";
        else if ("ROLE_GUARDIANS".equals(defaultTarget) || "ROLE_ENCARGADOS".equals(defaultTarget)) targetLabel = "Destinatarios: [👥 Encargados y Familias]";
        else if ("ROLE_ADMINS".equals(defaultTarget)) targetLabel = "Destinatarios: [⚙️ Administración]";

        if (isEditing) targetLabel = "Destinatarios: [" + getNotificationTargetLabel(existingNotif) + "]";
        tvSelectedTarget.setText(targetLabel);

        btnSelectTarget.setOnClickListener(v -> {
            View targetDialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_select_target, null);
            MaterialAlertDialogBuilder targetBuilder = new MaterialAlertDialogBuilder(requireContext(), R.style.Theme_MyApplication_Dialog);
            targetBuilder.setView(targetDialogView);

            AlertDialog targetDialog = targetBuilder.create();
            if (targetDialog.getWindow() != null) {
                targetDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }

            View closeBtn = targetDialogView.findViewById(R.id.btnCloseDialog);
            View cancelBtn = targetDialogView.findViewById(R.id.btnCancelDialog);
            View confirmBtn = targetDialogView.findViewById(R.id.btnConfirmDialog);

            closeBtn.setOnClickListener(v2 -> targetDialog.dismiss());
            cancelBtn.setOnClickListener(v2 -> targetDialog.dismiss());

            MaterialCardView cardGen = targetDialogView.findViewById(R.id.cardTargetGeneral);
            MaterialCardView cardStu = targetDialogView.findViewById(R.id.cardTargetStudents);
            MaterialCardView cardProf = targetDialogView.findViewById(R.id.cardTargetProfessors);
            MaterialCardView cardAdm = targetDialogView.findViewById(R.id.cardTargetAdmins);
            MaterialCardView cardSub = targetDialogView.findViewById(R.id.cardTargetSubject);
            MaterialCardView cardFac = targetDialogView.findViewById(R.id.cardTargetFacility);

            ImageView radioGen = targetDialogView.findViewById(R.id.radioTargetGeneral);
            ImageView radioStu = targetDialogView.findViewById(R.id.radioTargetStudents);
            ImageView radioProf = targetDialogView.findViewById(R.id.radioTargetProfessors);
            ImageView radioAdm = targetDialogView.findViewById(R.id.radioTargetAdmins);

            TextView tvSubTitleSub = targetDialogView.findViewById(R.id.tvSubTargetSubject);
            TextView tvSubTitleFac = targetDialogView.findViewById(R.id.tvSubTargetFacility);

            final String[] tempTargetType = {targetType[0]};
            final String[] tempTargetValue = {targetValue[0]};

            Runnable updateTargetCardsUI = () -> {
                int activeColor = Color.parseColor("#2563EB");
                int inactiveColor = Color.parseColor("#1E293B");

                boolean isGen = "ALL".equals(tempTargetType[0]);
                boolean isStu = "ROLE_STUDENTS".equals(tempTargetType[0]);
                boolean isProf = "ROLE_PROFESSORS".equals(tempTargetType[0]);
                boolean isAdm = "ROLE_ADMINS".equals(tempTargetType[0]);
                boolean isSub = "SUBJECT".equals(tempTargetType[0]);
                boolean isFac = "FACILITY".equals(tempTargetType[0]);

                cardGen.setStrokeWidth(isGen ? (int) (2 * getResources().getDisplayMetrics().density) : (int) (1 * getResources().getDisplayMetrics().density));
                cardGen.setStrokeColor(isGen ? activeColor : inactiveColor);
                radioGen.setImageResource(isGen ? R.drawable.ic_radio_checked : R.drawable.ic_radio_unchecked);
                radioGen.setImageTintList(ColorStateList.valueOf(isGen ? Color.parseColor("#38BDF8") : Color.parseColor("#64748B")));

                cardStu.setStrokeWidth(isStu ? (int) (2 * getResources().getDisplayMetrics().density) : (int) (1 * getResources().getDisplayMetrics().density));
                cardStu.setStrokeColor(isStu ? activeColor : inactiveColor);
                radioStu.setImageResource(isStu ? R.drawable.ic_radio_checked : R.drawable.ic_radio_unchecked);
                radioStu.setImageTintList(ColorStateList.valueOf(isStu ? Color.parseColor("#38BDF8") : Color.parseColor("#64748B")));

                cardProf.setStrokeWidth(isProf ? (int) (2 * getResources().getDisplayMetrics().density) : (int) (1 * getResources().getDisplayMetrics().density));
                cardProf.setStrokeColor(isProf ? activeColor : inactiveColor);
                radioProf.setImageResource(isProf ? R.drawable.ic_radio_checked : R.drawable.ic_radio_unchecked);
                radioProf.setImageTintList(ColorStateList.valueOf(isProf ? Color.parseColor("#38BDF8") : Color.parseColor("#64748B")));

                cardAdm.setStrokeWidth(isAdm ? (int) (2 * getResources().getDisplayMetrics().density) : (int) (1 * getResources().getDisplayMetrics().density));
                cardAdm.setStrokeColor(isAdm ? activeColor : inactiveColor);
                radioAdm.setImageResource(isAdm ? R.drawable.ic_radio_checked : R.drawable.ic_radio_unchecked);
                radioAdm.setImageTintList(ColorStateList.valueOf(isAdm ? Color.parseColor("#38BDF8") : Color.parseColor("#64748B")));

                cardSub.setStrokeWidth(isSub ? (int) (2 * getResources().getDisplayMetrics().density) : (int) (1 * getResources().getDisplayMetrics().density));
                cardSub.setStrokeColor(isSub ? activeColor : inactiveColor);

                cardFac.setStrokeWidth(isFac ? (int) (2 * getResources().getDisplayMetrics().density) : (int) (1 * getResources().getDisplayMetrics().density));
                cardFac.setStrokeColor(isFac ? activeColor : inactiveColor);

                if (isSub && tempTargetValue[0] != null) {
                    tvSubTitleSub.setText("Seleccionado: " + tempTargetValue[0]);
                }
                if (isFac && tempTargetValue[0] != null) {
                    tvSubTitleFac.setText("Seleccionado: " + tempTargetValue[0]);
                }
            };

            updateTargetCardsUI.run();

            cardGen.setOnClickListener(v2 -> { tempTargetType[0] = "ALL"; tempTargetValue[0] = null; updateTargetCardsUI.run(); });
            cardStu.setOnClickListener(v2 -> { tempTargetType[0] = "ROLE_STUDENTS"; tempTargetValue[0] = null; updateTargetCardsUI.run(); });
            cardProf.setOnClickListener(v2 -> { tempTargetType[0] = "ROLE_PROFESSORS"; tempTargetValue[0] = null; updateTargetCardsUI.run(); });
            cardAdm.setOnClickListener(v2 -> { tempTargetType[0] = "ROLE_ADMINS"; tempTargetValue[0] = null; updateTargetCardsUI.run(); });

            cardSub.setOnClickListener(v2 -> {
                observeOnce(viewModel.getAllSubjects(), subjects -> {
                    if (subjects != null && !subjects.isEmpty()) {
                        String[] subNames = subjects.stream().map(s -> s.name).toArray(String[]::new);
                        new MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Seleccionar Materia")
                            .setItems(subNames, (d3, w3) -> {
                                tempTargetType[0] = "SUBJECT";
                                tempTargetValue[0] = subNames[w3];
                                updateTargetCardsUI.run();
                            }).show();
                    } else {
                        Toast.makeText(getContext(), "No hay materias registradas", Toast.LENGTH_SHORT).show();
                    }
                });
            });

            cardFac.setOnClickListener(v2 -> {
                observeOnce(viewModel.getAllFacilities(), facilities -> {
                    if (facilities != null && !facilities.isEmpty()) {
                        String[] facNames = facilities.stream().map(f -> f.name).toArray(String[]::new);
                        new MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Seleccionar Instalación")
                            .setItems(facNames, (d3, w3) -> {
                                tempTargetType[0] = "FACILITY";
                                tempTargetValue[0] = facNames[w3];
                                updateTargetCardsUI.run();
                            }).show();
                    } else {
                        Toast.makeText(getContext(), "No hay instalaciones registradas", Toast.LENGTH_SHORT).show();
                    }
                });
            });

            confirmBtn.setOnClickListener(v2 -> {
                targetType[0] = tempTargetType[0];
                targetValue[0] = tempTargetValue[0];

                String label = "Destinatarios: [📢 General]";
                if ("ROLE_STUDENTS".equals(targetType[0])) label = "Destinatarios: [🎓 Estudiantes]";
                else if ("ROLE_PROFESSORS".equals(targetType[0])) label = "Destinatarios: [👨‍🏫 Profesores]";
                else if ("ROLE_ADMINS".equals(targetType[0])) label = "Destinatarios: [⚙️ Administración]";
                else if ("SUBJECT".equals(targetType[0])) label = "Materia: [" + (targetValue[0] != null ? targetValue[0] : "") + "]";
                else if ("FACILITY".equals(targetType[0])) label = "Instalación: [" + (targetValue[0] != null ? targetValue[0] : "") + "]";

                tvSelectedTarget.setText(label);
                targetDialog.dismiss();
            });

            targetDialog.show();
        });

        View btnCancel = dialogView.findViewById(R.id.btnCancelDialog);
        View btnSend = dialogView.findViewById(R.id.btnSendDialog);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSend.setOnClickListener(v -> {
            hideKeyboard(etTitle);
            hideKeyboard(etMessage);
            hideKeyboard();
            String title = etTitle.getText().toString().trim();
            String message = etMessage.getText().toString().trim();

            boolean isValid = true;
            if (title.isEmpty()) {
                etTitle.setError("El título es obligatorio");
                isValid = false;
            }
            if (message.isEmpty()) {
                etMessage.setError("El mensaje es obligatorio");
                isValid = false;
            }

            if (isValid) {
                if (isEditing) {
                    existingNotif.title = title;
                    existingNotif.message = message;
                    existingNotif.targetType = targetType[0];
                    existingNotif.targetValue = targetValue[0];
                    existingNotif.iconType = selectedIconType[0];
                    viewModel.updateNotification(existingNotif, () -> {
                        triggerLocalSystemNotification(existingNotif);
                        Toast.makeText(getContext(), "¡Notificación actualizada y reenviada!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        viewModel.fetchNotifications();
                    });
                } else {
                    String timestamp = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
                    Notification notif = new Notification(title, message, targetType[0], targetValue[0], currentUser.name, timestamp, selectedIconType[0]);

                    viewModel.insertNotification(notif, () -> {
                        triggerLocalSystemNotification(notif);
                        Toast.makeText(getContext(), "¡Notificación creada y enviada con éxito!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        viewModel.fetchNotifications();
                    });
                }
            }
        });

        dialog.show();
    }

    private void triggerLocalSystemNotification(Notification notif) {
        if (getContext() == null || notif == null) return;
        Context context = requireContext().getApplicationContext();
        int notificationId = notif.id != 0 ? notif.id : (int) System.currentTimeMillis();
        VirtualFirebaseMessagingService.showNotification(context, notif.title, notif.message, notif.iconType, notificationId);
    }

    private void hideKeyboard(View view) {
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    private void hideKeyboard() {
        View focus = requireActivity().getCurrentFocus();
        if (focus != null) {
            hideKeyboard(focus);
        }
    }

    private String getNotificationTargetLabel(Notification n) {
        if (n == null || n.targetType == null) return "📢 Canal General";
        String type = n.targetType.toUpperCase();
        String val = n.targetValue != null ? n.targetValue : "";

        switch (type) {
            case "SUPPORT": return "✉️ Soporte";
            case "ROLE_STUDENTS": return "🎓 Estudiantes";
            case "ROLE_PROFESSORS": return "👨‍🏫 Profesores";
            case "ROLE_GUARDIANS":
            case "ROLE_ENCARGADOS": return "👥 Encargados y Familias";
            case "ROLE_ADMINS": return "⚙️ Administración";
            case "SUBJECT": return "📚 Materia: " + val;
            case "FACILITY": return "🏛️ Instalación: " + val;
            default: return "📢 Canal General";
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
