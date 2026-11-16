import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import net.ornithemc.ploceus.api.PloceusGradleExtensionApi
import java.net.URI
import java.security.MessageDigest
import java.util.zip.ZipFile

plugins {
    kotlin("jvm") version "2.4.20"
    kotlin("plugin.serialization") version "2.4.20"
    id("net.fabricmc.fabric-loom-remap") version "1.17-SNAPSHOT"
    id("ploceus") version "1.17.4"
}

val modId = "rewardclaim"
val modName = "Reward Claim"
val modVersion = "1.0.0"
val mcVersion = "1.8.9"
val loaderVersion = "0.16.10"
val featherBuild = "2"

version = "$modVersion+$mcVersion"
group = "tech.thatgravyboat"
base.archivesName = modId

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net/") { name = "FabricMC" }
    maven("https://maven.ornithemc.net/releases") { name = "Ornithe" }
}

dependencies {
    minecraft("com.mojang:minecraft:$mcVersion")
    mappings("net.ornithemc:feather-gen2:$mcVersion+build.$featherBuild:v2")

    modImplementation("net.fabricmc:fabric-loader:$loaderVersion")
    compileOnly(files(fileTree(layout.buildDirectory.dir("oneconfig-api")) {
        include("*.jar")
    }))

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("com.google.code.gson:gson:2.10.1")

    for (module in arrayOf("lwjgl", "lwjgl-opengl")) {
        compileOnly("org.lwjgl:$module:3.3.1")
    }
}

val prepareOneConfigApi by tasks.registering {
    val apiDirectory = layout.buildDirectory.dir("oneconfig-api")
    outputs.dir(apiDirectory)

    doLast {
        val directory = apiDirectory.get().asFile
        directory.mkdirs()
        val oneConfigJar = directory.resolve("OneConfig-1.8.9-ornithe-1.7.1.jar")
        if (!oneConfigJar.exists()) {
            val connection = URI("https://cdn.modrinth.com/data/AibBIVmj/versions/sBhh9VyF/OneConfig-1.8.9-ornithe-1.7.1.jar")
                .toURL().openConnection()
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            connection.getInputStream().use { input ->
                oneConfigJar.outputStream().use(input::copyTo)
            }
        }

        val actualHash = MessageDigest.getInstance("SHA-512")
            .digest(oneConfigJar.readBytes())
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        check(actualHash == "407d71020c815a449bd19b22cbf7bafa03dfc462952c2b764c4bf67701e28a391db653040f18950e0f03e0d64c79a6a85b6070febb68184446b2d5db453bf5ee") {
            "OneConfig 1.7.1 failed SHA-512 verification."
        }

        ZipFile(oneConfigJar).use { archive ->
            val entries = archive.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                if (!entry.isDirectory && entry.name.startsWith("META-INF/jars/") && entry.name.endsWith(".jar")) {
                    val nestedJar = directory.resolve(entry.name.substringAfterLast('/'))
                    archive.getInputStream(entry).use { input ->
                        nestedJar.outputStream().use(input::copyTo)
                    }
                }
            }
        }
    }
}

tasks.withType<KotlinCompile>().configureEach {
    dependsOn(prepareOneConfigApi)
}

tasks.withType<JavaCompile>().configureEach {
    dependsOn(prepareOneConfigApi)
}

extensions.getByType<PloceusGradleExtensionApi>().apply {
    setIntermediaryGeneration(2)
}

configurations.configureEach {
    exclude(group = "org.lwjgl.lwjgl")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 25
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_25
    }
}
