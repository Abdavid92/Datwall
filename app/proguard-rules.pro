# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-printconfiguration "r8/full-r8-config.txt"
-printusage "r8/full-r8-usage.txt"

-keepclassmembers class * extends com.stephentuso.welcome.WelcomeActivity {
    public static java.lang.String welcomeKey();
}

-keepclassmembers,allowobfuscation class * {
@com.google.gson.annotations.SerializedName <fields>;
}

-keep,allowobfuscation class * extends com.google.gson.JsonDeserializer
-keep,allowobfuscation class * extends com.google.gson.JsonSerializer

-keepattributes InnerClasses

-keep class io.jsonwebtoken.** { *; }
-keepnames class io.jsonwebtoken.* { *; }
-keepnames interface io.jsonwebtoken.* { *; }

-keep class org.bouncycastle.** { *; }
-keepnames class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**