import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.convention.kmp.library)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.kover)
}

// Settings tab: theme, backup / restore / browser export, app version
kotlin {
    android {
        namespace = "com.greenrobotdev.lordofthelinks.settings"
        compileSdk = 37
        minSdk = 28 // same as app/androidApp
    }

    jvm()

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.materialIconsExtended)
            implementation(libs.koin.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.navigation3.ui)
            implementation(libs.lifecycle.viewmodel.navigation3)

            implementation(project(":core:design"))
            implementation(project(":core:utils"))
            implementation(project(":core:model"))
            implementation(project(":bookmarks")) // LinksBackupService
        }

        jvmTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.multiplatform.settings.test) // in-memory theme storage
            implementation(project(":core:testing"))
        }

        androidMain.dependencies {
            implementation(libs.molecule.runtime)
            implementation(libs.androidx.activity.compose) // file picker / saver
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.molecule.runtime)
        }
    }
}
