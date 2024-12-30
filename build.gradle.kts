import com.github.jengelman.gradle.plugins.shadow.tasks.ConfigureShadowRelocation
import fr.xpdustry.toxopid.ModPlatform
import fr.xpdustry.toxopid.task.GitHubArtifact
import fr.xpdustry.toxopid.task.GitHubDownload
import fr.xpdustry.toxopid.util.ModMetadata
import fr.xpdustry.toxopid.util.anukenJitpack
import fr.xpdustry.toxopid.util.mindustryDependencies
import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone

plugins {
    id("net.kyori.indra") version "3.0.1"
    id("net.kyori.indra.publishing") version "3.0.1"
    id("net.kyori.indra.git") version "3.0.1"
    id("net.kyori.indra.licenser.spotless") version "3.0.1"
    id("net.ltgt.errorprone") version "2.0.2"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("fr.xpdustry.toxopid") version "2.1.1"
}

val metadata = ModMetadata.fromJson(file("plugin.json").readText())
group = property("props.project-group").toString()
metadata.version = metadata.version + if (indraGit.headTag() == null) "-SNAPSHOT" else ""
version = metadata.version
description = metadata.description

toxopid {
    compileVersion.set("v" + metadata.minGameVersion)
    platforms.add(ModPlatform.HEADLESS)
}

repositories {
    mavenCentral()
    maven("https://maven.xpdustry.com/releases") {
        name = "xpdustry-repository-releases"
        mavenContent { releasesOnly() }
    }
    maven("https://maven.xpdustry.com/legacy-releases") {
        name = "xpdustry-repository-releases-legacy"
        mavenContent { releasesOnly() }
    }
    maven("https://maven.xpdustry.com/snapshots") {
        name = "xpdustry-repository-snapshots"
        mavenContent { snapshotsOnly() }
    }
    anukenJitpack()
}

dependencies {
    mindustryDependencies()
    compileOnly("fr.xpdustry:distributor-api:3.0.0-SNAPSHOT")
    annotationProcessor("fr.xpdustry:distributor-api:3.0.0-SNAPSHOT")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("net.mindustry_ddns:file-store:2.1.0")

    val junit = "5.9.0"
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junit")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junit")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junit")

    // Static analysis
    annotationProcessor("com.uber.nullaway:nullaway:0.10.5")
    errorprone("com.google.errorprone:error_prone_core:2.16")
}

tasks.withType(JavaCompile::class.java).configureEach {
    options.errorprone {
        disableWarningsInGeneratedCode.set(true)
        disable("MissingSummary", "FutureReturnValueIgnored")
        if (!name.contains("test", true)) {
            check("NullAway", CheckSeverity.ERROR)
            option("NullAway:AnnotatedPackages", project.property("props.root-package").toString())
        }
    }
}

// Required for the GitHub actions
tasks.register("getArtifactPath") {
    doLast { println(tasks.shadowJar.get().archiveFile.get().toString()) }
}

// Relocates dependencies
val relocate = tasks.create<ConfigureShadowRelocation>("relocateShadowJar") {
    target = tasks.shadowJar.get()
    prefix = project.property("props.root-package").toString() + ".shadow"
}

tasks.shadowJar {
    // Configure the dependencies shading
    dependsOn(relocate)
    // Reduce shadow jar size by removing unused classes
    minimize()
    // Include the plugin.json file with the modified version
    doFirst {
        val temp = temporaryDir.resolve("plugin.json")
        temp.writeText(metadata.toJson(true))
        from(temp)
    }
    // Include the license of your project
    from(rootProject.file("LICENSE.md")) {
        into("META-INF")
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

val distributor = tasks.register<GitHubDownload>("downloadDistributor") {
    artifacts.add(
        GitHubArtifact.release("Xpdustry", "Distributor", "v3.0.0-rc.3", "Distributor.jar")
    )
}

tasks.runMindustryServer {
    mods.from(tasks.shadowJar, distributor)
}

tasks.runMindustryClient {
    mods.setFrom()
}

signing {
    val signingKey: String? by project
    val signingPassword: String? by project
    useInMemoryPgpKeys(signingKey, signingPassword)
}

indra {
    javaVersions {
        target(17)
        minimumToolchain(17)
    }

    publishSnapshotsTo("xpdustry", "https://maven.xpdustry.fr/snapshots")
    publishReleasesTo("xpdustry", "https://maven.xpdustry.fr/releases")

    gpl3OnlyLicense()

    if (metadata.repo.isNotBlank()) {
        val repo = metadata.repo.split("/")
        github(repo[0], repo[1]) {
            ci(true)
            issues(true)
            scm(true)
        }
    }

    configurePublications {
        pom {
            organization {
                name.set("Xpdustry")
                url.set("https://www.xpdustry.fr")
            }

            developers {
                developer {
                    id.set("Phinner")
                    timezone.set("Europe/Brussels")
                }
            }
        }
    }
}

indraSpotlessLicenser {
    licenseHeaderFile(rootProject.file("LICENSE_HEADER.md"))
}
