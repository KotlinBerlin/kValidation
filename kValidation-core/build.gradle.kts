@file:Suppress("UNUSED_VARIABLE")

import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

plugins {
    kotlin("multiplatform")
    id("maven-publish")
    id("org.jetbrains.dokka") version "0.10.1"
}

repositories {
    maven("https://dl.bintray.com/konform-kt/konform")
}

val ideaActive = System.getProperty("idea.active") == "true"
lateinit var tempJvm9KotlinOutputDir: File

kotlin {
    jvm("jvm8") {
        attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 8)
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }
    }
    js {
        browser {
        }
        nodejs {
        }
    }
    if (ideaActive) {
        mingwX64()
    } else {
        jvm("jvm9") {
            attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 9)
            compilations.all {
                kotlinOptions.jvmTarget = "9"
                if (!compileKotlinTaskName.contains("Test")) {
                    tempJvm9KotlinOutputDir = compileKotlinTask.destinationDir
                }
            }
            withJava()
        }

        mingwX64()
        mingwX86()

        androidNativeArm32()
        androidNativeArm64()

        watchosArm32()
        watchosArm64()
        watchosX86()

        iosArm32()
        iosArm64()
        iosX64()

        tvosArm64()
        tvosX64()

        macosX64()

        wasm32()

        linuxArm64()
        linuxArm32Hfp()
        linuxMips32()
        linuxMipsel32()
        linuxX64()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }

        val jvm8Main by getting {
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
            }
        }

        val jvm8Test by getting {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }

        if (!ideaActive) {
            val jvm9Main by getting {
                dependsOn(jvm8Main)
                dependencies {
                    implementation(kotlin("stdlib-jdk8"))
                }
            }

            val jvm9Test by getting {
                dependencies {
                    implementation(kotlin("test-junit"))
                }
            }
        }

        val jsMain by getting {
            dependencies {
                implementation(kotlin("stdlib-js"))
            }
        }

        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }

        targets.forEach {
            if (it.platformType == KotlinPlatformType.native) {
                it.compilations.forEach { compilation ->
                    compilation.kotlinSourceSets.forEach { sourceSet ->
                        if (sourceSet.name.endsWith("Test")) {
                            sourceSet.kotlin.setSrcDirs(listOf("src/nativeTest/kotlin"))
                        } else {
                            sourceSet.kotlin.setSrcDirs(listOf("src/nativeMain/kotlin"))
                        }
                    }
                }
            }
        }
    }

    tasks {
        if (!ideaActive) {
            withType(JavaCompile::class) {
                val moduleName = "de.kotlinBerlin.${project.rootProject.name}.core"
                doFirst {
                    println(tempJvm9KotlinOutputDir.absolutePath)
                    options.compilerArgs = listOf(
                        "--module-path",
                        classpath.asPath,
                        "--patch-module",
                        "${moduleName}=${tempJvm9KotlinOutputDir.absolutePath}"
                    )
                    classpath = files()
                }
            }
        }

        task("createSourceDirs") {
            group = "kotlinBerlin"
            doLast {
                sourceSets.forEach {
                    it.kotlin.sourceDirectories.forEach { tempSourceDir ->
                        if (!tempSourceDir.exists()) {
                            tempSourceDir.mkdirs()
                            println("Dir: ${tempSourceDir.absolutePath} created.")
                        } else {
                            println("Dir: ${tempSourceDir.absolutePath} exists.")
                        }
                    }
                }
            }
        }
    }
}