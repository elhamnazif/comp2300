plugins {
    `kotlin-dsl`
    alias(libs.plugins.spotless)
    alias(libs.plugins.detekt)
    kotlin("jvm")
}

kotlin {
    jvmToolchain(21)
    jvmToolchain(8)
}

dependencies {
    compileOnly(libs.spotless.gradlePlugin)
    compileOnly(libs.detekt.gradlePlugin)
    detektPlugins(libs.detekt.formatting)

    // https://github.com/gradle/gradle/issues/15383#issuecomment-779893192
    compileOnly(files(libs::class.java.superclass.protectionDomain.codeSource.location))
    implementation(kotlin("stdlib-jdk8"))
}

gradlePlugin {
    plugins {
        register("comp2300Detekt") {
            id = "comp2300.detekt"
            implementationClass = "DetektConventionPlugin"
        }
        register("comp2300Spotless") {
            id = "comp2300.spotless"
            implementationClass = "SpotlessConventionPlugin"
        }
    }
}
repositories {
    mavenCentral()
}