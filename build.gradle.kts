import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.minecrell.pluginyml.paper.PaperPluginDescription

plugins {
    id("java")
    id("java-library")

    alias(libs.plugins.shadow)
    //alias(libs.plugins.indra)
    alias(libs.plugins.plugin.yml)
}

group = "com.creatorsplash.oxygenheist"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.codemc.io/repository/maven-releases/")
    maven("https://repo.triumphteam.dev/snapshots")
    maven("https://jitpack.io")
    maven("https://repo.extendedclip.com/releases/")

//    maven("https://maven.pkg.github.com/Creator-Splash/MainEventCore") {
//        name = "GitHubPackages"
//        credentials {
//            username = findProperty("gpr.user") as String?
//                ?: System.getenv("GITHUB_ACTOR")
//            password = findProperty("gpr.key") as String?
//                ?: System.getenv("GITHUB_TOKEN")
//        }
//    }
}

dependencies {
    // Paper
    compileOnly(libs.paper.api)

    // Event Core
    //compileOnly(libs.creatorsplashcore.api)

    // PAPI
    paperLibrary(libs.papi)

    // GUI
    paperLibrary(libs.triumph.gui)

    // Commands
    paperLibrary(libs.cloud.paper)
    paperLibrary(libs.cloud.annotations)

    // Development
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    annotationProcessor(libs.auto.service)
    compileOnly(libs.auto.service.annotations)

    compileOnly(libs.jetbrains.annotations)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks {
    build {
        dependsOn(shadowJar)
        finalizedBy("exportJars")
    }

    shadowJar {
        archiveClassifier.set("")
        minimize()
    }

    withType<JavaCompile> {
        options.release.set(21)
        options.encoding = Charsets.UTF_8.name()
        options.compilerArgs = listOf("-parameters")
    }

    register<Copy>("exportJars") {
        val shadowJar = named<ShadowJar>("shadowJar")

        dependsOn(shadowJar)

        from(
            shadowJar.flatMap { it.archiveFile },
        )

        into(rootProject.layout.projectDirectory.dir("target"))
    }
}

paper {
    name = "OxygenHeist"

    apiVersion = "1.21"
    //version = "Git-${indraGit.commit()?.name?.take(7) ?: "unknown"}"
    version = "1.0.0"

    main = "com.creatorsplash.oxygenheist.platform.paper.OxygenHeistPlugin"

    loader = "com.creatorsplash.oxygenheist.platform.paper.bootstrap.LibLoader"
    generateLibrariesJson = true

    serverDependencies {
        register("PlaceholderAPI") {
            required = false
            load = PaperPluginDescription.RelativeLoadOrder.AFTER
        }
//
//        register("CreatorSplashCore") {
//            required = true
//            load = PaperPluginDescription.RelativeLoadOrder.AFTER
//        }
    }
}