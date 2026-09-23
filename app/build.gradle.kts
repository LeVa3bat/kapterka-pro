plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.skladpro"
    minSdk = 24
    targetSdk = 34
    // Standalone universal product. It never updates/replaces "Каптёрка ПРО".
    versionCode = 9
    versionName = "0.8.0-alpha9"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    // Future payment client talks only to our backend; YooKassa secret never enters the APK.
    val paymentApiUrl = System.getenv("SKLADPRO_API_URL")?.trim().orEmpty() // Dedicated Sklad PRO Worker only.
    buildConfigField("String", "PAYMENT_API_URL", "\"$paymentApiUrl\"")
    buildConfigField("String", "PAYMENT_CALLBACK_SCHEME", "\"skladpro\"")
    buildConfigField("boolean", "IS_NEXT_SAFE_TEST", "false")
    buildConfigField("boolean", "IS_UNIVERSAL_APP", "true")
    manifestPlaceholders["paymentScheme"] = "skladpro"
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("SKLADPRO_KEYSTORE_PATH")?.takeIf { it.isNotBlank() }
      if (keystorePath != null) {
        storeFile = file(keystorePath)
      }
      storePassword = System.getenv("SKLADPRO_STORE_PASSWORD")
      // Never fall back to a generic upload/debug alias for a production APK.
      // Release workflows must provide the exact recovered historical signer.
      keyAlias = System.getenv("SKLADPRO_KEY_ALIAS")?.takeIf { it.isNotBlank() }
      keyPassword = System.getenv("SKLADPRO_KEY_PASSWORD")
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
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      // A future public release gets its own Sklad PRO signer.
      signingConfig = signingConfigs.getByName("release")
    }

    debug {
      signingConfig = signingConfigs.getByName("debugConfig")
      resValue("string", "app_name", "Склад ПРО • Alpha")
    }
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

  // Sklad PRO uses Firebase Email/Password authentication.
  implementation(libs.firebase.auth)
  // Google Sign-In is intentionally not enabled.
  // implementation(libs.androidx.credentials)
  // implementation(libs.androidx.credentials.play.services)
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
