plugins {
    id("java")
}

group = "com.duckcraftian.upf"
version = "0.1.0-Resonance"

val lwjglVersion = "3.4.2"
val jomlVersion = "1.10.9"
val lwjglNatives = "natives-linux"
val tinylogVersion = "2.7.0"
val gsonVersion = "2.14.0"

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

tasks.test {
    useJUnitPlatform()
    workingDir = rootProject.projectDir
    maxHeapSize = "2g"      // the Indiana tests hold several copies of the ETF's bands (about 220 MB each)
}