plugins {
    id("java")
    application
}

group = "com.duckcraftian.upf"
version = "0.1.9-Resonance"

val lwjglVersion = "3.4.2"
val jomlVersion = "1.10.9"
val lwjglNatives = "natives-linux"
val tinylogVersion = "2.7.0"
val gsonVersion = "2.14.0"

val engineFolders = listOf("data", "config", "saves", "libs", "mods")

dependencies {

    implementation(project(":upf-api"))

    implementation("com.google.code.gson:gson:${gsonVersion}")
    implementation("org.tinylog:tinylog-api:${tinylogVersion}")
    implementation("org.tinylog:tinylog-impl:${tinylogVersion}")

    implementation(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))

    implementation("org.lwjgl:lwjgl")
    implementation("org.lwjgl:lwjgl-openal")
    implementation("org.lwjgl:lwjgl-opengl")
    implementation("org.lwjgl:lwjgl-sdl")
    implementation("org.lwjgl:lwjgl-stb")
    implementation("org.lwjgl:lwjgl-yoga")
    implementation("org.lwjgl:lwjgl-zstd")
    implementation("org.lwjgl:lwjgl::$lwjglNatives")
    implementation("org.lwjgl:lwjgl-openal::$lwjglNatives")
    implementation("org.lwjgl:lwjgl-opengl::$lwjglNatives")
    implementation("org.lwjgl:lwjgl-sdl::$lwjglNatives")
    implementation("org.lwjgl:lwjgl-stb::$lwjglNatives")
    implementation("org.lwjgl:lwjgl-yoga::$lwjglNatives")
    implementation("org.lwjgl:lwjgl-zstd::$lwjglNatives")
    implementation("org.joml:joml:$jomlVersion")

    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    applicationName = "mitochondrial-engine"
    mainClass.set("com.duckcraftian.upf.engine.system.Engine")
    applicationDefaultJvmArgs = listOf(
        "--enable-native-access=ALL-UNNAMED",
        "--sun-misc-unsafe-memory-access=allow"
    )
}

tasks.installDist {
    eachFile {
        if (path.startsWith("lib/")) {
            path = path.replaceFirst("lib/", "bin/mitochondria/libs/");
        }
    }

    includeEmptyDirs = false

    preserve {
        include("bin/mitochondria/data/**", "bin/mitochondria/saves/**", "bin/mitochondria/config/**", "bin/mitochondria/mods/**")
    }

    doLast {
        engineFolders.forEach { destinationDir.resolve("bin/mitochondria/$it").mkdirs()}
    }

}

tasks.startScripts {
    doLast {
        unixScript.writeText(
            unixScript.readText().replace("\$APP_HOME/lib/", "\$APP_HOME/bin/mitochondria/libs/")
                .replace("exec \"\$JAVACMD\" \"\$@\"", "cd \"\$APP_HOME/bin\"\nexec \"\$JAVACMD\" \"\$@\"")
        )

        windowsScript.writeText(
            windowsScript.readText().replace("%APP_HOME%\\lib\\", "%APP_HOME%\\bin\\mitochondria\\libs\\")
        )
    }
}

tasks.test {
    useJUnitPlatform()
    workingDir = rootProject.projectDir
    maxHeapSize = "2g"
}

tasks.register<JavaExec>("runWithArgs") {
    classpath = sourceSets["main"].runtimeClasspath
    workingDir = rootProject.projectDir
    mainClass.set("com.duckcraftian.upf.engine.system.Engine")
    jvmArgs(
        "--enable-native-access=ALL-UNNAMED",
        "--sun-misc-unsafe-memory-access=allow"
    )
}