plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
    id("app.cash.paparazzi") version "2.0.0-alpha05"
}

android {
    namespace = "com.polishmediahub.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.polishmediahub.app"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "com.polishmediahub.app.HiltTestRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.runtime)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.tooling.preview)

    implementation(libs.androidx.tv.provider)

    implementation(libs.androidx.navigation.compose)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.session)

    implementation(libs.coil.compose)

    implementation(libs.kotlinx.serialization.json)

    // HTML scraping for configured legal web sources
    implementation(libs.jsoup)

    // QR code generation for OAuth/API keys
    implementation(libs.zxing.core)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Persistence
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Preferences
    implementation(libs.androidx.datastore.preferences)

    // Splash screen
    implementation(libs.androidx.core.splashscreen)

    // TV launcher recommendations / WatchNext use framework TvContract.
    // Offline downloads
    implementation(libs.androidx.work.runtime)
    implementation(libs.hilt.androidx.work)
    ksp(libs.hilt.androidx.compiler)

    // BitTorrent engine (jlibtorrent) - use only for legal content/magnets you own or have rights to
    implementation(libs.jlibtorrent)
    implementation(libs.jlibtorrent.android.arm)
    implementation(libs.jlibtorrent.android.arm64)
    implementation(libs.jlibtorrent.android.x86)
    //noinspection UseTomlInstead
    implementation("com.frostwire:jlibtorrent-android-x86_64:${libs.versions.jlibtorrent.get()}")

    // QuickJS JavaScript engine for plugin scripts
    implementation(libs.quickjs.wrapper)

    // Haze frosted-glass blur for modern sidebar overlay
    implementation(libs.haze.android)

    // LibVLC alternative video/audio player engine (DTS/AC3, AVI/MKV support)
    implementation(libs.libvlc)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockwebserver)
    //noinspection UseTomlInstead
    testImplementation("io.mockk:mockk:1.14.11")
    androidTestImplementation(libs.androidx.junit.ext)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.uiautomator)
    androidTestImplementation(libs.hilt.testing)
    kspAndroidTest(libs.hilt.compiler)

    testImplementation(libs.androidx.compose.ui.test.junit4)
}
