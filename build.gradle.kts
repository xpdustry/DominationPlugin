import com.xpdustry.toxopid.extension.anukeXpdustry
import com.xpdustry.toxopid.spec.ModMetadata
import com.xpdustry.toxopid.spec.ModPlatform
import com.xpdustry.toxopid.task.GithubAssetDownload
import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone

plugins {
    alias(libs.plugins.spotless)
    alias(libs.plugins.indra.common)
    alias(libs.plugins.indra.git)
    alias(libs.plugins.indra.publishing)
    alias(libs.plugins.shadow)
    alias(libs.plugins.toxopid)
    alias(libs.plugins.errorprone.gradle)
}

val metadata = ModMetadata.fromJson(rootProject.file("plugin.json"))
if (indraGit.headTag() == null) metadata.version += "-SNAPSHOT"
group = "com.xpdustry"
val rootPackage = "com.xpdustry.domination"
version = metadata.version
description = metadata.description

toxopid {
    compileVersion = "v${metadata.minGameVersion}"
    platforms = setOf(ModPlatform.SERVER)
}

repositories {
    mavenCentral()
    anukeXpdustry()
    maven("https://maven.xpdustry.com/releases") {
        name = "xpdustry-releases"
        mavenContent { releasesOnly() }
    }
    maven("https://maven.xpdustry.com/legacy-releases") {
        name = "xpdustry-legacy-releases"
        mavenContent { releasesOnly() }
    }
}

dependencies {
    compileOnly(toxopid.dependencies.mindustryCore)
    compileOnly(toxopid.dependencies.arcCore)

    compileOnly(libs.distributor.api)
    implementation(libs.distributor.cloud)
    implementation(libs.cloud.core)
    implementation(libs.cloud.annotations)

    implementation(libs.gson)
    implementation(libs.filestore)

    testImplementation(libs.junit.api)
    testRuntimeOnly(libs.junit.engine)

    compileOnly(libs.checker.qual)
    testCompileOnly(libs.checker.qual)

    annotationProcessor(libs.nullaway)
    errorprone(libs.errorprone.core)
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

    publishSnapshotsTo("xpdustry", "https://maven.xpdustry.com/snapshots")
    publishReleasesTo("xpdustry", "https://maven.xpdustry.com/releases")

    mitLicense()

    if (metadata.repository.isNotBlank()) {
        val repo = metadata.repository.split("/")
        github(repo[0], repo[1]) {
            ci(true)
            issues(true)
            scm(true)
        }
    }

    configurePublications {
        pom {
            organization {
                name = "xpdustry"
                url = "https://www.xpdustry.com"
            }
        }
    }
}

spotless {
    java {
        palantirJavaFormat()
        formatAnnotations()
        importOrder("", "\\#")
        // custom("no-wildcard-imports") { it.apply { if (contains("*;\n")) error("No wildcard imports allowed") } }
        licenseHeaderFile(rootProject.file("HEADER.txt"))
        bumpThisNumberIfACustomStepChanges(1)
    }
    kotlinGradle {
        ktlint()
    }
}

val generateMetadataFile by tasks.registering {
    inputs.property("metadata", metadata)
    val output = temporaryDir.resolve("plugin.json")
    outputs.file(output)
    doLast {
        output.writeText(ModMetadata.toJson(metadata))
    }
}

tasks.shadowJar {
    archiveFileName = "${metadata.name}.jar"
    archiveClassifier = "plugin"
    from(generateMetadataFile)
    from(rootProject.file("LICENSE.md")) { into("META-INF") }
    minimize()
    // relocate("com.distributor")
    // relocate("com.distributor")
    // relocate("com.distributor")
}

tasks.register<Copy>("release") {
    dependsOn(tasks.build)
    from(tasks.shadowJar)
    destinationDir = temporaryDir
}

tasks.withType<JavaCompile> {
    options.errorprone {
        disableWarningsInGeneratedCode = true
        disable("MissingSummary", "InlineMeSuggester")
        if (!name.contains("test", ignoreCase = true)) {
            check("NullAway", CheckSeverity.ERROR)
            option("NullAway:AnnotatedPackages", rootPackage)
            option("NullAway:TreatGeneratedAsUnannotated", true)
        }
    }
}

val downloadSlf4md by tasks.registering(GithubAssetDownload::class) {
    owner = "xpdustry"
    repo = "slf4md"
    asset = "slf4md-simple.jar"
    version = "v${libs.versions.slf4md.get()}"
}

val downloadDistributorCommon by tasks.registering(GithubAssetDownload::class) {
    owner = "xpdustry"
    repo = "distributor"
    asset = "distributor-common.jar"
    version = "v${libs.versions.distributor.get()}"
}

tasks.runMindustryServer {
    mods.from(downloadSlf4md, downloadDistributorCommon)
}
