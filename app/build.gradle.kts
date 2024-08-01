import org.gradle.util.GUtil.loadProperties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("kotlin-parcelize")
    id("com.google.dagger.hilt.android")
}

android {
    compileSdk = 34

    defaultConfig {
        applicationId = "jp.co.c_lis.ccl.morelocale"
        namespace = "jp.co.c_lis.ccl.morelocale"

        minSdk = 24
        maxSdk = 30
        targetSdk = 34
        multiDexEnabled = true

        versionCode = 14247
        versionName = "2.4.7"

        buildConfigField(
            "String",
            "PREFERENCES_FILE_NAME",
            "\"pref.xml\""
        )

        buildConfigField(
            "String",
            "DATABASE_FILE_NAME",
            "\"locale.db\""
        )

        buildFeatures {
            buildConfig = true
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
        kotlinOptions {
            jvmTarget = JavaVersion.VERSION_17.toString()
        }

        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val releaseKeyStoreFile = File(rootDir, "keystore.properties")
            if (!releaseKeyStoreFile.exists()) {
                return@create
            }

            val releaseKeyStoreProps = loadProperties(releaseKeyStoreFile)

            val keystorePath = releaseKeyStoreProps.getProperty("keystore_path", "")
            val keystorePassword = releaseKeyStoreProps.getProperty("keystore_password", "")
            val keystoreKeyAlias = releaseKeyStoreProps.getProperty("keystore_key_alias", "")
            val keystoreKeyPassword = releaseKeyStoreProps.getProperty("keystore_key_password", "")

            if (arrayOf(
                    keystorePath,
                    keystorePassword,
                    keystoreKeyAlias,
                    keystoreKeyPassword
                ).none { it.isNullOrEmpty() }
            ) {
                storeFile = file(keystorePath)
                storePassword = keystorePassword
                keyAlias = keystoreKeyAlias
                keyPassword = keystoreKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
    buildFeatures {
        dataBinding = true
    }
}

dependencies {
    val lifecycleVersion = "2.7.0-beta01"
    val fragmentVersion = "1.7.0-alpha06"
    val multidexVersion = "2.0.1"
    val roomVersion = "2.6.0"
    val hiltVersion = "2.48.1"

    implementation(
        fileTree(
            mapOf(
                "dir" to "libs",
                "include" to listOf("*.aar", "*.jar"),
                "exclude" to listOf<String>()
            )
        )
    )

    implementation(project(":lib"))

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    implementation("androidx.fragment:fragment-ktx:$fragmentVersion")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycleVersion")

    implementation("androidx.multidex:multidex:$multidexVersion")

    implementation("androidx.room:room-runtime:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")
    androidTestImplementation("androidx.room:room-testing:$roomVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    implementation("com.jakewharton.timber:timber:5.0.1")

    implementation("com.google.dagger:hilt-android:$hiltVersion")
    ksp("com.google.dagger:hilt-android-compiler:$hiltVersion")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
