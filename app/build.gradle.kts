import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

apply(plugin = "com.mappls.services.android")

// In CI or environments without Mappls portal credentials, generate mock configuration files
// so that the Mappls Services Gradle task can execute and compile successfully without failing the build.
val ensureMapplsConfigFiles by tasks.registering {
    doLast {
        val hasOlf = fileTree(projectDir) {
            include("**/*.a.olf")
            exclude("mock.a.olf")
        }.files.isNotEmpty()
        if (!hasOlf) {
            val mockOlf = file("mock.a.olf")
            val mockConf = file("mock.a.conf")
            if (!mockOlf.exists()) {
                mockOlf.writeBytes(byteArrayOf(0))
            }
            if (!mockConf.exists()) {
                mockConf.writeText("{\"client_id\":\"mock\",\"client_secret\":\"mock\"}")
            }
        }

        // Clean up any stale variant-specific assets folders so only MapplsServicesTask manages assets/mappls-conf.txt
        file("src/debug/assets").deleteRecursively()
        file("src/release/assets").deleteRecursively()
    }
}

tasks.matching { it.name.startsWith("process") && it.name.endsWith("MapplsServices") }.configureEach {
    dependsOn(ensureMapplsConfigFiles)
    // Prevent Gradle from marking MapplsServicesTask UP-TO-DATE when switching between build variants
    outputs.upToDateWhen { false }
}

tasks.matching { it.name.startsWith("merge") && it.name.endsWith("Assets") }.configureEach {
    val variant = name.removePrefix("merge").removeSuffix("Assets")
    // Serialize MapplsServicesTask before the asset merger to eliminate duplicate resource collisions and race conditions
    dependsOn(tasks.matching { it.name == "process${variant}MapplsServices" })
}

tasks.named<Delete>("clean") {
    delete(file("src/main/assets/mappls-conf.txt"))
    delete(file("src/debug/assets"))
    delete(file("src/release/assets"))
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    namespace = "com.prismpixelvertex.palatelog"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.prismpixelvertex.palatelog"
        minSdk = 26
        targetSdk = 37
        versionCode = 34
        versionName = "1.2.26"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                val storeFilePath = keystoreProperties.getProperty("storeFile")
                if (!storeFilePath.isNullOrBlank()) {
                    storeFile = file(storeFilePath)
                    storePassword = keystoreProperties.getProperty("storePassword")
                    keyAlias = keystoreProperties.getProperty("keyAlias")
                    keyPassword = keystoreProperties.getProperty("keyPassword")
                }
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
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
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    lint {
        lintConfig = file("lint.xml")
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/LICENSE.txt"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/NOTICE.txt"
        }
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
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // Hilt Dependency Injection
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // Room Database
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // WorkManager (Background Sync)
    implementation(libs.androidx.work.runtime.ktx)

    // Location & Free Maps & Updates
    implementation(libs.play.services.location)
    implementation(libs.play.services.update)
    implementation(libs.osmdroid.android)
    implementation(platform(libs.mappls.bom))
    implementation(libs.mappls.android.sdk)

    // Image Loading
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // CameraX (In-App Camera Capture with Guaranteed Back Camera)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // Coroutines & Serialization
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    // Credentials, Identity & HTTP
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.google.id)
    implementation(libs.play.services.auth)
    implementation(libs.okhttp)

    // Ktor Client (KMP-ready HTTP networking)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
