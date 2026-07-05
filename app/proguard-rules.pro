# Add project specific ProGuard rules here.
# Room, Hilt, and Compose ship their own consumer rules; keep entity/enum names.
-keep class com.valsi.invoicesystem.data.entity.** { *; }
-keepclassmembers enum * { *; }
