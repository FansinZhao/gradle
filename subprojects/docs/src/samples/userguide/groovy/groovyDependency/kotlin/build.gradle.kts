plugins {
    groovy
}

repositories {
    mavenCentral()
}

// tag::groovy-test-dependency[]
dependencies {
    testImplementation("org.codehaus.groovy:groovy-all:2.5.4")
}
// end::groovy-test-dependency[]

// tag::bundled-groovy-dependency[]
dependencies {
    implementation(localGroovy())
}
// end::bundled-groovy-dependency[]
