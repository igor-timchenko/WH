// === Плагины ===
plugins {
    // Плагин для сборки Android-приложения
    alias(libs.plugins.android.application)

    // Поддержка Kotlin в Android-проекте
    alias(libs.plugins.kotlin.android)

    // Поддержка kotlinx.serialization для работы с JSON
    alias(libs.plugins.kotlin.serialization)
}

// === Конфигурация Android ===
android {
    // Уникальное имя пакета приложения (должно совпадать с package в манифесте и коде)
    namespace = "ru.contlog.mobile.helper"

    // Версия SDK, против которой компилируется приложение
    compileSdk = 36

    defaultConfig {
        // Идентификатор приложения в Google Play и системе Android
        applicationId = "ru.contlog.mobile.helper"

        // Минимальная версия Android, которую поддерживает приложение (Android 10+)
        minSdk = 29

        // Целевая версия SDK — приложение оптимизировано под Android 14 (API 34+)
        targetSdk = 36

        // Версия сборки (целое число, увеличивается с каждой новой сборкой)
        versionCode = 1

        // Версия для пользователя (отображается в магазине)
        versionName = "1.0"

        // Runner для инструментальных тестов
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Конфигурация сборок
    buildTypes {
        release {
            // Отключено сжатие кода (ProGuard/R8). В production рекомендуется включить!
            isMinifyEnabled = false

            // Файлы правил для ProGuard/R8 (даже если minify выключен, иногда нужен для ресурсов)
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // Настройки совместимости с Java
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // Настройки Kotlin: генерировать байткод для JVM 11
    kotlinOptions {
        jvmTarget = "11"
    }

    // Включённые функции сборки
    buildFeatures {
        // Включить генерацию BuildConfig (доступен как BuildConfig.VERSION_NAME и т.д.)
        buildConfig = true

        // Включить ViewBinding (альтернатива findViewById и ButterKnife)
        viewBinding = true
    }
}

// === Зависимости ===
dependencies {
    // Основные библиотеки AndroidX
    implementation(libs.androidx.core.ktx)          // Расширения Kotlin для Android
    implementation(libs.androidx.appcompat)         // Совместимость с новыми API на старых устройствах
    implementation(libs.material)                   // Компоненты Material Design
    implementation(libs.androidx.activity)          // Activity с поддержкой ViewModel и т.д.
    implementation(libs.androidx.constraintlayout)  // ConstraintLayout — гибкая верстка
    implementation(libs.androidx.cardview)          // CardView для карточек
    implementation(libs.androidx.swiperefreshlayout) // SwipeRefreshLayout ("потянуть для обновления")
    implementation(libs.androidx.fragment.ktx)      // Расширения Kotlin для Fragment
    implementation(libs.androidx.navigation.fragment) // Navigation Component (фрагменты)
    implementation(libs.androidx.navigation.ui)       // Navigation Component (UI)

    // Сетевые и данные
    implementation(libs.okhttp)                     // HTTP-клиент для сетевых запросов
    implementation(libs.kotlinx.serialization.json) // JSON-сериализация (альтернатива Gson/Moshi)
    implementation(libs.kotlinx.datetime)           // Работа с датами (LocalDateTime и др.)

    // UI и утилиты
    implementation(libs.androidx.recyclerview)      // RecyclerView — список с переиспользуемыми элементами
    implementation(libs.zxing.android.embedded)     // Сканер штрихкодов (на основе ZXing)
    implementation(libs.glide)                      // Загрузка и кэширование изображений

    // Тестирование
    testImplementation(libs.junit)                  // Юнит-тесты (JVM)
    androidTestImplementation(libs.androidx.junit)  // Инструментальные тесты (на устройстве/эмуляторе)
    androidTestImplementation(libs.androidx.espresso.core) // UI-тесты (Espresso)
}