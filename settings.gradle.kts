pluginManagement {
    val mirrorFirst = providers.gradleProperty("useMirrors").orNull?.toBoolean() == true
    repositories {
        if (mirrorFirst) {
            maven { url = uri("https://maven.aliyun.com/repository/google") }
            maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
            maven { url = uri("https://maven.aliyun.com/repository/public") }
            maven { url = uri("https://repo.huaweicloud.com/repository/maven/") }
        }
        google()
        mavenCentral()
        gradlePluginPortal()
        if (!mirrorFirst) {
            // Automatic fallback mirrors when dl.google.com returns 404/Resource missing
            maven { url = uri("https://maven.aliyun.com/repository/google") }
            maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
            maven { url = uri("https://maven.aliyun.com/repository/public") }
            maven { url = uri("https://repo.huaweicloud.com/repository/maven/") }
        }
    }
}

dependencyResolutionManagement {
    val mirrorFirst = providers.gradleProperty("useMirrors").orNull?.toBoolean() == true
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        if (mirrorFirst) {
            maven { url = uri("https://maven.aliyun.com/repository/google") }
            maven { url = uri("https://maven.aliyun.com/repository/public") }
            maven { url = uri("https://repo.huaweicloud.com/repository/maven/") }
        }
        google()
        mavenCentral()
        if (!mirrorFirst) {
            // Automatic fallback mirrors when dl.google.com returns 404/Resource missing
            maven { url = uri("https://maven.aliyun.com/repository/google") }
            maven { url = uri("https://maven.aliyun.com/repository/public") }
            maven { url = uri("https://repo.huaweicloud.com/repository/maven/") }
        }
    }
}

rootProject.name = "DGPBoosterPumpController"
include(":app")
