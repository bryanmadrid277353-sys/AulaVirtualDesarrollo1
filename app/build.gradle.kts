import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}

fun normalizeUrl(url: String): String {
    val trimmed = url.trim()
    return if (trimmed.endsWith("/")) trimmed else "$trimmed/"
}

// Lógica para variante Debug
val validDebugModes = setOf("emulador", "lan", "cloudflare", "tailscale")
val rawMode = localProperties.getProperty("backend.mode")?.trim()?.lowercase()

val (debugBackendMode, debugBaseUrl) = if (rawMode.isNullOrEmpty()) {
    // Modo retrocompatible: si existe backend.url.debug, la usamos directamente
    val legacyDebugUrl = localProperties.getProperty("backend.url.debug")?.trim()
    if (!legacyDebugUrl.isNullOrEmpty()) {
        if (!legacyDebugUrl.startsWith("http://") && !legacyDebugUrl.startsWith("https://")) {
            throw GradleException("Error en local.properties: 'backend.url.debug' debe iniciar con 'http://' o 'https://'. Valor ingresado: '$legacyDebugUrl'")
        }
        "debug_custom" to normalizeUrl(legacyDebugUrl)
    } else {
        // Por defecto: emulador
        "emulador" to normalizeUrl(localProperties.getProperty("backend.url.emulador")?.trim() ?: "http://10.0.2.2:8000/")
    }
} else {
    if (rawMode !in validDebugModes) {
        throw GradleException("Error en local.properties: 'backend.mode' no es válido. Valor ingresado: '$rawMode'. Modos permitidos: ${validDebugModes.joinToString(", ")}")
    }
    val defaultUrl = when (rawMode) {
        "emulador" -> "http://10.0.2.2:8000/"
        "lan" -> "http://192.168.1.35:8000/"
        "cloudflare" -> "https://api.tudominio.com/"
        "tailscale" -> "http://nombre-pc.tailXXXX.ts.net:8000/"
        else -> "http://10.0.2.2:8000/"
    }
    val configuredUrl = localProperties.getProperty("backend.url.$rawMode")?.trim()
    val urlToUse = if (!configuredUrl.isNullOrEmpty()) configuredUrl else defaultUrl
    if (!urlToUse.startsWith("http://") && !urlToUse.startsWith("https://")) {
        throw GradleException("Error en local.properties: 'backend.url.$rawMode' debe iniciar con 'http://' o 'https://'. Valor ingresado: '$urlToUse'")
    }
    rawMode to normalizeUrl(urlToUse)
}

// Lógica para variante Release
val rawReleaseUrl = localProperties.getProperty("backend.url.release")?.trim() ?: "https://api.tudominio.com/"
if (!rawReleaseUrl.startsWith("https://")) {
    throw GradleException("Error en local.properties: En la variante 'release', la URL debe ser HTTPS (iniciar con 'https://'). Valor ingresado: '$rawReleaseUrl'")
}
val releaseBaseUrl = normalizeUrl(rawReleaseUrl)

android {
    namespace = "com.aula.virtual"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.aula.virtual"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            buildConfigField("String", "BASE_URL", "\"$debugBaseUrl\"")
            buildConfigField("String", "BACKEND_MODE", "\"$debugBackendMode\"")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            buildConfigField("String", "BASE_URL", "\"$releaseBaseUrl\"")
            buildConfigField("String", "BACKEND_MODE", "\"release\"")
        }
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)

    // Navigation
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)

    // Lifecycle
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)

    // Retrofit & Network
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)

    // Room Database
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)

    implementation(libs.androidx.biometric)
    implementation(libs.androidx.security.crypto)

    // Firebase Cloud Messaging (FCM)
    implementation(platform("com.google.firebase:firebase-bom:33.8.0"))
    implementation("com.google.firebase:firebase-messaging")

    // Apache POI for Excel (.xlsx) manipulation
    implementation("org.apache.poi:poi:5.2.3")
    implementation("org.apache.poi:poi-ooxml:5.2.3")

    // WorkManager
    implementation("androidx.work:work-runtime:2.9.1")

    // Password Hashing (BCrypt)
    implementation("at.favre.lib:bcrypt:0.10.2")

    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}

