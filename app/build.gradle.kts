import java.net.URI
import java.security.MessageDigest

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// PDFium comes prebuilt from bblanchon/pdfium-binaries, pinned by tag and SHA-256 in libs.versions.toml.
// Nothing binary is committed; each ABI's archive is downloaded once into build/ and verified.
val pdfiumDir = layout.buildDirectory.dir("pdfium")

android {
    namespace = "io.github.heppycat.fcat"
    compileSdk = 37
    ndkVersion = libs.versions.ndk.get()

    defaultConfig {
        // Provisional, see docs/decisions/0004-platform-baseline.md
        applicationId = "io.github.heppycat.fcat"
        minSdk = 28
        targetSdk = 37
        versionCode = 1
        versionName = "0.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        externalNativeBuild {
            cmake {
                arguments += "-DPDFIUM_DIR=${pdfiumDir.get().asFile}"
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = libs.versions.cmake.get()
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    dependenciesInfo {
        // Keep the dependency block out of APKs and bundles: it is only readable
        // by Google Play and is not reproducible for F-Droid.
        includeInApk = false
        includeInBundle = false
    }
}

val pdfiumArchives = mapOf(
    "armeabi-v7a" to ("arm" to libs.versions.pdfiumSha256Arm),
    "arm64-v8a" to ("arm64" to libs.versions.pdfiumSha256Arm64),
    "x86" to ("x86" to libs.versions.pdfiumSha256X86),
    "x86_64" to ("x64" to libs.versions.pdfiumSha256X64),
)
val fetchPdfium = pdfiumArchives.map { (abi, archive) ->
    val (cpu, sha256) = archive
    tasks.register<FetchPdfium>("fetchPdfium-$abi") {
        url = "https://github.com/bblanchon/pdfium-binaries/releases/download/" +
            "${libs.versions.pdfium.get()}/pdfium-android-$cpu.tgz"
        this.sha256 = sha256
        outputDir = pdfiumDir.map { it.dir(abi) }
    }
}
tasks.configureEach {
    if (name.startsWith("configureCMake") || name.startsWith("buildCMake")) dependsOn(fetchPdfium)
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.mupdf.fitz)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
}

abstract class FetchPdfium : DefaultTask() {
    @get:Input abstract val url: Property<String>

    @get:Input abstract val sha256: Property<String>

    @get:OutputDirectory abstract val outputDir: DirectoryProperty

    @get:Inject abstract val archives: ArchiveOperations

    @get:Inject abstract val files: FileSystemOperations

    @TaskAction
    fun fetch() {
        val archive = temporaryDir.resolve("pdfium.tgz")
        URI(url.get()).toURL().openStream().use { input -> archive.outputStream().use { input.copyTo(it) } }
        val digest = MessageDigest.getInstance("SHA-256").digest(archive.readBytes())
        val actual = digest.joinToString("") { "%02x".format(it) }
        if (actual != sha256.get()) throw GradleException("${url.get()}: SHA-256 is $actual, expected ${sha256.get()}")
        files.sync {
            from(archives.tarTree(archives.gzip(archive)))
            into(outputDir)
        }
        archive.delete()
    }
}
