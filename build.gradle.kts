import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
plugins {
    idea
    kotlin("jvm") version "2.3.0"
    alias(libs.plugins.loom)
}

base {
    archivesName.set(project.name.lowercase())
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
    withSourcesJar()
}

loom {
    runs {
        getByName("client") {
            vmArg("-Ddevauth.enabled=true")
        }
    }
}

repositories {
    maven(url = "https://maven.teamresourceful.com/repository/maven-public/")
    maven(url = "https://repo.hypixel.net/repository/Hypixel/")
    maven(url = "https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")
    maven(url = "https://api.modrinth.com/maven")
}

dependencies {
    minecraft(libs.minecraft)

    implementation(libs.loader)
    implementation(libs.fabrickotlin)
    implementation(libs.fabric)

    implementation(libs.hypixelapi)
    implementation(libs.modapi)
    implementation(libs.olympus)
    implementation(libs.rlib)

    include(libs.hypixelapi)
    include(libs.olympus)
    include(libs.rlib)

    runtimeOnly(libs.devauth)
}

tasks.processResources {
    inputs.property("version", project.version)
    inputs.property("minecraft_version", libs.versions.minecraft.get())
    inputs.property("loader_version", libs.versions.loader.get())
    filteringCharset = "UTF-8"

    filesMatching("fabric.mod.json") {
        expand(
            "version" to project.version,
            "minecraft_version" to libs.versions.minecraft.get(),
            "loader_version" to libs.versions.loader.get(),
            "kotlin_loader_version" to libs.versions.fabrickotlin.get(),
        )
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(25)
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_25)
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true

        excludeDirs.add(file("run"))
    }
}