cmake_minimum_required(VERSION 3.22.1)
project(reelaudio_whisper C CXX)

# whisper.cpp (speech-to-text) is fetched at a pinned release and compiled into the app.
include(FetchContent)
set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(GGML_NATIVE OFF CACHE BOOL "" FORCE)
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)
if (ANDROID_ABI STREQUAL "arm64-v8a")
    # fp16 math roughly doubles speed and is on every arm64 phone from about 2018 onward.
    set(GGML_CPU_ARM_ARCH "armv8.2-a+fp16" CACHE STRING "" FORCE)
endif ()

FetchContent_Declare(
    whisper
    URL https://github.com/ggml-org/whisper.cpp/archive/refs/tags/v1.8.2.tar.gz
)
FetchContent_MakeAvailable(whisper)

add_library(reelaudio_whisper SHARED whisper_jni.c)
target_link_libraries(reelaudio_whisper PRIVATE whisper log)
target_compile_options(reelaudio_whisper PRIVATE -O3)
