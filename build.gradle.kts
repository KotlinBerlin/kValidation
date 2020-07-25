@file:Suppress("SpellCheckingInspection")

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

plugins {
    kotlin("multiplatform") version "1.4-M3" apply false
}

allprojects {
    group = "de.kotlin-berlin"
    version = "1.0-RC3"

    repositories {
        jcenter()
        mavenLocal()
        maven {
            url = uri("https://dl.bintray.com/kotlin/kotlin-eap")
        }
        maven {
            url = uri("https://dl.bintray.com/kotlinberlin/snapshot")
        }
    }
}

buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.7")
    }
}

tasks {
    task("buildAll") {
        group = "kotlinBerlin"
        doFirst {
            val tempBuildOrder = project.findProperty("de.kotlinBerlin.${project.name}.buildOrder") as String?
            tempBuildOrder?.split(",")?.forEach { tempProjectNames ->
                runBlocking {
                    for (it in tempProjectNames.split("#")) {
                        launch(Dispatchers.Default) {
                            println("Building ${project.name}-$it")
                            exec {
                                executable = "gradle.bat"
                                workingDir = File(projectDir, "${project.name}-$it")
                                args = listOf("build", "publishToMavenLocal")
                                standardOutput = java.io.ByteArrayOutputStream()
                            }
                        }
                    }
                }
            }
        }
    }
}