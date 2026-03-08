package com.voicetext.common.audios.core;

//import com.voicetext.common.audio.config.AudioProperties;
//import com.voicetext.common.audio.dsp.model.AudioSegment;
//import lombok.extern.slf4j.Slf4j;
//
//import javax.sound.sampled.AudioFileFormat;
//import javax.sound.sampled.AudioFormat;
//import javax.sound.sampled.AudioInputStream;
//import javax.sound.sampled.AudioSystem;
//import java.io.ByteArrayInputStream;
//import java.io.File;
//import java.util.*;
//import java.util.concurrent.ConcurrentHashMap;
//
///**
// * 基于TarsosDSP的音频处理器实现
// */
//@Slf4j
//public class TarsosAudioProcessor implements com.voicetext.common.audio.core.AudioProcessor {
//
//    private final AudioProperties properties;
//    private final Map<String, Object> cache = new ConcurrentHashMap<>();
//    private final File tempDir;
//
//    public TarsosAudioProcessor(AudioProperties properties) {
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
//        try {
//            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(audioFile);
//            AudioFormat format = audioInputStream.getFormat();
//
//            info.put("format", format.getEncoding());
//            info.put("sampleRate", format.getSampleRate());
//            info.put("sampleSizeInBits", format.getSampleSizeInBits());
//            info.put("channels", format.getChannels());
//            info.put("frameRate", format.getFrameRate());
//            info.put("frameSize", format.getFrameSize());
//            info.put("bigEndian", format.isBigEndian());
//
//            // 计算时长
//            long frames = audioInputStream.getFrameLength();
//            double duration = frames / format.getFrameRate();
//            info.put("duration", duration);
//
//            // 文件大小
//            info.put("fileSize", audioFile.length());
//
//            audioInputStream.close();
//
//        } catch (Exception e) {
//            log.error("获取音频信息失败: {}", audioFile.getName(), e);
//            throw new RuntimeException("获取音频信息失败", e);
//        }
//        return info;
//    }
//
//    @Override
//    public File convertFormat(File inputFile, String targetFormat, Map<String, Object> options) {
//        // TarsosDSP支持有限，这里简化实现
//        // 实际项目中应该调用FFmpeg
//        log.warn("TarsosDSP格式转换能力有限，建议使用FFmpegProcessor");
//        return inputFile;
//    }
//
//    @Override
//    public File trim(File inputFile, double startTime, double endTime) {
//        String cacheKey = "trim_" + inputFile.getName() + "_" + startTime + "_" + endTime;
//        if (properties.getProcessor().isEnableCache() && cache.containsKey(cacheKey)) {
//            return (File) cache.get(cacheKey);
//        }
//
//        try {
//            AudioInputStream originalStream = AudioSystem.getAudioInputStream(inputFile);
//            AudioFormat format = originalStream.getFormat();
//
//            long startFrame = (long) (startTime * format.getFrameRate());
//            long endFrame = (long) (endTime * format.getFrameRate());
//            long framesToSkip = startFrame;
//            long framesToRead = endFrame - startFrame;
//
//            originalStream.skip(framesToSkip * format.getFrameSize());
//
//            AudioInputStream trimmedStream = new AudioInputStream(
//                originalStream, format, framesToRead);
//
//            File outputFile = new File(tempDir, "trim_" + UUID.randomUUID() + ".wav");
//            AudioSystem.write(trimmedStream, AudioFileFormat.Type.WAVE, outputFile);
//
//            trimmedStream.close();
//            originalStream.close();
//
//            if (properties.getProcessor().isEnableCache()) {
//                cache.put(cacheKey, outputFile);
//            }
//
//            return outputFile;
//
//        } catch (Exception e) {
//            log.error("裁剪音频失败: {}", inputFile.getName(), e);
//            throw new RuntimeException("裁剪音频失败", e);
//        }
//    }
//
//    @Override
//    public File concat(List<File> audioFiles, double crossFade) {
//        // TarsosDSP不直接支持拼接，这里返回第一个文件作为占位
//        log.warn("TarsosDSP不直接支持拼接，建议使用FFmpegProcessor");
//        return audioFiles.get(0);
//    }
//
//    @Override
//    public File adjustVolume(File inputFile, double ratio) {
//        try {
//            AudioInputStream originalStream = AudioSystem.getAudioInputStream(inputFile);
//            AudioFormat format = originalStream.getFormat();
//
//            // 读取所有音频数据
//            byte[] audioBytes = originalStream.readAllBytes();
//            short[] samples = new short[audioBytes.length / 2];
//
//            // 将byte数组转换为short数组(16位PCM)
//            for (int i = 0; i < samples.length; i++) {
//                samples[i] = (short) ((audioBytes[i * 2] & 0xff) | (audioBytes[i * 2 + 1] << 8));
//            }
//
//            // 调整音量
//            for (int i = 0; i < samples.length; i++) {
//                samples[i] = (short) Math.min(32767, Math.max(-32768, samples[i] * ratio));
//            }
//
//            // 转换回byte数组
//            for (int i = 0; i < samples.length; i++) {
//                audioBytes[i * 2] = (byte) (samples[i] & 0xff);
//                audioBytes[i * 2 + 1] = (byte) ((samples[i] >> 8) & 0xff);
//            }
//
//            // 创建新的音频流
//            ByteArrayInputStream bais = new ByteArrayInputStream(audioBytes);
//            AudioInputStream adjustedStream = new AudioInputStream(
//                bais, format, audioBytes.length / format.getFrameSize());
//
//            File outputFile = new File(tempDir, "volume_" + UUID.randomUUID() + ".wav");
//            AudioSystem.write(adjustedStream, AudioFileFormat.Type.WAVE, outputFile);
//
//            adjustedStream.close();
//            originalStream.close();
//
//            return outputFile;
//
//        } catch (Exception e) {
//            log.error("调整音量失败: {}", inputFile.getName(), e);
//            throw new RuntimeException("调整音量失败", e);
//        }
//    }
//
//    @Override
//    public File normalize(File inputFile, double targetDb) {
//        // 简化实现：先计算当前音量，再调整
//        try {
//            double currentDb = measureVolume(inputFile);
//            double ratio = Math.pow(10, (targetDb - currentDb) / 20);
//            return adjustVolume(inputFile, ratio);
//        } catch (Exception e) {
//            log.error("音量归一化失败", e);
//            return inputFile;
//        }
//    }
//
//    @Override
//    public File addFade(File inputFile, double fadeIn, double fadeOut) {
//        try {
//            AudioInputStream originalStream = AudioSystem.getAudioInputStream(inputFile);
//            AudioFormat format = originalStream.getFormat();
//            float sampleRate = format.getSampleRate();
//
//            // 读取所有音频数据
//            byte[] audioBytes = originalStream.readAllBytes();
//            short[] samples = new short[audioBytes.length / 2];
//
//            for (int i = 0; i < samples.length; i++) {
//                samples[i] = (short) ((audioBytes[i * 2] & 0xff) | (audioBytes[i * 2 + 1] << 8));
//            }
//
//            // 淡入
//            int fadeInSamples = (int) (fadeIn * sampleRate);
//            for (int i = 0; i < fadeInSamples && i < samples.length; i++) {
//                double factor = (double) i / fadeInSamples;
//                samples[i] = (short) (samples[i] * factor);
//            }
//
//            // 淡出
//            int fadeOutSamples = (int) (fadeOut * sampleRate);
//            for (int i = 0; i < fadeOutSamples && i < samples.length; i++) {
//                int index = samples.length - 1 - i;
//                double factor = (double) i / fadeOutSamples;
//                samples[index] = (short) (samples[index] * (1 - factor));
//            }
//
//            // 转换回byte数组
//            for (int i = 0; i < samples.length; i++) {
//                audioBytes[i * 2] = (byte) (samples[i] & 0xff);
//                audioBytes[i * 2 + 1] = (byte) ((samples[i] >> 8) & 0xff);
//            }
//
//            ByteArrayInputStream bais = new ByteArrayInputStream(audioBytes);
//            AudioInputStream fadedStream = new AudioInputStream(
//                bais, format, audioBytes.length / format.getFrameSize());
//
//            File outputFile = new File(tempDir, "fade_" + UUID.randomUUID() + ".wav");
//            AudioSystem.write(fadedStream, AudioFileFormat.Type.WAVE, outputFile);
//
//            fadedStream.close();
//            originalStream.close();
//
//            return outputFile;
//
//        } catch (Exception e) {
//            log.error("添加淡入淡出失败: {}", inputFile.getName(), e);
//            throw new RuntimeException("添加淡入淡出失败", e);
//        }
//    }
//
//    @Override
//    public File extractSegments(File inputFile, List<AudioSegment> segments) {
//        // 简化实现：依次裁剪每个片段然后拼接
//        // 实际应用需要更复杂的处理
//        try {
//            List<File> segmentFiles = new ArrayList<>();
//            for (AudioSegment segment : segments) {
//                File trimmed = trim(inputFile, segment.getStartTime(), segment.getEndTime());
//                segmentFiles.add(trimmed);
//            }
//
//            // 如果有多个片段，拼接它们
//            if (segmentFiles.size() > 1) {
//                return concat(segmentFiles, 0);
//            } else if (segmentFiles.size() == 1) {
//                return segmentFiles.get(0);
//            } else {
//                throw new IllegalArgumentException("没有有效的片段");
//            }
//
//        } catch (Exception e) {
//            log.error("提取片段失败", e);
//            throw new RuntimeException("提取片段失败", e);
//        }
//    }
//
//    @Override
//    public float[] generateWaveform(File inputFile, int samplesCount) {
//        String cacheKey = "waveform_" + inputFile.getName() + "_" + samplesCount;
//        if (properties.getProcessor().isEnableCache() && cache.containsKey(cacheKey)) {
//            return (float[]) cache.get(cacheKey);
//        }
//
//        try {
//            AudioInputStream audioStream = AudioSystem.getAudioInputStream(inputFile);
//            AudioFormat format = audioStream.getFormat();
//
//            // 读取所有音频数据
//            byte[] audioBytes = audioStream.readAllBytes();
//            int sampleSize = format.getSampleSizeInBits() / 8;
//            int totalSamples = audioBytes.length / sampleSize / format.getChannels();
//
//            // 计算需要跳过的步长
//            int step = Math.max(1, totalSamples / samplesCount);
//
//            float[] waveform = new float[samplesCount];
//            short[] tempSamples = new short[step];
//
//            int sampleIndex = 0;
//            int byteIndex = 0;
//
//            for (int i = 0; i < samplesCount && byteIndex < audioBytes.length; i++) {
//                // 读取一个步长的样本
//                int samplesRead = 0;
//                for (int j = 0; j < step && byteIndex < audioBytes.length; j++) {
//                    if (byteIndex + 1 < audioBytes.length) {
//                        tempSamples[j] = (short) ((audioBytes[byteIndex] & 0xff) |
//                                                  (audioBytes[byteIndex + 1] << 8));
//                        byteIndex += sampleSize * format.getChannels();
//                        samplesRead++;
//                    }
//                }
//
//                // 计算平均值
//                float max = 0;
//                for (int j = 0; j < samplesRead; j++) {
//                    float absVal = Math.abs(tempSamples[j] / 32768.0f);
//                    if (absVal > max) {
//                        max = absVal;
//                    }
//                }
//
//                waveform[i] = max;
//            }
//
//            audioStream.close();
//
//            if (properties.getProcessor().isEnableCache()) {
//                cache.put(cacheKey, waveform);
//            }
//
//            return waveform;
//
//        } catch (Exception e) {
//            log.error("生成波形失败: {}", inputFile.getName(), e);
//            throw new RuntimeException("生成波形失败", e);
//        }
//    }
//
//    @Override
//    public String generateFingerprint(File inputFile) {
//        // 简单的哈希指纹，实际应用应使用Chromaprint等算法
//        try {
//            float[] waveform = generateWaveform(inputFile, 256);
//            StringBuilder fingerprint = new StringBuilder();
//            for (float value : waveform) {
//                int quantized = (int) (value * 100);
//                fingerprint.append(Integer.toHexString(quantized));
//            }
//            return fingerprint.toString().substring(0, Math.min(64, fingerprint.length()));
//
//        } catch (Exception e) {
//            log.error("生成指纹失败", e);
//            return UUID.randomUUID().toString();
//        }
//    }
//
//    @Override
//    public boolean isFormatSupported(String format) {
//        String lowerFormat = format.toLowerCase();
//        for (String supported : properties.getProcessor().getSupportedFormats()) {
//            if (supported.equals(lowerFormat)) {
//                return true;
//            }
//        }
//        return false;
//    }
//
//    /**
//     * 测量音量(dB)
//     */
//    private double measureVolume(File inputFile) throws Exception {
//        AudioInputStream audioStream = AudioSystem.getAudioInputStream(inputFile);
//        byte[] audioBytes = audioStream.readAllBytes();
//        short[] samples = new short[audioBytes.length / 2];
//
//        for (int i = 0; i < samples.length; i++) {
//            samples[i] = (short) ((audioBytes[i * 2] & 0xff) | (audioBytes[i * 2 + 1] << 8));
//        }
//
//        // 计算RMS
//        double sum = 0;
//        for (short sample : samples) {
//            sum += (sample / 32768.0) * (sample / 32768.0);
//        }
//        double rms = Math.sqrt(sum / samples.length);
//        double db = 20 * Math.log10(rms + 1e-10);
//
//        audioStream.close();
//        return db;
//    }
//}
