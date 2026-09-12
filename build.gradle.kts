import com.gtnewhorizons.retrofuturagradle.mcp.ReobfuscatedJar
plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

val serverFixture by sourceSets.creating {
    java.srcDir("src/serverTest/java")
    compileClasspath += sourceSets.main.get().output + sourceSets.main.get().compileClasspath
}

val clientFixture by sourceSets.creating {
    java.srcDir("src/clientTest/java")
    compileClasspath += sourceSets.main.get().output + sourceSets.main.get().compileClasspath
}

val productionFixture by sourceSets.creating {
    java.srcDir("src/productionTest/java")
    compileClasspath += sourceSets.main.get().output + sourceSets.main.get().compileClasspath
}

val productionFixtureDevJar by tasks.registering(org.gradle.jvm.tasks.Jar::class) {
    group = "verification"
    description = "Packages acceptance tests for real obfuscated Minecraft; never part of the released mod."
    from(productionFixture.output, serverFixture.output)
    archiveFileName.set("hotncold-production-fixture-dev.jar")
    destinationDirectory.set(layout.buildDirectory.dir("production-test"))
}

tasks.register<ReobfuscatedJar>("productionFixtureJar") {
    group = "verification"
    description = "Reobfuscates the acceptance fixture using the same mappings as the released mod."
    val modJar = tasks.named<ReobfuscatedJar>("reobfJar")
    dependsOn(modJar)
    setInputJarFromTask(productionFixtureDevJar)
    mcVersion.set(modJar.flatMap { it.mcVersion })
    srg.set(modJar.flatMap { it.srg })
    fieldCsv.set(modJar.flatMap { it.fieldCsv })
    methodCsv.set(modJar.flatMap { it.methodCsv })
    exceptorCfg.set(modJar.flatMap { it.exceptorCfg })
    recompMcJar.set(modJar.flatMap { it.recompMcJar })
    referenceClasspath.from(productionFixture.compileClasspath, serverFixture.output)
    archiveFileName.set("hotncold-production-fixture.jar")
    destinationDirectory.set(layout.buildDirectory.dir("production-test"))
}

configurations[serverFixture.annotationProcessorConfigurationName].extendsFrom(configurations.annotationProcessor.get())
configurations[clientFixture.annotationProcessorConfigurationName].extendsFrom(configurations.annotationProcessor.get())
configurations[productionFixture.annotationProcessorConfigurationName].extendsFrom(configurations.annotationProcessor.get())

val prepareModernClientConfig by tasks.registering {
    group = "verification"
    description = "Prepares compatibility settings needed by the War of the Ring development client on modern Java."

    val clientDirectory = project.findProperty("runClientWorkingDirectory")?.toString() ?: "run/client"
    val configFile = project.file("$clientDirectory/config/hodgepodge.cfg")

    doLast {
        val setting = "B:preventLoadingChunksWhenTickingBlocks"
        val disabledSetting = "$setting=false"

        configFile.parentFile.mkdirs()
        if (!configFile.exists()) {
            configFile.writeText("speedups {\n    $disabledSetting\n}\n")
        } else {
            val config = configFile.readText()
            val enabledSetting = "$setting=true"
            when {
                config.contains(enabledSetting) -> configFile.writeText(config.replace(enabledSetting, disabledSetting))
                !config.contains(disabledSetting) -> configFile.appendText("\nspeedups {\n    $disabledSetting\n}\n")
            }
        }
    }
}

tasks.withType<JavaExec>().matching { it.name.matches(Regex("runClient(17|21|25)?")) }.configureEach {
    dependsOn(clientFixture.classesTaskName)
    val normalClasspath = classpath
    classpath = files(clientFixture.output, normalClasspath)
}

tasks.withType<JavaExec>().configureEach {
    if (project.hasProperty("badMobsRuntime")) {
        systemProperty("hotncold.fixture.expectBadMobs", "true")
    }
    project.findProperty("spawnStressIterations")?.toString()?.let {
        systemProperty("hotncold.fixture.spawnStressIterations", it)
    }
    if (project.hasProperty("clientSmokeTest") || project.hasProperty("clientLightningTest")) {
        systemProperty("hotncold.fixture.clientSmokeTest", "true")
        systemProperty("hotncold.fixture.clientLightningTest", project.hasProperty("clientLightningTest").toString())
        systemProperty(
            "hotncold.fixture.clientWorld",
            project.findProperty("clientFixtureWorld")?.toString() ?: "hotncold-client-fixture",
        )
        systemProperty(
            "hotncold.fixture.clientTerrain",
            project.findProperty("clientFixtureTerrain")?.toString() ?: "new",
        )
    }
}

tasks.withType<JavaExec>().matching { it.name.matches(Regex("runClient(17|21|25)")) }.configureEach {
    dependsOn(prepareModernClientConfig)
}

tasks.register<Jar>("serverFixtureJar") {
    group = "verification"
    description = "Builds the fake War of the Ring mod used by the dedicated-server smoke test."
    from(serverFixture.output)
    archiveFileName.set("wotrmc-server-fixture.jar")
    destinationDirectory.set(layout.projectDirectory.dir("run/server/mods"))
}

val badMobsServerFixtureJar by tasks.registering(Jar::class) {
    group = "verification"
    description = "Builds the server fixture in the isolated Bad Mobs test directory."
    from(serverFixture.output)
    archiveFileName.set("wotrmc-server-fixture.jar")
    destinationDirectory.set(layout.projectDirectory.dir("run/badmobs-server/mods"))
}

val prepareBadMobsServer by tasks.registering {
    group = "verification"
    description = "Prepares the isolated Bad Mobs compatibility server."

    doLast {
        val serverDirectory = layout.projectDirectory.dir("run/badmobs-server").asFile
        serverDirectory.mkdirs()
        val eulaFile = serverDirectory.resolve("eula.txt")
        if (!eulaFile.exists()) {
            eulaFile.writeText("eula=true\n")
        }
        val propertiesFile = serverDirectory.resolve("server.properties")
        propertiesFile.writeText(
            "level-name=hotncold-badmobs-test\n" +
                "online-mode=false\n" +
                "server-port=25577\n",
        )
    }
}

tasks.withType<JavaExec>().matching { it.name.matches(Regex("runServer(17|21|25)?")) }.configureEach {
    if (project.hasProperty("badMobsRuntime")) {
        dependsOn(badMobsServerFixtureJar, prepareBadMobsServer)
        workingDir(layout.projectDirectory.dir("run/badmobs-server"))
    }
}
