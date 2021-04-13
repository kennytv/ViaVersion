dependencies {
    implementation(project(":viaversion-common"))
    implementation(project(":java-compat"))
    compileOnly("net.md-5", "bungeecord-api", Versions.bungee)
}

configure<JavaPluginConvention> {
    // This is necessary to allow compilation for Java 8 while still including
    // newer Java versions in the code.
    disableAutoTargetJvm()
}
