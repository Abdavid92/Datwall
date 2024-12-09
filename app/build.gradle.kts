import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.archivesName

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("kotlin-parcelize")
    id("kotlin-kapt")
    //alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.google.dagger.hilt.android)
}

/**
 * Número de la versión oficial. Cambie la versión aquí.
 */
val officialVersionCode = 38

/**
 * Nombre de la versión oficial.
 */
val officialVersionName = "4.4.0"

//TODO: Temp
val kotlin_version = "1.7.10"
val hilt_version = "2.43.2"
val lifecycle_version = "2.5.1"

android {
    namespace = "com.smartsolutions.paquetes"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.smartsolutions.paquetes"
        minSdk = 21
        targetSdk = 35
        versionCode = officialVersionCode
        versionName = officialVersionName
        archivesName = "Mis Datos-$versionName-v$versionCode"

        testInstrumentationRunner = "com.smartsolutions.paquetes.HiltTestRunner"
    }
    buildFeatures {
        viewBinding = true
        dataBinding = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "GITHUB_URL", "\"https://github.com/Abdavid92/Datwall\"")
            buildConfigField("String", "APKLIS_URL", "\"https://apklis.cu/application/com.smartsolutions.paquetes\"")
            buildConfigField("String", "DEVELOPERS_EMAIL", "\"apps.smartsolutions.cuba@gmail.com\"")
        }
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "GITHUB_URL", "\"https://github.com/Abdavid92/Datwall\"")
            buildConfigField("String", "APKLIS_URL", "\"https://apklis.cu/application/com.smartsolutions.paquetes\"")
            buildConfigField("String", "DEVELOPERS_EMAIL", "\"apps.smartsolutions.cuba@gmail.com\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    implementation(fileTree(baseDir = "libs") {
        include("*.jar", "*.aar")
    })
    implementation(project(":vpncore"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version")
    implementation(libs.androidx.core.ktx)
    implementation("androidx.appcompat:appcompat:1.5.0")
    implementation("com.google.android.material:material:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.navigation:navigation-fragment-ktx:2.5.1")
    implementation("androidx.navigation:navigation-ui-ktx:2.5.1")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycle_version")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycle_version")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("androidx.preference:preference-ktx:1.2.0")
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("androidx.work:work-runtime-ktx:2.7.1")
    implementation("androidx.recyclerview:recyclerview:1.2.1")

    //Three party libraries
    implementation("moe.feng:MaterialStepperView:0.2.5")
    implementation("com.getkeepsafe.taptargetview:taptargetview:1.15.0")
    implementation("com.stephentuso:welcome:1.4.1")
    implementation("org.apache.commons:commons-lang3:3.17.0")
    implementation("com.github.PhilJay:MPAndroidChart:3.1.0")
    implementation("com.github.warkiz.widget:indicatorseekbar:2.1.2")
    implementation("com.github.Z17-CU:apklisupdate:v1.4")

    //Hilt
    implementation(libs.google.dagger.hilt.android)
    implementation(libs.androidx.hilt.work)
    kapt(libs.google.dagger.hilt.compiler)
    kapt(libs.google.dagger.hilt.android.compiler)

    //Gson
    implementation(libs.google.code.gson)

    //Retrofit
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)

    /** rx java **/
    implementation("io.reactivex.rxjava2:rxjava:2.2.19")
    implementation("io.reactivex.rxjava2:rxkotlin:2.4.0")
    implementation("io.reactivex.rxjava2:rxandroid:2.1.1")
    implementation("com.squareup.retrofit2:adapter-rxjava2:2.8.1")

    //html
    implementation("org.sufficientlysecure:html-textview:4.0")
    implementation("com.squareup.picasso:picasso:2.71828")

    //Room database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)

    //Zxing
    implementation("com.journeyapps:zxing-android-embedded:4.1.0") {
        isTransitive = false
    }
    implementation("com.google.zxing:core:3.4.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.google.dagger.hilt.android.testing)
    kaptAndroidTest(libs.google.dagger.hilt.compiler)
}