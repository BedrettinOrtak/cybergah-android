# Varsayılan ProGuard kuralları
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception

# Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# WebView
-keepclassmembers class com.cybergah.app.WebAppInterface {
    @android.webkit.JavascriptInterface <methods>;
}
