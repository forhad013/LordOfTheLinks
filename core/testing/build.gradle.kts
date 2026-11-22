// Test helpers shared by feature modules' jvmTest: in-memory fake repositories and the Molecule presenter harness.
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvm()

    sourceSets {
        jvmMain.dependencies {
            api(project(":core:model"))
            api(compose.runtime)
            api(libs.molecule.runtime)
            api(libs.kotlinx.coroutines.test)
        }
    }
}
