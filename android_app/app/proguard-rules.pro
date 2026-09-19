# Proguard rules for PALASH VoiceBridge
# Keep Room entities
-keep class com.palash.voicebridge.data.local.entity.** { *; }
# Keep Gson serialization
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
