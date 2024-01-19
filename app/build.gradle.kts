plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    signingConfigs {
        create("release") {
            keyAlias = "dbgikey"
            keyPassword = "dbgi_dbgi"
            storePassword = "dbgi_dbgi"
            storeFile =
                file("C:\\Users\\edoua\\Desktop\\DBGI_project\\DBGI_tracking_android\\dbgikey.kts")
        }
    }
    namespace = "org.example.dbgitracking"
    compileSdk = 34

    defaultConfig {
        applicationId = "org.example.dbgitracking"
        minSdk = 29
        //noinspection OldTargetApi
        targetSdk = 33
        versionCode = 1
        versionName = "0.6"

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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    //implementation("com.google.android.gms:play-services-vision:20.1.3")
    implementation("com.google.mlkit:barcode-scanning:17.2.0")
    implementation("org.javatuples:javatuples:1.2")
    implementation("androidx.activity:activity-ktx:1.6.1")
    implementation("androidx.fragment:fragment-ktx:1.5.5")
    implementation("com.google.zxing:core:3.5.2")
    implementation("androidx.camera:camera-core:1.2.1")
    implementation("androidx.camera:camera-camera2:1.2.1")
    implementation("com.android.support:support-annotations:28.0.0")
    implementation("com.bradyid:BradySdk:1.4.4")
    implementation("com.google.code.gson:gson:2.8.9")
    implementation("androidx.camera:camera-lifecycle:1.3.0")
    implementation("androidx.camera:camera-view:1.3.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}