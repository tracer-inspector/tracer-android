pluginManagement {
    includeBuild("gradle-plugin")
    repositories {
        google()
        mavenCentral()
        val artifactoryUser = providers.gradleProperty("careem_artifactory_username").orNull ?: System.getenv("careem_artifactory_username")
        val artifactoryKey = providers.gradleProperty("careem_artifactory_api_key").orNull ?: System.getenv("careem_artifactory_api_key")
        if (!artifactoryUser.isNullOrBlank() && !artifactoryKey.isNullOrBlank()) {
            maven("https://artifactory-pro.careem-internal.com/artifactory/mobile-libs-release") {
               credentials {
                   username = artifactoryUser
                   password = artifactoryKey
               }
            }
        }
        gradlePluginPortal()
    }
}

include(":sdk")
include(":sample")
rootProject.name = "TracerAndroid"
