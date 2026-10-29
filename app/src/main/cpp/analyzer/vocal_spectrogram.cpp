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

#include "vocal_spectrogram.h"

#include <cmath>
#include <complex>
#include <vector>

namespace aurelis::smart {
namespace {

constexpr double kPi = 3.14159265358979323846;

void Fft(std::vector<std::complex<double>>& values) {
  const size_t size = values.size();
  for (size_t index = 1, swapped = 0; index < size; ++index) {
    size_t bit = size >> 1;
    for (; swapped & bit; bit >>= 1) swapped ^= bit;
    swapped ^= bit;
    if (index < swapped) std::swap(values[index], values[swapped]);
  }
  for (size_t length = 2; length <= size; length <<= 1) {
    const std::complex<double> root = std::polar(1.0, -2.0 * kPi / length);
    for (size_t start = 0; start < size; start += length) {
      std::complex<double> weight(1, 0);
      for (size_t offset = 0; offset < length / 2; ++offset) {
        const auto even = values[start + offset];
        const auto odd = values[start + offset + length / 2] * weight;
        values[start + offset] = even + odd;
        values[start + offset + length / 2] = even - odd;
        weight *= root;
      }
    }
  }
}

std::vector<double> HannWindow(size_t size) {
  std::vector<double> window(size);
  for (size_t index = 0; index < size; ++index) {
    window[index] = 0.5 - 0.5 * std::cos(2.0 * kPi * static_cast<double>(index) / size);
  }
  return window;
}

}  // namespace

VocalSpectrogram ComputeVocalSpectrogram(
  const std::vector<std::vector<float>>& channels,
  double sample_rate
) {
  VocalSpectrogram result;
  if (std::abs(sample_rate - kVocalSpectrogramSampleRate) > 1.0) return result;
  if (channels.size() != kVocalSpectrogramChannels) return result;
  const size_t input_length = channels.front().size();
  if (channels[1].size() != input_length) return result;

  const size_t pad = kVocalSpectrogramFft / 2;
  if (input_length <= pad + 1) return result;

  const auto window = HannWindow(kVocalSpectrogramFft);
  std::vector<std::vector<float>> padded(kVocalSpectrogramChannels);
  for (size_t channel = 0; channel < kVocalSpectrogramChannels; ++channel) {
    auto& out = padded[channel];
    const auto& in = channels[channel];
    out.reserve(input_length + 2 * pad);
    for (size_t index = pad; index >= 1; --index) out.push_back(in[index]);
    out.insert(out.end(), in.begin(), in.end());
    for (size_t index = 1; index <= pad; ++index) out.push_back(in[input_length - 1 - index]);
  }

  const size_t padded_length = padded.front().size();
  if (padded_length < kVocalSpectrogramFft) return result;
  const size_t frames = (padded_length - kVocalSpectrogramFft) / kVocalSpectrogramHop + 1;

  result.frames = frames;
  result.values.assign(kVocalSpectrogramChannels * frames * kVocalSpectrogramBins, 0.0f);

  std::vector<std::complex<double>> spectrum(kVocalSpectrogramFft);
  for (size_t channel = 0; channel < kVocalSpectrogramChannels; ++channel) {
    const auto& source = padded[channel];
    for (size_t frame = 0; frame < frames; ++frame) {
      const size_t start = frame * kVocalSpectrogramHop;
      for (size_t index = 0; index < kVocalSpectrogramFft; ++index) {
        spectrum[index] = std::complex<double>(source[start + index] * window[index], 0.0);
      }
      Fft(spectrum);

      const size_t channel_base = channel * kVocalSpectrogramBins * frames;
      for (size_t bin = 0; bin < kVocalSpectrogramBins; ++bin) {
        result.values[channel_base + bin * frames + frame] =
          static_cast<float>(std::abs(spectrum[bin]));
      }
    }
  }
  return result;
}

}  // namespace aurelis::smart
