package org.dromara.common.audio.core;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.audio.dsp.AudioFeatures;
import org.springframework.stereotype.Component;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * TarsosDSP 音频处理器实现
 * 提供基于TarsosDSP库的音频处理能力
 *
 * @author VoiceText Weaver
 * @since 2026-03-01
 */
@Slf4j
@Component
public class TarsosAudioProcessor implements AudioProcessor {

    private static final List<String> SUPPORTED_FORMATS = Arrays.asList("wav", "aiff", "mp3", "flac");
    private static final int DEFAULT_BUFFER_SIZE = 1024;
    private static final int DEFAULT_WAVEFORM_POINTS = 1000;

    @Override
    public File convert(File sourceFile, String targetFormat) {
        return convert(sourceFile, targetFormat, new HashMap<>());
    }

    @Override
    public File convert(File sourceFile, String targetFormat, Map<String, Object> params) {
        log.info("Converting {} to {} with params: {}", sourceFile.getName(), targetFormat, params);

        // TarsosDSP主要支持WAV格式，复杂转换委托给FFmpeg
        // 这里实现WAV格式内部的转换（如采样率、声道数调整）
        try {
            if (!"wav".equalsIgnoreCase(targetFormat)) {
                throw new UnsupportedOperationException("TarsosDSP仅支持WAV格式转换，请使用FFmpegProcessor");
            }

            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(sourceFile);
            AudioFormat sourceFormat = audioInputStream.getFormat();

            // 构建目标格式
            Float sampleRate = (Float) params.getOrDefault("sampleRate", sourceFormat.getSampleRate());
            Integer sampleSizeInBits = (Integer) params.getOrDefault("sampleSizeInBits", sourceFormat.getSampleSizeInBits());
            Integer channels = (Integer) params.getOrDefault("channels", sourceFormat.getChannels());

            AudioFormat targetFormats = new AudioFormat(
                sampleRate,
                sampleSizeInBits,
                channels,
                true,  // signed
                false  // littleEndian
            );

            // 转换音频流
            AudioInputStream convertedStream = AudioSystem.getAudioInputStream(targetFormats, audioInputStream);

            // 生成输出文件
            String outputPath = sourceFile.getParent() + File.separator +
                sourceFile.getName().substring(0, sourceFile.getName().lastIndexOf('.')) +
                "_converted.wav";
            File outputFile = new File(outputPath);

            AudioSystem.write(convertedStream, AudioFileFormat.Type.WAVE, outputFile);

            log.info("Conversion completed: {}", outputFile.getAbsolutePath());
            return outputFile;

        } catch (UnsupportedAudioFileException | IOException e) {
            log.error("Audio conversion failed", e);
            throw new RuntimeException("Audio conversion failed: " + e.getMessage(), e);
        }
    }

    @Override
    public File trim(File sourceFile, double startTime, double endTime) {
        log.info("Trimming {} from {}s to {}s", sourceFile.getName(), startTime, endTime);

        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(sourceFile);
            AudioFormat format = audioInputStream.getFormat();

            long frameSize = format.getFrameSize();
            float frameRate = format.getFrameRate();
            long totalFrames = audioInputStream.getFrameLength();

            long startFrame = (long) (startTime * frameRate);
            long endFrame = (long) (endTime * frameRate);

            if (endFrame > totalFrames) {
                endFrame = totalFrames;
            }

            long framesToRead = endFrame - startFrame;

            // 跳过开始的帧
            audioInputStream.skip(startFrame * frameSize);

            // 读取指定长度的帧
            byte[] audioData = new byte[(int) (framesToRead * frameSize)];
            int bytesRead = audioInputStream.read(audioData);

            if (bytesRead <= 0) {
                throw new RuntimeException("Failed to read audio data");
            }

            // 创建新的音频输入流
            ByteArrayInputStream bais = new ByteArrayInputStream(audioData);
            AudioInputStream trimmedStream = new AudioInputStream(bais, format, framesToRead);

            // 写入文件
            String outputPath = sourceFile.getParent() + File.separator +
                sourceFile.getName().substring(0, sourceFile.getName().lastIndexOf('.')) +
                "_trimmed.wav";
            File outputFile = new File(outputPath);

            AudioSystem.write(trimmedStream, AudioFileFormat.Type.WAVE, outputFile);

            log.info("Trim completed: {}", outputFile.getAbsolutePath());
            return outputFile;

        } catch (UnsupportedAudioFileException | IOException e) {
            log.error("Audio trimming failed", e);
            throw new RuntimeException("Audio trimming failed: " + e.getMessage(), e);
        }
    }

    @Override
    public File merge(List<File> audioFiles) {
        log.info("Merging {} audio files", audioFiles.size());

        try {
            // 检查所有文件格式是否一致
            AudioFormat firstFormat = null;
            List<byte[]> allAudioData = new ArrayList<>();
            long totalFrames = 0;

            for (File file : audioFiles) {
                AudioInputStream ais = AudioSystem.getAudioInputStream(file);
                AudioFormat format = ais.getFormat();

                if (firstFormat == null) {
                    firstFormat = format;
                } else if (!firstFormat.matches(format)) {
                    throw new RuntimeException("Audio formats are not consistent: " + file.getName());
                }

                long frames = ais.getFrameLength();
                byte[] data = new byte[(int) (frames * format.getFrameSize())];
                ais.read(data);

                allAudioData.add(data);
                totalFrames += frames;

                ais.close();
            }

            // 合并所有音频数据
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            for (byte[] data : allAudioData) {
                baos.write(data);
            }

            // 创建新的音频输入流
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            AudioInputStream mergedStream = new AudioInputStream(bais, firstFormat, totalFrames);

            // 写入文件
            String outputPath = audioFiles.get(0).getParent() + File.separator +
                "merged_" + System.currentTimeMillis() + ".wav";
            File outputFile = new File(outputPath);

            AudioSystem.write(mergedStream, AudioFileFormat.Type.WAVE, outputFile);

            log.info("Merge completed: {}", outputFile.getAbsolutePath());
            return outputFile;

        } catch (UnsupportedAudioFileException | IOException e) {
            log.error("Audio merging failed", e);
            throw new RuntimeException("Audio merging failed: " + e.getMessage(), e);
        }
    }

    @Override
    public AudioFeatures extractFeatures(File audioFile) {
        log.info("Extracting features from: {}", audioFile.getName());

        AudioFeatures features = new AudioFeatures();
        features.setFileName(audioFile.getName());

        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(audioFile);
            AudioFormat format = audioInputStream.getFormat();

            // 基本信息
            features.setSampleRate(format.getSampleRate());
            features.setChannels(format.getChannels());
            features.setBitDepth(format.getSampleSizeInBits());
            features.setDuration((double) (audioInputStream.getFrameLength() / format.getFrameRate()));

            // 读取音频数据用于特征分析
            byte[] audioBytes = audioInputStream.readAllBytes();
            float[] audioSamples = bytesToFloats(audioBytes, format);

            // 计算RMS（均方根）音量
            features.setRms(calculateRMS(audioSamples));

            // 计算峰值
            features.setPeak(calculatePeak(audioSamples));

            // 计算过零率
            features.setZeroCrossingRate(calculateZeroCrossingRate(audioSamples));

            // 计算频谱质心
            features.setSpectralCentroid(calculateSpectralCentroid(audioSamples, format.getSampleRate()));

            // 提取基频（使用TarsosDSP的PitchProcessor）
            extractPitchInfo(audioFile, features);

            audioInputStream.close();

        } catch (Exception e) {
            log.error("Feature extraction failed", e);
            throw new RuntimeException("Feature extraction failed: " + e.getMessage(), e);
        }

        return features;
    }

    @Override
    public byte[] generateWaveform(File audioFile, int width, int height) {
        double[] waveformData = generateWaveformData(audioFile, width);
        return createWaveformImage(waveformData, width, height);
    }

    @Override
    public double[] generateWaveformData(File audioFile, int points) {
        log.info("Generating waveform data for: {} with {} points", audioFile.getName(), points);

        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(audioFile);
            AudioFormat format = audioInputStream.getFormat();

            int bytesPerFrame = format.getFrameSize();
            int totalFrames = (int) audioInputStream.getFrameLength();

            if (totalFrames <= 0) {
                // 如果无法获取长度，估计长度
                totalFrames = (int) (audioInputStream.available() / bytesPerFrame);
            }

            // 计算每段应该包含的帧数
            int framesPerPoint = Math.max(1, totalFrames / points);
            double[] waveform = new double[points];

            byte[] buffer = new byte[bytesPerFrame * framesPerPoint];
            int pointIndex = 0;

            while (pointIndex < points) {
                int bytesRead = audioInputStream.read(buffer);
                if (bytesRead <= 0) break;

                int framesRead = bytesRead / bytesPerFrame;
                double maxAmplitude = 0;

                // 计算这一段的最大振幅
                for (int i = 0; i < framesRead; i++) {
                    int offset = i * bytesPerFrame;
                    double amplitude = 0;

                    if (format.getSampleSizeInBits() == 16) {
                        short sample = (short) ((buffer[offset + 1] & 0xFF) << 8 | (buffer[offset] & 0xFF));
                        amplitude = Math.abs(sample) / 32768.0;
                    } else if (format.getSampleSizeInBits() == 8) {
                        amplitude = Math.abs(buffer[offset] - 128) / 128.0;
                    }

                    if (amplitude > maxAmplitude) {
                        maxAmplitude = amplitude;
                    }
                }

                if (pointIndex < points) {
                    waveform[pointIndex++] = maxAmplitude;
                }
            }

            // 如果points没填满，用最后一个值填充
            while (pointIndex < points) {
                waveform[pointIndex++] = 0;
            }

            audioInputStream.close();
            return waveform;

        } catch (Exception e) {
            log.error("Waveform generation failed", e);
            throw new RuntimeException("Waveform generation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> getAudioInfo(File audioFile) {
        Map<String, Object> info = new HashMap<>();

        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(audioFile);
            AudioFormat format = audioInputStream.getFormat();

            info.put("fileName", audioFile.getName());
            info.put("fileSize", audioFile.length());
            info.put("format", format.getEncoding().toString());
            info.put("sampleRate", format.getSampleRate());
            info.put("sampleSizeInBits", format.getSampleSizeInBits());
            info.put("channels", format.getChannels());
            info.put("frameRate", format.getFrameRate());
            info.put("frameSize", format.getFrameSize());
            info.put("frameLength", audioInputStream.getFrameLength());
            info.put("duration", audioInputStream.getFrameLength() / format.getFrameRate());
            info.put("bigEndian", format.isBigEndian());

            audioInputStream.close();

        } catch (Exception e) {
            log.error("Failed to get audio info", e);
        }

        return info;
    }

    @Override
    public boolean validateAudio(File audioFile) {
        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(audioFile);
            AudioFormat format = audioInputStream.getFormat();

            // 基本验证
            boolean valid = format.getSampleRate() > 0 &&
                format.getSampleSizeInBits() > 0 &&
                format.getChannels() > 0 &&
                audioInputStream.getFrameLength() > 0;

            audioInputStream.close();
            return valid;

        } catch (Exception e) {
            log.warn("Audio validation failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public List<String> getSupportedFormats() {
        return SUPPORTED_FORMATS;
    }

    /**
     * 字节数组转浮点数数组
     */
    private float[] bytesToFloats(byte[] bytes, AudioFormat format) {
        int sampleSizeInBits = format.getSampleSizeInBits();
        boolean isBigEndian = format.isBigEndian();
        int channels = format.getChannels();

        int sampleBytes = sampleSizeInBits / 8;
        int totalSamples = bytes.length / sampleBytes / channels;
        float[] samples = new float[totalSamples * channels];

        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        if (isBigEndian) {
            buffer.order(ByteOrder.BIG_ENDIAN);
        } else {
            buffer.order(ByteOrder.LITTLE_ENDIAN);
        }

        for (int i = 0; i < samples.length; i++) {
            if (sampleSizeInBits == 16) {
                samples[i] = buffer.getShort() / 32768.0f;
            } else if (sampleSizeInBits == 8) {
                samples[i] = (buffer.get() - 128) / 128.0f;
            } else if (sampleSizeInBits == 32) {
                samples[i] = buffer.getFloat();
            }
        }

        return samples;
    }

    /**
     * 计算RMS音量
     */
    private double calculateRMS(float[] samples) {
        double sum = 0;
        for (float sample : samples) {
            sum += sample * sample;
        }
        return Math.sqrt(sum / samples.length);
    }

    /**
     * 计算峰值
     */
    private double calculatePeak(float[] samples) {
        double peak = 0;
        for (float sample : samples) {
            peak = Math.max(peak, Math.abs(sample));
        }
        return peak;
    }

    /**
     * 计算过零率
     */
    private double calculateZeroCrossingRate(float[] samples) {
        int zeroCrossings = 0;
        for (int i = 1; i < samples.length; i++) {
            if (samples[i] * samples[i - 1] < 0) {
                zeroCrossings++;
            }
        }
        return (double) zeroCrossings / samples.length;
    }

    /**
     * 计算频谱质心
     */
    private double calculateSpectralCentroid(float[] samples, float sampleRate) {
        // 简化版本：使用FFT计算频谱质心
        // 这里需要更复杂的实现，暂时返回估算值
        return sampleRate / 4.0;
    }

    /**
     * 提取基频信息
     */
    private void extractPitchInfo(File audioFile, AudioFeatures features) {
        AtomicReference<Double> pitchSum = new AtomicReference<>(0.0);
        AtomicInteger pitchCount = new AtomicInteger(0);
        AtomicReference<Double> maxPitch = new AtomicReference<>(0.0);
        AtomicReference<Double> minPitch = new AtomicReference<>(Double.MAX_VALUE);

        try {
            AudioDispatcher dispatcher = AudioDispatcherFactory.fromFile(audioFile, 2048, 0);

            PitchDetectionHandler pitchHandler = (PitchDetectionResult pitchDetectionResult, AudioEvent audioEvent) -> {
                float pitch = pitchDetectionResult.getPitch();
                if (pitch > 0) {
                    pitchSum.updateAndGet(v -> v + pitch);
                    pitchCount.incrementAndGet();
                    maxPitch.updateAndGet(v -> Math.max(v, pitch));
                    minPitch.updateAndGet(v -> Math.min(v, pitch));
                }
            };

            dispatcher.addAudioProcessor(new PitchProcessor(
                PitchProcessor.PitchEstimationAlgorithm.YIN,
                (float) features.getSampleRate(),
                2048,
                pitchHandler
            ));

            dispatcher.run();

            if (pitchCount.get() > 0) {
                features.setMeanPitch(pitchSum.get() / pitchCount.get());
                features.setMaxPitch(maxPitch.get());
                features.setMinPitch(minPitch.get() == Double.MAX_VALUE ? 0 : minPitch.get());
            }

        } catch (Exception e) {
            log.warn("Pitch extraction failed: {}", e.getMessage());
        }
    }

    /**
     * 创建波形图图片
     */
    private byte[] createWaveformImage(double[] waveformData, int width, int height) {
        // 这里应该使用Java 2D生成图片
        // 简化实现：返回空数组，实际项目中需要使用ImageIO
        return new byte[0];
    }
}
