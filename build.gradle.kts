plugins {
    alias(libs.plugins.agp.app) apply false
    alias(libs.plugins.kotlin) apply false
    alias(libs.plugins.compose.compiler) apply false
}

extra["androidMinSdkVersion"] = 31
extra["androidTargetSdkVersion"] = 37
extra["androidCompileSdkVersion"] = 37
extra["androidCompileSdkVersionMinor"] = 0
extra["androidBuildToolsVersion"] = "37.0.0"
extra["androidSourceCompatibility"] = JavaVersion.VERSION_21
extra["androidTargetCompatibility"] = JavaVersion.VERSION_21
extra["managerVersionCode"] = getVersionCode()
extra["managerVersionName"] = getVersionName()

fun getGitCommitCount(): Int {
    return runCatching {
        val process = Runtime.getRuntime().exec(arrayOf("git", "rev-list", "--count", "HEAD"))
        process.inputStream.bufferedReader().use { it.readText().trim().toIntOrNull() ?: 0 }
    }.getOrDefault(0)
}

fun getGitDescribe(): String {
    return runCatching {
        val process = Runtime.getRuntime().exec(arrayOf("git", "describe", "--tags", "--always"))
        process.inputStream.bufferedReader().use { it.readText().trim().takeIf(String::isNotEmpty) ?: "demo" }
    }.getOrDefault("demo")
}

fun getVersionCode(): Int {
    val commitCount = getGitCommitCount()
    return 30000 + commitCount
}

fun getVersionName(): String {
    return "3.0"
}
