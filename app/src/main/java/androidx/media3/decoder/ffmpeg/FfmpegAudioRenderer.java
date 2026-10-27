package androidx.media3.decoder.ffmpeg;

import static androidx.media3.exoplayer.audio.AudioSink.SINK_FORMAT_SUPPORTED_DIRECTLY;
import static androidx.media3.exoplayer.audio.AudioSink.SINK_FORMAT_SUPPORTED_WITH_TRANSCODING;
import static androidx.media3.exoplayer.audio.AudioSink.SINK_FORMAT_UNSUPPORTED;
import static com.google.common.base.Preconditions.checkNotNull;

import android.content.Context;
import android.os.Handler;
import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.audio.AudioProcessor;
import androidx.media3.common.util.TraceUtil;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.common.util.Util;
import androidx.media3.decoder.CryptoConfig;
import androidx.media3.exoplayer.audio.AudioRendererEventListener;
import androidx.media3.exoplayer.audio.AudioSink;
import androidx.media3.exoplayer.audio.AudioSink.SinkFormatSupport;
import androidx.media3.exoplayer.audio.DecoderAudioRenderer;
import androidx.media3.exoplayer.audio.DefaultAudioSink;

@UnstableApi
public final class FfmpegAudioRenderer extends DecoderAudioRenderer<FfmpegAudioDecoder> {
  private static final String TAG = "FfmpegAudioRenderer";
  private static final int NUM_BUFFERS = 16;
  private static final int DEFAULT_INPUT_BUFFER_SIZE = 960 * 6;

  @SuppressWarnings("deprecation")
  @Deprecated
  public FfmpegAudioRenderer() { this(null, null); }

  public FfmpegAudioRenderer(Context context) { this(context, null, null); }

  @SuppressWarnings("deprecation")
  @Deprecated
  public FfmpegAudioRenderer(@Nullable Handler eventHandler, @Nullable AudioRendererEventListener eventListener, AudioProcessor... audioProcessors) {
    this(eventHandler, eventListener, new DefaultAudioSink.Builder().setAudioProcessors(audioProcessors).build());
  }

  public FfmpegAudioRenderer(Context context, @Nullable Handler eventHandler, @Nullable AudioRendererEventListener eventListener, AudioProcessor... audioProcessors) {
    this(eventHandler, eventListener, new DefaultAudioSink.Builder(context).setAudioProcessors(audioProcessors).build());
  }

  public FfmpegAudioRenderer(@Nullable Handler eventHandler, @Nullable AudioRendererEventListener eventListener, AudioSink audioSink) {
    super(eventHandler, eventListener, audioSink);
  }

  @Override public String getName() { return TAG; }

  @Override
  protected @C.FormatSupport int supportsFormatInternal(Format format) {
    String mimeType = checkNotNull(format.sampleMimeType);
    if (!FfmpegLibrary.isAvailable() || !MimeTypes.isAudio(mimeType)) return C.FORMAT_UNSUPPORTED_TYPE;
    else if (!FfmpegLibrary.supportsFormat(mimeType) || (!sinkSupportsFormat(format, C.ENCODING_PCM_16BIT) && !sinkSupportsFormat(format, C.ENCODING_PCM_FLOAT))) return C.FORMAT_UNSUPPORTED_SUBTYPE;
    else if (format.cryptoType != C.CRYPTO_TYPE_NONE) return C.FORMAT_UNSUPPORTED_DRM;
    else return C.FORMAT_HANDLED;
  }

  @Override public @AdaptiveSupport int supportsMixedMimeTypeAdaptation() { return ADAPTIVE_NOT_SEAMLESS; }

  @Override
  protected FfmpegAudioDecoder createDecoder(Format format, @Nullable CryptoConfig cryptoConfig) throws FfmpegDecoderException {
    TraceUtil.beginSection("createFfmpegAudioDecoder");
    int initialInputBufferSize = format.maxInputSize != Format.NO_VALUE ? format.maxInputSize : DEFAULT_INPUT_BUFFER_SIZE;
    FfmpegAudioDecoder decoder = new FfmpegAudioDecoder(format, NUM_BUFFERS, NUM_BUFFERS, initialInputBufferSize, shouldOutputFloat(format));
    TraceUtil.endSection();
    return decoder;
  }

  @Override
  protected Format getOutputFormat(FfmpegAudioDecoder decoder) {
    checkNotNull(decoder);
    return new Format.Builder().setSampleMimeType(MimeTypes.AUDIO_RAW).setChannelCount(decoder.getChannelCount()).setSampleRate(decoder.getSampleRate()).setPcmEncoding(decoder.getEncoding()).build();
  }

  private boolean sinkSupportsFormat(Format inputFormat, @C.PcmEncoding int pcmEncoding) {
    return sinkSupportsFormat(Util.getPcmFormat(pcmEncoding, inputFormat.channelCount, inputFormat.sampleRate));
  }

  private boolean shouldOutputFloat(Format inputFormat) {
    if (!sinkSupportsFormat(inputFormat, C.ENCODING_PCM_16BIT)) return true;
    @SinkFormatSupport int formatSupport = getSinkFormatSupport(Util.getPcmFormat(C.ENCODING_PCM_FLOAT, inputFormat.channelCount, inputFormat.sampleRate));
    switch (formatSupport) {
      case SINK_FORMAT_SUPPORTED_DIRECTLY: return !MimeTypes.AUDIO_AC3.equals(inputFormat.sampleMimeType);
      case SINK_FORMAT_UNSUPPORTED:
      case SINK_FORMAT_SUPPORTED_WITH_TRANSCODING:
      default: return false;
    }
  }
}
