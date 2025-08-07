plugins {
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21" // <--- 确保 Kotlin 版本与您项目一致
    id("com.android.application") version "8.10.1" // <--- 修正为冲突的版本 8.10.1
    id("org.jetbrains.kotlin.android") version "2.0.21"
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.app.mcp_kotlin"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.app.mcp_kotlin"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "apiKey", "\"${project.properties.getOrElse("apiKey") { "QLtpFajPo0FvRvIhsVE27oAPFCjwn9" }}\"")
        buildConfigField("String", "baseUrl", "\"${project.properties.getOrElse("baseUrl") { "http://192.168.2.18:8082/v1" }}\"")
        buildConfigField("String", "modelName", "\"${project.properties.getOrElse("modelName") { "Qwen2.5-7B-VL-Instruct" }}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
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
        freeCompilerArgs += "-Xjvm-default=all"
        javaParameters = true // 关键设置：保留方法参数名称以便反射
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            // 排除一些 META-INF 文件以避免打包冲突
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose BOM (Bill of Materials) for consistent Compose versions
    implementation(platform(libs.androidx.compose.bom))
    // Compose UI dependencies
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3) // Material Design 3 for Compose

    // Test dependencies (assuming these are defined in your libs.versions.toml)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation("androidx.appcompat:appcompat:1.6.1") // 或者使用 libs.androidx.appcompat

    // LangChain4j modules (versions kept consistent with your input: 1.0.1)
//    implementation("dev.langchain4j:langchain4j:1.0.1")
//    implementation("dev.langchain4j:langchain4j-open-ai:1.0.1")
    implementation("dev.langchain4j:langchain4j:1.0.1") {
        exclude(group = "dev.langchain4j", module = "langchain4j-http-client-jdk")
    }
    implementation("dev.langchain4j:langchain4j-open-ai:1.0.1") {
        exclude(group = "dev.langchain4j", module = "langchain4j-http-client-jdk")
    }
    implementation("dev.langchain4j:langchain4j-kotlin:1.0.1-beta6") // Kotlin extensions
    implementation("dev.langchain4j:langchain4j-mcp:1.0.1-beta6")
    // Jackson Kotlin module (as per LangChain4j docs, crucial for data class serialization)
    // Note: LangChain4j might pull this in transitively, but explicit declaration ensures it.
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")

    // Kotlin Coroutines library
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // OkHttp BOM and related artifacts (kept from your original build.gradle.kts)
    implementation(platform("com.squareup.okhttp3:okhttp-bom:4.12.0"))
    implementation("com.squareup.okhttp3:okhttp")
    implementation("com.squareup.okhttp3:logging-interceptor")
    implementation("io.modelcontextprotocol:kotlin-sdk:0.5.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
}