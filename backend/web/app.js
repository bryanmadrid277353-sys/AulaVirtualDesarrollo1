// Aula Virtual - Web Application Logic

const API_BASE = window.location.origin;

// State Management
let currentUser = null;
let currentTab = 'dashboard';

// --- Sesión: el servidor entrega un token firmado al iniciar sesión ---
const TOKEN_KEY = 'aula_token';
// Roles autorizados a usar este panel web (cambia esta lista si necesitas otro criterio)
const WEB_ALLOWED_ROLES = ['ADMIN', 'PROFESSOR'];

function getToken() {
  try { return localStorage.getItem(TOKEN_KEY); } catch (e) { return null; }
}

function clearSession() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem('aula_user');
  localStorage.removeItem('aula_saved_passwords'); // contraseñas en texto plano de versiones anteriores
  currentUser = null;
}

function escapeHtml(value) {
  return String(value ?? '').replace(/[&<>"']/g, ch => (
    { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[ch]
  ));
}

// FastAPI devuelve 'detail' como texto o como lista (errores de validación 422)
function formatApiError(detail) {
  if (!detail) return 'Error en la petición';
  if (typeof detail === 'string') return detail;
  if (Array.isArray(detail)) {
    return detail.map(d => (d && d.msg) ? `${(d.loc || []).slice(1).join('.') || 'campo'}: ${d.msg}` : String(d)).join('; ');
  }
  return 'Error en la petición';
}

let sessionExpiredHandled = false;
function handleSessionExpired() {
  if (sessionExpiredHandled) return;
  sessionExpiredHandled = true;
  clearSession();
  showLoginScreen();
  showToast('Tu sesión expiró. Inicia sesión nuevamente.', 'error');
  setTimeout(() => { sessionExpiredHandled = false; }, 1500);
}

// Helper for API calls
async function apiCall(endpoint, options = {}) {
  try {
    const token = getToken();
    const headers = {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(options.headers || {})
    };
    const res = await fetch(`${API_BASE}${endpoint}`, { ...options, headers });
    if (!res.ok) {
      const err = await res.json().catch(() => ({ detail: res.statusText }));
      if (res.status === 401 && token && endpoint !== '/login') handleSessionExpired();
      throw new Error(formatApiError(err.detail));
    }
    return await res.json();
  } catch (err) {
    console.error(`API Error [${endpoint}]:`, err);
    throw err;
  }
}

// Formateador de Fechas en Español (ej: "12 may 2026")
function formatDateSpanish(dateStr) {
  if (!dateStr || dateStr === '-') return '-';
  try {
    const cleanStr = String(dateStr).split('T')[0].trim();
    const parts = cleanStr.split('-');
    if (parts.length === 3) {
      const year = parts[0];
      const monthIdx = parseInt(parts[1], 10) - 1;
      const day = parseInt(parts[2], 10);
      const monthNames = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic'];
      if (monthIdx >= 0 && monthIdx < 12) {
        return `${day} ${monthNames[monthIdx]} ${year}`;
      }
    }
    return dateStr;
  } catch (e) {
    return dateStr;
  }
}

// UI Helpers
function showToast(message, type = 'success') {
  const container = document.getElementById('toast-container');
  const toast = document.createElement('div');
  toast.className = `toast ${type}`;
  toast.innerHTML = `
    <i class="fa-solid ${type === 'success' ? 'fa-circle-check' : 'fa-circle-exclamation'}"></i>
    <span>${message}</span>
  `;
  container.appendChild(toast);
  setTimeout(() => {
    toast.style.opacity = '0';
    setTimeout(() => toast.remove(), 300);
  }, 3500);
}

function openModal(title, bodyHtml, onSave) {
  document.getElementById('modal-title').innerText = title;
  document.getElementById('modal-body').innerHTML = bodyHtml;
  const overlay = document.getElementById('app-modal');
  overlay.classList.add('active');

  const submitBtn = document.getElementById('modal-submit-btn');
  submitBtn.onclick = async () => {
    if (onSave) {
      try {
        await onSave();
        closeModal();
      } catch (e) {
        showToast(e.message, 'error');
      }
    }
  };
}

function closeModal() {
  document.getElementById('app-modal').classList.remove('active');
}

// System Health Check
let wasDbOffline = false;

async function checkSystemHealth() {
  const statusBadge = document.getElementById('status-badge');

  try {
    const res = await fetch(`${API_BASE}/health`);
    if (res.ok) {
      if (statusBadge) {
        statusBadge.className = 'badge badge-presente';
        statusBadge.innerHTML = '<i class="fa-solid fa-circle" style="color:#10b981; font-size: 8px; margin-right: 4px;"></i> BD MySQL Conectada';
      }
      // Auto-recuperación: Si la BD estaba caída y se encendió, recargar el contenido actual automáticamente
      if (wasDbOffline) {
        wasDbOffline = false;
        switchTab(currentTab, false);
      }
      return true;
    } else {
      wasDbOffline = true;
      if (statusBadge) {
        statusBadge.className = 'badge badge-ausente';
        statusBadge.innerHTML = '<i class="fa-solid fa-triangle-exclamation" style="color:#ef4444; font-size: 10px; margin-right: 4px;"></i> MySQL Desconectado';
      }
      return false;
    }
  } catch (e) {
    wasDbOffline = true;
    if (statusBadge) {
      statusBadge.className = 'badge badge-ausente';
      statusBadge.innerHTML = '<i class="fa-solid fa-plug-circle-xmark" style="color:#ef4444; font-size: 10px; margin-right: 4px;"></i> Servidor Desconectado';
    }
    return false;
  }
}

function renderDbErrorScreen(tabName, errorMessage) {
  const mainContent = document.getElementById('main-content');
  if (!mainContent) return;

  mainContent.innerHTML = `
    <div class="db-error-card">
      <div style="width: 72px; height: 72px; background: #fee2e2; color: var(--danger); border-radius: 50%; display: inline-flex; align-items: center; justify-content: center; font-size: 2rem; margin-bottom: 1.25rem;">
        <i class="fa-solid fa-database"></i>
      </div>
      <h2 style="font-size: 1.4rem; font-weight: 700; color: var(--text-dark); margin-bottom: 0.5rem;">Sin Conexión a la Base de Datos MySQL</h2>
      <p style="color: var(--text-muted); font-size: 0.95rem; margin-bottom: 1.5rem; line-height: 1.6;">
        El servidor Python/FastAPI está activo, pero <strong>no fue posible conectar con MySQL</strong> en XAMPP.
      </p>

      <div class="db-error-steps">
        <strong style="color: var(--accent); display: block; margin-bottom: 0.5rem;"><i class="fa-solid fa-wrench"></i> Pasos para solucionar:</strong>
        <ol style="margin-left: 1.25rem; margin-top: 0.25rem; line-height: 1.6; color: var(--text-dark);">
          <li>Abre la aplicación <strong>XAMPP Control Panel</strong> en tu computadora.</li>
          <li>Localiza el módulo <strong>MySQL</strong>.</li>
          <li>Haz clic en el botón <strong style="color:var(--success);">Start</strong> para iniciar la base de datos.</li>
          <li>Presiona el botón de abajo para reintentar la conexión.</li>
        </ol>
        ${errorMessage ? `<div class="db-error-tech">Detalle técnico: ${errorMessage}</div>` : ''}
      </div>

      <button class="btn btn-primary" onclick="switchTab('${tabName}')" style="width: auto; padding: 0.75rem 2rem;">
        <i class="fa-solid fa-rotate-right"></i> Reintentar Conexión
      </button>
    </div>
  `;
}

// Theme Manager Logic
function initTheme() {
  const savedTheme = localStorage.getItem('aula_theme');
  let currentTheme = 'light';

  if (savedTheme) {
    currentTheme = savedTheme;
  } else if (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) {
    currentTheme = 'dark';
  }

  applyTheme(currentTheme);
}

function applyTheme(theme) {
  const icon = document.getElementById('theme-toggle-icon');
  const text = document.getElementById('theme-toggle-text');

  if (theme === 'dark') {
    document.body.classList.add('dark-theme');
    if (icon) icon.className = 'fa-solid fa-sun';
    if (text) text.innerText = 'Claro';
  } else {
    document.body.classList.remove('dark-theme');
    if (icon) icon.className = 'fa-solid fa-moon';
    if (text) text.innerText = 'Oscuro';
  }
  localStorage.setItem('aula_theme', theme);
}

function toggleTheme() {
  const isDark = document.body.classList.contains('dark-theme');
  applyTheme(isDark ? 'light' : 'dark');
}

// Init App
document.addEventListener('DOMContentLoaded', () => {
  // Nunca se aceptan credenciales por URL; si alguien las puso, se borran de la barra de direcciones
  if (window.location.search) {
    window.history.replaceState({}, document.title, window.location.pathname + window.location.hash);
  }

  // Inicializar tema guardado o preferencia del sistema operativo
  initTheme();

  restoreSession();

  // Verificación periódica de salud del sistema
  checkSystemHealth();
  setInterval(checkSystemHealth, 8000);

  // Event Listeners
  const loginForm = document.getElementById('login-form');
  if (loginForm) {
    loginForm.onsubmit = (e) => {
      if (e && e.preventDefault) e.preventDefault();
      handleLogin(e);
      return false;
    };
  }

  const submitBtn = document.getElementById('login-submit-btn');
  if (submitBtn) {
    submitBtn.onclick = (e) => {
      if (e && e.preventDefault) e.preventDefault();
      handleLogin(e);
    };
  }

  setupPasswordToggle();
  const logoutBtn = document.getElementById('btn-logout');
  if (logoutBtn) {
    logoutBtn.addEventListener('click', handleLogout);
  }

  const themeBtn = document.getElementById('theme-toggle-btn');
  if (themeBtn) {
    themeBtn.addEventListener('click', toggleTheme);
  }

  const brandLogo = document.getElementById('brand-logo');
  if (brandLogo) {
    brandLogo.addEventListener('click', () => switchTab('dashboard'));
  }

  document.querySelectorAll('.sidebar-menu .menu-link').forEach(link => {
    link.addEventListener('click', (e) => {
      e.preventDefault();
      const tab = link.getAttribute('data-tab');
      switchTab(tab);
    });
  });

  // Navegación con historial y recarga del navegador (Atrás/Adelante y F5)
  window.addEventListener('popstate', () => {
    if (currentUser) {
      const activeTab = getHashTab();
      switchTab(activeTab, false);
    }
  });
});

// Authentication
// Las contraseñas ya no se guardan en la página: el formulario de login usa los atributos
// estándar (autocomplete) para que el administrador de contraseñas del propio navegador las recuerde.

async function restoreSession() {
  const savedUser = localStorage.getItem('aula_user');
  if (!savedUser || !getToken()) {
    clearSession();
    showLoginScreen();
    return;
  }
  // Oculta el login mientras el servidor confirma quién eres (evita un parpadeo)
  document.getElementById('login-screen').style.display = 'none';
  try {
    // El rol que manda es el del servidor, no el guardado en el navegador (que se puede editar)
    const me = await apiCall('/me');
    if (!WEB_ALLOWED_ROLES.includes(me.role)) throw new Error('Rol no autorizado');
    currentUser = me;
    localStorage.setItem('aula_user', JSON.stringify(me));
    setupAppUI();
  } catch (e) {
    clearSession();
    showLoginScreen();
  }
}

function getHashTab() {
  const hash = window.location.hash.replace('#', '').trim().toLowerCase();
  const path = window.location.pathname.replace('/', '').trim().toLowerCase();
  const target = hash || path || 'dashboard';

  const validTabs = ['dashboard', 'admins', 'professors', 'encargados', 'users', 'students', 'subjects', 'enrollments', 'attendance', 'conduct', 'facilities', 'calendar', 'notifications'];
  return validTabs.includes(target) ? target : 'dashboard';
}

function togglePasswordVisibility() {
  const passwordInput = document.getElementById('login-password');
  const toggleIcon = document.getElementById('toggle-password-icon');
  const toggleBtn = document.getElementById('toggle-password-btn');

  if (!passwordInput) return;
  const isPassword = passwordInput.type === 'password';
  passwordInput.type = isPassword ? 'text' : 'password';
  if (toggleIcon) {
    toggleIcon.className = isPassword ? 'fa-solid fa-eye-slash' : 'fa-solid fa-eye';
  }
  if (toggleBtn) {
    toggleBtn.title = isPassword ? 'Ocultar contraseña' : 'Mostrar contraseña';
    toggleBtn.style.color = isPassword ? 'var(--primary)' : 'var(--text-muted)';
  }
}
window.togglePasswordVisibility = togglePasswordVisibility;

function setupPasswordToggle() {
  const toggleBtn = document.getElementById('toggle-password-btn');
  const passwordInput = document.getElementById('login-password');
  const toggleIcon = document.getElementById('toggle-password-icon');

  if (toggleBtn && passwordInput && toggleIcon) {
    toggleBtn.onclick = (e) => {
      if (e) {
        if (e.preventDefault) e.preventDefault();
        if (e.stopPropagation) e.stopPropagation();
      }
      togglePasswordVisibility();
      return false;
    };
  }
}

function showLoginScreen() {
  document.getElementById('login-screen').style.display = 'flex';
  document.getElementById('app-layout').style.display = 'none';
  setupPasswordToggle();
}

function setupAppUI() {
  document.getElementById('login-screen').style.display = 'none';
  document.getElementById('app-layout').style.display = 'flex';

  document.getElementById('current-user-name').innerText = currentUser.name || currentUser.carnet;
  document.getElementById('current-user-role').innerText = currentUser.role || 'USUARIO';
  document.getElementById('current-user-avatar').innerText = (currentUser.name || currentUser.carnet).charAt(0).toUpperCase();

  // Cargar la pestaña indicada en la URL hash o dashboard por defecto
  const initialTab = getHashTab();
  switchTab(initialTab, false);
}

async function handleLogin(e) {
  if (e) {
    if (e.preventDefault) e.preventDefault();
    if (e.stopPropagation) e.stopPropagation();
  }

  const carnetInput = document.getElementById('login-carnet');
  const passwordInput = document.getElementById('login-password');
  const carnet = carnetInput ? carnetInput.value.trim() : '';
  const password = passwordInput ? passwordInput.value.trim() : '';
  const errorDiv = document.getElementById('login-error');
  const submitBtn = document.getElementById('login-submit-btn');

  if (submitBtn && submitBtn.disabled) return;

  if (errorDiv) {
    errorDiv.style.display = 'none';
    errorDiv.innerHTML = '';
  }

  if (!carnet || !password) {
    if (errorDiv) {
      errorDiv.innerHTML = `<div style="background: #fef2f2; border: 1px solid #fca5a5; padding: 0.75rem 1rem; border-radius: 8px; color: #991b1b; text-align: left; font-size: 0.85rem; display: flex; align-items: flex-start; gap: 0.5rem; margin-top: 1rem;"><i class="fa-solid fa-triangle-exclamation" style="font-size: 1.1rem; color: #dc2626; margin-top: 2px; flex-shrink: 0;"></i> <div><strong>Atención:</strong> Por favor ingresa el carnet/usuario y la contraseña.</div></div>`;
      errorDiv.style.display = 'block';
    }
    return;
  }

  if (submitBtn) {
    submitBtn.disabled = true;
    submitBtn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Verificando credenciales...';
  }

  try {
    // POST con cuerpo JSON: la contraseña ya no viaja en la URL
    const { token, ...user } = await apiCall('/login', {
      method: 'POST',
      body: JSON.stringify({ carnet, password })
    });
    if (!WEB_ALLOWED_ROLES.includes(user.role)) {
      throw new Error('Este panel web es solo para administradores y profesores.');
    }
    currentUser = user;
    localStorage.setItem(TOKEN_KEY, token);
    localStorage.setItem('aula_user', JSON.stringify(user));
    if (passwordInput) passwordInput.value = '';

    showToast(`¡Bienvenido/a ${user.name || user.carnet}!`);
    setupAppUI();
  } catch (err) {
    if (errorDiv) {
      const errorMsg = escapeHtml(err.message || 'Carnet o contraseña incorrectos');
      errorDiv.innerHTML = `<div style="background: #fef2f2; border: 1px solid #fca5a5; padding: 0.75rem 1rem; border-radius: 8px; color: #991b1b; text-align: left; font-size: 0.85rem; display: flex; align-items: flex-start; gap: 0.5rem; margin-top: 1rem;"><i class="fa-solid fa-circle-xmark" style="font-size: 1.1rem; color: #dc2626; margin-top: 2px; flex-shrink: 0;"></i> <div><strong>Error de inicio de sesión:</strong><br>${errorMsg}</div></div>`;
      errorDiv.style.display = 'block';
    }
  } finally {
    if (submitBtn) {
      submitBtn.disabled = false;
      submitBtn.innerHTML = '<i class="fa-solid fa-right-to-bracket"></i> Iniciar Sesión';
    }
  }
}

window.handleLogin = handleLogin;

function handleLogout() {
  clearSession();
  window.location.hash = '';
  showLoginScreen();
  showToast('Sesión cerrada correctamente');
}

// Navigation & Tab Switching
async function switchTab(tabName, updateHash = true) {
  currentTab = tabName;

  if (updateHash && window.location.hash !== `#${tabName}`) {
    window.history.pushState(null, '', `#${tabName}`);
  }

  document.querySelectorAll('.sidebar-menu .menu-link').forEach(link => {
    link.classList.toggle('active', link.getAttribute('data-tab') === tabName);
  });

  const titles = {
    dashboard: 'Dashboard General',
    admins: 'Gestión de Administradores',
    professors: 'Gestión de Profesores',
    encargados: 'Gestión de Encargados / Padres',
    users: 'Gestión de Usuarios del Sistema',
    students: 'Expedientes de Estudiantes',
    subjects: 'Asignaturas y Cursos',
    enrollments: 'Inscripciones y Calificaciones',
    attendance: 'Control de Asistencia',
    conduct: 'Registro de Conducta',
    facilities: 'Instalaciones y Aulas',
    calendar: 'Calendario Académico',
    notifications: 'Gestión de Notificaciones'
  };

  document.getElementById('tab-title').innerText = titles[tabName] || 'Aula Virtual';
  const mainContent = document.getElementById('main-content');
  mainContent.innerHTML = '<div style="text-align:center; padding:3rem;"><i class="fa-solid fa-spinner fa-spin fa-2x" style="color:var(--accent);"></i><p style="margin-top:1rem; color:var(--text-muted); font-weight:500;">Cargando datos...</p></div>';

  const isDbHealthy = await checkSystemHealth();
  if (!isDbHealthy) {
    renderDbErrorScreen(tabName, 'Servidor MySQL no está corriendo o la base de datos aula_virtual no está disponible.');
    return;
  }

  try {
    switch (tabName) {
      case 'dashboard': await renderDashboard(); break;
      case 'admins': await renderUsersByRole('ADMIN'); break;
      case 'professors': await renderUsersByRole('PROFESSOR'); break;
      case 'encargados': await renderUsersByRole('ENCARGADO'); break;
      case 'users': await renderUsersByRole('ADMIN'); break;
      case 'students': await renderStudents(); break;
      case 'subjects': await renderSubjects(); break;
      case 'enrollments': await renderEnrollments(); break;
      case 'attendance': await renderAttendance(); break;
      case 'conduct': await renderConduct(); break;
      case 'facilities': await renderFacilities(); break;
      case 'calendar': await renderCalendar(); break;
      case 'notifications': await renderNotifications(); break;
    }
  } catch (err) {
    console.error("Error en render de tab:", err);
    renderDbErrorScreen(tabName, err.message);
  }
}

// 1. DASHBOARD
async function renderDashboard() {
  const [students, professors, admins, subjects, facilities] = await Promise.all([
    apiCall('/students'),
    apiCall('/users/professors'),
    apiCall('/users/admins'),
    apiCall('/subjects'),
    apiCall('/facilities')
  ]);

  document.getElementById('main-content').innerHTML = `
    <div class="stats-grid">
      <div class="stat-card" onclick="switchTab('students')" title="Ir a Expedientes de Estudiantes">
        <div class="stat-icon blue"><i class="fa-solid fa-user-graduate"></i></div>
        <div>
          <div class="stat-value">${students.length}</div>
          <div class="stat-label">Estudiantes Registrados</div>
        </div>
      </div>
      <div class="stat-card" onclick="switchTab('professors')" title="Ir a Gestión de Profesores">
        <div class="stat-icon green"><i class="fa-solid fa-chalkboard-user"></i></div>
        <div>
          <div class="stat-value">${professors.length}</div>
          <div class="stat-label">Docentes Activos</div>
        </div>
      </div>
      <div class="stat-card" onclick="switchTab('subjects')" title="Ir a Asignaturas y Cursos">
        <div class="stat-icon purple"><i class="fa-solid fa-book-bookmark"></i></div>
        <div>
          <div class="stat-value">${subjects.length}</div>
          <div class="stat-label">Asignaturas</div>
        </div>
      </div>
      <div class="stat-card" onclick="switchTab('facilities')" title="Ir a Instalaciones y Aulas">
        <div class="stat-icon amber"><i class="fa-solid fa-building"></i></div>
        <div>
          <div class="stat-value">${facilities.length}</div>
          <div class="stat-label">Instalaciones / Aulas</div>
        </div>
      </div>
    </div>

    <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 1.5rem;">
      <div class="table-card" style="padding: 1.5rem;">
        <h3 style="margin-bottom: 1rem; font-size: 1.1rem;"><i class="fa-solid fa-bolt" style="color:var(--accent);"></i> Acciones Rápidas</h3>
        <div style="display: flex; flex-direction: column; gap: 0.75rem;">
          <button class="btn btn-primary" onclick="switchTab('admins')"><i class="fa-solid fa-user-shield"></i> Administradores</button>
          <button class="btn btn-secondary" onclick="switchTab('professors')"><i class="fa-solid fa-chalkboard-user"></i> Profesores</button>
          <button class="btn btn-secondary" onclick="switchTab('encargados')"><i class="fa-solid fa-user-group"></i> Encargados</button>
        </div>
      </div>

      <div class="table-card" style="padding: 1.5rem;">
        <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom: 1rem;">
          <h3 style="margin:0; font-size: 1.1rem;"><i class="fa-solid fa-info-circle" style="color:var(--primary);"></i> Estado del Sistema</h3>
          <span class="badge badge-admin" id="status-badge"><i class="fa-solid fa-circle" style="color:#10b981; font-size: 8px; margin-right: 4px;"></i> Servidor Online</span>
        </div>
        <p style="color:var(--text-muted); font-size:0.95rem; line-height: 1.6;">
          Servidor FastAPI conectado correctamente a la base de datos MySQL <strong>aula_virtual</strong>.<br>
          Acceso habilitado vía <strong>Localhost</strong> y <strong>Túnel Cloudflare HTTPS</strong>.
        </p>
      </div>
    </div>
  `;
}

// 2. USERS MANAGEMENT BY SEPARATE ROLES
const ROLE_CONFIG = {
  ADMIN: {
    singular: 'Administrador',
    plural: 'Administradores',
    endpoint: '/users/admins',
    badge: 'badge-admin',
    icon: 'fa-user-shield',
    placeholderCarnet: 'ADM002'
  },
  PROFESSOR: {
    singular: 'Profesor',
    plural: 'Profesores',
    endpoint: '/users/professors',
    badge: 'badge-professor',
    icon: 'fa-chalkboard-user',
    placeholderCarnet: 'DOC002'
  },
  ENCARGADO: {
    singular: 'Encargado',
    plural: 'Encargados',
    endpoint: '/users/encargados',
    badge: 'badge-encargado',
    icon: 'fa-user-group',
    placeholderCarnet: 'ENC002'
  }
};

async function renderUsersByRole(role = 'ADMIN') {
  const config = ROLE_CONFIG[role] || ROLE_CONFIG.ADMIN;
  const users = await apiCall(config.endpoint);

  // Ordenar para que el rol activo aparezca primero a la izquierda, coloreado y más grande
  const rolesOrder = ['ADMIN', 'PROFESSOR', 'ENCARGADO'];
  const sortedRoles = [role, ...rolesOrder.filter(r => r !== role)];

  const subTabsHtml = sortedRoles.map(r => {
    const conf = ROLE_CONFIG[r];
    const isActive = r === role;
    const tabTarget = r === 'ADMIN' ? 'admins' : r === 'PROFESSOR' ? 'professors' : 'encargados';
    if (isActive) {
      return `
        <button class="btn btn-primary" onclick="switchTab('${tabTarget}')" style="padding: 0.7rem 1.35rem; font-size: 0.98rem; font-weight: 700; box-shadow: var(--shadow-md);">
          <i class="fa-solid ${conf.icon}"></i> ${conf.plural}
        </button>
      `;
    } else {
      return `
        <button class="btn btn-secondary" onclick="switchTab('${tabTarget}')" style="padding: 0.55rem 1rem; font-size: 0.88rem; opacity: 0.85;">
          <i class="fa-solid ${conf.icon}"></i> ${conf.plural}
        </button>
      `;
    }
  }).join('');

  document.getElementById('main-content').innerHTML = `
    <!-- Pestañas de Sub-Navegación de Roles: El activo en primer lugar a la izquierda -->
    <div style="display:flex; gap:0.75rem; align-items:center; margin-bottom:1.5rem; flex-wrap:wrap;">
      ${subTabsHtml}
    </div>

    <div class="panel-header" style="display:flex; justify-content:space-between; align-items:center; gap:1rem; margin-bottom:1.5rem; flex-wrap:wrap;">
      <div class="search-box" style="flex:1; max-width:380px;">
        <i class="fa-solid fa-magnifying-glass search-icon"></i>
        <input type="text" id="search-users" class="form-input" placeholder="Buscar en ${config.plural.toLowerCase()}..." oninput="filterUsersTable()">
      </div>
      <button class="btn btn-primary" onclick="showAddUserModal('${role}')" style="white-space:nowrap;">
        <i class="fa-solid fa-user-plus"></i> Nuevo ${config.singular}
      </button>
    </div>

    <div class="table-card">
      <div class="table-responsive">
        <table class="data-table" id="users-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Carnet</th>
              <th>Nombre</th>
              <th>Correo Personal</th>
              <th>Dirección</th>
              <th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            ${users.length === 0 ? `<tr><td colspan="6" style="text-align:center; color:var(--text-muted); padding:2rem;">No hay ${config.plural.toLowerCase()} registrados.</td></tr>` : ''}
            ${users.map(u => `
              <tr data-user-str="${(u.name + ' ' + u.carnet).toLowerCase()}">
                <td>${u.id}</td>
                <td><strong>${u.carnet}</strong></td>
                <td>${u.name}</td>
                <td>${u.personal_email || '-'}</td>
                <td>${u.address || '-'}</td>
                <td>
                  <button class="btn btn-secondary btn-sm" onclick="showEditUserModal(${u.id}, '${u.carnet}', '${u.name}', '${role}', '${u.personal_email || ''}', '${u.address || ''}')" title="Editar"><i class="fa-solid fa-pen"></i> Editar</button>
                  <button class="btn btn-danger btn-sm" onclick="deleteUser(${u.id}, '${u.name}', '${role}')" title="Eliminar"><i class="fa-solid fa-trash"></i></button>
                </td>
              </tr>
            `).join('')}
          </tbody>
        </table>
      </div>
    </div>
  `;
}

function filterUsersTable() {
  const query = document.getElementById('search-users').value.toLowerCase();
  document.querySelectorAll('#users-table tbody tr').forEach(row => {
    const match = row.getAttribute('data-user-str').includes(query);
    row.style.display = match ? '' : 'none';
  });
}

function showAddUserModal(targetRole) {
  const config = ROLE_CONFIG[targetRole] || ROLE_CONFIG.ADMIN;
  const html = `
    <form id="form-add-user" autocomplete="off">
      <div class="form-group">
        <label class="form-label">Carnet de ${config.singular}</label>
        <input type="text" id="user-carnet" name="new_user_carnet" class="form-input" required placeholder="Ej: ${config.placeholderCarnet}" autocomplete="off">
      </div>
      <div class="form-group">
        <label class="form-label">Nombre Completo</label>
        <input type="text" id="user-name" name="new_user_fullname" class="form-input" required placeholder="Nombre y Apellidos" autocomplete="off">
      </div>
      <div class="form-group">
        <label class="form-label">Contraseña</label>
        <input type="password" id="user-password" name="new_user_password" class="form-input" required placeholder="••••••••" autocomplete="new-password">
      </div>
      <div class="form-group">
        <label class="form-label">Correo Personal</label>
        <input type="email" id="user-email" name="new_user_email" class="form-input" placeholder="correo@ejemplo.com" autocomplete="off">
      </div>
      <div class="form-group">
        <label class="form-label">Dirección</label>
        <input type="text" id="user-address" name="new_user_address" class="form-input" placeholder="Dirección física" autocomplete="off">
      </div>
    </form>
  `;

  openModal(`Crear Nuevo ${config.singular}`, html, async () => {
    const carnet = document.getElementById('user-carnet').value.trim();
    const name = document.getElementById('user-name').value.trim();
    const password = document.getElementById('user-password').value.trim();
    const personal_email = document.getElementById('user-email').value.trim();
    const address = document.getElementById('user-address').value.trim();

    if (!carnet || !name || !password) throw new Error('Carnet, nombre y contraseña son requeridos');

    await apiCall('/users', {
      method: 'POST',
      body: JSON.stringify({ carnet, name, password, role: targetRole, personal_email, address })
    });
    showToast(`${config.singular} creado exitosamente`);
    renderUsersByRole(targetRole);
  });
}

function showEditUserModal(id, carnet, name, targetRole, email, address) {
  const config = ROLE_CONFIG[targetRole] || ROLE_CONFIG.ADMIN;
  const html = `
    <form id="form-edit-user" autocomplete="off">
      <div class="form-group">
        <label class="form-label">Carnet de ${config.singular}</label>
        <input type="text" id="edit-user-carnet" name="edit_user_carnet" class="form-input" value="${carnet}" required autocomplete="off">
      </div>
      <div class="form-group">
        <label class="form-label">Nombre Completo</label>
        <input type="text" id="edit-user-name" name="edit_user_fullname" class="form-input" value="${name}" required autocomplete="off">
      </div>
      <div class="form-group">
        <label class="form-label">Nueva Contraseña (dejar en blanco para conservar)</label>
        <input type="password" id="edit-user-password" name="edit_user_password" class="form-input" placeholder="Opcional" autocomplete="new-password">
      </div>
      <div class="form-group">
        <label class="form-label">Correo Personal</label>
        <input type="email" id="edit-user-email" name="edit_user_email" class="form-input" value="${email}" autocomplete="off">
      </div>
      <div class="form-group">
        <label class="form-label">Dirección</label>
        <input type="text" id="edit-user-address" name="edit_user_address" class="form-input" value="${address}" autocomplete="off">
      </div>
    </form>
  `;

  openModal(`Editar ${config.singular}`, html, async () => {
    const updatedCarnet = document.getElementById('edit-user-carnet').value.trim();
    const updatedName = document.getElementById('edit-user-name').value.trim();
    const pwd = document.getElementById('edit-user-password').value.trim();
    const updatedEmail = document.getElementById('edit-user-email').value.trim();
    const updatedAddr = document.getElementById('edit-user-address').value.trim();

    const payload = {
      carnet: updatedCarnet,
      name: updatedName,
      role: targetRole,
      personal_email: updatedEmail,
      address: updatedAddr
    };
    if (pwd) payload.password = pwd;

    await apiCall(`/users/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    });
    showToast(`${config.singular} actualizado`);
    renderUsersByRole(targetRole);
  });
}

async function deleteUser(id, name, targetRole) {
  const config = ROLE_CONFIG[targetRole] || ROLE_CONFIG.ADMIN;
  if (confirm(`¿Estás seguro de eliminar a "${name}"?`)) {
    try {
      await apiCall(`/users/${id}`, { method: 'DELETE' });
      showToast(`${config.singular} eliminado`);
      renderUsersByRole(targetRole);
    } catch (e) {
      showToast(e.message, 'error');
    }
  }
}

// 3. STUDENTS RECORDS
async function renderStudents() {
  const [students, encargados] = await Promise.all([
    apiCall('/students'),
    apiCall('/users/encargados')
  ]);

  const encargadoMap = {};
  encargados.forEach(e => { encargadoMap[e.id] = e; });

  document.getElementById('main-content').innerHTML = `
    <div class="panel-header">
      <div class="search-box">
        <i class="fa-solid fa-magnifying-glass search-icon"></i>
        <input type="text" id="search-students" class="form-input" placeholder="Buscar expediente..." oninput="filterStudentsTable()">
      </div>
      <button class="btn btn-primary" onclick="showAddStudentModal()">
        <i class="fa-solid fa-plus"></i> Nuevo Expediente
      </button>
    </div>

    <div class="table-card">
      <div class="table-responsive">
        <table class="data-table" id="students-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Carnet Estudiante</th>
              <th>Nombre Estudiante</th>
              <th>Grado</th>
              <th>Carnet Encargado</th>
              <th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            ${students.length === 0 ? '<tr><td colspan="6" style="text-align:center; color:var(--text-muted); padding:2rem;">No hay expedientes estudiantiles registrados.</td></tr>' : ''}
            ${students.map(s => {
              const enc = s.encargadoId ? encargadoMap[s.encargadoId] : null;
              const encDisplay = enc ? `<strong>${enc.carnet}</strong> <span style="color:var(--text-muted); font-size:0.85rem;">(${enc.name})</span>` : '<span style="color:var(--text-muted);">Sin asignar</span>';
              return `
                <tr data-student-str="${(s.name + ' ' + s.carnet + ' ' + (enc ? enc.carnet + ' ' + enc.name : '')).toLowerCase()}">
                  <td>${s.id}</td>
                  <td><strong>${s.carnet}</strong></td>
                  <td>${s.name}</td>
                  <td>${s.grade || '-'}</td>
                  <td>${encDisplay}</td>
                  <td>
                    <button class="btn btn-secondary btn-sm" onclick="showEditStudentModal(${s.id}, '${s.carnet}', '${s.name}', '${s.grade || ''}', ${s.encargadoId || 'null'})" title="Editar"><i class="fa-solid fa-pen"></i> Editar</button>
                    <button class="btn btn-danger btn-sm" onclick="deleteStudent(${s.id}, '${s.name}')" title="Eliminar"><i class="fa-solid fa-trash"></i></button>
                  </td>
                </tr>
              `;
            }).join('')}
          </tbody>
        </table>
      </div>
    </div>
  `;
}

function filterStudentsTable() {
  const query = document.getElementById('search-students').value.toLowerCase();
  document.querySelectorAll('#students-table tbody tr').forEach(row => {
    const match = row.getAttribute('data-student-str').includes(query);
    row.style.display = match ? '' : 'none';
  });
}

async function showAddStudentModal() {
  const encargados = await apiCall('/users/encargados').catch(() => []);
  const html = `
    <form id="form-student">
      <div class="form-group">
        <label class="form-label">Carnet Estudiantil</label>
        <input type="text" id="st-carnet" class="form-input" required placeholder="Ej: EST002">
      </div>
      <div class="form-group">
        <label class="form-label">Nombre del Estudiante</label>
        <input type="text" id="st-name" class="form-input" required placeholder="Nombre completo">
      </div>
      <div class="form-group">
        <label class="form-label">Grado / Nivel</label>
        <input type="text" id="st-grade" class="form-input" placeholder="Ej: 7mo Grado A">
      </div>
      <div class="form-group">
        <label class="form-label">Encargado / Padre</label>
        <select id="st-encargado" class="form-select">
          <option value="">-- Sin Encargado --</option>
          ${encargados.map(e => `<option value="${e.id}">${e.name} (${e.carnet})</option>`).join('')}
        </select>
      </div>
    </form>
  `;

  openModal('Nuevo Expediente Estudiantil', html, async () => {
    const carnet = document.getElementById('st-carnet').value.trim();
    const name = document.getElementById('st-name').value.trim();
    const grade = document.getElementById('st-grade').value.trim();
    const encVal = document.getElementById('st-encargado').value;
    const encargadoId = encVal ? parseInt(encVal) : null;

    if (!carnet || !name) throw new Error('Carnet y nombre son obligatorios');

    await apiCall('/students', {
      method: 'POST',
      body: JSON.stringify({ carnet, name, grade, encargadoId })
    });
    showToast('Expediente creado');
    renderStudents();
  });
}

async function showEditStudentModal(id, carnet, name, grade, encargadoId) {
  const encargados = await apiCall('/users/encargados').catch(() => []);
  const html = `
    <form id="form-edit-student">
      <div class="form-group">
        <label class="form-label">Carnet Estudiantil</label>
        <input type="text" id="edit-st-carnet" class="form-input" value="${carnet}" required>
      </div>
      <div class="form-group">
        <label class="form-label">Nombre del Estudiante</label>
        <input type="text" id="edit-st-name" class="form-input" value="${name}" required>
      </div>
      <div class="form-group">
        <label class="form-label">Grado</label>
        <input type="text" id="edit-st-grade" class="form-input" value="${grade}">
      </div>
      <div class="form-group">
        <label class="form-label">Encargado / Padre</label>
        <select id="edit-st-encargado" class="form-select">
          <option value="">-- Sin Encargado --</option>
          ${encargados.map(e => `<option value="${e.id}" ${e.id === encargadoId ? 'selected':''}>${e.name} (${e.carnet})</option>`).join('')}
        </select>
      </div>
    </form>
  `;

  openModal('Editar Expediente', html, async () => {
    const c = document.getElementById('edit-st-carnet').value.trim();
    const n = document.getElementById('edit-st-name').value.trim();
    const g = document.getElementById('edit-st-grade').value.trim();
    const encVal = document.getElementById('edit-st-encargado').value;
    const encId = encVal ? parseInt(encVal) : null;

    await apiCall(`/students/${id}`, {
      method: 'PUT',
      body: JSON.stringify({ carnet: c, name: n, grade: g, encargadoId: encId })
    });
    showToast('Expediente actualizado');
    renderStudents();
  });
}

async function deleteStudent(id, name) {
  if (confirm(`¿Desea eliminar el expediente de ${name}?`)) {
    try {
      await apiCall(`/students/${id}`, { method: 'DELETE' });
      showToast('Estudiante eliminado');
      renderStudents();
    } catch (e) {
      showToast(e.message, 'error');
    }
  }
}

// 4. SUBJECTS
async function renderSubjects() {
  const [subjects, professors] = await Promise.all([
    apiCall('/subjects').catch(() => []),
    apiCall('/users/professors').catch(() => [])
  ]);

  const profMap = {};
  professors.forEach(p => profMap[p.id] = p.name);

  document.getElementById('main-content').innerHTML = `
    <div class="panel-header">
      <h2>Asignaturas y Cursos</h2>
      <button class="btn btn-primary" onclick="showAddSubjectModal()"><i class="fa-solid fa-plus"></i> Nueva Asignatura</button>
    </div>

    <div style="display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 1.5rem;">
      ${subjects.map(sub => `
        <div class="table-card" style="padding:1.25rem; border-top: 5px solid ${sub.color || '#2563eb'};">
          <div style="display:flex; justify-content:space-between; align-items:start;">
            <span class="badge badge-admin">${sub.code || 'SUB-01'}</span>
            <span style="font-size:0.8rem; color:var(--text-muted);">Sección ${sub.section || '01'}</span>
          </div>
          <h3 style="margin: 0.75rem 0 0.25rem 0; font-size:1.1rem;">${sub.name}</h3>
          <p style="color:var(--text-muted); font-size:0.85rem; margin-bottom:1rem;">${sub.description || 'Sin descripción'}</p>
          <div style="font-size:0.85rem; color:var(--text-dark); margin-bottom:1rem;">
            <i class="fa-solid fa-chalkboard-user"></i> Prof: <strong>${profMap[sub.professorId] || 'Sin Docente'}</strong>
          </div>
          <div style="display:flex; gap:0.5rem;">
            <button class="btn btn-secondary btn-sm" style="flex:1;" onclick="showEditSubjectModal(${sub.id}, '${sub.code||''}', '${sub.name}', '${sub.description||''}', ${sub.professorId}, '${sub.section||'01'}')"><i class="fa-solid fa-pen"></i> Editar</button>
            <button class="btn btn-danger btn-sm" onclick="deleteSubject(${sub.id}, '${sub.name}')"><i class="fa-solid fa-trash"></i></button>
          </div>
        </div>
      `).join('')}
    </div>
  `;
}

async function showAddSubjectModal() {
  const professors = await apiCall('/users/professors').catch(() => []);
  const html = `
    <form id="form-add-sub">
      <div class="form-group">
        <label class="form-label">Código de Asignatura</label>
        <input type="text" id="sub-code" class="form-input" placeholder="Ej: MAT-101">
      </div>
      <div class="form-group">
        <label class="form-label">Nombre de la Asignatura</label>
        <input type="text" id="sub-name" class="form-input" required placeholder="Ej: Matemáticas">
      </div>
      <div class="form-group">
        <label class="form-label">Descripción</label>
        <textarea id="sub-desc" class="form-textarea" rows="2" placeholder="Detalles de la materia"></textarea>
      </div>
      <div class="form-group">
        <label class="form-label">Profesor Asignado</label>
        <select id="sub-prof" class="form-select" required>
          ${professors.map(p => `<option value="${p.id}">${p.name} (${p.carnet})</option>`).join('')}
        </select>
      </div>
      <div class="form-group">
        <label class="form-label">Sección</label>
        <input type="text" id="sub-sec" class="form-input" value="01">
      </div>
    </form>
  `;

  openModal('Nueva Asignatura', html, async () => {
    const code = document.getElementById('sub-code').value.trim();
    const name = document.getElementById('sub-name').value.trim();
    const description = document.getElementById('sub-desc').value.trim();
    const professorId = parseInt(document.getElementById('sub-prof').value);
    const section = document.getElementById('sub-sec').value.trim();

    if (!name || !professorId) throw new Error('Nombre y profesor son requeridos');

    await apiCall('/subjects', {
      method: 'POST',
      body: JSON.stringify({ code, name, description, professorId, section, color: '#2563eb' })
    });
    showToast('Asignatura creada');
    renderSubjects();
  });
}

async function showEditSubjectModal(id, code, name, description, professorId, section) {
  const professors = await apiCall('/users/professors').catch(() => []);
  const html = `
    <form id="form-edit-sub">
      <div class="form-group">
        <label class="form-label">Código</label>
        <input type="text" id="edit-sub-code" class="form-input" value="${code}">
      </div>
      <div class="form-group">
        <label class="form-label">Nombre</label>
        <input type="text" id="edit-sub-name" class="form-input" value="${name}" required>
      </div>
      <div class="form-group">
        <label class="form-label">Descripción</label>
        <textarea id="edit-sub-desc" class="form-textarea" rows="2">${description}</textarea>
      </div>
      <div class="form-group">
        <label class="form-label">Profesor</label>
        <select id="edit-sub-prof" class="form-select">
          ${professors.map(p => `<option value="${p.id}" ${p.id===professorId?'selected':''}>${p.name}</option>`).join('')}
        </select>
      </div>
      <div class="form-group">
        <label class="form-label">Sección</label>
        <input type="text" id="edit-sub-sec" class="form-input" value="${section}">
      </div>
    </form>
  `;

  openModal('Editar Asignatura', html, async () => {
    const c = document.getElementById('edit-sub-code').value.trim();
    const n = document.getElementById('edit-sub-name').value.trim();
    const d = document.getElementById('edit-sub-desc').value.trim();
    const p = parseInt(document.getElementById('edit-sub-prof').value);
    const s = document.getElementById('edit-sub-sec').value.trim();

    await apiCall(`/subjects/${id}`, {
      method: 'PUT',
      body: JSON.stringify({ code: c, name: n, description: d, professorId: p, section: s, color: '#2563eb' })
    });
    showToast('Asignatura actualizada');
    renderSubjects();
  });
}

async function deleteSubject(id, name) {
  if (confirm(`¿Eliminar la asignatura ${name}?`)) {
    try {
      await apiCall(`/subjects/${id}`, { method: 'DELETE' });
      showToast('Asignatura eliminada');
      renderSubjects();
    } catch (e) {
      showToast(e.message, 'error');
    }
  }
}

// 5. ENROLLMENTS & GRADES
async function renderEnrollments() {
  const subjects = await apiCall('/subjects').catch(() => []);
  let selectedSubId = subjects.length > 0 ? subjects[0].id : null;

  const renderSubjectEnrollments = async (subId) => {
    if (!subId) return;
    const enrollments = await apiCall(`/subjects/${subId}/enrollments`).catch(() => []);
    const container = document.getElementById('enrollment-list-container');

    container.innerHTML = `
      <div class="table-card">
        <div class="table-responsive">
          <table class="data-table">
            <thead>
              <tr>
                <th>ID Inscripción</th>
                <th>Estudiante</th>
                <th>Carnet</th>
                <th>Nota / Calificación</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              ${enrollments.length === 0 ? '<tr><td colspan="5" style="text-align:center; color:var(--text-muted); padding:2rem;">No hay estudiantes inscritos en esta materia.</td></tr>' : ''}
              ${enrollments.map(item => {
                const en = item.enrollment || {};
                const stu = item.student || {};
                const gradeVal = en.grade !== null && en.grade !== undefined ? en.grade : null;
                const studentName = stu.name || 'Desconocido';
                const studentCarnet = stu.carnet || '-';

                return `
                  <tr>
                    <td>${en.id || '-'}</td>
                    <td><strong>${studentName}</strong></td>
                    <td>${studentCarnet}</td>
                    <td>
                      <span style="font-weight:700; font-size:1.05rem; color:${gradeVal !== null && gradeVal >= 70 ? 'var(--success)' : gradeVal !== null ? 'var(--danger)' : 'var(--text-muted)'};">
                        ${gradeVal !== null ? gradeVal : 'Sin Nota'}
                      </span>
                    </td>
                    <td>
                      <button class="btn btn-secondary btn-sm" onclick="showEditGradeModal(${en.id}, '${studentName.replace(/'/g, "\\'")}', ${gradeVal || 0}, '${(en.notes || '').replace(/'/g, "\\'")}')">
                        <i class="fa-solid fa-pen"></i> Asignar Nota
                      </button>
                    </td>
                  </tr>
                `;
              }).join('')}
            </tbody>
          </table>
        </div>
      </div>
    `;
  };

  document.getElementById('main-content').innerHTML = `
    <div class="panel-header">
      <div style="display:flex; gap:1rem; align-items:center;">
        <label class="form-label" style="margin:0;">Seleccionar Asignatura:</label>
        <select id="select-enrollment-subject" class="form-select" style="min-width:250px;">
          ${subjects.map(s => `<option value="${s.id}">${s.name} (${s.code||''})</option>`).join('')}
        </select>
      </div>
      <button class="btn btn-primary" onclick="showEnrollStudentModal()"><i class="fa-solid fa-user-plus"></i> Inscribir Estudiante</button>
    </div>

    <div id="enrollment-list-container"></div>
  `;

  const sel = document.getElementById('select-enrollment-subject');
  sel.addEventListener('change', (e) => renderSubjectEnrollments(e.target.value));
  if (selectedSubId) renderSubjectEnrollments(selectedSubId);
}

async function showEnrollStudentModal() {
  const [students, subjects] = await Promise.all([
    apiCall('/students').catch(() => []),
    apiCall('/subjects').catch(() => [])
  ]);

  const html = `
    <form id="form-enroll">
      <div class="form-group">
        <label class="form-label">Estudiante</label>
        <select id="enroll-student" class="form-select" required>
          ${students.map(s => `<option value="${s.id}">${s.name} (${s.carnet})</option>`).join('')}
        </select>
      </div>
      <div class="form-group">
        <label class="form-label">Asignatura</label>
        <select id="enroll-subject" class="form-select" required>
          ${subjects.map(s => `<option value="${s.id}">${s.name} (${s.code||''})</option>`).join('')}
        </select>
      </div>
      <div class="form-group">
        <label class="form-label">Nota Inicial (Opcional)</label>
        <input type="number" id="enroll-grade" class="form-input" min="0" max="100" placeholder="0-100">
      </div>
    </form>
  `;

  openModal('Inscribir Estudiante en Materia', html, async () => {
    const studentId = parseInt(document.getElementById('enroll-student').value);
    const subjectId = parseInt(document.getElementById('enroll-subject').value);
    const grVal = document.getElementById('enroll-grade').value;
    const grade = grVal ? parseFloat(grVal) : 0;

    await apiCall('/enrollments', {
      method: 'POST',
      body: JSON.stringify({ studentId, subjectId, grade, notes: '' })
    });
    showToast('Estudiante inscrito exitosamente');
    renderEnrollments();
  });
}

function showEditGradeModal(enrollmentId, studentName, currentGrade, currentNotes) {
  const html = `
    <form id="form-edit-grade">
      <div class="form-group">
        <label class="form-label">Estudiante</label>
        <input type="text" class="form-input" value="${studentName}" disabled>
      </div>
      <div class="form-group">
        <label class="form-label">Calificación (0 - 100)</label>
        <input type="number" id="grade-val" class="form-input" value="${currentGrade}" min="0" max="100" step="0.1" required>
      </div>
      <div class="form-group">
        <label class="form-label">Observaciones / Notas</label>
        <textarea id="grade-notes" class="form-textarea" rows="2">${currentNotes}</textarea>
      </div>
    </form>
  `;

  openModal(`Asignar Nota - ${studentName}`, html, async () => {
    const grade = parseFloat(document.getElementById('grade-val').value);
    const notes = document.getElementById('grade-notes').value.trim();

    await apiCall(`/enrollments/${enrollmentId}`, {
      method: 'PUT',
      body: JSON.stringify({ grade, notes })
    });
    showToast('Calificación guardada');
    renderEnrollments();
  });
}

// 6. ATTENDANCE
async function renderAttendance() {
  const subjects = await apiCall('/subjects').catch(() => []);
  let selectedSubId = subjects.length > 0 ? subjects[0].id : null;

  const renderSubjectAttendance = async (subId) => {
    if (!subId) return;
    const records = await apiCall(`/subjects/${subId}/attendance-records`).catch(() => []);
    const container = document.getElementById('attendance-list-container');

    container.innerHTML = `
      <div class="table-card">
        <div class="table-responsive">
          <table class="data-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>ID Estudiante</th>
                <th>Fecha</th>
                <th>Estado</th>
                <th>Observación</th>
              </tr>
            </thead>
            <tbody>
              ${records.length === 0 ? '<tr><td colspan="5" style="text-align:center; color:var(--text-muted);">No hay registros de asistencia para esta materia.</td></tr>' : ''}
              ${records.map(r => `
                <tr>
                  <td>${r.id}</td>
                  <td>Student #${r.studentId}</td>
                  <td>${formatDateSpanish(r.date)}</td>
                  <td><span class="badge badge-${(r.state||'').toLowerCase()}">${r.state}</span></td>
                  <td>${r.notes || '-'}</td>
                </tr>
              `).join('')}
            </tbody>
          </table>
        </div>
      </div>
    `;
  };

  document.getElementById('main-content').innerHTML = `
    <div class="panel-header">
      <div style="display:flex; gap:1rem; align-items:center;">
        <label class="form-label" style="margin:0;">Asignatura:</label>
        <select id="select-att-subject" class="form-select" style="min-width:250px;">
          ${subjects.map(s => `<option value="${s.id}">${s.name}</option>`).join('')}
        </select>
      </div>
      <button class="btn btn-primary" onclick="showRecordAttendanceModal()"><i class="fa-solid fa-plus"></i> Registrar Asistencia</button>
    </div>

    <div id="attendance-list-container"></div>
  `;

  const sel = document.getElementById('select-att-subject');
  sel.addEventListener('change', (e) => renderSubjectAttendance(e.target.value));
  if (selectedSubId) renderSubjectAttendance(selectedSubId);
}

async function showRecordAttendanceModal() {
  const [students, subjects] = await Promise.all([
    apiCall('/students').catch(() => []),
    apiCall('/subjects').catch(() => [])
  ]);

  const today = new Date().toISOString().split('T')[0];

  const html = `
    <form id="form-att">
      <div class="form-group">
        <label class="form-label">Estudiante</label>
        <select id="att-student" class="form-select" required>
          ${students.map(s => `<option value="${s.id}">${s.name} (${s.carnet})</option>`).join('')}
        </select>
      </div>
      <div class="form-group">
        <label class="form-label">Asignatura</label>
        <select id="att-subject" class="form-select" required>
          ${subjects.map(s => `<option value="${s.id}">${s.name}</option>`).join('')}
        </select>
      </div>
      <div class="form-group">
        <label class="form-label">Fecha</label>
        <input type="date" id="att-date" class="form-input" value="${today}" required>
      </div>
      <div class="form-group">
        <label class="form-label">Estado de Asistencia</label>
        <select id="att-state" class="form-select">
          <option value="PRESENTE">PRESENTE</option>
          <option value="AUSENTE">AUSENTE</option>
          <option value="TARDANZA">TARDANZA</option>
          <option value="JUSTIFICADO">JUSTIFICADO</option>
        </select>
      </div>
      <div class="form-group">
        <label class="form-label">Observaciones</label>
        <input type="text" id="att-notes" class="form-input" placeholder="Opcional">
      </div>
    </form>
  `;

  openModal('Registrar Asistencia', html, async () => {
    const studentId = parseInt(document.getElementById('att-student').value);
    const subjectId = parseInt(document.getElementById('att-subject').value);
    const date = document.getElementById('att-date').value;
    const state = document.getElementById('att-state').value;
    const notes = document.getElementById('att-notes').value.trim();

    await apiCall('/attendance-records', {
      method: 'POST',
      body: JSON.stringify({ studentId, subjectId, date, state, notes })
    });
    showToast('Asistencia registrada');
    renderAttendance();
  });
}

// 7. CONDUCT
async function renderConduct() {
  const subjects = await apiCall('/subjects').catch(() => []);
  let selectedSubId = subjects.length > 0 ? subjects[0].id : null;

  const renderSubjectConduct = async (subId) => {
    if (!subId) return;
    const records = await apiCall(`/subjects/${subId}/conduct-records`).catch(() => []);
    const container = document.getElementById('conduct-list-container');

    container.innerHTML = `
      <div class="table-card">
        <div class="table-responsive">
          <table class="data-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Estudiante ID</th>
                <th>Fecha</th>
                <th>Nivel / Falta</th>
                <th>Puntos Restados</th>
                <th>Detalles</th>
              </tr>
            </thead>
            <tbody>
              ${records.length === 0 ? '<tr><td colspan="6" style="text-align:center; color:var(--text-muted);">Sin reportes de conducta en esta materia.</td></tr>' : ''}
              ${records.map(r => `
                <tr>
                  <td>${r.id}</td>
                  <td>Student #${r.studentId}</td>
                  <td>${formatDateSpanish(r.date)}</td>
                  <td><span class="badge badge-tardanza">Conducta Nivel #${r.conductLevelId}</span></td>
                  <td><strong style="color:var(--danger);">-${r.pointsDeducted} pts</strong></td>
                  <td>${r.details || '-'}</td>
                </tr>
              `).join('')}
            </tbody>
          </table>
        </div>
      </div>
    `;
  };

  document.getElementById('main-content').innerHTML = `
    <div class="panel-header">
      <div style="display:flex; gap:1rem; align-items:center;">
        <label class="form-label" style="margin:0;">Asignatura:</label>
        <select id="select-cond-subject" class="form-select" style="min-width:250px;">
          ${subjects.map(s => `<option value="${s.id}">${s.name}</option>`).join('')}
        </select>
      </div>
      <button class="btn btn-primary" onclick="showRecordConductModal()"><i class="fa-solid fa-shield-cat"></i> Reportar Conducta</button>
    </div>

    <div id="conduct-list-container"></div>
  `;

  const sel = document.getElementById('select-cond-subject');
  sel.addEventListener('change', (e) => renderSubjectConduct(e.target.value));
  if (selectedSubId) renderSubjectConduct(selectedSubId);
}

async function showRecordConductModal() {
  const [students, subjects, levels] = await Promise.all([
    apiCall('/students').catch(() => []),
    apiCall('/subjects').catch(() => []),
    apiCall('/conduct-levels').catch(() => [])
  ]);

  const today = new Date().toISOString().split('T')[0];

  const html = `
    <form id="form-cond">
      <div class="form-group">
        <label class="form-label">Estudiante</label>
        <select id="cond-student" class="form-select" required>
          ${students.map(s => `<option value="${s.id}">${s.name} (${s.carnet})</option>`).join('')}
        </select>
      </div>
      <div class="form-group">
        <label class="form-label">Asignatura</label>
        <select id="cond-subject" class="form-select" required>
          ${subjects.map(s => `<option value="${s.id}">${s.name}</option>`).join('')}
        </select>
      </div>
      <div class="form-group">
        <label class="form-label">Nivel de Falta</label>
        <select id="cond-level" class="form-select" required>
          ${levels.map(l => `<option value="${l.id}">${l.name} (-${l.pointsDeducted} pts)</option>`).join('')}
        </select>
      </div>
      <div class="form-group">
        <label class="form-label">Fecha</label>
        <input type="date" id="cond-date" class="form-input" value="${today}" required>
      </div>
      <div class="form-group">
        <label class="form-label">Detalles de la Falta</label>
        <textarea id="cond-details" class="form-textarea" rows="2" placeholder="Describa el incidente..."></textarea>
      </div>
    </form>
  `;

  openModal('Reportar Incidencia de Conducta', html, async () => {
    const studentId = parseInt(document.getElementById('cond-student').value);
    const subjectId = parseInt(document.getElementById('cond-subject').value);
    const conductLevelId = parseInt(document.getElementById('cond-level').value);
    const date = document.getElementById('cond-date').value;
    const details = document.getElementById('cond-details').value.trim();

    const selectedLvl = levels.find(l => l.id === conductLevelId);
    const pointsDeducted = selectedLvl ? selectedLvl.pointsDeducted : 5;

    await apiCall('/conduct-records', {
      method: 'POST',
      body: JSON.stringify({ studentId, subjectId, conductLevelId, date, details, pointsDeducted })
    });
    showToast('Reporte de conducta registrado');
    renderConduct();
  });
}

// 8. FACILITIES
async function renderFacilities() {
  const facilities = await apiCall('/facilities').catch(() => []);

  document.getElementById('main-content').innerHTML = `
    <div class="panel-header">
      <h2>Instalaciones y Aulas</h2>
      <button class="btn btn-primary" onclick="showAddFacilityModal()"><i class="fa-solid fa-plus"></i> Nueva Instalación</button>
    </div>

    <div style="display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 1.5rem;">
      ${facilities.map(f => `
        <div class="table-card" style="padding:1.25rem;">
          <div style="display:flex; justify-content:space-between; align-items:center;">
            <h3 style="font-size:1.1rem; margin:0;">${f.name}</h3>
            <span class="badge badge-admin"><i class="fa-solid fa-users"></i> ${f.capacity || 'N/A'}</span>
          </div>
          <p style="color:var(--text-muted); font-size:0.85rem; margin: 0.5rem 0 1rem 0;">
            <i class="fa-solid fa-location-dot"></i> ${f.location || 'Sin ubicación'}
          </p>
          <div style="display:flex; gap:0.5rem;">
            <button class="btn btn-danger btn-sm" onclick="deleteFacility(${f.id}, '${f.name}')"><i class="fa-solid fa-trash"></i> Eliminar</button>
          </div>
        </div>
      `).join('')}
    </div>
  `;
}

function showAddFacilityModal() {
  const html = `
    <form id="form-fac">
      <div class="form-group">
        <label class="form-label">Nombre de la Instalación</label>
        <input type="text" id="fac-name" class="form-input" required placeholder="Ej: Laboratorio 1, Aula A-102">
      </div>
      <div class="form-group">
        <label class="form-label">Ubicación</label>
        <input type="text" id="fac-loc" class="form-input" placeholder="Ej: Edificio Principal 2do Piso">
      </div>
      <div class="form-group">
        <label class="form-label">Capacidad de Estudiantes</label>
        <input type="number" id="fac-cap" class="form-input" value="30" min="1">
      </div>
      <div class="form-group">
        <label class="form-label">Descripción</label>
        <textarea id="fac-desc" class="form-textarea" rows="2"></textarea>
      </div>
    </form>
  `;

  openModal('Agregar Instalación / Aula', html, async () => {
    const name = document.getElementById('fac-name').value.trim();
    const location = document.getElementById('fac-loc').value.trim();
    const cap = document.getElementById('fac-cap').value;
    const capacity = cap ? parseInt(cap) : 30;
    const description = document.getElementById('fac-desc').value.trim();

    if (!name) throw new Error('El nombre es requerido');

    await apiCall('/facilities', {
      method: 'POST',
      body: JSON.stringify({ name, location, capacity, description })
    });
    showToast('Instalación agregada');
    renderFacilities();
  });
}

async function deleteFacility(id, name) {
  if (confirm(`¿Eliminar la instalación "${name}"?`)) {
    try {
      await apiCall(`/facilities/${id}`, { method: 'DELETE' });
      showToast('Instalación eliminada');
      renderFacilities();
    } catch (e) {
      showToast(e.message, 'error');
    }
  }
}

// 9. ACADEMIC CALENDAR
async function renderCalendar() {
  const [periods, nonSchoolDays] = await Promise.all([
    apiCall('/academic-periods').catch(() => []),
    apiCall('/non-school-days').catch(() => [])
  ]);

  document.getElementById('main-content').innerHTML = `
    <div style="display:grid; grid-template-columns: repeat(auto-fit, minmax(320px, 1fr)); gap:1.5rem;">
      <div class="table-card" style="padding:1.5rem;">
        <h3 style="margin-bottom:1rem;"><i class="fa-solid fa-calendar"></i> Periodos Académicos</h3>
        <table class="data-table">
          <thead>
            <tr>
              <th>Periodo</th>
              <th>Inicio</th>
              <th>Fin</th>
            </tr>
          </thead>
          <tbody>
            ${periods.length === 0 ? '<tr><td colspan="3" style="text-align:center; color:var(--text-muted); padding:1.5rem;">No hay periodos registrados.</td></tr>' : ''}
            ${periods.map(p => `
              <tr>
                <td><strong>Periodo ${p.period}</strong></td>
                <td>${formatDateSpanish(p.startDate)}</td>
                <td>${formatDateSpanish(p.endDate)}</td>
              </tr>
            `).join('')}
          </tbody>
        </table>
      </div>

      <div class="table-card" style="padding:1.5rem;">
        <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:1rem;">
          <h3 style="margin:0;"><i class="fa-solid fa-calendar-xmark" style="color:var(--danger);"></i> Días No Lectivos</h3>
          <button class="btn btn-primary btn-sm" onclick="showAddNonSchoolDayModal()"><i class="fa-solid fa-plus"></i> Agregar</button>
        </div>
        <table class="data-table">
          <thead>
            <tr>
              <th>Fecha</th>
              <th>Motivo</th>
              <th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            ${nonSchoolDays.length === 0 ? '<tr><td colspan="3" style="text-align:center; color:var(--text-muted); padding:1.5rem;">No hay días no lectivos.</td></tr>' : ''}
            ${nonSchoolDays.map(d => `
              <tr>
                <td><strong>${formatDateSpanish(d.day || d.date)}</strong></td>
                <td>${d.reason || 'Asueto / Feriado'}</td>
                <td>
                  <button class="btn btn-danger btn-sm" onclick="deleteNonSchoolDay(${d.id})" title="Eliminar"><i class="fa-solid fa-trash"></i></button>
                </td>
              </tr>
            `).join('')}
          </tbody>
        </table>
      </div>
    </div>
  `;
}

function showAddNonSchoolDayModal() {
  const today = new Date().toISOString().split('T')[0];
  const html = `
    <form id="form-nsd">
      <div class="form-group">
        <label class="form-label">Fecha del Día No Lectivo</label>
        <input type="date" id="nsd-date" class="form-input" value="${today}" required>
      </div>
      <div class="form-group">
        <label class="form-label">Motivo / Descripción</label>
        <input type="text" id="nsd-reason" class="form-input" placeholder="Ej: Feriado Nacional, Día del Maestro" required>
      </div>
    </form>
  `;

  openModal('Agregar Día No Lectivo', html, async () => {
    const day = document.getElementById('nsd-date').value;
    const reason = document.getElementById('nsd-reason').value.trim();

    if (!day || !reason) throw new Error('Fecha y motivo son obligatorios');

    await apiCall('/non-school-days', {
      method: 'POST',
      body: JSON.stringify({ day, reason })
    });
    showToast('Día no lectivo agregado');
    renderCalendar();
  });
}

async function deleteNonSchoolDay(id) {
  if (confirm('¿Eliminar este día no lectivo?')) {
    try {
      await apiCall(`/non-school-days/${id}`, { method: 'DELETE' });
      showToast('Día eliminado');
      renderCalendar();
    } catch (e) {
      showToast(e.message, 'error');
    }
  }
}

// 10. NOTIFICATIONS MANAGEMENT
async function renderNotifications() {
  const notifications = await apiCall('/notifications');

  const iconBadges = {
    GENERAL: '<span class="badge badge-admin"><i class="fa-solid fa-bullhorn"></i> GENERAL</span>',
    ACADEMIC: '<span class="badge badge-professor"><i class="fa-solid fa-graduation-cap"></i> ACADÉMICO</span>',
    ALERT: '<span class="badge badge-ausente"><i class="fa-solid fa-triangle-exclamation"></i> ALERTA</span>',
    SYSTEM: '<span class="badge badge-secondary"><i class="fa-solid fa-gear"></i> SISTEMA</span>',
    SUBJECT: '<span class="badge badge-student"><i class="fa-solid fa-book"></i> MATERIA</span>'
  };

  document.getElementById('main-content').innerHTML = `
    <div class="panel-header">
      <div class="search-box">
        <i class="fa-solid fa-magnifying-glass search-icon"></i>
        <input type="text" id="search-notif" class="form-input" placeholder="Buscar notificaciones..." oninput="filterNotificationsTable()">
      </div>
      <button class="btn btn-primary" onclick="showAddNotificationModal()"><i class="fa-solid fa-bullhorn"></i> Nueva Notificación</button>
    </div>

    <div class="table-card">
      <div class="table-responsive">
        <table class="data-table" id="notif-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Icono</th>
              <th>Título</th>
              <th>Mensaje</th>
              <th>Destino</th>
              <th>Remitente</th>
              <th>Fecha / Hora</th>
              <th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            ${notifications.length === 0 ? '<tr><td colspan="8" style="text-align:center; color:var(--text-muted); padding:2rem;">No hay notificaciones registradas.</td></tr>' : ''}
            ${notifications.map(n => `
              <tr data-notif-str="${(n.title + ' ' + n.message + ' ' + n.senderName + ' ' + (n.iconType||'')).toLowerCase()}">
                <td>${n.id}</td>
                <td>${iconBadges[n.iconType] || iconBadges.GENERAL}</td>
                <td><strong>${n.title}</strong></td>
                <td>${n.message}</td>
                <td><span class="badge badge-admin">${n.targetType}</span> ${n.targetValue ? '('+n.targetValue+')' : ''}</td>
                <td>${n.senderName}</td>
                <td>${n.timestamp}</td>
                <td>
                  <button class="btn btn-danger btn-sm" onclick="deleteNotification(${n.id})" title="Eliminar"><i class="fa-solid fa-trash"></i> Eliminar</button>
                </td>
              </tr>
            `).join('')}
          </tbody>
        </table>
      </div>
    </div>
  `;
}

function filterNotificationsTable() {
  const query = document.getElementById('search-notif').value.toLowerCase();
  document.querySelectorAll('#notif-table tbody tr').forEach(row => {
    const match = row.getAttribute('data-notif-str').includes(query);
    row.style.display = match ? '' : 'none';
  });
}

function showAddNotificationModal() {
  const html = `
    <form id="form-notif" autocomplete="off">
      <div class="form-group">
        <label class="form-label">Título del Aviso</label>
        <input type="text" id="notif-title" class="form-input" required placeholder="Ej: Aviso importante de inicio de clases">
      </div>
      <div class="form-group">
        <label class="form-label">Mensaje</label>
        <textarea id="notif-message" class="form-textarea" rows="3" required placeholder="Escriba el contenido de la notificación..."></textarea>
      </div>
      <div class="form-group">
        <label class="form-label">Tipo de Icono</label>
        <select id="notif-icon-type" class="form-select">
          <option value="GENERAL">📢 GENERAL (Aviso General)</option>
          <option value="ACADEMIC">🎓 ACADEMIC (Académico / Calificaciones)</option>
          <option value="ALERT">🚨 ALERT (Alerta / Importante)</option>
          <option value="SYSTEM">⚙️ SYSTEM (Sistema / Mantenimiento)</option>
          <option value="SUBJECT">📖 SUBJECT (Materia / Asignatura)</option>
        </select>
      </div>
      <div class="form-group">
        <label class="form-label">Tipo de Destino</label>
        <select id="notif-target" class="form-select">
          <option value="ALL">ALL (Todos)</option>
          <option value="GLOBAL">GLOBAL (Institucional)</option>
          <option value="STUDENT">STUDENT (Estudiantes)</option>
          <option value="PROFESSOR">PROFESSOR (Profesores)</option>
        </select>
      </div>
    </form>
  `;

  openModal('Enviar Nueva Notificación', html, async () => {
    const title = document.getElementById('notif-title').value.trim();
    const message = document.getElementById('notif-message').value.trim();
    const iconType = document.getElementById('notif-icon-type').value;
    const targetType = document.getElementById('notif-target').value;
    const senderName = currentUser ? (currentUser.name || currentUser.carnet) : 'Administrador';

    if (!title || !message) throw new Error('Título y mensaje son obligatorios');

    await apiCall('/notifications', {
      method: 'POST',
      body: JSON.stringify({
        title,
        message,
        targetType,
        targetValue: null,
        senderName,
        timestamp: '',
        iconType: iconType
      })
    });
    showToast('Notificación enviada exitosamente');
    renderNotifications();
  });
}


async function deleteNotification(id) {
  if (confirm('¿Estás seguro de eliminar esta notificación?')) {
    try {
      await apiCall(`/notifications/${id}`, { method: 'DELETE' });
      showToast('Notificación eliminada');
      renderNotifications();
    } catch (e) {
      showToast(e.message, 'error');
    }
  }
}