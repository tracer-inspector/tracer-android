val PUBLISH_GROUP_ID = "io.tracer"
val PUBLISH_VERSION = "1.0.3"
val PUBLISH_ARTIFACT_ID = extra.get("artifactId") as? String ?: project.name

group = PUBLISH_GROUP_ID
version = PUBLISH_VERSION

apply(plugin = "maven-publish")
apply(plugin = "signing")

afterEvaluate {
    configure<PublishingExtension> {
        publications {
            create<MavenPublication>("release") {
                groupId = PUBLISH_GROUP_ID
                artifactId = PUBLISH_ARTIFACT_ID
                version = PUBLISH_VERSION

                if (project.plugins.hasPlugin("com.android.library")) {
                    from(components["release"])
                } else {
                    // For JVM libraries and Gradle plugins
                    from(components["java"])
                }

                pom {
                    name.set(PUBLISH_ARTIFACT_ID)
                    description.set("Tracer - Android Network Inspector")
                    url.set("https://github.com/landoulsi/tracer")
                    
                    licenses {
                        license {
                            name.set("MIT License")
                            url.set("https://github.com/landoulsi/tracer/blob/main/LICENSE")
                        }
                    }
                    
                    developers {
                        developer {
                            id.set("landoulsi")
                            name.set("Tracer Team")
                            email.set("info@tracer.io")
                        }
                    }
                    
                    scm {
                        connection.set("scm:git:github.com/landoulsi/tracer.git")
                        developerConnection.set("scm:git:ssh://github.com/landoulsi/tracer.git")
                        url.set("https://github.com/landoulsi/tracer/tree/main")
                    }
                }
            }
        }
        
        repositories {
            maven {
                name = "OSSRH"
                url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
                credentials {
                    username = System.getenv("OSSRH_USERNAME")
                    password = System.getenv("OSSRH_PASSWORD")
                }
            }
        }
    }

    // Use explicit configuration for signing to avoid script compilation issues
    project.extensions.configure<org.gradle.plugins.signing.SigningExtension> {
        val signingKey = System.getenv("SIGNING_KEY")
        val signingPassword = System.getenv("SIGNING_PASSWORD")
        if (!signingKey.isNullOrEmpty()) {
            useInMemoryPgpKeys(signingKey, signingPassword)
            sign(project.extensions.getByType<PublishingExtension>().publications)
        }
    }
}
