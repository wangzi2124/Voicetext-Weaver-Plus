package org.dromara.common.audio.dsp;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 语音活动检测器
 * 检测音频中的语音片段
 *
 * @author VoiceText Weaver
 * @since 2026-03-01
 */
@Slf4j
@Component
public class VoiceActivityDetector {

    private static final int BUFFER_SIZE = 1024;
    private static final double ENERGY_THRESHOLD = 0.01;  // 能量阈值
    private static final double MIN_SPEECH_DURATION = 0.2; // 最小语音时长（秒）
    private static final double MIN_SILENCE_DURATION = 0.1; // 最小静音时长（秒）

    // 音频格式常量
    private static final float DEFAULT_SAMPLE_RATE = 44100.0f; // 默认采样率，实际会从文件读取

    /**
     * 检测语音活动
     *
     * @param audioFile 音频文件
     * @return 语音片段列表
     */
    public List<SpeechSegment> detect(File audioFile) {
        return detect(audioFile, ENERGY_THRESHOLD, MIN_SPEECH_DURATION);
    }

    /**
     * 检测语音活动（可配置参数）
     *
     * @param audioFile 音频文件
     * @param energyThreshold 能量阈值
     * @param minSpeechDuration 最小语音时长
     * @return 语音片段列表
     */
    public List<SpeechSegment> detect(File audioFile, double energyThreshold, double minSpeechDuration) {
        log.info("Detecting voice activity in: {}", audioFile.getName());

        List<SpeechSegment> segments = new ArrayList<>();
        List<FrameEnergy> frameEnergies = new ArrayList<>();

        try {
            // 获取音频格式信息
            var audioInputStream = AudioSystem.getAudioInputStream(audioFile);
            var format = audioInputStream.getFormat();
            float sampleRate = format.getSampleRate();
            audioInputStream.close();

            // 计算每帧能量
            calculateFrameEnergies(audioFile, frameEnergies, sampleRate);

            // 确定语音/非语音阈值
            double threshold = determineThreshold(frameEnergies, energyThreshold);

            // 标记语音帧
            List<Boolean> speechFlags = markSpeechFrames(frameEnergies, threshold);

            // 合并连续帧为片段
            segments = mergeFramesToSegments(speechFlags, frameEnergies, minSpeechDuration);

            log.info("Detected {} speech segments", segments.size());

        } catch (Exception e) {
            log.error("Voice activity detection failed", e);
        }

        return segments;
    }

    /**
     * 计算每帧能量
     */
    private void calculateFrameEnergies(File audioFile, List<FrameEnergy> frameEnergies, float sampleRate)
            throws UnsupportedAudioFileException, IOException {

        AudioDispatcher dispatcher = AudioDispatcherFactory.fromFile(audioFile, BUFFER_SIZE, 0);

        // 计算每帧的时长（秒）
        final double frameDuration = BUFFER_SIZE / (double) sampleRate;

        dispatcher.addAudioProcessor(new AudioProcessor() {
            private int frameIndex = 0;
            private double time = 0;

            @Override
            public boolean process(AudioEvent audioEvent) {
                float[] samples = audioEvent.getFloatBuffer();

                // 计算RMS能量
                double energy = 0;
                for (float sample : samples) {
                    energy += sample * sample;
                }
                energy = Math.sqrt(energy / samples.length);

                // 计算过零率（作为辅助特征）
                int zeroCrossings = 0;
                for (int i = 1; i < samples.length; i++) {
                    if (samples[i] * samples[i - 1] < 0) {
                        zeroCrossings++;
                    }
                }
                double zeroCrossingRate = zeroCrossings / (double) samples.length;

                FrameEnergy fe = new FrameEnergy();
                fe.setFrameIndex(frameIndex++);
                fe.setStartTime(time);
                fe.setEndTime(time + frameDuration);
                fe.setEnergy(energy);
                fe.setZeroCrossingRate(zeroCrossingRate);

                frameEnergies.add(fe);
                time += frameDuration;

                return true;
            }

            @Override
            public void processingFinished() {
                log.debug("Frame energy calculation completed. Total frames: {}", frameEnergies.size());
            }
        });

        dispatcher.run();
    }

    /**
     * 确定语音/非语音阈值
     */
    private double determineThreshold(List<FrameEnergy> frameEnergies, double baseThreshold) {
        if (frameEnergies.isEmpty()) {
            return ENERGY_THRESHOLD;
        }

        // 计算平均能量和标准差
        double sum = 0;
        for (FrameEnergy fe : frameEnergies) {
            sum += fe.getEnergy();
        }
        double mean = sum / frameEnergies.size();

        double variance = 0;
        for (FrameEnergy fe : frameEnergies) {
            variance += Math.pow(fe.getEnergy() - mean, 2);
        }
        double std = Math.sqrt(variance / frameEnergies.size());

        // 阈值 = 均值 + 系数 * 标准差
        return mean + baseThreshold * std;
    }

    /**
     * 标记语音帧
     */
    private List<Boolean> markSpeechFrames(List<FrameEnergy> frameEnergies, double threshold) {
        List<Boolean> speechFlags = new ArrayList<>();

        for (FrameEnergy fe : frameEnergies) {
            // 基于能量和过零率判断
            boolean isSpeech = fe.getEnergy() > threshold;

            // 语音通常有过零率范围（语音的过零率通常在0.05-0.15之间）
            // 过零率过高（>0.3）通常是噪音
            if (isSpeech && (fe.getZeroCrossingRate() > 0.3 || fe.getZeroCrossingRate() < 0.02)) {
                isSpeech = false; // 可能是噪声或静音
            }

            speechFlags.add(isSpeech);
        }

        return speechFlags;
    }

    /**
     * 合并连续帧为片段
     */
    private List<SpeechSegment> mergeFramesToSegments(List<Boolean> speechFlags,
                                                       List<FrameEnergy> frameEnergies,
                                                       double minSpeechDuration) {
        List<SpeechSegment> segments = new ArrayList<>();

        int i = 0;
        while (i < speechFlags.size()) {
            // 寻找语音开始
            while (i < speechFlags.size() && !speechFlags.get(i)) {
                i++;
            }
            if (i >= speechFlags.size()) break;

            int startIndex = i;
            double startTime = frameEnergies.get(i).getStartTime();

            // 寻找语音结束
            while (i < speechFlags.size() && speechFlags.get(i)) {
                i++;
            }
            int endIndex = i - 1;
            double endTime = frameEnergies.get(endIndex).getEndTime();

            double duration = endTime - startTime;

            // 过滤太短的片段
            if (duration >= minSpeechDuration) {
                SpeechSegment segment = new SpeechSegment();
                segment.setStartTime(startTime);
                segment.setEndTime(endTime);
                segment.setDuration(duration);

                // 计算平均能量
                double avgEnergy = 0;
                for (int j = startIndex; j <= endIndex; j++) {
                    avgEnergy += frameEnergies.get(j).getEnergy();
                }
                segment.setAverageEnergy(avgEnergy / (endIndex - startIndex + 1));

                segments.add(segment);
            }
        }

        return segments;
    }

    /**
     * 获取语音活动比例
     *
     * @param audioFile 音频文件
     * @return 语音比例（0-1）
     */
    public double getSpeechRatio(File audioFile) {
        List<SpeechSegment> segments = detect(audioFile);

        try {
            var audioInputStream = AudioSystem.getAudioInputStream(audioFile);
            var format = audioInputStream.getFormat();
            double totalDuration = audioInputStream.getFrameLength() / (double) format.getFrameRate();
            audioInputStream.close();

            if (totalDuration <= 0) {
                return 0;
            }

            double speechDuration = segments.stream()
                .mapToDouble(SpeechSegment::getDuration)
                .sum();

            return speechDuration / totalDuration;

        } catch (Exception e) {
            log.error("Failed to calculate speech ratio", e);
            return 0;
        }
    }

    /**
     * 获取语音活动统计信息
     *
     * @param audioFile 音频文件
     * @return 统计信息
     */
    public VADStatistics getStatistics(File audioFile) {
        List<SpeechSegment> segments = detect(audioFile);

        VADStatistics stats = new VADStatistics();
        stats.setSegmentCount(segments.size());

        if (segments.isEmpty()) {
            stats.setSpeechRatio(0);
            stats.setAverageSegmentDuration(0);
            stats.setTotalSpeechDuration(0);
            return stats;
        }

        double totalSpeechDuration = segments.stream()
            .mapToDouble(SpeechSegment::getDuration)
            .sum();

        double avgSegmentDuration = totalSpeechDuration / segments.size();

        double maxSegmentDuration = segments.stream()
            .mapToDouble(SpeechSegment::getDuration)
            .max()
            .orElse(0);

        double minSegmentDuration = segments.stream()
            .mapToDouble(SpeechSegment::getDuration)
            .min()
            .orElse(0);

        try {
            var audioInputStream = AudioSystem.getAudioInputStream(audioFile);
            var format = audioInputStream.getFormat();
            double totalDuration = audioInputStream.getFrameLength() / (double) format.getFrameRate();
            audioInputStream.close();

            stats.setSpeechRatio(totalSpeechDuration / totalDuration);
        } catch (Exception e) {
            log.warn("Failed to calculate total duration", e);
        }

        stats.setTotalSpeechDuration(totalSpeechDuration);
        stats.setAverageSegmentDuration(avgSegmentDuration);
        stats.setMaxSegmentDuration(maxSegmentDuration);
        stats.setMinSegmentDuration(minSegmentDuration);

        return stats;
    }

    /**
     * 帧能量数据
     */
    @Data
    private static class FrameEnergy {
        private int frameIndex;
        private double startTime;
        private double endTime;
        private double energy;
        private double zeroCrossingRate;
    }

    /**
     * 语音片段
     */
    @Data
    public static class SpeechSegment {
        private double startTime;
        private double endTime;
        private double duration;
        private double averageEnergy;

        @Override
        public String toString() {
            return String.format("SpeechSegment{start=%.2f, end=%.2f, duration=%.2f, energy=%.3f}",
                startTime, endTime, duration, averageEnergy);
        }
    }

    /**
     * VAD统计信息
     */
    @Data
    public static class VADStatistics {
        private int segmentCount;              // 语音片段数量
        private double speechRatio;             // 语音占比
        private double totalSpeechDuration;     // 总语音时长（秒）
        private double averageSegmentDuration;  // 平均片段时长（秒）
        private double maxSegmentDuration;      // 最大片段时长（秒）
        private double minSegmentDuration;      // 最小片段时长（秒）

        @Override
        public String toString() {
            return String.format("VADStatistics{segments=%d, ratio=%.2f%%, total=%.2fs, avg=%.2fs, max=%.2fs, min=%.2fs}",
                segmentCount, speechRatio * 100, totalSpeechDuration, averageSegmentDuration, maxSegmentDuration, minSegmentDuration);
        }
    }
}
