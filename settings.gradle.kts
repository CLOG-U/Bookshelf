pluginManagement {
    repositories {
        google { content { includeGroupByRegex("com\\.android.*"); includeGroupByRegex("com\\.google.*"); includeGroupByRegex("androidx.*") } }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google { content { includeGroupByRegex("androidx.*"); includeGroupByRegex("com\\.android.*"); includeGroupByRegex("com\\.google\\.android.*"); includeGroup("com.google.testing.platform") } }
        mavenCentral()
    }
}
rootProject.name = "Bookshelf"
include(":app")
