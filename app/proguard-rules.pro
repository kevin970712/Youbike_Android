# --- Retrofit ---
# Retrofit 會在執行時透過反射建立 API 實例，必須保留
-keepattributes Signature
-keepattributes Exceptions
-keepclasseswithmembers interface * {
    @retrofit2.http.* <methods>;
}

# --- Kotlinx Serialization ---
# 確保序列化相關的類別不被混淆
-keepattributes *Annotation*, InnerClasses
-dontwarn kotlinx.serialization.**
-keep class kotlinx.serialization.** { *; }
-keep interface kotlinx.serialization.** { *; }
-keepattributes SerializedName

# --- 您專案的資料模型 (Data Classes) ---
# 為了保險起見，保留您的資料模型不被改名，避免 JSON 解析失敗
# 請確保這裡的路徑與您的 package 名稱一致
-keep class com.android.youbike.data.** { *; }

# --- Jetpack Compose (通常不需要額外設定，但為了保險) ---
-keepattributes SourceFile,LineNumberTable

-keep,allowobfuscation,allowshrinking interface retrofit2.Call
 -keep,allowobfuscation,allowshrinking class retrofit2.Response
 # With R8 full mode generic signatures are stripped for classes that are not
 # kept. Suspend functions are wrapped in continuations where the type argument
 # is used.
 -keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation