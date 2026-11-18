plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

android {
    namespace = "com.fintrack.app"
    //noinspection GradleDependency
    compileSdk = 36

    defaultConfig {
        applicationId = "com.fintrack.app"
        minSdk = 26
        targetSdk = 36
        // CI passes these from the git tag (see .github/workflows/release.yml); local builds use the defaults
        versionCode = (project.findProperty("VERSION_CODE") as String?)?.toInt() ?: 1
        versionName = (project.findProperty("VERSION_NAME") as String?) ?: "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    signingConfigs {
        if (
            project.hasProperty("RELEASE_STORE_FILE") &&
            project.hasProperty("RELEASE_STORE_PASSWORD") &&
            project.hasProperty("RELEASE_KEY_ALIAS") &&
            project.hasProperty("RELEASE_KEY_PASSWORD")
        ) {
            create("release") {
                storeFile = file(project.property("RELEASE_STORE_FILE") as String)
                storePassword = project.property("RELEASE_STORE_PASSWORD") as String
                keyAlias = project.property("RELEASE_KEY_ALIAS") as String
                keyPassword = project.property("RELEASE_KEY_PASSWORD") as String
            }
        }
    }


    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            signingConfigs.findByName("release")?.let {
                signingConfig = it
            }
        }
    }

    flavorDimensions += listOf("distribution")
    
    productFlavors {
        create("store") {
            dimension = "distribution"
            isDefault = true
            // Google Services enabled for store variant
        }
        
        create("personal") {
            dimension = "distribution"
            // applicationIdSuffix = ".personal" // Commented out to match google-services.json for Google Sign-In
            // Note: This prevents side-by-side installation with 'store' flavor
        }
    }
    
    // Google Services configuration files are located in:
    // - src/storeRelease/google-services.json (for storeRelease builds - production)
    // - src/personalRelease/google-services.json (for personalRelease builds - production)
    // - src/storeDebug/google-services.json (for storeDebug builds - development)
    // - src/personalDebug/google-services.json (for personalDebug builds - development)
}

dependencies {
    // --- Core & Compose ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.material)


    // --- Navigation ---
    implementation(libs.androidx.navigation.compose)

    // --- Room (Local Database) ---
    implementation(libs.androidx.room.runtime)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.crashlytics)
    implementation(libs.litert.support.api)
    implementation(libs.androidx.compose.animation.core)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)

    // --- Hilt (Dependency Injection) ---
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // --- ViewModel ---
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Material Icons
    implementation(libs.androidx.compose.material.icons.extended)

    // Coil
    implementation(libs.coil)
    implementation(libs.coil.compose)

    // Vico (Charts)
    implementation(libs.vico.compose)
    implementation(libs.vico.compose.m3)
    implementation(libs.vico.core)

    // Google sign in
    implementation(libs.google.play.services.auth)

    // Biometrics
    implementation(libs.androidx.biometric)

    // Datastore
    implementation(libs.androidx.datastore.preferences)

    // --- WorkManager ---
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // Desugaring
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // --- Testing (Standard) ---
    testImplementation(libs.junit)
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("androidx.work:work-testing:2.9.0")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    releaseImplementation("androidx.profileinstaller:profileinstaller:1.3.1")
}


