cmake_minimum_required(VERSION 3.22.1)
project(aura_analysis LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# ─── 1. Analizador de Automix ────────────────────────────────────────────────
add_library(aura_analysis SHARED
    analyzer/audio_analysis.cpp
    analyzer/tempo_analysis.cpp
    analyzer/resampler.cpp
    analyzer/mel_spectrogram.cpp
    analyzer/vocal_spectrogram.cpp
    jni/analysis_jni.cpp
    jni/mel_jni.cpp
    jni/vocal_jni.cpp
)

target_include_directories(aura_analysis PRIVATE ${CMAKE_CURRENT_SOURCE_DIR})
target_compile_options(aura_analysis PRIVATE -O3 -fvisibility=hidden)
find_library(log-lib log)
target_link_libraries(aura_analysis ${log-lib})
target_link_options(aura_analysis PRIVATE "-Wl,-z,max-page-size=16384")

# ─── 2. Decodificador DSD (DSF/DFF) ──────────────────────────────────────────
add_library(aura_dsd SHARED dsd/dsd_decimator.cpp)
target_compile_options(aura_dsd PRIVATE -O3 -fno-fast-math -ffp-contract=off -fvisibility=hidden)
target_link_options(aura_dsd PRIVATE "-Wl,-z,max-page-size=16384")

# ─── 3. Decodificador DST (SACD comprimido) ──────────────────────────────────
add_library(aura_dst SHARED dsd/dst/decoder.c dsd/dst/jni.cpp)
target_compile_options(aura_dst PRIVATE -O3 -fno-strict-aliasing -fvisibility=hidden)
target_link_options(aura_dst PRIVATE "-Wl,-z,max-page-size=16384")

# ─── 4. FFmpeg (JNI) ─────────────────────────────────────────────────────────
# IMPORTANTE: Debes colocar libavcodec.so, libavformat.so, libavutil.so, etc.
# en app/src/main/jniLibs/ para que esta sección compile.
add_library(ffmpegJNI SHARED ffmpeg/ffmpeg_jni.cc)

target_include_directories(ffmpegJNI PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/ffmpeg/include
)

# Enlazar librerías FFmpeg precompiladas
set(FFMPEG_LIBS avcodec avformat avutil swresample)
foreach(lib ${FFMPEG_LIBS})
    add_library(${lib} SHARED IMPORTED)
    set_target_properties(${lib} PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/lib${lib}.so)
endforeach()

target_link_libraries(ffmpegJNI ${FFMPEG_LIBS} ${log-lib} android z m)
target_link_options(ffmpegJNI PRIVATE "-Wl,-z,max-page-size=16384")
