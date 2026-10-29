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

#include "audio_analysis.h"

#include <algorithm>
#include <cmath>
#include <complex>
#include <numeric>
#include <vector>

namespace aurelis::smart {
namespace {

constexpr double kPi = 3.14159265358979323846;
constexpr double kMaxEnvelopeSeconds = 1200.0;
constexpr double kMaxTempoSearchSeconds = 180.0;
constexpr double kLowBandHz = 150.0;

double Clamp(double value, double minimum, double maximum) {
  return std::max(minimum, std::min(maximum, value));
}

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

struct OnsetEnvelopes {
  std::vector<double> full;
  std::vector<double> low;
};

void NormalizeEnvelope(std::vector<double>& envelope, double frames_per_second) {
  if (envelope.empty()) return;
  const size_t radius = std::max<size_t>(2, static_cast<size_t>(frames_per_second * 0.35));
  std::vector<double> prefix(envelope.size() + 1, 0);
  for (size_t index = 0; index < envelope.size(); ++index) {
    prefix[index + 1] = prefix[index] + envelope[index];
  }
  for (size_t index = 0; index < envelope.size(); ++index) {
    const size_t left = index > radius ? index - radius : 0;
    const size_t right = std::min(envelope.size(), index + radius + 1);
    const double local_mean = (prefix[right] - prefix[left]) / std::max<size_t>(1, right - left);
    envelope[index] = std::max(0.0, envelope[index] - local_mean * 1.08);
  }

  const double peak = *std::max_element(envelope.begin(), envelope.end());
  if (peak > 0) {
    for (double& value : envelope) value = std::sqrt(value / peak);
  }
}

OnsetEnvelopes OnsetEnvelope(
  const std::vector<float>& samples,
  double sample_rate,
  size_t frame_size,
  size_t hop_size
) {
  OnsetEnvelopes result;
  const size_t maximum_samples = std::min(
    samples.size(),
    static_cast<size_t>(sample_rate * kMaxEnvelopeSeconds)
  );
  if (maximum_samples < frame_size) return result;

  const size_t frame_count = 1 + (maximum_samples - frame_size) / hop_size;
  const size_t low_band_bins = std::min<size_t>(
    frame_size / 2,
    std::max<size_t>(2, static_cast<size_t>(kLowBandHz * frame_size / sample_rate))
  );
  result.full.assign(frame_count, 0);
  result.low.assign(frame_count, 0);
  std::vector<double> previous(frame_size / 2, 0);
  std::vector<std::complex<double>> spectrum(frame_size);
  std::vector<double> window(frame_size);
  for (size_t index = 0; index < frame_size; ++index) {
    window[index] = 0.5 - 0.5 * std::cos(2.0 * kPi * index / (frame_size - 1));
  }

  for (size_t frame = 0; frame < frame_count; ++frame) {
    const size_t start = frame * hop_size;
    for (size_t index = 0; index < frame_size; ++index) {
      spectrum[index] = std::complex<double>(samples[start + index] * window[index], 0);
    }
    Fft(spectrum);

    double flux = 0;
    double low_flux = 0;
    for (size_t bin = 1; bin < frame_size / 2; ++bin) {
      const double magnitude = std::log1p(std::abs(spectrum[bin]));
      const double rise = std::max(0.0, magnitude - previous[bin]);
      flux += rise;
      if (bin < low_band_bins) low_flux += rise;
      previous[bin] = magnitude;
    }
    result.full[frame] = flux;
    result.low[frame] = low_flux;
  }

  const double frames_per_second = sample_rate / hop_size;
  NormalizeEnvelope(result.full, frames_per_second);
  NormalizeEnvelope(result.low, frames_per_second);
  return result;
}

double Correlation(const std::vector<double>& values, int lag, size_t limit) {
  const size_t length = std::min(limit, values.size());
  if (lag <= 0 || static_cast<size_t>(lag) >= length) return 0;
  double cross = 0;
  double left_energy = 0;
  double right_energy = 0;
  for (size_t index = lag; index < length; ++index) {
    const double left = values[index];
    const double right = values[index - lag];
    cross += left * right;
    left_energy += left * left;
    right_energy += right * right;
  }
  return cross / std::sqrt(std::max(1e-12, left_energy * right_energy));
}

double SampleEnvelope(const std::vector<double>& values, double position) {
  if (position < 0 || position >= values.size() - 1) return 0;
  const size_t left = static_cast<size_t>(position);
  const double fraction = position - left;
  return values[left] * (1.0 - fraction) + values[left + 1] * fraction;
}

double MetricalPrior(double bpm) {
  if (!(bpm > 0)) return 0;
  const double octaves = std::log2(bpm / 120.0) / 0.7;
  return std::exp(-0.5 * octaves * octaves);
}

}  // namespace

TempoResult AnalyzeTempo(
  const std::vector<float>& samples,
  double sample_rate,
  double duration,
  double audible_start
) {
  TempoResult result;
  constexpr size_t frame_size = 512;
  constexpr size_t hop_size = 128;
  const auto envelopes = OnsetEnvelope(samples, sample_rate, frame_size, hop_size);
  const auto& envelope = envelopes.full;
  if (envelope.size() < 64) return result;

  const double frames_per_second = sample_rate / hop_size;
  const size_t search_limit = std::min(
    envelope.size(),
    static_cast<size_t>(frames_per_second * kMaxTempoSearchSeconds)
  );
  const int minimum_lag = std::max(2, static_cast<int>(std::floor(frames_per_second * 60.0 / 200.0)));
  const int maximum_lag = static_cast<int>(std::ceil(frames_per_second * 60.0 / 70.0));
  std::vector<double> scores(maximum_lag + 1, 0);
  int best_lag = minimum_lag;
  for (int lag = minimum_lag; lag <= maximum_lag; ++lag) {
    const double bpm = frames_per_second * 60.0 / lag;
    const double tempo_prior = std::exp(-std::pow((bpm - 118.0) / 75.0, 2.0));
    scores[lag] = Correlation(envelope, lag, search_limit) +
      0.42 * Correlation(envelope, lag * 2, search_limit) +
      0.08 * tempo_prior;
    if (scores[lag] > scores[best_lag]) best_lag = lag;
  }

  {
    double best_metrical = -1;
    int metrical_lag = best_lag;
    for (const double ratio : {0.5, 1.0, 2.0}) {
      const int candidate = static_cast<int>(std::lround(best_lag * ratio));
      if (candidate < minimum_lag || candidate > maximum_lag) continue;
      const double bpm = frames_per_second * 60.0 / candidate;
      const double score =
        Correlation(envelope, candidate, search_limit) * MetricalPrior(bpm);
      if (score > best_metrical) {
        best_metrical = score;
        metrical_lag = candidate;
      }
    }
    best_lag = metrical_lag;
  }

  double refined_lag = best_lag;
  if (best_lag > minimum_lag && best_lag < maximum_lag) {
    const double left = Correlation(envelope, best_lag - 1, search_limit);
    const double center = Correlation(envelope, best_lag, search_limit);
    const double right = Correlation(envelope, best_lag + 1, search_limit);
    const double denominator = left - 2.0 * center + right;
    if (std::abs(denominator) > 1e-9) {
      refined_lag += Clamp(0.5 * (left - right) / denominator, -0.5, 0.5);
    }
  }

  const auto estimate_phase = [&](double lag, size_t limit) {
    const int phase_count = std::max(1, static_cast<int>(std::round(lag)));
    const double end = static_cast<double>(std::min(limit, envelope.size()));
    double best_score = -1;
    int best = 0;
    for (int phase = 0; phase < phase_count; ++phase) {
      double score = 0;
      int count = 0;
      for (double position = phase; position < end; position += lag) {
        score += SampleEnvelope(envelope, position);
        ++count;
      }
      score /= std::max(1, count);
      if (score > best_score) {
        best_score = score;
        best = phase;
      }
    }
    return std::pair<int, double>{best, best_score};
  };

  constexpr double kPhaseSearchSeconds = 30.0;
  auto [best_phase, best_phase_score] = estimate_phase(
    refined_lag,
    static_cast<size_t>(frames_per_second * kPhaseSearchSeconds)
  );

  const auto anchor_first_beat = [&](int phase, double lag) {
    const double interval_seconds = lag / frames_per_second;
    double first = phase / frames_per_second;
    while (first + interval_seconds < audible_start - 0.15) first += interval_seconds;
    while (first > audible_start + interval_seconds) first -= interval_seconds;
    result.first_beat = std::max(0.0, first);
  };
  anchor_first_beat(best_phase, refined_lag);

  const double envelope_end = static_cast<double>(envelope.size()) - 1;
  const double last_frame = duration * frames_per_second;

  struct TrackedGrid {
    std::vector<double> beats;
    std::vector<double> intervals;
  };

  const auto track = [&](double start_lag) {
    TrackedGrid grid;
    const double search_radius = start_lag * 0.25;
    constexpr double kPhaseGain = 0.20;
    constexpr double kIntervalGain = 0.01;
    const double max_interval_drift = start_lag * 0.03;

    double position = result.first_beat * frames_per_second;
    double interval = start_lag;

    while (position <= last_frame + 1e-6) {
      grid.beats.push_back(std::max(0.0, position));
      const double predicted = position + interval;
      if (predicted > last_frame + 1e-6) break;

      if (predicted + search_radius < envelope_end) {
        double best_value = -1;
        double best_offset = 0;
        const int low = static_cast<int>(std::floor(predicted - search_radius));
        const int high = static_cast<int>(std::ceil(predicted + search_radius));
        for (int frame = std::max(0, low);
             frame <= high && frame < static_cast<int>(envelope.size()); ++frame) {
          if (envelope[frame] > best_value) {
            best_value = envelope[frame];
            best_offset = frame;
          }
        }
        if (best_value > 0.15) {
          const auto index = static_cast<size_t>(best_offset);
          if (index > 0 && index + 1 < envelope.size()) {
            const double left = envelope[index - 1];
            const double center = envelope[index];
            const double right = envelope[index + 1];
            const double denominator = left - 2.0 * center + right;
            if (std::abs(denominator) > 1e-9) {
              best_offset += Clamp(0.5 * (left - right) / denominator, -0.5, 0.5);
            }
          }
          const double error = best_offset - predicted;
          position = predicted + kPhaseGain * error;
          interval = Clamp(
            interval + kIntervalGain * error,
            start_lag - max_interval_drift,
            start_lag + max_interval_drift
          );
          grid.intervals.push_back(interval);
          continue;
        }
      }
      position = predicted;
    }
    return grid;
  };

  const auto learning = track(refined_lag);
  auto grid = learning;
  if (learning.intervals.size() >= 8) {
    auto sorted = learning.intervals;
    std::nth_element(sorted.begin(), sorted.begin() + sorted.size() / 2, sorted.end());
    const double locked = sorted[sorted.size() / 2];
    if (locked > 0) refined_lag = locked;
    const auto relocked = estimate_phase(refined_lag, envelope.size());
    best_phase = relocked.first;
    best_phase_score = relocked.second;
    anchor_first_beat(best_phase, refined_lag);
    grid = track(refined_lag);
  }
  result.beats.reserve(grid.beats.size());
  for (const double frame : grid.beats) result.beats.push_back(frame / frames_per_second);
  result.bpm = frames_per_second * 60.0 / refined_lag;
  result.beat_interval = 60.0 / result.bpm;

  int downbeat_offset = 0;
  double downbeat_score = -1;
  for (int offset = 0; offset < 4; ++offset) {
    double low_score = 0;
    double full_score = 0;
    int count = 0;
    for (size_t beat = offset; beat < result.beats.size() && beat < 256; beat += 4) {
      const double position_frames = result.beats[beat] * frames_per_second;
      low_score += SampleEnvelope(envelopes.low, position_frames);
      full_score += SampleEnvelope(envelope, position_frames);
      ++count;
    }
    const double score = (low_score + 0.4 * full_score) / std::max(1, count);
    if (score > downbeat_score) {
      downbeat_score = score;
      downbeat_offset = offset;
    }
  }
  for (size_t beat = downbeat_offset; beat < result.beats.size(); beat += 4) {
    result.downbeats.push_back(result.beats[beat]);
  }

  const double frame_centre_seconds = frame_size / (2.0 * sample_rate);
  result.first_beat += frame_centre_seconds;
  for (double& beat : result.beats) beat += frame_centre_seconds;
  for (double& beat : result.downbeats) beat += frame_centre_seconds;

  double runner_up = 0;
  for (int lag = minimum_lag; lag <= maximum_lag; ++lag) {
    if (std::abs(lag - best_lag) > 2) runner_up = std::max(runner_up, scores[lag]);
  }
  const double separation = (scores[best_lag] - runner_up) / std::max(0.05, scores[best_lag]);
  result.confidence = Clamp(
    0.35 * scores[best_lag] + 0.35 * best_phase_score + 0.3 * std::max(0.0, separation),
    0.0,
    1.0
  );
  if (!std::isfinite(result.bpm) || result.bpm < 60 || result.bpm > 220) return TempoResult{};
  return result;
}

}  // namespace aurelis::smart
