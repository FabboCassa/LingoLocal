cmake_minimum_required(VERSION 3.22.1)

# Force optimized build regardless of AGP debug/release variant.
# Without this, AGP sets CMAKE_BUILD_TYPE=Debug → -O0 → inference 10-20x slower.
if(NOT CMAKE_BUILD_TYPE OR CMAKE_BUILD_TYPE STREQUAL "Debug")
    set(CMAKE_BUILD_TYPE "RelWithDebInfo" CACHE STRING "Build type" FORCE)
    message(STATUS "LingoLocal: Overriding build type to RelWithDebInfo for llama.cpp performance")
endif()

project("lingolocal_llama" VERSION 1.0.0 LANGUAGES C CXX)

set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED true)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED true)

# ABI-specific GGML feature flags
if(DEFINED ANDROID_ABI)
    message(STATUS "LingoLocal: Detected Android ABI: ${ANDROID_ABI}")
    if(ANDROID_ABI STREQUAL "arm64-v8a")
        set(GGML_OPENMP OFF CACHE BOOL "" FORCE)
        # PERF: ARM KleidiAI fornisce kernel matmul ottimizzati per CPU moderne
        # (Cortex-X3/A715, Tensor G3, Snapdragon 8 Gen 2/3). Con quant Q4_0 dà
        # 1.5-2x di speedup nel token generation rispetto al kernel default.
        set(GGML_CPU_KLEIDIAI ON CACHE BOOL "" FORCE)
        # Online repack: se l'utente carica un GGUF Q4_0, llama.cpp lo riorganizza
        # in formato AArch64 ottimizzato al load (q4_0_4_4 / q4_0_4_8 / q4_0_8_8
        # in base alle istruzioni CPU disponibili: dotprod, i8mm, sve).
        set(GGML_CPU_AARCH64 ON CACHE BOOL "" FORCE)
    elseif(ANDROID_ABI STREQUAL "x86_64")
        set(GGML_OPENMP OFF CACHE BOOL "" FORCE)
    endif()
endif()

# Configure llama.cpp build options for embedded use
set(LLAMA_BUILD_COMMON   ON  CACHE BOOL "" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TESTS    OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER   OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TOOLS    OFF CACHE BOOL "" FORCE)
set(LLAMA_CURL           OFF CACHE BOOL "" FORCE)

set(LLAMA_SRC ${CMAKE_CURRENT_LIST_DIR}/llama.cpp)
add_subdirectory(${LLAMA_SRC} build-llama)

add_library(${CMAKE_PROJECT_NAME} SHARED
        llama_engine_jni.cpp)

target_include_directories(${CMAKE_PROJECT_NAME} PRIVATE
        ${LLAMA_SRC}
        ${LLAMA_SRC}/common
        ${LLAMA_SRC}/include
        ${LLAMA_SRC}/ggml/include)

target_link_libraries(${CMAKE_PROJECT_NAME}
        llama
        llama-common
        android
        log)
