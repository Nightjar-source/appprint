# Android application keep rules (R8 full mode, AGP 9 defaults).
# MainActivity is referenced from the manifest; R8 needs the component classes.
-keep class com.appprint.studio.MainActivity { *; }

# @Serializable Nav keys must survive for the Nav3 runtime.
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-keep @kotlinx.serialization.Serializable class * { *; }
