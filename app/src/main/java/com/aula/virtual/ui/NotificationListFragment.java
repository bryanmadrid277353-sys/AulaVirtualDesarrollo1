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
import com.aula.virtual.data.entity.Student;
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
import java.util.stream.Collectors;

import androidx.core.content.ContextCompat;
import android.content.res.ColorStateList;

public class NotificationListFragment extends Fragment {
    private FragmentNotificationListBinding binding;
    private MainViewModel viewModel;
    private boolean isSpecificTab = false;
    private User currentUser;
    private List<Notification> allUserNotifications = new ArrayList<>();
    private List<Notification> rawServerNotifications = new ArrayList<>();
    private final List<String> userSubjectNames = new ArrayList<>();
    private final List<String> userFacilityNames = new ArrayList<>();
    private final List<Integer> userStudentIds = new ArrayList<>();
    private final List<String> userStudentCarnets = new ArrayList<>();
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
            binding.chipsScrollView.setVisibility(View.GONE);
            if (binding.layoutSearchContainer != null) binding.layoutSearchContainer.setVisibility(View.GONE);
            if (binding.layoutTopActionRow != null) binding.layoutTopActionRow.setVisibility(View.GONE);

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

        boolean isAdmin = "ADMIN".equals(currentUser.role);

        if (isAdmin) {
            if (binding.layoutTopActionRow != null) binding.layoutTopActionRow.setVisibility(View.VISIBLE);
            if (binding.btnCreateNotification != null) {
                binding.btnCreateNotification.setVisibility(View.VISIBLE);
                binding.btnCreateNotification.setOnClickListener(v -> showCreateNotificationDialog(null, "ALL"));
            }
            if (binding.btnToggleSearch != null) {
                binding.btnToggleSearch.setVisibility(View.VISIBLE);
                binding.btnToggleSearch.setOnClickListener(v -> {
                    if (binding.layoutSearchContainer != null) {
                        boolean isVisible = binding.layoutSearchContainer.getVisibility() == View.VISIBLE;
                        binding.layoutSearchContainer.setVisibility(isVisible ? View.GONE : View.VISIBLE);
                        if (!isVisible) {
                            binding.etSearchNotification.requestFocus();
                        }
                    }
                });
            }
            if (binding.layoutSearchContainer != null) binding.layoutSearchContainer.setVisibility(View.GONE);
        } else {
            if (binding.layoutTopActionRow != null) binding.layoutTopActionRow.setVisibility(View.GONE);
            if (binding.btnCreateNotification != null) binding.btnCreateNotification.setVisibility(View.GONE);
            if (binding.btnToggleSearch != null) binding.btnToggleSearch.setVisibility(View.GONE);
            if (binding.layoutSearchContainer != null) binding.layoutSearchContainer.setVisibility(View.VISIBLE);
        }

        binding.btnConfigurePush.setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_notificationListFragment_to_notificationSettingsFragment));

        if (binding.btnViewAllChannels != null) {
            binding.btnViewAllChannels.setOnClickListener(v -> {
                selectedChannelFilter[0] = "ALL";
                renderNotificationsAndChips();
            });
        }

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
        } else if ("ENCARGADO".equals(currentUser.role) || "GUARDIAN".equals(currentUser.role)) {
            viewModel.fetchStudentsForEncargado(currentUser.id);
            viewModel.getMyStudents().observe(getViewLifecycleOwner(), students -> {
                if (students != null) {
                    userStudentIds.clear();
                    userStudentCarnets.clear();
                    for (Student s : students) {
                        if (s != null) {
                            userStudentIds.add(s.id);
                            if (s.carnet != null) userStudentCarnets.add(s.carnet.trim().toUpperCase());
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
        boolean isPriorityAlert = ("URGENT_CITATION".equalsIgnoreCase(n.targetType) || "URGENT_CITATION".equalsIgnoreCase(n.iconType) ||
                (n.iconType != null && ("ALERT".equalsIgnoreCase(n.iconType) || "SYSTEM".equalsIgnoreCase(n.iconType)))) &&
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
        if ("URGENT_CITATION".equals(type)) {
            if ("ADMIN".equals(currentUser.role)) return true;

            if (n.targetValue != null && !n.targetValue.trim().isEmpty()) {
                String val = n.targetValue.trim();
                String[] parts = val.split(":");
                // Format: ESTUDIANTE:studentId:carnet:name:encargadoId

                // 1. Direct check against encargadoId in parts[4] if available
                if (parts.length >= 5 && ("ENCARGADO".equalsIgnoreCase(currentUser.role) || "GUARDIAN".equalsIgnoreCase(currentUser.role))) {
                    try {
                        int encIdInNotif = Integer.parseInt(parts[4]);
                        if (encIdInNotif == currentUser.id) return true;
                    } catch (NumberFormatException ignored) {}
                }

                // 2. Check if studentId in parts[1] matches userStudentIds
                if (parts.length >= 2) {
                    try {
                        int stIdInNotif = Integer.parseInt(parts[1]);
                        if (userStudentIds.contains(stIdInNotif)) return true;
                        if ("STUDENT".equalsIgnoreCase(currentUser.role) && stIdInNotif == currentUser.id) return true;
                    } catch (NumberFormatException ignored) {}
                }

                // 3. Check if student carnet in parts[2] matches userStudentCarnets
                if (parts.length >= 3) {
                    String carnetInNotif = parts[2].trim();
                    if (!carnetInNotif.isEmpty()) {
                        if (userStudentCarnets.contains(carnetInNotif.toUpperCase())) return true;
                        if ("STUDENT".equalsIgnoreCase(currentUser.role) && carnetInNotif.equalsIgnoreCase(currentUser.carnet)) return true;
                    }
                }
            }
            return false;
        }

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
        if (binding == null || !isAdded()) return;

        if (binding.chipsScrollView != null) {
            binding.chipsScrollView.setVisibility(View.GONE);
        }

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

        // Separate Urgent Citations (Pinned at Top) from Regular Notifications
        List<Notification> urgentPinnedNotifs = new ArrayList<>();
        List<Notification> regularNotifs = new ArrayList<>();

        for (Notification n : queryFiltered) {
            if (n == null) continue;
            boolean isUrgent = (n.targetType != null && "URGENT_CITATION".equalsIgnoreCase(n.targetType.trim()))
                            || (n.iconType != null && "URGENT_CITATION".equalsIgnoreCase(n.iconType.trim()));
            if (isUrgent) {
                urgentPinnedNotifs.add(n);
            } else {
                regularNotifs.add(n);
            }
        }

        // Sort both lists by timestamp / ID (newest first)
        java.util.Comparator<Notification> notifComparator = (n1, n2) -> {
            if (n1.timestamp != null && n2.timestamp != null) {
                int cmp = n2.timestamp.compareTo(n1.timestamp);
                if (cmp != 0) return cmp;
            }
            return Integer.compare(n2.id, n1.id);
        };

        urgentPinnedNotifs.sort(notifComparator);
        regularNotifs.sort(notifComparator);

        List<Notification> finalOrderedList = new ArrayList<>();
        finalOrderedList.addAll(urgentPinnedNotifs);
        finalOrderedList.addAll(regularNotifs);

        if (binding != null && binding.tvRecordCount != null) {
            int total = finalOrderedList.size();
            binding.tvRecordCount.setText("Avisos publicados (" + total + (total == 1 ? " registro)" : " registros)"));
        }

        binding.notificationsContainer.removeAllViews();
        boolean isDark = ThemeHelper.isDarkMode(requireContext());
        TypedValue typedValue = new TypedValue();
        requireContext().getTheme().resolveAttribute(android.R.attr.textColorPrimary, typedValue, true);
        int primaryTextColor = typedValue.data;
        requireContext().getTheme().resolveAttribute(android.R.attr.textColorSecondary, typedValue, true);
        int secondaryTextColor = typedValue.data;

        if (finalOrderedList.isEmpty()) {
            TextView tvEmpty = new TextView(requireContext());
            tvEmpty.setText("\nNo hay avisos o notificaciones registradas.\n");
            tvEmpty.setGravity(Gravity.CENTER);
            tvEmpty.setTextColor(0xFF757575);
            binding.notificationsContainer.addView(tvEmpty);
        } else {
            renderNotificationCards(binding.notificationsContainer, finalOrderedList, primaryTextColor, secondaryTextColor, isDark);
        }
    }

    private void renderNotificationCards(LinearLayout container, List<Notification> notifs, int primaryTextColor, int secondaryTextColor, boolean isDark) {
        for (Notification n : notifs) {
            boolean isUrgentNotif = (n.targetType != null && "URGENT_CITATION".equalsIgnoreCase(n.targetType.trim()))
                                 || (n.iconType != null && "URGENT_CITATION".equalsIgnoreCase(n.iconType.trim()));

            MaterialCardView card = new MaterialCardView(requireContext());
            card.setCardElevation(0f);
            card.setRadius(24f);
            card.setStrokeWidth(1);
            card.setStrokeColor(ColorStateList.valueOf(isDark ? 0xFF1E293B : 0xFFCBD5E1));

            if (isUrgentNotif) {
                card.setCardBackgroundColor(ColorStateList.valueOf(isDark ? 0xFF1C1318 : 0xFFFFF1F2));
            } else {
                card.setCardBackgroundColor(ColorStateList.valueOf(isDark ? 0xFF121929 : 0xFFF1F5F9));
            }
            
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
            if ("URGENT_CITATION".equals(iconKind) || isUrgentNotif) {
                capsuleBg.setColor(isDark ? 0xFF2A161B : 0xFFFFE4E6);
                capsuleBg.setStroke((int) (1f * getResources().getDisplayMetrics().density), 0x80F87171);
                ivIcon.setImageResource(R.drawable.ic_urgent_citation);
                ivIcon.setImageTintList(ColorStateList.valueOf(0xFFF87171));
            } else if ("ACADEMIC".equals(iconKind)) {
                capsuleBg.setColor(isDark ? 0xFF291F15 : 0xFFFEF3C7);
                capsuleBg.setStroke((int) (1f * getResources().getDisplayMetrics().density), 0x66D97706);
                ivIcon.setImageResource(R.drawable.ic_person);
                ivIcon.setImageTintList(ColorStateList.valueOf(0xFFD97706));
            } else if ("ALERT".equals(iconKind)) {
                capsuleBg.setColor(isDark ? 0xFF0E242D : 0xFFCFFAFE);
                capsuleBg.setStroke((int) (1f * getResources().getDisplayMetrics().density), 0x6606B6D4);
                ivIcon.setImageResource(R.drawable.ic_notification);
                ivIcon.setImageTintList(ColorStateList.valueOf(0xFF0284C7));
            } else if ("SYSTEM".equals(iconKind)) {
                capsuleBg.setColor(isDark ? 0xFF1E1B2E : 0xFFF3E8FF);
                capsuleBg.setStroke((int) (1f * getResources().getDisplayMetrics().density), 0x668B5CF6);
                ivIcon.setImageResource(R.drawable.ic_settings);
                ivIcon.setImageTintList(ColorStateList.valueOf(0xFF7C3AED));
            } else if ("SUBJECT".equals(iconKind)) {
                capsuleBg.setColor(isDark ? 0xFF0D281E : 0xD1FAE5);
                capsuleBg.setStroke((int) (1f * getResources().getDisplayMetrics().density), 0x6610B981);
                ivIcon.setImageResource(R.drawable.ic_open_book);
                ivIcon.setImageTintList(ColorStateList.valueOf(0xFF059669));
            } else {
                capsuleBg.setColor(isDark ? 0xFF131D33 : 0xFFE0F2FE);
                capsuleBg.setStroke((int) (1f * getResources().getDisplayMetrics().density), 0x6638BDF8);
                ivIcon.setImageResource(R.drawable.ic_megaphone);
                ivIcon.setImageTintList(ColorStateList.valueOf(0xFF0284C7));
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

            int tagColor = 0xFF0284C7;
            int tagBgColor = isDark ? 0xFF131D33 : 0xFFE0F2FE;
            int tagBorderColor = 0x8038BDF8;

            String type = n.targetType != null ? n.targetType.toUpperCase() : "ALL";
            if ("URGENT_CITATION".equals(type) || isUrgentNotif) {
                tagColor = 0xFFF87171;
                tagBgColor = isDark ? 0xFF2A161B : 0xFFFFE4E6;
                tagBorderColor = 0x80F87171;
            } else if ("ROLE_PROFESSORS".equals(type)) {
                tagColor = 0xFFD97706;
                tagBgColor = isDark ? 0xFF291F15 : 0xFFFEF3C7;
                tagBorderColor = 0x80D97706;
            } else if ("ROLE_STUDENTS".equals(type)) {
                tagColor = 0xFF087990;
                tagBgColor = isDark ? 0xFF0E242D : 0xFFCFFAFE;
                tagBorderColor = 0x8006B6D4;
            } else if ("ROLE_ADMINS".equals(type)) {
                tagColor = 0xFF7C3AED;
                tagBgColor = isDark ? 0xFF1E1B2E : 0xFFF3E8FF;
                tagBorderColor = 0x808B5CF6;
            } else if ("SUBJECT".equals(type)) {
                tagColor = 0xFF059669;
                tagBgColor = isDark ? 0xFF0D281E : 0xFFD1FAE5;
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
            tvTitle.setTextColor(primaryTextColor);

            TextView tvMsg = new TextView(requireContext());
            tvMsg.setText(n.message != null ? n.message : "");
            tvMsg.setTextSize(13);
            tvMsg.setTextColor(secondaryTextColor);
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
            divider.setBackgroundColor(isDark ? 0xFF1E293B : 0xFFE2E8F0);
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

                int btnBgColor = isDark ? 0xFF131B2E : ContextCompat.getColor(requireContext(), R.color.input_bg);
                int btnBorderColor = isDark ? 0xFF1E293B : ContextCompat.getColor(requireContext(), R.color.input_border);
                int btnTextColor = isDark ? 0xFF8E9BB0 : ContextCompat.getColor(requireContext(), R.color.bs_dark);
                int btnDeleteIconColor = isDark ? 0xFFFFB4AB : ContextCompat.getColor(requireContext(), R.color.accent_red);

                if (isSupport) {
                    MaterialButton btnProfile = new MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
                    btnProfile.setText("👤 Ver Perfil");
                    btnProfile.setTextSize(12);
                    btnProfile.setAllCaps(false);
                    btnProfile.setTextColor(0xFF2563EB);
                    btnProfile.setCornerRadius((int) (10 * getResources().getDisplayMetrics().density));
                    btnProfile.setStrokeColor(ColorStateList.valueOf(btnBorderColor));
                    btnProfile.setStrokeWidth((int) (1 * getResources().getDisplayMetrics().density));
                    btnProfile.setBackgroundTintList(ColorStateList.valueOf(btnBgColor));
                    btnProfile.setInsetTop(0);
                    btnProfile.setInsetBottom(0);
                    btnProfile.setOnClickListener(v -> navigateToUserProfile(n.senderName));
                    actionLayout.addView(btnProfile);
                } else {
                    MaterialButton btnEdit = new MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
                    btnEdit.setText("Editar");
                    btnEdit.setTextSize(12);
                    btnEdit.setTextColor(btnTextColor);
                    btnEdit.setAllCaps(false);
                    btnEdit.setIconResource(R.drawable.ic_edit);
                    btnEdit.setIconTint(ColorStateList.valueOf(btnTextColor));
                    btnEdit.setIconSize((int) (15 * getResources().getDisplayMetrics().density));
                    btnEdit.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
                    btnEdit.setIconPadding((int) (6 * getResources().getDisplayMetrics().density));
                    btnEdit.setCornerRadius((int) (10 * getResources().getDisplayMetrics().density));
                    btnEdit.setStrokeColor(ColorStateList.valueOf(btnBorderColor));
                    btnEdit.setStrokeWidth((int) (1 * getResources().getDisplayMetrics().density));
                    btnEdit.setBackgroundTintList(ColorStateList.valueOf(btnBgColor));
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
                btnDelete.setIconTint(ColorStateList.valueOf(btnDeleteIconColor));
                btnDelete.setIconSize((int) (18 * getResources().getDisplayMetrics().density));
                btnDelete.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
                btnDelete.setIconPadding(0);
                btnDelete.setCornerRadius((int) (10 * getResources().getDisplayMetrics().density));
                btnDelete.setStrokeColor(ColorStateList.valueOf(btnBorderColor));
                btnDelete.setStrokeWidth((int) (1 * getResources().getDisplayMetrics().density));
                btnDelete.setBackgroundTintList(ColorStateList.valueOf(btnBgColor));
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
        String senderStr = (n.senderName != null && !n.senderName.isEmpty()) ? ("Emisor: " + n.senderName) : "Notificación";
        DialogUtils.showConfirmDeleteDialog(
            requireContext(),
            "Confirmar Eliminación",
            "¿Estás seguro de que deseas eliminar este aviso o notificación? Se removerá del historial de todos los destinatarios.",
            n.title,
            senderStr + " • Comunicado",
            "AVISO",
            null,
            R.drawable.ic_notification,
            () -> {
                viewModel.deleteNotification(n.id, () -> {
                    Toast.makeText(getContext(), "Notificación eliminada", Toast.LENGTH_SHORT).show();
                    viewModel.fetchNotifications();
                });
            }
        );
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
        MaterialCardView btnIconUrgent = dialogView.findViewById(R.id.btnIconUrgent);

        final String defaultTarget = (isEditing && existingNotif.targetType != null) ? existingNotif.targetType : (initialTargetType != null ? initialTargetType : "ALL");
        final String[] targetType = {defaultTarget};
        final String[] targetValue = {isEditing ? existingNotif.targetValue : null};

        final String[] selectedIconType = {isEditing && existingNotif.iconType != null ? existingNotif.iconType : ("URGENT_CITATION".equals(targetType[0]) ? "URGENT_CITATION" : "GENERAL")};

        Runnable updateIconSelectionUI = () -> {
            boolean isUrgentTarget = "URGENT_CITATION".equalsIgnoreCase(targetType[0]);

            if (isUrgentTarget) {
                selectedIconType[0] = "URGENT_CITATION";

                if (btnIconUrgent != null) btnIconUrgent.setVisibility(View.VISIBLE);
                btnIconGeneral.setVisibility(View.GONE);
                btnIconAcademic.setVisibility(View.GONE);
                btnIconAlert.setVisibility(View.GONE);
                btnIconSystem.setVisibility(View.GONE);
                btnIconSubject.setVisibility(View.GONE);

                if (btnIconUrgent != null) {
                    btnIconUrgent.setStrokeWidth((int) (2 * getResources().getDisplayMetrics().density));
                    btnIconUrgent.setStrokeColor(Color.parseColor("#FF5252"));
                }
                tvSelectedIconLabel.setText("🚨 Citación Urgente (Obligatorio)");
            } else {
                if (btnIconUrgent != null) btnIconUrgent.setVisibility(View.GONE);
                btnIconGeneral.setVisibility(View.VISIBLE);
                btnIconAcademic.setVisibility(View.VISIBLE);
                btnIconAlert.setVisibility(View.VISIBLE);
                btnIconSystem.setVisibility(View.VISIBLE);
                btnIconSubject.setVisibility(View.VISIBLE);

                if ("URGENT_CITATION".equalsIgnoreCase(selectedIconType[0])) {
                    selectedIconType[0] = "GENERAL";
                }

                int activeBorder = ContextCompat.getColor(requireContext(), R.color.entity_materia);
                int inactiveBorder = ContextCompat.getColor(requireContext(), R.color.input_border);

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
            }
        };

        btnIconGeneral.setOnClickListener(v -> { if (!"URGENT_CITATION".equalsIgnoreCase(targetType[0])) { selectedIconType[0] = "GENERAL"; updateIconSelectionUI.run(); } });
        btnIconAcademic.setOnClickListener(v -> { if (!"URGENT_CITATION".equalsIgnoreCase(targetType[0])) { selectedIconType[0] = "ACADEMIC"; updateIconSelectionUI.run(); } });
        btnIconAlert.setOnClickListener(v -> { if (!"URGENT_CITATION".equalsIgnoreCase(targetType[0])) { selectedIconType[0] = "ALERT"; updateIconSelectionUI.run(); } });
        btnIconSystem.setOnClickListener(v -> { if (!"URGENT_CITATION".equalsIgnoreCase(targetType[0])) { selectedIconType[0] = "SYSTEM"; updateIconSelectionUI.run(); } });
        btnIconSubject.setOnClickListener(v -> { if (!"URGENT_CITATION".equalsIgnoreCase(targetType[0])) { selectedIconType[0] = "SUBJECT"; updateIconSelectionUI.run(); } });
        if (btnIconUrgent != null) {
            btnIconUrgent.setOnClickListener(v -> { selectedIconType[0] = "URGENT_CITATION"; updateIconSelectionUI.run(); });
        }

        updateIconSelectionUI.run();

        if (isEditing && existingNotif.title != null) etTitle.setText(existingNotif.title);
        if (isEditing && existingNotif.message != null) etMessage.setText(existingNotif.message);

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
            MaterialCardView cardUrgent = targetDialogView.findViewById(R.id.cardTargetUrgentParentCitation);
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
                boolean isUrg = "URGENT_CITATION".equals(tempTargetType[0]);
                boolean isStu = "ROLE_STUDENTS".equals(tempTargetType[0]);
                boolean isProf = "ROLE_PROFESSORS".equals(tempTargetType[0]);
                boolean isAdm = "ROLE_ADMINS".equals(tempTargetType[0]);
                boolean isSub = "SUBJECT".equals(tempTargetType[0]);
                boolean isFac = "FACILITY".equals(tempTargetType[0]);

                cardGen.setStrokeWidth(isGen ? (int) (2 * getResources().getDisplayMetrics().density) : (int) (1 * getResources().getDisplayMetrics().density));
                cardGen.setStrokeColor(isGen ? activeColor : inactiveColor);
                radioGen.setImageResource(isGen ? R.drawable.ic_radio_checked : R.drawable.ic_radio_unchecked);
                radioGen.setImageTintList(ColorStateList.valueOf(isGen ? Color.parseColor("#38BDF8") : Color.parseColor("#64748B")));

                if (cardUrgent != null) {
                    cardUrgent.setStrokeWidth(isUrg ? (int) (2 * getResources().getDisplayMetrics().density) : (int) (1 * getResources().getDisplayMetrics().density));
                    cardUrgent.setStrokeColor(isUrg ? Color.parseColor("#FF5252") : inactiveColor);
                }

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

            if (cardUrgent != null) {
                cardUrgent.setOnClickListener(v2 -> {
                    showSelectStudentCitationDialog(student -> {
                        if (student == null) return;
                        tempTargetType[0] = "URGENT_CITATION";
                        tempTargetValue[0] = "ESTUDIANTE:" + student.id + ":" + (student.carnet != null ? student.carnet : "") + ":" + (student.name != null ? student.name : "") + ":" + (student.encargadoId != null ? student.encargadoId : 0);

                        targetType[0] = tempTargetType[0];
                        targetValue[0] = tempTargetValue[0];

                        tvSelectedTarget.setText("Destinatarios: [🚨 Citación Urgente: " + student.name + "]");
                        updateIconSelectionUI.run();

                        if (etTitle.getText().toString().trim().isEmpty()) {
                            etTitle.setText("CITACIÓN URGENTE: " + student.name);
                        }
                        if (etMessage.getText().toString().trim().isEmpty()) {
                            etMessage.setText("Se solicita la presencia urgente del Encargado/Padre de familia del estudiante " + student.name + " (Carnet: " + (student.carnet != null ? student.carnet : "-") + ") en la dirección del centro escolar.");
                        }

                        targetDialog.dismiss();
                    });
                });
            }

            cardStu.setOnClickListener(v2 -> { tempTargetType[0] = "ROLE_STUDENTS"; tempTargetValue[0] = null; updateTargetCardsUI.run(); });
            cardProf.setOnClickListener(v2 -> { tempTargetType[0] = "ROLE_PROFESSORS"; tempTargetValue[0] = null; updateTargetCardsUI.run(); });
            cardAdm.setOnClickListener(v2 -> { tempTargetType[0] = "ROLE_ADMINS"; tempTargetValue[0] = null; updateTargetCardsUI.run(); });

            cardSub.setOnClickListener(v2 -> {
                observeOnce(viewModel.getAllSubjects(), subjects -> {
                    if (subjects != null && !subjects.isEmpty()) {
                        String[] subNames = subjects.stream().map(s -> s.name).toArray(String[]::new);
                        DialogUtils.showCustomChoiceDialog(
                            requireContext(),
                            "Seleccionar Materia",
                            "Elige la materia o aula para publicar la notificación.",
                            subNames,
                            -1,
                            (w3, optionText) -> {
                                tempTargetType[0] = "SUBJECT";
                                tempTargetValue[0] = subNames[w3];
                                updateTargetCardsUI.run();
                            }
                        );
                    } else {
                        Toast.makeText(getContext(), "No hay materias registradas", Toast.LENGTH_SHORT).show();
                    }
                });
            });

            cardFac.setOnClickListener(v2 -> {
                observeOnce(viewModel.getAllFacilities(), facilities -> {
                    if (facilities != null && !facilities.isEmpty()) {
                        String[] facNames = facilities.stream().map(f -> f.name).toArray(String[]::new);
                        DialogUtils.showCustomChoiceDialog(
                            requireContext(),
                            "Seleccionar Instalación",
                            "Elige el espacio o área física de la instalación.",
                            facNames,
                            -1,
                            (w3, optionText) -> {
                                tempTargetType[0] = "FACILITY";
                                tempTargetValue[0] = facNames[w3];
                                updateTargetCardsUI.run();
                            }
                        );
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
                else if ("URGENT_CITATION".equals(targetType[0])) {
                    if (targetValue[0] != null && targetValue[0].contains(":")) {
                        String[] parts = targetValue[0].split(":");
                        label = "Destinatarios: [🚨 Citación Urgente: " + (parts.length >= 4 ? parts[3] : "") + "]";
                    } else {
                        label = "Destinatarios: [🚨 Citación Urgente]";
                    }
                }

                tvSelectedTarget.setText(label);
                updateIconSelectionUI.run();
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

    private interface OnStudentCitationSelectedListener {
        void onSelected(Student student);
    }

    private void showSelectStudentCitationDialog(OnStudentCitationSelectedListener listener) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_select_student_citation, null);
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext(), R.style.Theme_MyApplication_Dialog);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        View btnClose = dialogView.findViewById(R.id.btnCloseDialog);
        View btnCancel = dialogView.findViewById(R.id.btnCancelDialog);
        EditText etSearch = dialogView.findViewById(R.id.etSearchStudentCitation);
        TextView tvCountHeader = dialogView.findViewById(R.id.tvStudentCountHeader);
        LinearLayout container = dialogView.findViewById(R.id.layoutStudentsCitationContainer);

        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        final List<Student>[] allStudentsHolder = new List[]{new ArrayList<>()};

        Runnable renderStudentList = () -> {
            container.removeAllViews();
            String query = etSearch.getText().toString().trim().toLowerCase();
            List<Student> filtered = allStudentsHolder[0].stream().filter(s -> {
                if (s == null) return false;
                if (query.isEmpty()) return true;
                String name = s.name != null ? s.name.toLowerCase() : "";
                String carnet = s.carnet != null ? s.carnet.toLowerCase() : "";
                String grade = s.grade != null ? s.grade.toLowerCase() : "";
                return name.contains(query) || carnet.contains(query) || grade.contains(query);
            }).collect(Collectors.toList());

            tvCountHeader.setText("ESTUDIANTES ENCONTRADOS (" + filtered.size() + ")");

            if (filtered.isEmpty()) {
                TextView tvEmpty = new TextView(requireContext());
                tvEmpty.setText("No se encontraron estudiantes.");
                tvEmpty.setTextColor(0xFF8E9BB0);
                tvEmpty.setPadding(0, 16, 0, 16);
                container.addView(tvEmpty);
                return;
            }

            for (Student s : filtered) {
                View rowView = LayoutInflater.from(requireContext()).inflate(R.layout.item_student_citation_row, container, false);
                TextView tvInitials = rowView.findViewById(R.id.tvStudentInitials);
                TextView tvName = rowView.findViewById(R.id.tvStudentName);
                TextView tvCarnetGrade = rowView.findViewById(R.id.tvStudentCarnetGrade);

                if (tvInitials != null) tvInitials.setText(ImageUtils.getInitials(s.name));
                if (tvName != null) tvName.setText(s.name != null ? s.name : "Estudiante");
                String carnetStr = s.carnet != null ? s.carnet : "-";
                String gradeStr = s.grade != null ? s.grade : "Primaria";
                if (tvCarnetGrade != null) tvCarnetGrade.setText("Carnet: " + carnetStr + " • " + gradeStr);

                rowView.setOnClickListener(v -> {
                    dialog.dismiss();
                    if (listener != null) listener.onSelected(s);
                });

                container.addView(rowView);
            }
        };

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { renderStudentList.run(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        observeOnce(viewModel.getAllStudentRecords(), students -> {
            allStudentsHolder[0] = students != null ? students : new ArrayList<>();
            renderStudentList.run();
        });

        dialog.show();
    }

    private String getNotificationTargetLabel(Notification n) {
        if (n == null || n.targetType == null) return "📢 Canal General";
        String type = n.targetType.toUpperCase();
        String val = n.targetValue != null ? n.targetValue : "";

        if ("URGENT_CITATION".equals(type)) {
            if (!val.isEmpty() && val.contains(":")) {
                String[] parts = val.split(":");
                if (parts.length >= 4) {
                    return "🚨 Citación Urgente: " + parts[3];
                }
            }
            return "🚨 Citación Urgente";
        }

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
