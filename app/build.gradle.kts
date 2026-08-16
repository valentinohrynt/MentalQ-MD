import java.util.Properties

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use(::load)
    }
}

fun configurationValue(name: String, fallback: String): String =
    providers.gradleProperty(name).orNull
        ?: localProperties.getProperty(name)
        ?: fallback

fun optionalConfigurationValue(name: String): String? =
    (providers.gradleProperty(name).orNull ?: localProperties.getProperty(name))
        ?.trim()
        ?.takeIf(String::isNotEmpty)

fun String.asBuildConfigString(): String =
    "\"${replace("\\", "\\\\").replace("\"", "\\\"")}\""

val releaseSigningValues = mapOf(
    "RELEASE_STORE_FILE" to optionalConfigurationValue("RELEASE_STORE_FILE"),
    "RELEASE_STORE_PASSWORD" to optionalConfigurationValue("RELEASE_STORE_PASSWORD"),
    "RELEASE_KEY_ALIAS" to optionalConfigurationValue("RELEASE_KEY_ALIAS"),
    "RELEASE_KEY_PASSWORD" to optionalConfigurationValue("RELEASE_KEY_PASSWORD"),
)
val missingReleaseSigningValues = releaseSigningValues
    .filterValues { it == null }
    .keys
val releaseTaskRequested = gradle.startParameter.taskNames.any {
    it.contains("release", ignoreCase = true)
}

if (releaseTaskRequested && missingReleaseSigningValues.isNotEmpty()) {
    throw GradleException(
        "Release signing is incomplete. Missing: ${missingReleaseSigningValues.joinToString()}"
    )
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    namespace = "com.c242_ps246.mentalq"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.c242_ps246.mentalq"
        minSdk = 26
        targetSdk = 35
        versionCode = optionalConfigurationValue("VERSION_CODE")?.toIntOrNull() ?: 1
        versionName = optionalConfigurationValue("VERSION_NAME") ?: "1.0"

        buildConfigField(
            "String",
            "BASE_URL",
            configurationValue(
                "MENTALQ_BASE_URL",
                "https://mentalq-backend.vercel.app/api/"
            ).asBuildConfigString()
        )
        buildConfigField(
            "String",
            "GOOGLE_WEB_CLIENT_ID",
            configurationValue("GOOGLE_WEB_CLIENT_ID", "").asBuildConfigString()
        )

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (missingReleaseSigningValues.isEmpty()) {
            create("release") {
                storeFile = rootProject.file(releaseSigningValues.getValue("RELEASE_STORE_FILE")!!)
                storePassword = releaseSigningValues.getValue("RELEASE_STORE_PASSWORD")
                keyAlias = releaseSigningValues.getValue("RELEASE_KEY_ALIAS")
                keyPassword = releaseSigningValues.getValue("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (missingReleaseSigningValues.isEmpty()) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug { }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.exifinterface)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.database)
    implementation(libs.play.services.auth)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(libs.androidx.lifecycle.viewmodel.savedstate)
    implementation(libs.androidx.material.icons.extended)

    implementation(libs.retrofit2.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.logging.interceptor)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)

    implementation(libs.androidx.work.runtime.ktx)

    implementation(libs.coil.compose)

}
