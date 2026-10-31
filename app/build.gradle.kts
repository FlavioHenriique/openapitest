import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val localProperties = Properties().apply {
    rootProject.file("local.properties")
        .takeIf { it.isFile }
        ?.inputStream()
        ?.use(::load)
}

fun String.asBuildConfigString(): String =
    "\"" + replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r") + "\""

val openAiApiKey = providers.environmentVariable("OPENAI_API_KEY").orNull
    ?: localProperties.getProperty("OPENAI_API_KEY")
    ?: ""
val openAiModel = providers.environmentVariable("OPENAI_MODEL").orNull
    ?: localProperties.getProperty("OPENAI_MODEL")
    ?: "gpt-6.1-sol"
val openAiVectorStoreId = providers.environmentVariable("OPENAI_VECTOR_STORE_ID").orNull
    ?: localProperties.getProperty("OPENAI_VECTOR_STORE_ID")
    ?: ""

android {
    namespace = "io.github.openapitest"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.openapitest"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "OPENAI_API_KEY", openAiApiKey.asBuildConfigString())
        buildConfigField("String", "OPENAI_MODEL", openAiModel.asBuildConfigString())
        buildConfigField("String", "OPENAI_VECTOR_STORE_ID", openAiVectorStoreId.asBuildConfigString())

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
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
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
