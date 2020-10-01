import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm") version "1.4.10"
    application
}

group = "me.geek.tom"
version = "1.0-SNAPSHOT"

application {
	mainClassName = "me.geek.tom.cardgame.GameKt"
}

repositories {
    mavenCentral()
}


// Compile against java 8
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8.toString()
    sourceCompatibility = JavaVersion.VERSION_1_8.toString()
    targetCompatibility = JavaVersion.VERSION_1_8.toString()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("com.google.code.gson:gson:2.8.6")
    implementation("io.netty:netty-all:4.1.52.Final")

    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.13.1")
    implementation("org.apache.logging.log4j:log4j-iostreams:2.13.1")
}
