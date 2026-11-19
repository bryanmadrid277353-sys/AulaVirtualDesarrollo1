# 14. Glosario de Términos

[⬅️ Anterior](13_observaciones_y_mejoras.md) | [📑 Índice](00_INDICE.md)

Este glosario define los términos técnicos, siglas y conceptos del dominio académico utilizados en la documentación y código fuente del proyecto **Aula Virtual**.

---

## Tabla de Contenido
- [14.1 Términos del Dominio Académico](#141-términos-del-dominio-académico)
- [14.2 Términos Técnicos y de Desarrollo](#142-términos-técnicos-y-de-desarrollo)

---

## 14.1 Términos del Dominio Académico

- **Academic Period (Período Académico)**: Cada uno de los 5 lapsos de evaluación en los que se divide el año lectivo institucional para asentar calificaciones y conducta.
- **Attendance Record (Registro de Asistencia)**: Ficha diaria por estudiante y materia que especifica uno de los 4 estados de presencia: Presente (`PRESENT`), Ausente (`ABSENT`), Llegada Tardía (`LATE`) o Permiso Justificado (`PERMIT`).
- **Carnet**: Código único alfanumérico asignado institucionalmente a un usuario o estudiante para su identificación (ej. `ADM001`, `PROF001`, `ENC001`, `EST001`).
- **Conduct Record (Evaluación de Conducta)**: Registro cuantitativo basado en 5 criterios (Atención/Participación, Normas/Rutinas, Autorregulación/Esfuerzo, Respeto/Colaboración, Responsabilidad) calificados de 1 a 5, con observaciones del docente.
- **Encargado**: Usuario registrado en el sistema que representa legalmente a uno o más estudiantes menores de edad (Padre, Madre o Tutor).
- **Enrollment (Inscripción)**: Relación que vincula a un estudiante menor de edad con una materia específica y almacena sus calificaciones acumuladas por período.
- **Facility (Instalación)**: Espacio físico de la institución educativa (Aula, Laboratorio, Cancha) gestionado para asignación de horarios de clase.

[⬆️ Volver al inicio del documento](#14-glosario-de-términos)

---

## 14.2 Términos Técnicos y de Desarrollo

- **AES-256 GCM**: Algoritmo de cifrado simétrico avanzado utilizado por `EncryptedSharedPreferences` para proteger las credenciales guardadas en el dispositivo Android.
- **BCrypt**: Función de hashing de contraseñas adaptable basada en el cifrado Blowfish, utilizada para almacenar las claves encriptadas en MySQL y Android.
- **BiometricPrompt**: API nativa de Android que proporciona un cuadro de diálogo del sistema operativo para autenticar usuarios mediante huella dactilar o reconocimiento facial.
- **Cloudflare Tunnel (`cloudflared`)**: Herramienta de red que crea un túnel seguro de salida para exponer el servidor FastAPI local a Internet mediante una URL HTTPS sin necesidad de abrir puertos en el router.
- **DAO (Data Access Object)**: Interfaz annotated de Room en Android que define los métodos de consulta SQL (`@Query`, `@Insert`, `@Update`, `@Delete`) para interactuar con SQLite.
- **FastAPI**: Framework web moderno y de alto rendimiento en Python para la creación de APIs REST con tipado estático y generación automática de OpenAPI.
- **FCM (Firebase Cloud Messaging)**: Servicio de mensajería multiplataforma de Google para enviar notificaciones push gratuitas y confiables a dispositivos Android.
- **MVVM (Model-View-ViewModel)**: Patrón de arquitectura de software que separa la lógica de interfaz de usuario (View) de la lógica de negocio (Model) utilizando un intermediario reactivo (ViewModel).
- **POI (Apache POI)**: Librería Java de código abierto empleada en el proyecto para la lectura, escritura y generación de hojas de cálculo de Microsoft Excel en formato `.xlsx`.
- **Retrofit**: Cliente HTTP de tipo seguro para Android y Java desarrollado por Square, que convierte interfaces Java en llamadas de red REST.
- **Room Database**: Librería de abstracción de persistencia de Android Jetpack sobre SQLite que proporciona acceso fluido a la base de datos con comprobación de consultas en tiempo de compilación.
- **SQLAlchemy**: Kit de herramientas SQL y Mapeador Objeto-Relacional (ORM) de código abierto para el lenguaje de programación Python.

[⬆️ Volver al inicio del documento](#14-glosario-de-términos)

---
[⬅️ Anterior](13_observaciones_y_mejoras.md) | [📑 Índice](00_INDICE.md)
