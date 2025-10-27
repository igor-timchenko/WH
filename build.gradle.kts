// Top-level build file where you can add configuration options common to all sub-projects/modules.
// Файл: build.gradle.kts (уровень проекта)

// === Плагины уровня проекта ===
plugins {
    // Плагин для сборки Android-приложений.
    // apply false — означает, что плагин НЕ применяется к текущему (проектному) build-скрипту,
    // а только регистрируется для использования в модулях (например, в app/build.gradle.kts).
    alias(libs.plugins.android.application) apply false

    // Плагин для поддержки Kotlin в Android-проектах.
    // Также не применяется на уровне проекта, а доступен для подмодулей.
    alias(libs.plugins.kotlin.android) apply false
}