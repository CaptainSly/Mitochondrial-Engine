plugins {
    java
}

allprojects {

    group = "com.duckcraftian.upf"
    version = "0.1.0-Resonance"

    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }

}

subprojects {
    apply(plugin = "java")


    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(25)
            vendor = JvmVendorSpec.GRAAL_VM
        }
    }

}