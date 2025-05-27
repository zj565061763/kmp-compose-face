import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.DefaultCInteropSettings

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
    iosArm64(),
  ).forEach { target ->
    target.binaries.framework {
      baseName = "com_sd_lib_kmp_compose_face"
      isStatic = true
    }
    target.compilations.getByName("main") {
      val InspireFace by createFrameworkCinterop("InspireFace")
    }
    target.binaries.all {
      createFrameworkLinkerOpts("InspireFace")
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

fun org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation.createFrameworkCinterop(
  name: String,
): NamedDomainObjectContainerCreatingDelegateProvider<DefaultCInteropSettings> {
  val frameworksDir = "$projectDir/frameworks/"
  return cinterops.creating {
    definitionFile.set(project.file("frameworks/cinterop/${name}.def"))
    compilerOpts("-framework", name, "-F$frameworksDir")
  }
}

fun org.jetbrains.kotlin.gradle.plugin.mpp.NativeBinary.createFrameworkLinkerOpts(
  name: String,
) {
  val frameworksDir = "$projectDir/frameworks/"
  linkerOpts("-framework", name, "-F$frameworksDir")
}