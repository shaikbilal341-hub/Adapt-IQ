import java.net.NetworkInterface
import java.net.Inet4Address

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

fun getLocalIpAddress(): String {
    val override = project.findProperty("backendHost") as? String
        ?: System.getenv("BACKEND_HOST")
    if (!override.isNullOrBlank()) {
        return override
    }
    try {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        val candidateIps = mutableListOf<String>()
        while (interfaces.hasMoreElements()) {
            val networkInterface = interfaces.nextElement()
            if (networkInterface.isLoopback || !networkInterface.isUp) continue
            val name = networkInterface.name.lowercase()
            val displayName = networkInterface.displayName.lowercase()
            val isVirtual = name.contains("virtual") || displayName.contains("virtual") ||
                    displayName.contains("hyper-v") || displayName.contains("wsl") ||
                    displayName.contains("warp") || displayName.contains("box")
            val addresses = networkInterface.inetAddresses
            while (addresses.hasMoreElements()) {
                val address = addresses.nextElement()
                if (!address.isLoopbackAddress && address is Inet4Address) {
                    val hostAddress = address.hostAddress
                    if (!hostAddress.startsWith("127.") && !hostAddress.startsWith("169.254.")) {
                        if (!isVirtual) {
                            return hostAddress
                        }
                        candidateIps.add(hostAddress)
                    }
                }
            }
        }
        if (candidateIps.isNotEmpty()) {
            return candidateIps.first()
        }
    } catch (_: Exception) {}
    return "192.168.0.103"
}

fun getProductionUrl(): String {
    val prodUrl = project.findProperty("prodBackendUrl") as? String
        ?: System.getenv("PROD_BACKEND_URL")
        ?: ""
    return if (prodUrl.isNotBlank() && !prodUrl.endsWith("/")) "$prodUrl/" else prodUrl
}

android {
    namespace = "com.example.adaptiq"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.adaptiq"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val detectedHost = getLocalIpAddress()
        val prodUrl = getProductionUrl()
        buildConfigField("String", "DEFAULT_EMULATOR_URL", "\"http://10.0.2.2:4000/\"")
        buildConfigField("String", "DEFAULT_DEVICE_URL", "\"http://$detectedHost:4000/\"")
        buildConfigField("String", "HOST_LAN_IP", "\"$detectedHost\"")
        buildConfigField("String", "PROD_URL", "\"$prodUrl\"")
    }

    buildTypes {
        debug {
            manifestPlaceholders["usesCleartextTraffic"] = "true"
            buildConfigField("Boolean", "IS_RELEASE", "false")
        }
        release {
            manifestPlaceholders["usesCleartextTraffic"] = "false"
            buildConfigField("Boolean", "IS_RELEASE", "true")
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.squareup.retrofit)
    implementation(libs.squareup.retrofit.gson)
    implementation(libs.squareup.okhttp.logging)
    implementation(libs.androidx.security.crypto)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
