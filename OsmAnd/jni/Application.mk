APP_STL := c++_shared
APP_CPPFLAGS := -std=c++11 -fexceptions -frtti
APP_SHORT_COMMANDS := true

# Specify least supported Android platform version
APP_PLATFORM := android-14

NDK_TOOLCHAIN_VERSION := clang
APP_ABI := x86 armeabi-v7a arm64-v8a
ifdef ARMV7_ONLY
    APP_ABI := armeabi-v7a
endif 
ifdef ARM64_ONLY
    APP_ABI := arm64-v8a
endif
ifdef X86_ONLY
    APP_ABI := x86
endif

# APP_ABI := armeabi-v7a
ifndef OSMAND_DEBUG_NATIVE
    # Force release compilation in release optimizations, even if application is debuggable by manifest
    APP_OPTIM := release
endif
