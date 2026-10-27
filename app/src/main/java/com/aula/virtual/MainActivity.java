package com.aula.virtual;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.aula.virtual.data.entity.User;
import com.aula.virtual.service.VirtualFirebaseMessagingService;
import com.aula.virtual.ui.MainViewModel;
import com.aula.virtual.ui.ThemeHelper;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.messaging.FirebaseMessaging;

public class MainActivity extends AppCompatActivity {
    private NavController navController;
    private MainViewModel viewModel;

    private LinearLayout layoutBottomNav;
    private View btnBottomHome, btnBottomMessages, btnBottomNotifications, btnBottomSettings, btnBottomProfile;
    private ImageView ivNavHome, ivNavMessages, ivNavNotifications, ivNavSettings, ivNavProfile;
    private TextView tvNavHomeLabel, tvNavMessagesLabel, tvNavNotificationsLabel, tvNavSettingsLabel, tvNavProfileLabel;
    private View pillNavHome, pillNavMessages, pillNavNotifications, pillNavSettings, pillNavProfile;

    private View cardOfflineBanner;
    private View btnRetryConnection;
    private View btnCloseOfflineBanner;
    private boolean isBannerDismissedManually = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        layoutBottomNav = findViewById(R.id.layoutBottomNav);
        View mainRoot = findViewById(R.id.mainRootLayout);
        if (mainRoot != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainRoot, (v, windowInsets) -> {
                Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(0, systemBars.top, 0, 0);
                if (layoutBottomNav != null) {
                    layoutBottomNav.setPadding(0, 0, 0, systemBars.bottom);
                }
                return windowInsets;
            });
        }

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
        }

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        layoutBottomNav = findViewById(R.id.layoutBottomNav);
        btnBottomHome = findViewById(R.id.btnBottomHome);
        btnBottomMessages = findViewById(R.id.btnBottomMessages);
        btnBottomNotifications = findViewById(R.id.btnBottomNotifications);
        btnBottomSettings = findViewById(R.id.btnBottomSettings);
        btnBottomProfile = findViewById(R.id.btnBottomProfile);

        ivNavHome = findViewById(R.id.ivNavHome);
        ivNavMessages = findViewById(R.id.ivNavMessages);
        ivNavNotifications = findViewById(R.id.ivNavNotifications);
        ivNavSettings = findViewById(R.id.ivNavSettings);
        ivNavProfile = findViewById(R.id.ivNavProfile);

        tvNavHomeLabel = findViewById(R.id.tvNavHomeLabel);
        tvNavMessagesLabel = findViewById(R.id.tvNavMessagesLabel);
        tvNavNotificationsLabel = findViewById(R.id.tvNavNotificationsLabel);
        tvNavSettingsLabel = findViewById(R.id.tvNavSettingsLabel);
        tvNavProfileLabel = findViewById(R.id.tvNavProfileLabel);

        pillNavHome = findViewById(R.id.pillNavHome);
        pillNavMessages = findViewById(R.id.pillNavMessages);
        pillNavNotifications = findViewById(R.id.pillNavNotifications);
        pillNavSettings = findViewById(R.id.pillNavSettings);
        pillNavProfile = findViewById(R.id.pillNavProfile);

        cardOfflineBanner = findViewById(R.id.cardOfflineBanner);
        btnRetryConnection = findViewById(R.id.btnRetryConnection);
        btnCloseOfflineBanner = findViewById(R.id.btnCloseOfflineBanner);

        if (btnCloseOfflineBanner != null) {
            btnCloseOfflineBanner.setOnClickListener(v -> {
                isBannerDismissedManually = true;
                if (cardOfflineBanner != null) {
                    cardOfflineBanner.setVisibility(View.GONE);
                }
            });
        }

        if (btnRetryConnection != null) {
            btnRetryConnection.setOnClickListener(v -> {
                isBannerDismissedManually = false;
                Toast.makeText(this, "Comprobando conexión...", Toast.LENGTH_SHORT).show();
                if (viewModel != null) {
                    viewModel.refreshData();
                    viewModel.startKeepAlive();
                }
            });
        }

        btnBottomHome.setOnClickListener(v -> {
            triggerHaptic(v);
            navigateToHome();
        });

        btnBottomMessages.setOnClickListener(v -> {
            triggerHaptic(v);
            hideKeyboard();
            try {
                if (navController != null) {
                    if (navController.getCurrentDestination() == null || 
                        (navController.getCurrentDestination().getId() != R.id.directMessagesFragment && 
                         navController.getCurrentDestination().getId() != R.id.directChatFragment)) {
                        navController.navigate(R.id.action_global_directMessagesFragment);
                    }
                }
            } catch (Exception e) {
                try {
                    navController.navigate(R.id.directMessagesFragment);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        btnBottomNotifications.setOnClickListener(v -> {
            triggerHaptic(v);
            hideKeyboard();
            if (navController != null && navController.getCurrentDestination() != null && 
                navController.getCurrentDestination().getId() != R.id.notificationListFragment) {
                navController.navigate(R.id.notificationListFragment);
            }
        });

        btnBottomSettings.setOnClickListener(v -> {
            triggerHaptic(v);
            hideKeyboard();
            if (navController != null && navController.getCurrentDestination() != null && 
                navController.getCurrentDestination().getId() != R.id.settingsFragment) {
                navController.navigate(R.id.settingsFragment);
            }
        });

        btnBottomProfile.setOnClickListener(v -> {
            triggerHaptic(v);
            hideKeyboard();
            if (navController != null && navController.getCurrentDestination() != null && 
                navController.getCurrentDestination().getId() != R.id.studentProfileFragment) {
                navController.navigate(R.id.studentProfileFragment);
            }
        });

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            int id = destination.getId();
            if (id == R.id.loginFragment || id == R.id.blogDiscussionFragment || id == R.id.directChatFragment) {
                if (layoutBottomNav != null) layoutBottomNav.setVisibility(View.GONE);
            } else {
                if (layoutBottomNav != null) layoutBottomNav.setVisibility(View.VISIBLE);
                updateBottomNavSelection(id);
            }
            // Mantener estado del banner al cambiar de pantalla sin reiniciar reintentos
            updateOfflineBannerVisibility(viewModel != null ? viewModel.isServerConnected().getValue() : null);
        });

        viewModel.isServerConnected().observe(this, connected -> {
            if (Boolean.TRUE.equals(connected)) {
                isBannerDismissedManually = false;
            }
            updateOfflineBannerVisibility(connected);
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isCurrentDestinationHomeOrLogin()) {
                    showExitConfirmationDialog();
                } else if (navController != null && navController.getCurrentDestination() != null && 
                           (navController.getCurrentDestination().getId() == R.id.settingsFragment || 
                            navController.getCurrentDestination().getId() == R.id.studentProfileFragment ||
                            navController.getCurrentDestination().getId() == R.id.notificationListFragment)) {
                    navigateToHome();
                } else {
                    if (!navController.popBackStack()) {
                        showExitConfirmationDialog();
                    }
                }
            }
        });

        setupNotificationsAndPermissions();
    }

    private void updateOfflineBannerVisibility(Boolean isConnected) {
        if (cardOfflineBanner == null) return;
        boolean isLocal = ThemeHelper.isLocalMode(this);
        boolean connected = Boolean.TRUE.equals(isConnected);

        if (isLocal || connected || isBannerDismissedManually) {
            cardOfflineBanner.setVisibility(View.GONE);
        } else {
            if (Boolean.FALSE.equals(isConnected)) {
                cardOfflineBanner.setVisibility(View.VISIBLE);
            } else {
                cardOfflineBanner.setVisibility(View.GONE);
            }
        }
    }

    private void setupNotificationsAndPermissions() {
        try {
            // Crear canal de notificaciones
            VirtualFirebaseMessagingService.createNotificationChannel(this);

            // Solicitar permiso de notificaciones en Android 13+ (API 33+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
                }
            }

            // Verificar si FirebaseApp está correctamente inicializado (requiere google-services.json)
            if (!com.google.firebase.FirebaseApp.getApps(this).isEmpty()) {
                FirebaseMessaging.getInstance().subscribeToTopic("all")
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Log.d("MainActivity", "Suscrito con éxito al tema FCM 'all'");
                            }
                        });

                FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        String token = task.getResult();
                        Log.d("MainActivity", "FCM Token de este dispositivo: " + token);
                        getSharedPreferences("fcm_prefs", MODE_PRIVATE)
                                .edit()
                                .putString("fcm_token", token)
                                .apply();
                    }
                });
            } else {
                Log.w("MainActivity", "FirebaseApp no inicializado (falta google-services.json). Notificaciones push remotas no activas.");
            }
        } catch (Throwable t) {
            Log.e("MainActivity", "Error seguro al configurar notificaciones/FCM", t);
        }
    }

    private void updateBottomNavSelection(int currentDestinationId) {
        int primaryColor = getColorFromAttr(android.R.attr.colorPrimary);
        int secondaryColor = getColorFromAttr(android.R.attr.textColorSecondary);

        boolean isHome = currentDestinationId == R.id.adminHomeFragment || 
                         currentDestinationId == R.id.studentHomeFragment || 
                         currentDestinationId == R.id.professorHomeFragment;
        boolean isMessages = currentDestinationId == R.id.directMessagesFragment || currentDestinationId == R.id.directChatFragment;
        boolean isNotif = currentDestinationId == R.id.notificationListFragment;
        boolean isSettings = currentDestinationId == R.id.settingsFragment || currentDestinationId == R.id.settingsAdvancedFragment;
        boolean isProfile = currentDestinationId == R.id.studentProfileFragment;

        setTabState(pillNavHome, ivNavHome, tvNavHomeLabel, isHome, primaryColor, secondaryColor);
        if (pillNavMessages != null) setTabState(pillNavMessages, ivNavMessages, tvNavMessagesLabel, isMessages, primaryColor, secondaryColor);
        setTabState(pillNavNotifications, ivNavNotifications, tvNavNotificationsLabel, isNotif, primaryColor, secondaryColor);
        setTabState(pillNavSettings, ivNavSettings, tvNavSettingsLabel, isSettings, primaryColor, secondaryColor);
        setTabState(pillNavProfile, ivNavProfile, tvNavProfileLabel, isProfile, primaryColor, secondaryColor);
    }

    private void setTabState(View pill, ImageView iv, TextView tv, boolean isActive, int primaryColor, int secondaryColor) {
        if (isActive) {
            pill.setBackgroundResource(R.drawable.bg_badge_primary);
            iv.setImageTintList(ColorStateList.valueOf(0xFFFFFFFF));
            tv.setTextColor(primaryColor);
        } else {
            pill.setBackground(null);
            iv.setImageTintList(ColorStateList.valueOf(secondaryColor));
            tv.setTextColor(secondaryColor);
        }
    }

    private int getColorFromAttr(int attr) {
        TypedValue tv = new TypedValue();
        getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.startKeepAlive();
            isBannerDismissedManually = false;
            updateOfflineBannerVisibility(viewModel.isServerConnected().getValue());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (viewModel != null) {
            viewModel.stopKeepAlive();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController.navigateUp() || super.onSupportNavigateUp();
    }

    private void showExitConfirmationDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Salir")
                .setMessage("¿Estás seguro de que deseas salir de la aplicación?")
                .setPositiveButton("Sí", (dialog, which) -> finish())
                .setNegativeButton("No", null)
                .show();
    }

    private void hideKeyboard(View view) {
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    private void hideKeyboard() {
        View focus = getCurrentFocus();
        if (focus != null) {
            hideKeyboard(focus);
        }
    }

    public void triggerHaptic(View view) {
        if (ThemeHelper.isHapticFeedbackEnabled(this)) {
            if (view != null) {
                view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);
            }
        }
    }

    public void navigateToHome() {
        if (viewModel == null || viewModel.getCurrentUser() == null) return;
        User user = viewModel.getCurrentUser().getValue();
        if (user == null || user.role == null) return;

        String role = user.role;
        int homeId = -1;
        if ("ADMIN".equalsIgnoreCase(role)) homeId = R.id.adminHomeFragment;
        else if ("PROFESSOR".equalsIgnoreCase(role)) homeId = R.id.professorHomeFragment;
        else if ("STUDENT".equalsIgnoreCase(role) || "ENCARGADO".equalsIgnoreCase(role) || "GUARDIAN".equalsIgnoreCase(role)) homeId = R.id.studentHomeFragment;

        if (homeId != -1 && navController != null) {
            if (navController.getCurrentDestination() != null && navController.getCurrentDestination().getId() == homeId) {
                return;
            }
            try {
                boolean popped = navController.popBackStack(homeId, false);
                if (!popped) {
                    navController.navigate(homeId);
                }
            } catch (Exception e) {
                try {
                    navController.navigate(homeId);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private boolean isCurrentDestinationHomeOrLogin() {
        if (navController == null || navController.getCurrentDestination() == null) return false;
        int id = navController.getCurrentDestination().getId();
        return id == R.id.adminHomeFragment || 
               id == R.id.studentHomeFragment || 
               id == R.id.professorHomeFragment ||
               id == R.id.loginFragment;
    }
}
