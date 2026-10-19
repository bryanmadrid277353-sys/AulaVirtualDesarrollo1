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
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.aula.virtual.service.VirtualFirebaseMessagingService;
import com.aula.virtual.ui.MainViewModel;
import com.aula.virtual.ui.ThemeHelper;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.messaging.FirebaseMessaging;

public class MainActivity extends AppCompatActivity {
    private NavController navController;
    private MainViewModel viewModel;

    private LinearLayout layoutBottomNav;
    private View btnBottomHome, btnBottomNotifications, btnBottomSettings, btnBottomProfile;
    private ImageView ivNavHome, ivNavNotifications, ivNavSettings, ivNavProfile;
    private TextView tvNavHomeLabel, tvNavNotificationsLabel, tvNavSettingsLabel, tvNavProfileLabel;
    private View pillNavHome, pillNavNotifications, pillNavSettings, pillNavProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
        }

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        layoutBottomNav = findViewById(R.id.layoutBottomNav);
        btnBottomHome = findViewById(R.id.btnBottomHome);
        btnBottomNotifications = findViewById(R.id.btnBottomNotifications);
        btnBottomSettings = findViewById(R.id.btnBottomSettings);
        btnBottomProfile = findViewById(R.id.btnBottomProfile);

        ivNavHome = findViewById(R.id.ivNavHome);
        ivNavNotifications = findViewById(R.id.ivNavNotifications);
        ivNavSettings = findViewById(R.id.ivNavSettings);
        ivNavProfile = findViewById(R.id.ivNavProfile);

        tvNavHomeLabel = findViewById(R.id.tvNavHomeLabel);
        tvNavNotificationsLabel = findViewById(R.id.tvNavNotificationsLabel);
        tvNavSettingsLabel = findViewById(R.id.tvNavSettingsLabel);
        tvNavProfileLabel = findViewById(R.id.tvNavProfileLabel);

        pillNavHome = findViewById(R.id.pillNavHome);
        pillNavNotifications = findViewById(R.id.pillNavNotifications);
        pillNavSettings = findViewById(R.id.pillNavSettings);
        pillNavProfile = findViewById(R.id.pillNavProfile);

        btnBottomHome.setOnClickListener(v -> navigateToHome());

        btnBottomNotifications.setOnClickListener(v -> {
            hideKeyboard();
            if (navController != null && navController.getCurrentDestination() != null && 
                navController.getCurrentDestination().getId() != R.id.notificationListFragment) {
                navController.navigate(R.id.notificationListFragment);
            }
        });

        btnBottomSettings.setOnClickListener(v -> {
            hideKeyboard();
            if (navController != null && navController.getCurrentDestination() != null && 
                navController.getCurrentDestination().getId() != R.id.settingsFragment) {
                navController.navigate(R.id.settingsFragment);
            }
        });

        btnBottomProfile.setOnClickListener(v -> {
            hideKeyboard();
            if (navController != null && navController.getCurrentDestination() != null && 
                navController.getCurrentDestination().getId() != R.id.studentProfileFragment) {
                navController.navigate(R.id.studentProfileFragment);
            }
        });

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            int id = destination.getId();
            if (id == R.id.loginFragment) {
                if (layoutBottomNav != null) layoutBottomNav.setVisibility(View.GONE);
            } else {
                if (layoutBottomNav != null) layoutBottomNav.setVisibility(View.VISIBLE);
                updateBottomNavSelection(id);
            }
        });

        TextView tvStatus = findViewById(R.id.tvConnectionStatus);
        if (ThemeHelper.isLocalMode(this)) {
            tvStatus.setVisibility(View.GONE);
        }
        
        viewModel.isServerConnected().observe(this, connected -> {
            if (ThemeHelper.isLocalMode(this) || Boolean.TRUE.equals(connected)) {
                tvStatus.setVisibility(View.GONE);
            } else {
                tvStatus.setVisibility(View.VISIBLE);
            }
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
        boolean isNotif = currentDestinationId == R.id.notificationListFragment;
        boolean isSettings = currentDestinationId == R.id.settingsFragment || currentDestinationId == R.id.settingsAdvancedFragment;
        boolean isProfile = currentDestinationId == R.id.studentProfileFragment;

        setTabState(pillNavHome, ivNavHome, tvNavHomeLabel, isHome, primaryColor, secondaryColor);
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

    public void navigateToHome() {
        if (viewModel == null || viewModel.getCurrentUser().getValue() == null) return;
        String role = viewModel.getCurrentUser().getValue().role;
        int homeId = -1;
        if ("ADMIN".equals(role)) homeId = R.id.adminHomeFragment;
        else if ("PROFESSOR".equals(role)) homeId = R.id.professorHomeFragment;
        else if ("STUDENT".equals(role) || "ENCARGADO".equals(role) || "GUARDIAN".equals(role)) homeId = R.id.studentHomeFragment;
        
        if (homeId != -1 && navController != null && navController.getCurrentDestination() != null && 
            navController.getCurrentDestination().getId() != homeId) {
            navController.popBackStack(homeId, false);
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
