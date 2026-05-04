import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

plugins {
    id("io.papermc.paperweight.userdev")
    id("xyz.jpenilla.run-paper") version Versions.Bukkit.runPaper
}

dependencies {
    // Required for :platforms:bukkit:runDevBundleServer task
    paperweight.paperDevBundle(Versions.Bukkit.paperDevBundle)

    shaded(project(":platforms:bukkit:common"))
    shaded(project(":platforms:bukkit:nms"))
    shaded("xyz.jpenilla", "reflection-remapper", Versions.Bukkit.reflectionRemapper)
}

tasks {
    shadowJar {
        val buildTimestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
        archiveVersion.set("${project.version}-$buildTimestamp")

        relocate("io.papermc.lib", "com.dfsek.terra.lib.paperlib")
        relocate("com.google.common", "com.dfsek.terra.lib.google.common")
        relocate("org.apache.logging.slf4j", "com.dfsek.terra.lib.slf4j-over-log4j")
        exclude("org/slf4j/**")
        exclude("org/checkerframework/**")
        exclude("org/jetbrains/annotations/**")
        exclude("org/intellij/**")
        exclude("com/google/errorprone/**")
        exclude("com/google/j2objc/**")
        exclude("javax/**")
        doLast {
            val overrideManifest = project.file("common/src/main/resources/META-INF/CLASS_MANIFEST_seismic").toPath()
            val archivePath = archiveFile.get().asFile.toPath()

            FileSystems.newFileSystem(archivePath, null as ClassLoader?).use { fs ->
                val target = fs.getPath("/META-INF/CLASS_MANIFEST_seismic")
                Files.writeString(
                    target,
                    Files.readString(overrideManifest),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
                )
            }
        }
    }

    runServer {
        minecraftVersion(Versions.Bukkit.minecraft)
        dependsOn(shadowJar)
        pluginJars(shadowJar.get().archiveFile)

        downloadPlugins {
            modrinth("viaversion", "5.5.0")
            modrinth("viabackwards", "5.5.0")
        }
    }
}


addonDir(project.file("./run/plugins/Terra/addons"), tasks.named("runServer").get())
