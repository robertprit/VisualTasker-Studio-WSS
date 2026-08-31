pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "VisualTaskerStudio-WSS"
include(":app")
includeBuild("visualtasker-blockeditor") {
    dependencySubstitution {
        substitute(module("de.visualtasker.blockeditor:blockeditor-compose")).using(project(":blockeditor-compose"))
        substitute(module("de.visualtasker.blockeditor:blockeditor-domain")).using(project(":blockeditor-domain"))
        substitute(module("de.visualtasker.blockeditor:blockeditor-registry")).using(project(":blockeditor-registry"))
        substitute(module("de.visualtasker.blockeditor:blockeditor-layout")).using(project(":blockeditor-layout"))
        substitute(module("de.visualtasker.blockeditor:blockeditor-interaction")).using(project(":blockeditor-interaction"))
        substitute(module("de.visualtasker.blockeditor:blockeditor-validation")).using(project(":blockeditor-validation"))
        substitute(module("de.visualtasker.blockeditor:blockeditor-serialization")).using(project(":blockeditor-serialization"))
        substitute(module("de.visualtasker.blockeditor:blockeditor-ir")).using(project(":blockeditor-ir"))
        substitute(module("de.visualtasker.blockeditor:blockeditor-emscript")).using(project(":blockeditor-emscript"))
    }
}
includeBuild("visualtasker-flowchart") {
    dependencySubstitution {
        substitute(module("de.visualtasker.flowchart:flowchart-compose")).using(project(":flowchart-compose"))
        substitute(module("de.visualtasker.flowchart:flowchart-test-support")).using(project(":flowchart-test-support"))
        substitute(module("de.visualtasker.flowchart:flowchart-domain")).using(project(":flowchart-domain"))
        substitute(module("de.visualtasker.flowchart:flowchart-layout")).using(project(":flowchart-layout"))
        substitute(module("de.visualtasker.flowchart:flowchart-interaction")).using(project(":flowchart-interaction"))
        substitute(module("de.visualtasker.flowchart:flowchart-validation")).using(project(":flowchart-validation"))
        substitute(module("de.visualtasker.flowchart:flowchart-serialization")).using(project(":flowchart-serialization"))
    }
}
