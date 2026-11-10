// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.google.devtools.ksp) apply false
  alias(libs.plugins.roborazzi) apply false
  alias(libs.plugins.secrets) apply false
  alias(libs.plugins.google.services) apply false
}

/**
 * Keep Gradle outputs off iCloud Desktop.
 *
 * Desktop sync creates conflict copies like "BuildConfig 2.dex" / "AppDatabase_Impl 2.kt"
 * inside app/build, which then breaks dex merge when running from Android Studio.
 */
val externalBuildRoot =
  file("${System.getProperty("user.home")}/Library/Caches/volunteer_management_android/build")

layout.buildDirectory.set(externalBuildRoot.resolve("root"))

subprojects {
  val moduleBuildDir = externalBuildRoot.resolve(name)
  layout.buildDirectory.set(moduleBuildDir)
}
