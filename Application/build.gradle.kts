// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    id("com.google.gms.google-services") version "4.4.1" apply false
     id("com.android.library") version "8.1.1" apply false // Also, check if you need this. Typically, a project is either an application or a library, not both at the top level.
    id("org.jetbrains.kotlin.android") version "1.9.0" apply false
}
