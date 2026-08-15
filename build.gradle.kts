
plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

val serverFixture by sourceSets.creating {
    java.srcDir("src/serverTest/java")
    compileClasspath += sourceSets.main.get().output + sourceSets.main.get().compileClasspath
}

configurations[serverFixture.annotationProcessorConfigurationName].extendsFrom(configurations.annotationProcessor.get())

tasks.register<Jar>("serverFixtureJar") {
    group = "verification"
    description = "Builds the fake War of the Ring mod used by the dedicated-server smoke test."
    from(serverFixture.output)
    archiveFileName.set("wotrmc-server-fixture.jar")
    destinationDirectory.set(layout.projectDirectory.dir("run/server/mods"))
}
