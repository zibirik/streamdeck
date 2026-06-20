pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "StreamPanel"

include(":app")
include(":core:model")
include(":core:database")
include(":core:datastore")
include(":core:network")
include(":core:execution")
include(":core:integrations")
include(":core:plugin-api")
include(":core:designsystem")
include(":feature:dashboard")
include(":feature:editor")
include(":feature:settings")
include(":feature:connections")
include(":feature:obs")
