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

#include "resampler.h"

#include <algorithm>
#include <cmath>

namespace aurelis::smart {
namespace {

constexpr double kPi = 3.14159265358979323846;
constexpr double kKernelResolution = 512.0;

double Sinc(double x) {
  if (std::abs(x) < 1e-12) return 1.0;
  const double scaled = kPi * x;
  return std::sin(scaled) / scaled;
}

double Blackman(double position) {
  return 0.42 - 0.5 * std::cos(2.0 * kPi * position) +
         0.08 * std::cos(4.0 * kPi * position);
}

}  // namespace

std::vector<float> Resample(
  const std::vector<float>& input,
  double input_rate,
  double output_rate
) {
  if (input.empty() || input_rate <= 0.0 || output_rate <= 0.0) return {};
  if (std::abs(input_rate - output_rate) < 1e-6) return input;

  const double ratio = output_rate / input_rate;
  const double cutoff = 0.5 * std::min(1.0, ratio);
  const double half_width = static_cast<double>(kResamplerZeroCrossings) / (2.0 * cutoff);

  const size_t output_count =
    static_cast<size_t>(std::floor(static_cast<double>(input.size()) * ratio));
  if (output_count == 0) return {};

  const size_t table_size =
    static_cast<size_t>(std::ceil(half_width * kKernelResolution)) + 2;
  std::vector<double> kernel(table_size);
  for (size_t index = 0; index < table_size; ++index) {
    const double offset = static_cast<double>(index) / kKernelResolution;
    const double window = Blackman((offset + half_width) / (2.0 * half_width));
    kernel[index] = 2.0 * cutoff * Sinc(2.0 * cutoff * offset) * window;
  }

  std::vector<float> output(output_count, 0.0f);
  const auto last = static_cast<long>(input.size()) - 1;

  for (size_t index = 0; index < output_count; ++index) {
    const double centre = static_cast<double>(index) / ratio;
    const long first_tap = static_cast<long>(std::ceil(centre - half_width));
    const long last_tap = static_cast<long>(std::floor(centre + half_width));

    double sum = 0.0;
    double weight_sum = 0.0;
    for (long tap = first_tap; tap <= last_tap; ++tap) {
      const double offset = std::abs(centre - static_cast<double>(tap));
      const double scaled = offset * kKernelResolution;
      const auto slot = static_cast<size_t>(scaled);
      if (slot + 1 >= table_size) continue;
      const double fraction = scaled - static_cast<double>(slot);
      const double weight =
        kernel[slot] + fraction * (kernel[slot + 1] - kernel[slot]);

      const long clamped = std::min(std::max(tap, 0L), last);
      sum += weight * static_cast<double>(input[static_cast<size_t>(clamped)]);
      weight_sum += weight;
    }

    output[index] = static_cast<float>(weight_sum != 0.0 ? sum / weight_sum : 0.0);
  }

  return output;
}

}  // namespace aurelis::smart
