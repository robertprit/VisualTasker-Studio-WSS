plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.visualtasker.vision.yolo"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    androidResources {
        noCompress += "tflite"
    }
}

dependencies {
    api(project(":vision-contracts"))
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("com.google.ai.edge.litert:litert:2.1.5")
}
