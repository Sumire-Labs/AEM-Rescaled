plugins {
    `java-library`
    id("com.cleanroommc.cleanroomgradle")
}

java { toolchain.languageVersion.set(JavaLanguageVersion.of(25)) }

repositories {
    maven {
        name = "AemUnderReview"
        url = uri(providers.gradleProperty("aemRepository").get())
        content { includeGroup("com.yogpc.qp") }
    }
    maven("https://cursemaven.com") { content { includeGroup("curse.maven") } }
    mavenCentral()
}

dependencies {
    implementation(cleanroom.userdev("0.7.14-alpha"))
    implementation(cleanroom.deobf("com.yogpc.qp:quarryplus:${providers.gradleProperty("aemVersion").get()}"))
}

tasks.register("verifyPublishedMod") {
    group = "verification"
    description = "Resolves AEM from its published metadata in an independent Cleanroom project."
    inputs.files(configurations.compileClasspath, configurations.runtimeClasspath)
    doLast {
        check(inputs.files.files.any { it.name.startsWith("quarryplus-") && it.extension == "jar" }) {
            "The published AEM artifact was not resolved."
        }
        logger.lifecycle("Resolved published AEM and its compile/runtime dependencies.")
    }
}
