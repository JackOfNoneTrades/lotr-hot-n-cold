
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

configurations[serverFixture.annotationProcessorConfigurationName].extendsFrom(configurations.annotationProcessor.get())
configurations[clientFixture.annotationProcessorConfigurationName].extendsFrom(configurations.annotationProcessor.get())

val prepareModernClientConfig by tasks.registering {
    group = "verification"
    description = "Prepares compatibility settings needed by the War of the Ring development client on modern Java."

    doLast {
        val configFile = layout.projectDirectory.file("run/client/config/hodgepodge.cfg").asFile
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
