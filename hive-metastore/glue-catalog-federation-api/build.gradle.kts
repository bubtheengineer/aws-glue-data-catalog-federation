plugins {
    java
}

group = "com.amazonaws.glue.federation"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("software.amazon.awssdk:aws-core:2.29.26")
    implementation("software.amazon.awssdk:sdk-core:2.29.26")
    implementation("software.amazon.awssdk:annotations:2.29.26")
}
