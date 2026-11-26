# Guía del Sistema de Colores por Tipo de Registro - Aula Virtual

Esta guía define el sistema visual de **"Color por Tipo de Registro"** aplicado en la plataforma Aula Virtual. Su propósito es estandarizar la identificación visual de entidades en toda la aplicación de manera consistente, accesible y reutilizable.

---

## 1. Tabla de Tipos de Registro y Colores Constantes

Los colores de registro son **fijos e inmutables**; no varían si la aplicación cambia entre Modo Claro y Modo Oscuro:

| Tipo de Registro | Token de Color | Hex Constante | Versión Suave (`_soft`) | Texto sobre Acento (`_on`) |
|---|---|---|---|---|
| **Encargado** | `entity_encargado` | `#F97316` (Naranja) | `#26F97316` (~15% opacidad) | `#FFFFFF` |
| **Alumno / Estudiante** | `entity_alumno` | `#2563EB` (Azul) | `#262563EB` (~15% opacidad) | `#FFFFFF` |
| **Profesor / Docente** | `entity_profesor` | `#10B981` (Verde Esmeralda) | `#2610B981` (~15% opacidad) | `#FFFFFF` |
| **Materia** | `entity_materia` | `#06B6D4` (Cian) | `#2606B6D4` (~15% opacidad) | `#FFFFFF` |
| **Instalación** | `entity_instalacion` | `#F59E0B` (Amarillo / Dorado) | `#26F59E0B` (~15% opacidad) | `#FFFFFF` |
| **Administrador** | `entity_administrador` | `#EF4444` (Rojo) | `#26EF4444` (~15% opacidad) | `#FFFFFF` |

---

## 2. Reglas de Diseño Obligatorias

1. **Acción define el color:** El color de un elemento interactivo (botón, diálogo, chip) está determinado por la **entidad sobre la que actúa**, NO por la pantalla contenedora.
   * *Ejemplo:* En la pantalla de un Encargado (naranja), el botón "+ Añadir alumno a cargo" es **Azul** porque su acción crea un Alumno.
2. **Acentos pequeños, superficies neutras:** Solo se colorean etiquetas, subtítulos, botones principales/outlined de entidad, avatares, chips/insignias y bordes de foco. Los fondos de pantalla, tarjetas y textos de contenido principal se mantienen neutros.
3. **Doble accesibilidad:** El color NUNCA es el único indicador. Siempre debe ir acompañado de texto explicativo o ícono identificador.
4. **Contraste adecuado:** Todo texto de acento debe cumplir un contraste mínimo de 4.5:1 sobre su fondo, y elementos de UI al menos 3:1.
5. **Diferenciabilidad:** Los tonos de Encargado (Naranja `#F97316`) e Instalación (Amarillo/Ámbar `#F59E0B`) se mantienen claramente distinguidos en matiz y luminosidad.
6. **Pantallas neutras:** Las secciones globales de navegación (Bottom Navigation, Login, Mensajes Directos, Ajustes generales) conservan el azul de marca neutro (`colorPrimary`).

---

## 3. Catálogo de Componentes Estándar

### Estilos XML Reutilizables

* **Botón Principal de Entidad (`Widget.Aula.Button.Entity`):**
  Botón relleno que toma el color de acento de la entidad.
  ```xml
  <com.google.android.material.button.MaterialButton
      style="@style/Widget.Aula.Button.Entity"
      android:layout_width="match_parent"
      android:layout_height="52dp"
      android:text="+ Añadir estudiante" />
  ```

* **Botón Outlined de Entidad (`Widget.Aula.Button.EntityOutlined`):**
  Botón con borde y texto del color de la entidad.
  ```xml
  <com.google.android.material.button.MaterialButton
      style="@style/Widget.Aula.Button.EntityOutlined"
      android:layout_width="wrap_content"
      android:layout_height="44dp"
      android:text="Detalle" />
  ```

* **Subtítulo / Etiqueta de Sección (`TextAppearance.Aula.EntitySubtitle`):**
  ```xml
  <TextView
      style="@style/TextAppearance.Aula.EntitySubtitle"
      android:layout_width="wrap_content"
      android:layout_height="wrap_content"
      android:text="Encargado de Familia" />
  ```

* **Chip / Insignia de Entidad (`Widget.Aula.EntityChip`):**
  ```xml
  <com.google.android.material.chip.Chip
      style="@style/Widget.Aula.EntityChip"
      android:layout_width="wrap_content"
      android:layout_height="wrap_content"
      android:text="• Regular" />
  ```

### Uso programático en Java con `EntityType` y `EntityTheme`

```java
// Obtener tipo de entidad a partir del rol
EntityType type = EntityType.fromRole(user.role);

// Aplicar tema a un botón
EntityTheme.applyToButton(binding.btnAdd, type, false); // false = relleno, true = outlined

// Aplicar tema a un avatar
EntityTheme.applyToAvatar(binding.layoutAvatar, binding.tvInitials, type);

// Aplicar tema a un Chip o Badge
EntityTheme.applyToChip(binding.tvBadge, type);
```

---

## 4. Paso a Paso para Agregar un Nuevo Tipo de Registro

1. **Definir los tokens de color:**
   * En `res/values/colors.xml` y `res/values-night/colors.xml`, agrega:
     ```xml
     <color name="entity_nuevo">#HEX</color>
     <color name="entity_nuevo_soft">#26HEX</color>
     <color name="entity_nuevo_on">#FFFFFF</color>
     ```
2. **Crear el Overlay de Tema:**
   * En `res/values/themes.xml`, añade:
     ```xml
     <style name="ThemeOverlay.Aula.Nuevo" parent="">
         <item name="entityAccent">@color/entity_nuevo</item>
         <item name="entityAccentSoft">@color/entity_nuevo_soft</item>
         <item name="entityOnAccent">@color/entity_nuevo_on</item>
     </style>
     ```
3. **Actualizar el enum `EntityType`:**
   * Agrega el valor al enum en `EntityType.java`:
     ```java
     NUEVO("Nuevo Registro", R.drawable.ic_icon, R.style.ThemeOverlay_Aula_Nuevo, R.color.entity_nuevo, R.color.entity_nuevo_soft, R.color.entity_nuevo_on);
     ```

---

## 5. Lista de Verificación (Checklist) para Nuevas Pantallas

- [ ] ¿El encabezado o badge indica claramente el tipo de registro con su color estándar?
- [ ] ¿El botón de acción principal utiliza el color de la entidad sobre la que actúa?
- [ ] ¿Los colores hardcodeados (`#RRGGBB`) han sido completamente reemplazados por tokens `entity_*` o `EntityType`?
- [ ] ¿Los campos de entrada y texto secundario mantienen contraste legible tanto en modo claro como en modo oscuro?
- [ ] ¿Las acciones secundarias utilizan el estilo Outlined o Soft de la entidad?
