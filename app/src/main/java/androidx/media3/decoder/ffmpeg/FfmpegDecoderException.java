package androidx.media3.decoder.ffmpeg;

import androidx.media3.common.util.UnstableApi;
import androidx.media3.decoder.DecoderException;

@UnstableApi
public final class FfmpegDecoderException extends DecoderException {
  FfmpegDecoderException(String message) { super(message); }
  FfmpegDecoderException(String message, Throwable cause) { super(message, cause); }
}
