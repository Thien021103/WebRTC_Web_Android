buildscript {
  repositories {
    google()
    mavenCentral()  // Maven Central repository
  }
  dependencies {
    // Add the Maven coordinates and latest version of the plugin
    classpath ("com.google.gms:google-services:4.4.2")
    classpath ("com.google.firebase:firebase-appdistribution-gradle:5.1.1")
    classpath ("com.google.firebase:firebase-crashlytics-gradle:3.0.3")
    classpath ("com.google.firebase:perf-plugin:1.4.2")
  }
}

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.jvm) apply false
  alias(libs.plugins.kotlin.serialization) apply false
  alias(libs.plugins.compose.compiler) apply false
  alias(libs.plugins.spotless) apply false
  id("com.google.gms.google-services") version "4.4.2" apply false
}

subprojects {
  apply(plugin = rootProject.libs.plugins.spotless.get().pluginId)

  tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().all {
    kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8.toString()
    kotlinOptions.freeCompilerArgs += listOf(
      "-Xopt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
      "-Xopt-in=kotlin.time.ExperimentalTime",
    )
  }

  extensions.configure<com.diffplug.gradle.spotless.SpotlessExtension> {
    kotlin {
      target("**/*.kt")
      targetExclude("$buildDir/**/*.kt")
      ktlint().setUseExperimental(true).editorConfigOverride(
        mapOf(
          "indent_size" to "2",
          "continuation_indent_size" to "2"
        )
      )
      licenseHeaderFile(rootProject.file("spotless/copyright.kt"))
      trimTrailingWhitespace()
      endWithNewline()
    }
    format("kts") {
      target("**/*.kts")
      targetExclude("$buildDir/**/*.kts")
      licenseHeaderFile(rootProject.file("spotless/copyright.kt"), "(^(?![\\/ ]\\*).*$)")
    }
    format("xml") {
      target("**/*.xml")
      targetExclude("**/build/**/*.xml")
      licenseHeaderFile(rootProject.file("spotless/copyright.xml"), "(<[^!?])")
    }
  }
}