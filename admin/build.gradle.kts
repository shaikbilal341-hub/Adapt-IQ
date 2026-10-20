import java.net.NetworkInterface
import java.net.Inet4Address
import java.net.URI

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

fun validateProductionUrl(url: String?): String {
    if (url.isNullOrBlank()) {
        throw GradleException(
            "FATAL: Production release build requires an explicitly configured HTTPS backend URL. " +
            "Please provide it via project property '-PprodBackendUrl=https://<domain>/' or environment variable 'PROD_BACKEND_URL=https://<domain>/'."
        )
    }
    val trimmed = url.trim()
    if (!trimmed.startsWith("https://", ignoreCase = true)) {
        throw GradleException(
            "FATAL: Production URL must use the HTTPS scheme. Insecure HTTP is strictly forbidden in production release builds. Provided: '$trimmed'"
        )
    }
    try {
        val uri = URI(trimmed)
        val host = uri.host?.lowercase() ?: throw GradleException("FATAL: Production URL is malformed: missing valid host. Provided: '$trimmed'")
        
        // Disallow localhost, loopback, emulator (10.0.2.2), and private LAN IP ranges
        if (host == "localhost" || host == "127.0.0.1" || host == "10.0.2.2" ||
            host.startsWith("192.168.") || host.startsWith("10.") ||
            host.matches(Regex("^172\\.(1[6-9]|2[0-9]|3[0-1])\\..*")) ||
            host.endsWith(".local") || host.endsWith(".internal")) {
            throw GradleException(
                "FATAL: Production URL cannot target localhost, emulator (10.0.2.2), or local/private network addresses ($host). A public HTTPS production domain is required."
            )
        }
    } catch (e: Exception) {
        if (e is GradleException) throw e
        throw GradleException("FATAL: Production URL is invalid: ${e.message}")
    }
    return if (trimmed.endsWith("/")) trimmed else "$trimmed/"
}

fun getProductionUrl(): String {
    val rawUrl = project.findProperty("prodBackendUrl") as? String
        ?: System.getenv("PROD_BACKEND_URL")
    val isReleaseRequested = gradle.startParameter.taskNames.any { it.contains("Release", ignoreCase = true) }
    return if (isReleaseRequested) {
        validateProductionUrl(rawUrl)
    } else {
        if (!rawUrl.isNullOrBlank()) {
            validateProductionUrl(rawUrl)
        } else {
            ""
        }
    }
}

android {
    namespace = "com.example.adaptiq.admin"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.adaptiq.admin"
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
