plugins {
    `java-library`
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.21"
    id("com.diffplug.spotless") version "8.8.0"
}

group = "com.null_codes"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    paperweight.paperDevBundle("26.1.2.build.+")

    implementation("org.xerial:sqlite-jdbc:3.46.1.0")

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks.processResources {
    expand("version" to project.version)
}

tasks.test {
    useJUnitPlatform()
}

spotless {
    java {
        importOrder()
        removeUnusedImports()
        expandWildcardImports()
        cleanthat().sourceCompatibility("1.25")
        googleJavaFormat()
        formatAnnotations()
    }
    yaml {
        target("src/main/resources/*.yml")
        jackson()
    }
}