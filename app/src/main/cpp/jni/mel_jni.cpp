/*
 * Ported from Orchard (https://github.com/SFG5453/Orchard).
 *
 * Copyright (C) 2026 SFG545 (original Orchard implementation)
 * Copyright (C) 2026 Kushagra Singh (Aurelis adaptation)
 *
 * Orchard's original source is licensed under the GNU Affero General Public
 * License, version 3 or later. Per AGPLv3 section 13, this file is combined
 * here into Aurelis -- a work licensed under the GNU General Public
 * License, version 3 or later -- and remains itself governed by the AGPLv3
 * as part of that combination.
 */

#include <jni.h>

#include <vector>

#include "analyzer/mel_spectrogram.h"
#include "analyzer/resampler.h"

extern "C" {

JNIEXPORT jfloatArray JNICALL
Java_com_music_aurelis_playback_smart_MelSpectrogram_nativeResample(
    JNIEnv* env,
    jclass /* clazz */,
    jfloatArray samples,
    jdouble input_rate,
    jdouble output_rate) {
  const jsize count = env->GetArrayLength(samples);
  std::vector<float> input(static_cast<size_t>(count));
  if (count > 0) {
    env->GetFloatArrayRegion(samples, 0, count, input.data());
  }

  const std::vector<float> resampled =
      aurelis::smart::Resample(input, input_rate, output_rate);

  const jsize produced = static_cast<jsize>(resampled.size());
  jfloatArray result = env->NewFloatArray(produced);
  if (result == nullptr) {
    return nullptr;
  }
  if (produced > 0) {
    env->SetFloatArrayRegion(result, 0, produced, resampled.data());
  }
  return result;
}

JNIEXPORT jfloatArray JNICALL
Java_com_music_aurelis_playback_smart_MelSpectrogram_nativeCompute(
    JNIEnv* env,
    jclass /* clazz */,
    jfloatArray samples,
    jdouble sample_rate) {
  const jsize count = env->GetArrayLength(samples);
  std::vector<float> input(static_cast<size_t>(count));
  if (count > 0) {
    env->GetFloatArrayRegion(samples, 0, count, input.data());
  }

  const aurelis::smart::BeatSpectrogram spectrogram =
      aurelis::smart::ComputeBeatSpectrogram(input, sample_rate);

  const jsize produced = static_cast<jsize>(spectrogram.values.size());
  jfloatArray result = env->NewFloatArray(produced);
  if (result == nullptr) {
    return nullptr;
  }
  if (produced > 0) {
    env->SetFloatArrayRegion(result, 0, produced, spectrogram.values.data());
  }
  return result;
}

JNIEXPORT jint JNICALL
Java_com_music_aurelis_playback_smart_MelSpectrogram_nativeMelCount(
    JNIEnv* /* env */,
    jclass /* clazz */) {
  return static_cast<jint>(aurelis::smart::kBeatSpectrogramMels);
}

JNIEXPORT jdouble JNICALL
Java_com_music_aurelis_playback_smart_MelSpectrogram_nativeSampleRate(
    JNIEnv* /* env */,
    jclass /* clazz */) {
  return aurelis::smart::kBeatSpectrogramSampleRate;
}

JNIEXPORT jint JNICALL
Java_com_music_aurelis_playback_smart_MelSpectrogram_nativeHop(
    JNIEnv* /* env */,
    jclass /* clazz */) {
  return static_cast<jint>(aurelis::smart::kBeatSpectrogramHop);
}

}  // extern "C"
