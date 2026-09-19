# Add project specific ProGuard rules here.
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable

-keep public class * extends java.lang.Exception

# Keep the application class
-keep class com.terminal.app.TerminalApplication { *; }

# Keep the main activity
-keep class com.terminal.app.MainActivity { *; }

# Keep fragments
-keep class com.terminal.app.ui.** { *; }
