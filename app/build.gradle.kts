plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val generatedEditorAssets = layout.buildDirectory.dir("generated/editorAssets")

val installEditorDependencies by tasks.registering(Exec::class) {
    workingDir(rootProject.projectDir)
    inputs.files(rootProject.file("package.json"), rootProject.file("package-lock.json"))
    outputs.dir(rootProject.file("node_modules"))
    commandLine("npm", "ci")
}

val bundleEditor by tasks.registering(Exec::class) {
    dependsOn(installEditorDependencies)
    workingDir(rootProject.projectDir)
    inputs.files(fileTree(rootProject.file("app/src/main/web/editor")))
    inputs.files(rootProject.file("package.json"), rootProject.file("package-lock.json"))
    outputs.file(generatedEditorAssets.map { it.file("editor/editor.js") })
    commandLine("npm", "run", "build:editor")
}

val copyEditorPage by tasks.registering(Copy::class) {
    from("src/main/web/editor/index.html", "src/main/web/editor/editor.css")
    into(generatedEditorAssets.map { it.dir("editor") })
}

android {
    namespace = "com.cphone.editor"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.cphone.editor"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "2.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    sourceSets.getByName("main").assets.srcDir(generatedEditorAssets)

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.webkit:webkit:1.12.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

tasks.named("preBuild").configure {
    dependsOn(bundleEditor, copyEditorPage)
}

tasks.matching { it.name.startsWith("merge") && it.name.endsWith("Assets") }.configureEach {
    dependsOn(bundleEditor, copyEditorPage)
}
