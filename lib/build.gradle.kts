import org.gradle.kotlin.dsl.implementation
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.androidLibrary)
  alias(libs.plugins.kotlinMultiplatform)
  alias(libs.plugins.composeMultiplatform)
  alias(libs.plugins.composeCompiler)
  alias(libs.plugins.mavenPublish)
}

kotlin {
  @OptIn(ExperimentalKotlinGradlePluginApi::class)
  androidTarget {
    publishLibraryVariants("release")
    compilerOptions {
      jvmTarget.set(JvmTarget.JVM_1_8)
    }
  }

  listOf(
    iosX64(),
    iosArm64(),
    iosSimulatorArm64()
  ).forEach {
    it.binaries.framework {
      baseName = "com_sd_lib_kmp_compose_face"
      isStatic = true
    }
  }

  sourceSets {
    commonMain.dependencies {
      implementation(compose.material3)
      implementation(compose.components.resources)
    }
    androidMain.dependencies {
      implementation(libs.camera.camera2)
      implementation(libs.camera.lifecycle)
      implementation(libs.camera.view)
      implementation(libs.inspireface.android)
    }
    commonTest.dependencies {
      implementation(libs.kmp.kotlin.test)
    }
  }
}

compose.resources {
  packageOfResClass = "com.sd.lib.kmp.compose_face.generated.resources"
}

android {
  namespace = "com.sd.lib.kmp.compose_face"
  compileSdk = 34
  defaultConfig {
    minSdk = 24
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }
}
