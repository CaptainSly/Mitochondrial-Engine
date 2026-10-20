plugins {
    java
}

group = "com.duckcraftian.upf"
version = "0.1.0-Resonance"

val lwjglVersion = "3.4.2"
val jomlVersion = "1.10.8"
val lwjglNatives = "natives-linux"
val tinylogVersion = "2.7.0"
val nettyVersion = "4.2.10.Final"

dependencies {
    implementation(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))

    implementation("org.lwjgl", "lwjgl-stb")
    implementation("org.lwjgl", "lwjgl-zstd")
    implementation("org.joml", "joml", jomlVersion)

    implementation("org.tinylog:tinylog-api:$tinylogVersion")
}