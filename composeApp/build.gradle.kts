import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.androidApplication)
  alias(libs.plugins.kotlinMultiplatform)
  alias(libs.plugins.composeMultiplatform)
  alias(libs.plugins.composeCompiler)
  alias(libs.plugins.kotlinxSerialization)
}

kotlin {
  androidTarget {
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
      jvmTarget.set(JvmTarget.JVM_1_8)
    }
  }

  listOf(
    iosArm64(),
    iosSimulatorArm64(),
  ).forEach { target ->
    target.binaries.framework {
      baseName = "ComposeApp"
      isStatic = true
    }
  }

  sourceSets {
    androidMain.dependencies {
      implementation(compose.preview)
      implementation(libs.androidx.activity.compose)
    }
    commonMain.dependencies {
      implementation(projects.lib)
      implementation(compose.material3)
      implementation(compose.components.resources)
      implementation(compose.components.uiToolingPreview)
      implementation(libs.kmp.androidx.lifecycle.viewmodel)
      implementation(libs.kmp.androidx.lifecycle.runtime.compose)
      implementation(libs.kmp.androidx.navigation.compose)
      implementation(libs.kmp.kotlinx.serialization.json)
    }
  }
}

android {
  namespace = "com.sd.demo.kmp.compose_face"
  compileSdk = 34
  defaultConfig {
    minSdk = 24
    targetSdk = 34
    applicationId = "com.sd.demo.kmp.compose_face"
    versionCode = 1
    versionName = "1.0"
  }
  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
  }
  buildTypes {
    release {
      isMinifyEnabled = false
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }
}

dependencies {
  debugImplementation(compose.uiTooling)
}

