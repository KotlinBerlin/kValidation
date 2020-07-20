rootProject.name = "kValidation"

include("kValidation-core")
include("kValidation-kModel")

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven {
            url = uri("https://dl.bintray.com/kotlin/kotlin-eap")
        }
    }
}