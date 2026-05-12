import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)

    alias(libs.plugins.ksp)

    alias(libs.plugins.serialization)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":common"))
//            implementation(project(":walletCore"))
            implementation(project(":commonView"))

            implementation(libs.kotlin.inject.runtime.kmp)

            implementation(libs.bundles.compose)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.navigation.compose)

            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlin.stdlib)
            implementation(libs.kotlinx.coroutines)

            implementation(libs.kermit)

            // Firebase
            implementation(libs.gitlive.firebase.firestore)
            implementation(libs.gitlive.firebase.functions)
            implementation(libs.gitlive.firebase.messaging.kmp)

            // Ktor (for image upload)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)

            // Coil (for image display)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.bundles.kotest)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
        }

        androidMain.dependencies {
            implementation(libs.ktor.client.android)
            implementation(libs.androidx.activity.compose)
        }

    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "wallet_android_private.supportchat.generated.resources"
}

dependencies {
    implementation(project(":common"))
//    implementation(project(":walletCore"))
    implementation(project(":commonView"))

    add("kspAndroid", libs.kotlin.inject.compiler.ksp)
}

android {
    namespace = "com.mycelium.supportchat"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            consumerProguardFiles("consumer-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}
