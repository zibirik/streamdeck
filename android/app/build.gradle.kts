plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

// Version comes from CI (-PappVersionName=1.2.3 -PappVersionCode=42) when a release is built from a tag.
val appVersionName: String = providers.gradleProperty("appVersionName").getOrElse("0.2.1")
val appVersionCode: Int = providers.gradleProperty("appVersionCode").map(String::toInt).getOrElse(17)

// Release signing key is supplied through the environment so it never lives in the repo.
// See tools/create-keystore.ps1 and docs/releasing.md.
val releaseKeystorePath: String? = System.getenv("STREAMPANEL_KEYSTORE_FILE")?.takeIf { it.isNotBlank() }
val hasReleaseKey: Boolean = releaseKeystorePath != null && file(releaseKeystorePath).exists()

android {
    namespace = "com.streampanel"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.streampanel"
        minSdk = 26
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersionName
    }

    signingConfigs {
        if (hasReleaseKey) {
            create("release") {
                storeFile = file(releaseKeystorePath!!)
                storePassword = System.getenv("STREAMPANEL_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("STREAMPANEL_KEY_ALIAS")
                keyPassword = System.getenv("STREAMPANEL_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Without a release key (local builds) fall back to the debug key so the APK still installs.
            // CI refuses to publish such an APK (see .github/workflows/release.yml).
            signingConfig = signingConfigs.getByName(if (hasReleaseKey) "release" else "debug")
        }
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:database"))
    implementation(project(":core:datastore"))
    implementation(project(":core:network"))
    implementation(project(":core:execution"))
    implementation(project(":core:integrations"))
    implementation(project(":core:designsystem"))
    implementation(project(":feature:dashboard"))
    implementation(project(":feature:editor"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:obs"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
