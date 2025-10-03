version = "0.1.0"

project.extra["PluginName"] = "ClanSplit Loot Keys"
project.extra["PluginDescription"] = "Automatically track and send PvP Loot Keys to ClanSplit sessions"

dependencies {
    compileOnly("com.squareup.okhttp3:okhttp:4.12.0")
    compileOnly("com.google.code.gson:gson:2.11.0")
}

tasks {
    jar {
        manifest {
            attributes(mapOf(
                "Plugin-Version" to project.version,
                "Plugin-Id" to nameToId(project.extra["PluginName"] as String),
                "Plugin-Provider" to project.extra["PluginProvider"],
                "Plugin-Description" to project.extra["PluginDescription"],
                "Plugin-License" to project.extra["PluginLicense"]
            ))
        }
    }
}

