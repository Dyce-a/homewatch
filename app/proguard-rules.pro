# OkHttp тянет опциональные библиотеки, которых нет на Android — глушим предупреждения R8
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
