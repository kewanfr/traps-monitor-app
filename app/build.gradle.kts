plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
}

val versionMajor = 1
val versionMinor = 4
val versionPatch = 16
// 1.4.14 Ajout du lancement au démarrage

val versionBuild = 28

val bundleId = "fr.kewan.trapsmonitor"

val minimumVersion = 19 // Android 4.4
val buildVersion = 34 // Android 11
// Old: 34

android {
    namespace = bundleId
    compileSdk = buildVersion

    defaultConfig {
        applicationId = bundleId
        minSdk = minimumVersion
        targetSdk = buildVersion
        versionCode = versionMajor * 10000 + versionMinor * 1000 + versionPatch * 100 + versionBuild
        versionName = "${versionMajor}.${versionMinor}.${versionPatch}"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug")
        }

        debug {
            // Set the file name using the version name
            // applicationIdSuffix = ".debug"
//            versionNameSuffix = "${versionMajor}.${versionMinor}.${versionPatch}-debug"

        }

    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

//    applicationVariants.all { variant ->
//        variant.outputs
//            // default type don't have outputFileName field
//            .map { it as com.android.build.gradle.internal.api.ApkVariantOutputImpl }
//            .all { output ->
//                output.outputFileName = "${variant.applicationId}-${variant.versionName}.apk"
//                false
//            }
//    }


}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.legacysupport)

    implementation(libs.paho.mqtt)
    implementation(libs.paho.mqtt.service)

    implementation(libs.androidAppUpdateLibrary)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}