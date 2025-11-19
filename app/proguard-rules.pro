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
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Сохранение классов модели, используемых в навигации
-keep class ru.contlog.mobile.helper.model.Division { *; }
-keepclassmembers class ru.contlog.mobile.helper.model.Division {
    <fields>;
    <init>(...);
}

# Сохранение всех классов модели, реализующих Serializable (для передачи через Bundle)
-keep class ru.contlog.mobile.helper.model.** implements java.io.Serializable { *; }
-keepclassmembers class ru.contlog.mobile.helper.model.** implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Сохранение атрибутов для сериализации
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Правила для kotlinx.serialization
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class ru.contlog.mobile.helper.model.**$$serializer { *; }
-keepclassmembers class ru.contlog.mobile.helper.model.** {
    *** Companion;
}
-keepclasseswithmembers class ru.contlog.mobile.helper.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Правила для Navigation Component
-keepnames class androidx.navigation.fragment.NavHostFragment
-keep class * implements androidx.navigation.NavArgs
-keepclassmembers class * implements androidx.navigation.NavArgs {
    <init>(android.os.Bundle);
}