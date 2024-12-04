plugins {
    java
}

group = "com.amazonaws.glue.federation"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":glue-catalog-federation-api"))

    implementation("software.amazon.awssdk:aws-core:2.29.26") {
        exclude(group = "*", module = "*")
    }
    implementation("software.amazon.awssdk:sdk-core:2.29.26") {
        exclude(group = "*", module = "*")
    }
    implementation("software.amazon.awssdk:utils:2.29.26") {
        exclude(group = "*", module = "*")
    }

    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.13.2")
    implementation("org.apache.hive:hive-standalone-metastore:3.1.3") {
        exclude(group = "org.slf4j", module = "*");
        exclude(group = "log4j", module = "*")
    }
    implementation("com.amazonaws:aws-lambda-java-core:1.2.3")
    implementation("com.amazonaws:aws-lambda-java-events:3.14.0")
    implementation("com.amazonaws.secretsmanager:aws-secretsmanager-caching-java:2.0.0")
}