package androidx.media3.decoder.ffmpeg;

import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.MediaLibraryInfo;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.util.LibraryLoader;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.UnstableApi;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

@UnstableApi
public final class FfmpegLibrary {
  static { MediaLibraryInfo.registerModule("media3.decoder.ffmpeg"); }
  private static final String TAG = "FfmpegLibrary";
  private static final LibraryLoader LOADER = new LibraryLoader("ffmpegJNI") {
    @Override protected void loadLibrary(String name) { System.loadLibrary(name); }
  };
  private static @MonotonicNonNull String version;
  private static int inputBufferPaddingSize = C.LENGTH_UNSET;
  private FfmpegLibrary() {}

  public static void setLibraries(String... libraries) { LOADER.setLibraries(libraries); }
  public static boolean isAvailable() { return LOADER.isAvailable(); }
  
  @Nullable
  public static String getVersion() {
    if (!isAvailable()) return null;
    if (version == null) version = ffmpegGetVersion();
    return version;
  }

  public static int getInputBufferPaddingSize() {
    if (!isAvailable()) return C.LENGTH_UNSET;
    if (inputBufferPaddingSize == C.LENGTH_UNSET) inputBufferPaddingSize = ffmpegGetInputBufferPaddingSize();
    return inputBufferPaddingSize;
  }

  public static boolean supportsFormat(String mimeType) {
    if (!isAvailable()) return false;
    @Nullable String codecName = getCodecName(mimeType);
    if (codecName == null) return false;
    if (!ffmpegHasDecoder(codecName)) {
      Log.w(TAG, "No " + codecName + " decoder available. Check the FFmpeg build configuration.");
      return false;
    }
    return true;
  }

  @Nullable
  static String getCodecName(String mimeType) {
    switch (mimeType) {
      case MimeTypes.AUDIO_AAC: return "aac";
      case MimeTypes.AUDIO_MPEG:
      case MimeTypes.AUDIO_MPEG_L1:
      case MimeTypes.AUDIO_MPEG_L2: return "mp3";
      case MimeTypes.AUDIO_AC3: return "ac3";
      case MimeTypes.AUDIO_E_AC3:
      case MimeTypes.AUDIO_E_AC3_JOC: return "eac3";
      case MimeTypes.AUDIO_TRUEHD: return "truehd";
      case MimeTypes.AUDIO_AC4: return "ac4";
      case MimeTypes.AUDIO_DTS:
      case MimeTypes.AUDIO_DTS_EXPRESS:
      case MimeTypes.AUDIO_DTS_HD: return "dca";
      case MimeTypes.AUDIO_VORBIS: return "vorbis";
      case MimeTypes.AUDIO_OPUS: return "opus";
      case MimeTypes.AUDIO_AMR_NB: return "amrnb";
      case MimeTypes.AUDIO_AMR_WB: return "amrwb";
      case MimeTypes.AUDIO_FLAC: return "flac";
      case MimeTypes.AUDIO_ALAC: return "alac";
      case MimeTypes.AUDIO_MLAW: return "pcm_mulaw";
      case MimeTypes.AUDIO_ALAW: return "pcm_alaw";
      case MimeTypes.VIDEO_H264: return "h264";
      case MimeTypes.VIDEO_H265: return "hevc";
      default: return null;
    }
  }

  private static native String ffmpegGetVersion();
  private static native int ffmpegGetInputBufferPaddingSize();
  private static native boolean ffmpegHasDecoder(String codecName);
}
