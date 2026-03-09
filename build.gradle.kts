plugins {
    alias(libs.plugins.android.application) apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false // Kotlin Android プラグイン (バージョンはプロジェクトによる)
    id("org.jetbrains.kotlin.kapt") version "1.9.22" apply false   // Kotlin kapt プラグイン (Kotlin のバージョンと合わせるのが推奨)
}

// Top-level build file where you can add configuration options common to all sub-projects/modules.