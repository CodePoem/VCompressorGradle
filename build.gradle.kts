plugins {
    `kotlin-dsl`
    maven
    id("com.gradle.plugin-publish") version "0.10.1"
    `java-gradle-plugin`
}

group = "com.vdreamers.vcompressor"
version = "1.0.0"

dependencies {
    implementation(gradleApi())
    implementation(localGroovy())
    compileOnly("com.android.tools.build:gradle:3.0.0")
}

tasks {
    "uploadArchives"(Upload::class) {
        repositories {
            withConvention(MavenRepositoryHandlerConvention::class) {
                mavenDeployer {
                    withGroovyBuilder {
                        "repository"("url" to uri("$rootDir/m2/releases"))
                        "snapshotRepository"("url" to uri("$rootDir/m2/snapshots"))
                    }
                    pom.project {
                        withGroovyBuilder {
                            "licenses" {
                                "license" {
                                    "name"("The Apache Software License, Version 2.0")
                                    "url"("http://www.apache.org/licenses/LICENSE-2.0.txt")
                                    "distribution"("repo")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

gradlePlugin {
    plugins {
        create("v-compressor-gradle") {
            id = "com.vdreamers.vcompressor.v-compressor-gradle"
            implementationClass = "com.vdreamers.vcompressor.CompressorPlugin"
        }
    }
}

pluginBundle {
    website = "https://github.com/CodePoem/VCompressorGradle"
    vcsUrl = "https://github.com/CodePoem/VCompressorGradle"
    description = "VCompressorGradle is our compressor to compress image for gradle plugin."

    (plugins) {

        // first plugin
        "v-compressor-gradle" {
            // id is captured from java-gradle-plugin configuration
            displayName = "Gradle ImageCompress plugin"
            tags = listOf("individual", "tags", "per", "plugin")
            version = "1.0.0"
        }
    }

    mavenCoordinates {
        groupId = "com.vdreamers.vcompressor"
        artifactId = "v-compressor-gradle"
        version = "1.0.0"
    }
}

repositories {
    mavenCentral()
    mavenLocal()
//    jcenter()
    maven {
        url = uri("http://maven.aliyun.com/nexus/content/groups/public/")
    }
    maven {
        url = uri("http://maven.aliyun.com/nexus/content/repositories/jcenter/")
    }
    maven {
        url = uri("http://maven.aliyun.com/nexus/content/repositories/google/")
    }
}