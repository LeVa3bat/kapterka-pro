import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

val hasLocalDebugKeystore = rootProject.file("debug.keystore").exists()

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.kapterka.jmwqve"
    minSdk = 24
    targetSdk = 34
    // Stable defaults: 3.7.0 / build 35. CI may override only for side-by-side test builds.
    versionCode = System.getenv("NEXT_SAFE_VERSION_CODE")?.toIntOrNull() ?: 35
    versionName = System.getenv("NEXT_SAFE_VERSION_NAME")?.takeIf { it.isNotBlank() } ?: "3.7.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    // Payment/license client talks only to our backend; no secret ever enters the APK.
    val paymentApiUrl = System.getenv("PAYMENT_API_URL")?.takeIf { it.isNotBlank() }
      ?: "https://kapterka-api.alex-666-881.workers.dev"
    buildConfigField("String", "PAYMENT_API_URL", "\"$paymentApiUrl\"")
    buildConfigField("String", "PAYMENT_CALLBACK_SCHEME", "\"kapterka\"")
    buildConfigField("boolean", "IS_NEXT_SAFE_TEST", "false")
    manifestPlaceholders["paymentScheme"] = "kapterka"
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH")?.takeIf { it.isNotBlank() }
      if (keystorePath != null) {
        storeFile = file(keystorePath)
      }
      storePassword = System.getenv("STORE_PASSWORD")
      // Never fall back to a generic upload/debug alias for a production APK.
      // Release workflows must provide the exact recovered historical signer.
      keyAlias = System.getenv("KEY_ALIAS")?.takeIf { it.isNotBlank() }
      keyPassword = System.getenv("KEY_PASSWORD")
      enableV1Signing = true
      enableV2Signing = true
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
      enableV1Signing = true
      enableV2Signing = true
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      // R8: shrink + obfuscate release code so the APK is harder to reverse-engineer.
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }

    // Never published automatically. This variant exists only for the gated manual
    // release-candidate workflow and uses the recovered historical signer.
    create("nextSafeRelease") {
      initWith(getByName("release"))
      signingConfig = signingConfigs.getByName("release")
      matchingFallbacks += listOf("release")
      isDebuggable = false
    }

    // Side-by-side test build. It has a different applicationId and app label, so it
    // cannot replace or modify the installed production 3.4.9 application.
    create("nextSafeTest") {
      initWith(getByName("debug"))
      applicationIdSuffix = ".nextsafe"
      versionNameSuffix = "-nextsafe"
      matchingFallbacks += listOf("debug")
      if (hasLocalDebugKeystore) signingConfig = signingConfigs.getByName("debugConfig")
      resValue("string", "app_name", "Каптёрка PRO NEXT-SAFE")
      buildConfigField("String", "PAYMENT_CALLBACK_SCHEME", "\"kapterka-nextsafe\"")
      buildConfigField("boolean", "IS_NEXT_SAFE_TEST", "true")
      manifestPlaceholders["paymentScheme"] = "kapterka-nextsafe"
    }

    // Local debug.keystore is optional (never committed); without it the
    // standard Android debug key is used.
    debug { if (hasLocalDebugKeystore) signingConfig = signingConfigs.getByName("debugConfig") }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
    resValues = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  ignoreList.add("FIREBASE_APPCHECK_DEBUG_TOKEN")
  // PAYMENT_API_URL is supplied explicitly through BuildConfig from the environment.
  // Do not let the Secrets plugin generate/override this field from .env files.
  ignoreList.add("PAYMENT_API_URL")
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  // QR: code generation (zxing core) and the camera scanner screen.
  implementation("com.journeyapps:zxing-android-embedded:4.3.0")
  implementation("com.google.zxing:core:3.5.3")
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  // implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  // implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  implementation(libs.firebase.ai)
  // Firestore database for online sync:
  implementation(libs.firebase.firestore)

  // Anonymous Firebase Auth: every device gets its own identity for unit membership.
  implementation(libs.firebase.auth)
  // Google Sign-In via Credential Manager (not used):
  // implementation(libs.androidx.credentials)
  // implementation(libs.androidx.credentials.play.services.auth)
  // implementation(libs.googleid)
  implementation(libs.firebase.appcheck.recaptcha)
  debugImplementation(libs.firebase.appcheck.debug)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}
