repositories {
    google()
    mavenCentral()
    // Принудительный прямой репозиторий Яндекса для этого модуля
    maven { url = uri("https://yandex.ru") }
}

plugins {
    alias(libs.plugins.android.application)
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10"
}

android {
    namespace = "com.lexus.launcher"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.lexus.launcher"
        minSdk = 29
        targetSdk = 37
        versionCode = 3
        versionName = "1.3.0"



        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // НАСТРОЙКА РАЗДЕЛЕНИЯ APK ПО АРХИТЕКТУРАМ
        splits {
            abi {
                isEnable = true // Включаем разделение
                reset()         // Сбрасываем стандартный список
                include("arm64-v8a", "x86_64") // Генерируем файлы только для этих платформ
                isUniversalApk = false // Нам не нужен один тяжелый Fat APK
            }
        }
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }

}

dependencies {

    // Yandex MapKit SDK Lite (версия без оффлайн-карт, идеальна для магнитол с 4G/Wi-Fi)
    implementation("com.yandex.android:maps.mobile:4.42.0-navikit")
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("com.yandex.mapkit.styling:automotivenavigation:4.42.0")
    implementation("com.yandex.mapkit.styling:roadevents:4.42.0")
        // implementation("com.yandex.mapkit:navikit-road-events-styles:4.42.0")
    //implementation("com.yandex.android:maps.navigation:4.42.0")


    // Базовый AndroidX и Compose
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    // Платформа Jetpack Compose (Material 3)
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    // Иконки (Важно! Без этого упадет компиляция FastForward, FastRewind и т.д.)
    implementation("androidx.compose.material:material-icons-extended")

    // Библиотека для связи с автомобильным менеджером Android
    implementation("androidx.car.app:app:1.4.0")


    implementation("com.google.accompanist:accompanist-drawablepainter:0.34.0")

}