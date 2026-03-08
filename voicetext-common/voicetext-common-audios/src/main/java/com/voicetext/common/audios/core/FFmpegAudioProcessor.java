package com.voicetext.common.audios.core;
//
//import com.voicetext.common.audio.config.AudioProperties;
//import com.voicetext.common.audio.dsp.model.AudioSegment;
//import lombok.extern.slf4j.Slf4j;
//import org.bytedeco.ffmpeg.global.avcodec;
//import org.bytedeco.javacv.*;
//
//import java.io.File;
//import java.io.PrintWriter;
//import java.nio.ShortBuffer;
//import java.util.*;
//import java.util.concurrent.ConcurrentHashMap;
//
///**
// * 基于FFmpeg的音频处理器实现
// * 功能更强大，支持更多格式和操作
// */
//@Slf4j
//public class FFmpegAudioProcessor implements com.voicetext.common.audio.core.AudioProcessor {
//
//    private final AudioProperties properties;
//    private final Map<String, Object> cache = new ConcurrentHashMap<>();
//    private final File tempDir;
//
//    public FFmpegAudioProcessor(AudioProperties properties) {
//        this.properties = properties;
//        this.tempDir = new File(properties.getProcessor().getTempDir());
//        if (!tempDir.exists()) {
//            tempDir.mkdirs();
//        }
//    }
//
//    @Override
//    public Map<String, Object> getAudioInfo(File audioFile) {
//        Map<String, Object> info = new HashMap<>();
//        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(audioFile)) {
//            grabber.start();
//
//            info.put("format", grabber.getFormat());
//            info.put("sampleRate", grabber.getSampleRate());
//            info.put("channels", grabber.getAudioChannels());
//            info.put("bitrate", grabber.getAudioBitrate());
//            info.put("duration", grabber.getLengthInTime() / 1_000_000.0); // 微秒转秒
//            info.put("frameCount", grabber.getLengthInFrames());
//            info.put("fileSize", audioFile.length());
//
//            grabber.stop();
//
//        } catch (Exception e) {
//            log.error("获取音频信息失败(FFmpeg): {}", audioFile.getName(), e);
//            throw new RuntimeException("获取音频信息失败", e);
//        }
//        return info;
//    }
//
//    @Override
//    public File convertFormat(File inputFile, String targetFormat, Map<String, Object> options) {
//        String cacheKey = "convert_" + inputFile.getName() + "_" + targetFormat + "_" + options;
//        if (properties.getProcessor().isEnableCache() && cache.containsKey(cacheKey)) {
//            return (File) cache.get(cacheKey);
//        }
//
//        File outputFile = new File(tempDir, "convert_" + UUID.randomUUID() + "." + targetFormat);
//
//        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(inputFile);
//             FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(outputFile, grabber.getAudioChannels())) {
//
//            grabber.start();
//
//            // 设置输出格式
//            recorder.setAudioCodec(getCodecForFormat(targetFormat));
//            recorder.setAudioBitrate(options != null && options.containsKey("bitrate")
//                ? (Integer) options.get("bitrate") : 192000);
//            recorder.setSampleRate(options != null && options.containsKey("sampleRate")
//                ? (Integer) options.get("sampleRate") : grabber.getSampleRate());
//            recorder.setAudioChannels(grabber.getAudioChannels());
//
//            recorder.start();
//
//            Frame frame;
//            while ((frame = grabber.grabFrame()) != null) {
//                if (frame.samples != null) {
//                    recorder.record(frame);
//                }
//            }
//
//            grabber.stop();
//            recorder.stop();
//
//            if (properties.getProcessor().isEnableCache()) {
//                cache.put(cacheKey, outputFile);
//            }
//
//            return outputFile;
//
//        } catch (Exception e) {
//            log.error("格式转换失败: {} -> {}", inputFile.getName(), targetFormat, e);
//            throw new RuntimeException("格式转换失败", e);
//        }
//    }
//
//    @Override
//    public File trim(File inputFile, double startTime, double endTime) {
//        String cacheKey = "trim_ff_" + inputFile.getName() + "_" + startTime + "_" + endTime;
//        if (properties.getProcessor().isEnableCache() && cache.containsKey(cacheKey)) {
//            return (File) cache.get(cacheKey);
//        }
//
//        File outputFile = new File(tempDir, "trim_ff_" + UUID.randomUUID() + ".wav");
//
//        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(inputFile);
//             FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(outputFile, grabber.getAudioChannels())) {
//
//            grabber.start();
//
//            // 设置时间戳(微秒)
//            long startTimestamp = (long) (startTime * 1_000_000);
//            long endTimestamp = (long) (endTime * 1_000_000);
//
//            grabber.setTimestamp(startTimestamp);
//
//            recorder.setAudioCodec(avcodec.AV_CODEC_ID_PCM_S16LE);
//            recorder.setSampleRate(grabber.getSampleRate());
//            recorder.setAudioChannels(grabber.getAudioChannels());
//            recorder.start();
//
//            Frame frame;
//            while ((frame = grabber.grabFrame()) != null) {
//                if (grabber.getTimestamp() > endTimestamp) {
//                    break;
//                }
//                if (frame.samples != null) {
//                    recorder.record(frame);
//                }
//            }
//
//            grabber.stop();
//            recorder.stop();
//
//            if (properties.getProcessor().isEnableCache()) {
//                cache.put(cacheKey, outputFile);
//            }
//
//            return outputFile;
//
//        } catch (Exception e) {
//            log.error("裁剪音频失败(FFmpeg): {}", inputFile.getName(), e);
//            throw new RuntimeException("裁剪音频失败", e);
//        }
//    }
//
//    @Override
//    public File concat(List<File> audioFiles, double crossFade) {
//        // 简化实现：使用FFmpeg的concat协议
//        // 实际应用中需要处理不同格式、采样率等问题
//        try {
//            // 创建concat文件列表
//            File listFile = new File(tempDir, "concat_list_" + UUID.randomUUID() + ".txt");
//            try (PrintWriter writer = new PrintWriter(listFile)) {
//                for (File file : audioFiles) {
//                    writer.println("file '" + file.getAbsolutePath() + "'");
//                }
//            }
//
//            File outputFile = new File(tempDir, "concat_" + UUID.randomUUID() + ".wav");
//
//            // 使用FFmpeg命令行
//            ProcessBuilder pb = new ProcessBuilder(
//                "ffmpeg", "-f", "concat", "-safe", "0", "-i", listFile.getAbsolutePath(),
//                "-c", "copy", outputFile.getAbsolutePath()
//            );
//
//            Process process = pb.start();
//            int exitCode = process.waitFor();
//
//            if (exitCode != 0) {
//                throw new RuntimeException("FFmpeg concat失败，退出码: " + exitCode);
//            }
//
//            listFile.delete();
//            return outputFile;
//
//        } catch (Exception e) {
//            log.error("拼接音频失败(FFmpeg)", e);
//            throw new RuntimeException("拼接音频失败", e);
//        }
//    }
//
//    @Override
//    public File adjustVolume(File inputFile, double ratio) {
//        File outputFile = new File(tempDir, "volume_ff_" + UUID.randomUUID() + ".wav");
//
//        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(inputFile);
//             FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(outputFile, grabber.getAudioChannels())) {
//
//            grabber.start();
//
//            recorder.setAudioCodec(avcodec.AV_CODEC_ID_PCM_S16LE);
//            recorder.setSampleRate(grabber.getSampleRate());
//            recorder.setAudioChannels(grabber.getAudioChannels());
//            recorder.start();
//
//            Frame frame;
//            while ((frame = grabber.grabFrame()) != null) {
//                if (frame.samples != null) {
//                    // 调整音量
//                    ShortBuffer[] samples = (ShortBuffer[]) frame.samples;
//                    for (ShortBuffer buffer : samples) {
//                        for (int i = 0; i < buffer.limit(); i++) {
//                            int original = buffer.get(i);
//                            int adjusted = (int) (original * ratio);
//                            buffer.put(i, (short) Math.min(32767, Math.max(-32768, adjusted)));
//                        }
//                    }
//                    recorder.record(frame);
//                }
//            }
//
//            grabber.stop();
//            recorder.stop();
//
//            return outputFile;
//
//        } catch (Exception e) {
//            log.error("调整音量失败(FFmpeg): {}", inputFile.getName(), e);
//            throw new RuntimeException("调整音量失败", e);
//        }
//    }
//
//    @Override
//    public File normalize(File inputFile, double targetDb) {
//        // 使用FFmpeg的loudnorm滤镜
//        File outputFile = new File(tempDir, "normalize_ff_" + UUID.randomUUID() + ".wav");
//
//        try {
//            ProcessBuilder pb = new ProcessBuilder(
//                "ffmpeg", "-i", inputFile.getAbsolutePath(),
//                "-af", "loudnorm=I=" + targetDb + ":TP=-1.5:LRA=11",
//                "-c:a", "pcm_s16le", outputFile.getAbsolutePath()
//            );
//
//            Process process = pb.start();
//            int exitCode = process.waitFor();
//
//            if (exitCode != 0) {
//                throw new RuntimeException("FFmpeg归一化失败，退出码: " + exitCode);
//            }
//
//            return outputFile;
//
//        } catch (Exception e) {
//            log.error("音量归一化失败(FFmpeg)", e);
//            return inputFile;
//        }
//    }
//
//    @Override
//    public File addFade(File inputFile, double fadeIn, double fadeOut) {
//        File outputFile = new File(tempDir, "fade_ff_" + UUID.randomUUID() + ".wav");
//
//        try {
//            // 构建滤镜字符串
//            StringBuilder af = new StringBuilder();
//            if (fadeIn > 0) {
//                af.append("afade=t=in:ss=").append(0).append(":d=").append(fadeIn);
//            }
//            if (fadeOut > 0) {
//                if (af.length() > 0) af.append(",");
//                af.append("afade=t=out:st=").append(getDuration(inputFile) - fadeOut)
//                  .append(":d=").append(fadeOut);
//            }
//
//            ProcessBuilder pb = new ProcessBuilder(
//                "ffmpeg", "-i", inputFile.getAbsolutePath(),
//                "-af", af.toString(),
//                "-c:a", "pcm_s16le", outputFile.getAbsolutePath()
//            );
//
//            Process process = pb.start();
//            int exitCode = process.waitFor();
//
//            if (exitCode != 0) {
//                throw new RuntimeException("FFmpeg添加淡入淡出失败，退出码: " + exitCode);
//            }
//
//            return outputFile;
//
//        } catch (Exception e) {
//            log.error("添加淡入淡出失败(FFmpeg)", e);
//            throw new RuntimeException("添加淡入淡出失败", e);
//        }
//    }
//
//    @Override
//    public File extractSegments(File inputFile, List<AudioSegment> segments) {
//        // 实现类似trim，但提取多个片段
//        try {
//            List<File> segmentFiles = new ArrayList<>();
//            for (int i = 0; i < segments.size(); i++) {
//                AudioSegment seg = segments.get(i);
//                File trimmed = trim(inputFile, seg.getStartTime(), seg.getEndTime());
//                segmentFiles.add(trimmed);
//            }
//
//            if (segmentFiles.size() == 1) {
//                return segmentFiles.get(0);
//            } else {
//                return concat(segmentFiles, 0);
//            }
//
//        } catch (Exception e) {
//            log.error("提取片段失败(FFmpeg)", e);
//            throw new RuntimeException("提取片段失败", e);
//        }
//    }
//
//    @Override
//    public float[] generateWaveform(File inputFile, int samplesCount) {
//        String cacheKey = "waveform_ff_" + inputFile.getName() + "_" + samplesCount;
//        if (properties.getProcessor().isEnableCache() && cache.containsKey(cacheKey)) {
//            return (float[]) cache.get(cacheKey);
//        }
//
//        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(inputFile)) {
//            grabber.start();
//
//            int totalFrames = grabber.getLengthInFrames();
//            int step = Math.max(1, totalFrames / samplesCount);
//
//            float[] waveform = new float[samplesCount];
//            float[] maxValues = new float[samplesCount];
//            Arrays.fill(maxValues, 0);
//
//            int sampleIndex = 0;
//            int frameCount = 0;
//
//            Frame frame;
//            while ((frame = grabber.grabFrame()) != null && sampleIndex < samplesCount) {
//                if (frame.samples != null) {
//                    // 处理音频帧
//                    ShortBuffer[] buffers = (ShortBuffer[]) frame.samples;
//                    for (ShortBuffer buffer : buffers) {
//                        for (int i = 0; i < buffer.limit(); i++) {
//                            float value = Math.abs(buffer.get(i) / 32768.0f);
//                            if (value > maxValues[sampleIndex]) {
//                                maxValues[sampleIndex] = value;
//                            }
//                        }
//                    }
//
//                    frameCount++;
//                    if (frameCount >= step) {
//                        waveform[sampleIndex] = maxValues[sampleIndex];
//                        sampleIndex++;
//                        frameCount = 0;
//                    }
//                }
//            }
//
//            grabber.stop();
//
//            if (properties.getProcessor().isEnableCache()) {
//                cache.put(cacheKey, waveform);
//            }
//
//            return waveform;
//
//        } catch (Exception e) {
//            log.error("生成波形失败(FFmpeg): {}", inputFile.getName(), e);
//            throw new RuntimeException("生成波形失败", e);
//        }
//    }
//
//    @Override
//    public String generateFingerprint(File inputFile) {
//        // 可以使用FFmpeg的chromaprint
//        try {
//            ProcessBuilder pb = new ProcessBuilder(
//                "ffmpeg", "-i", inputFile.getAbsolutePath(),
//                "-f", "chromaprint", "-fp_format", "raw", "-"
//            );
//
//            Process process = pb.start();
//            byte[] fingerprint = process.getInputStream().readAllBytes();
//            int exitCode = process.waitFor();
//
//            if (exitCode == 0 && fingerprint.length > 0) {
//                return Base64.getEncoder().encodeToString(fingerprint);
//            }
//
//        } catch (Exception e) {
//            log.warn("生成音频指纹失败，使用默认方式", e);
//        }
//
//        return UUID.randomUUID().toString();
//    }
//
//    @Override
//    public boolean isFormatSupported(String format) {
//        // FFmpeg支持几乎所有格式
//        return true;
//    }
//
//    /**
//     * 获取音频时长
//     */
//    private double getDuration(File inputFile) {
//        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(inputFile)) {
//            grabber.start();
//            double duration = grabber.getLengthInTime() / 1_000_000.0;
//            grabber.stop();
//            return duration;
//        } catch (Exception e) {
//            log.error("获取时长失败", e);
//            return 0;
//        }
//    }
//
//    /**
//     * 根据格式获取编码器
//     */
//    private int getCodecForFormat(String format) {
//        switch (format.toLowerCase()) {
//            case "mp3":
//                return avcodec.AV_CODEC_ID_MP3;
//            case "aac":
//                return avcodec.AV_CODEC_ID_AAC;
//            case "flac":
//                return avcodec.AV_CODEC_ID_FLAC;
//            case "wav":
//            default:
//                return avcodec.AV_CODEC_ID_PCM_S16LE;
//        }
//    }
//}
