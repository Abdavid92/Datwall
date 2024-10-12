pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/jcenter")
        maven("https://maven.aliyun.com/repository/central")

        maven("http://nexus.prod.uci.cu/repository/maven-all/") {
            isAllowInsecureProtocol = true
        }
        maven("http://nexus.prod.uci.cu/repository/maven-central/") {
            isAllowInsecureProtocol = true
        }
        maven("http://nexus.prod.uci.cu/repository/jcenter/") {
            isAllowInsecureProtocol = true
        }
        maven("https://jitpack.io")
    }
}

rootProject.name = "Datwall"
include(":vpncore")
include(":app")