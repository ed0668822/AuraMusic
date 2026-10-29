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

#include "analyzer/vocal_spectrogram.h"

extern "C" {

JNIEXPORT jfloatArray JNICALL
Java_com_music_aurelis_playback_smart_VocalSpectrogram_nativeCompute(
    JNIEnv* env,
    jclass /* clazz */,
    jfloatArray left,
    jfloatArray right,
    jdouble sample_rate) {
  const jsize left_count = env->GetArrayLength(left);
  const jsize right_count = env->GetArrayLength(right);

  std::vector<std::vector<float>> channels(2);
  channels[0].resize(static_cast<size_t>(left_count));
  channels[1].resize(static_cast<size_t>(right_count));
  if (left_count > 0) env->GetFloatArrayRegion(left, 0, left_count, channels[0].data());
  if (right_count > 0) env->GetFloatArrayRegion(right, 0, right_count, channels[1].data());

  const aurelis::smart::VocalSpectrogram spectrogram =
      aurelis::smart::ComputeVocalSpectrogram(channels, sample_rate);

  const jsize produced = static_cast<jsize>(spectrogram.values.size());
  jfloatArray result = env->NewFloatArray(produced);
  if (result == nullptr) return nullptr;
  if (produced > 0) {
    env->SetFloatArrayRegion(result, 0, produced, spectrogram.values.data());
  }
  return result;
}

JNIEXPORT jint JNICALL
Java_com_music_aurelis_playback_smart_VocalSpectrogram_nativeBins(
    JNIEnv* /* env */, jclass /* clazz */) {
  return static_cast<jint>(aurelis::smart::kVocalSpectrogramBins);
}

JNIEXPORT jdouble JNICALL
Java_com_music_aurelis_playback_smart_VocalSpectrogram_nativeSampleRate(
    JNIEnv* /* env */, jclass /* clazz */) {
  return aurelis::smart::kVocalSpectrogramSampleRate;
}

JNIEXPORT jint JNICALL
Java_com_music_aurelis_playback_smart_VocalSpectrogram_nativeHop(
    JNIEnv* /* env */, jclass /* clazz */) {
  return static_cast<jint>(aurelis::smart::kVocalSpectrogramHop);
}

JNIEXPORT jint JNICALL
Java_com_music_aurelis_playback_smart_VocalSpectrogram_nativeFftSize(
    JNIEnv* /* env */, jclass /* clazz */) {
  return static_cast<jint>(aurelis::smart::kVocalSpectrogramFft);
}

}  // extern "C"
