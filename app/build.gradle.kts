plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.tagger"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.tagger"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

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
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
        resources {
            excludes += listOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/notice.txt",
                "META-INF/ASL2.0"
            )
        }
    }
    
    androidResources {
        noCompress += listOf("tflite")
    }
    
    tasks.withType<JavaCompile> {
        options.compilerArgs.add("-Xlint:deprecation")
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)


    implementation ("androidx.core:core-ktx:1.12.0")
    implementation ("com.google.android.material:material:1.11.0")
    implementation ("androidx.camera:camera-core:1.3.1")
    implementation ("androidx.camera:camera-camera2:1.3.1")
    implementation ("androidx.camera:camera-lifecycle:1.3.1")
    implementation ("androidx.camera:camera-view:1.3.1")
    implementation ("androidx.camera:camera-extensions:1.3.1")
    implementation ("androidx.preference:preference:1.2.1")


    implementation ("org.tensorflow:tensorflow-lite:2.14.0")
    implementation ("org.tensorflow:tensorflow-lite-support:0.4.4")
    implementation ("org.tensorflow:tensorflow-lite-metadata:0.4.4")
    implementation ("org.tensorflow:tensorflow-lite-gpu:2.14.0")
    implementation ("org.tensorflow:tensorflow-lite-task-vision:0.4.4")
    
    implementation ("org.tensorflow:tensorflow-lite-select-tf-ops:2.14.0")
}