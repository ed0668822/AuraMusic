cmake_minimum_required(VERSION 3.22.1)
project(aura_analysis LANGUAGES C CXX)

get_filename_component(AURA_NATIVE_DIR
    "${CMAKE_CURRENT_SOURCE_DIR}/../../../../native"
    ABSOLUTE)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

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

target_include_directories(aura_analysis PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
)

target_compile_options(aura_analysis PRIVATE -O3 -fvisibility=hidden)

find_library(log-lib log)
target_link_libraries(aura_analysis ${log-lib})

target_link_options(aura_analysis PRIVATE "-Wl,-z,max-page-size=16384")
