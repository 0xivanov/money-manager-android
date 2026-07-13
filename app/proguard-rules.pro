# Money Manager uses explicit JSON parsing and does not require reflection keep rules.

# Tink references these compile-time annotations, but they are not required at runtime.
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
