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
    id("net.kyori.indra") version "3.0.0"
    id("net.kyori.indra.publishing") version "3.0.0"
    id("net.kyori.indra.git") version "3.0.0"
    id("net.kyori.indra.licenser.spotless") version "3.0.0"
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
    maven("https://maven.xpdustry.fr/releases") {
        name = "xpdustry-repository"
        mavenContent { releasesOnly() }
    }
    maven("https://repo.xpdustry.fr/releases") {
        name = "xpdustry-repository-legacy"
        mavenContent { releasesOnly() }
    }
    anukenJitpack()
}

dependencies {
    mindustryDependencies()
    compileOnly("fr.xpdustry:distributor-api:3.0.0-rc.2")
    implementation("com.google.code.gson:gson:2.10")
    implementation("net.mindustry_ddns:file-store:2.1.0")

    val junit = "5.9.0"
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junit")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junit")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junit")

    // Static analysis
    annotationProcessor("com.uber.nullaway:nullaway:0.10.1")
    errorprone("com.google.errorprone:error_prone_core:2.16")
    compileOnly("org.checkerframework:checker-compat-qual:2.5.5")
}

tasks.withType(JavaCompile::class.java).configureEach {
    options.errorprone {
        disableWarningsInGeneratedCode.set(true)
        disable("MissingSummary")
        if (!name.contains("test", true)) {
            check("NullAway", CheckSeverity.ERROR)
            option("NullAway:AnnotatedPackages", project.property("props.root-package").toString())
        }
    }
    // Github + Indra being funny
    sourceCompatibility = "17"
    targetCompatibility = "17"
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

tasks.build.get().dependsOn(tasks.shadowJar)

val distributor = tasks.register<GitHubDownload>("downloadDistributor") {
    artifacts.add(
        GitHubArtifact.release("Xpdustry", "Distributor", "v3.0.0-rc.2", "Distributor.jar")
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
                    id.set(metadata.author)
                }
            }
        }
    }
}

indraSpotlessLicenser {
    licenseHeaderFile(rootProject.file("LICENSE_HEADER.md"))
}
