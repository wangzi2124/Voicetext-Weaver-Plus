//package org.dromara.common.audio.dsp;
//
//import be.tarsos.dsp.AudioDispatcher;
//import be.tarsos.dsp.AudioEvent;
//import be.tarsos.dsp.AudioProcessor;
//import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
//import be.tarsos.dsp.mfcc.MFCC;
//import be.tarsos.dsp.pitch.PitchDetectionHandler;
//import be.tarsos.dsp.pitch.PitchDetectionResult;
//import be.tarsos.dsp.pitch.PitchProcessor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Component;
//
//import javax.sound.sampled.AudioSystem;
//import javax.sound.sampled.UnsupportedAudioFileException;
//import java.io.File;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.concurrent.atomic.AtomicInteger;
//import java.util.concurrent.atomic.AtomicReference;
//
///**
// * 音频分析器
// * 提供深入的音频分析功能
// *
// * @author VoiceText Weaver
// * @since 2026-03-01
// */
//@Slf4j
//@Component
//public class AudioAnalyzer {
//
//    private static final int BUFFER_SIZE = 2048;
//    private static final int STEP_SIZE = 512;
//
//    /**
//     * 执行完整音频分析
//     *
//     * @param audioFile 音频文件
//     * @return 分析结果
//     */
//    public AudioFeatures analyze(File audioFile) {
//        log.info("Starting audio analysis: {}", audioFile.getName());
//
//        AudioFeatures features = new AudioFeatures();
//        features.setFileName(audioFile.getName());
//
//        try {
//            // 获取基本信息
//            var audioInputStream = AudioSystem.getAudioInputStream(audioFile);
//            var format = audioInputStream.getFormat();
//            features.setSampleRate(format.getSampleRate());
//            features.setChannels(format.getChannels());
//            features.setBitDepth(format.getSampleSizeInBits());
//            features.setDuration((double) audioInputStream.getFrameLength() / format.getFrameRate());
//
//            audioInputStream.close();
//
//            // 执行各项分析
//            analyzeTimeDomain(audioFile, features);
//            analyzeFrequencyDomain(audioFile, features);
//            analyzePitch(audioFile, features);
//
//        } catch (UnsupportedAudioFileException | IOException e) {
//            log.error("Audio analysis failed", e);
//            throw new RuntimeException("Audio analysis failed: " + e.getMessage(), e);
//        }
//
//        return features;
//    }
//
//    /**
//     * 时域分析
//     */
//    private void analyzeTimeDomain(File audioFile, AudioFeatures features) {
//        try {
//            AudioDispatcher dispatcher = AudioDispatcherFactory.fromFile(audioFile, BUFFER_SIZE, 0);
//
//            AtomicReference<Double> rmsSum = new AtomicReference<>(0.0);
//            AtomicReference<Double> peakMax = new AtomicReference<>(0.0);
//            AtomicInteger frameCount = new AtomicInteger(0);
//            AtomicInteger zeroCrossings = new AtomicInteger(0);
//            AtomicReference<Double> lastSample = new AtomicReference<>(0.0);
//
//            dispatcher.addAudioProcessor(new AudioProcessor() {
//                @Override
//                public boolean process(AudioEvent audioEvent) {
//                    float[] samples = audioEvent.getFloatBuffer();
//
//                    // 计算RMS
//                    double rms = 0;
//                    for (float sample : samples) {
//                        rms += sample * sample;
//                        peakMax.updateAndGet(v -> Math.max(v, Math.abs(sample)));
//
//                        // 过零检测
//                        if (lastSample.get() * sample < 0) {
//                            zeroCrossings.incrementAndGet();
//                        }
//                        lastSample.set((double) sample);
//                    }
//                    rms = Math.sqrt(rms / samples.length);
//                    double finalRms = rms;
//                    rmsSum.updateAndGet(v -> v + finalRms);
//
//                    frameCount.incrementAndGet();
//                    return true;
//                }
//
//                @Override
//                public void processingFinished() {
//                    int count = frameCount.get();
//                    if (count > 0) {
//                        features.setRms(rmsSum.get() / count);
//                        features.setPeak(peakMax.get());
//                        features.setZeroCrossingRate(zeroCrossings.get() / (double) count);
//                    }
//                }
//            });
//
//            dispatcher.run();
//
//        } catch (Exception e) {
//            log.warn("Time domain analysis failed: {}", e.getMessage());
//        }
//    }
//
//    /**
//     * 频域分析 - 使用FFT计算频谱特征
//     */
//    private void analyzeFrequencyDomain(File audioFile, AudioFeatures features) {
//        try {
//            AudioDispatcher dispatcher = AudioDispatcherFactory.fromFile(audioFile, BUFFER_SIZE, 0);
//
//            // 使用MFCC分析
//            MFCC mfcc = new MFCC(
//                BUFFER_SIZE,                    // 缓冲区大小
//                (float) features.getSampleRate(), // 采样率
//                13,                              // 系数数量
//                40,                               // 最低频率
//                300,                              // 最高频率（实际是滤波器数量）
//                8000                              // 最大频率
//            );
//
//            // 存储MFCC系数的列表
//            List<double[]> mfccList = new ArrayList<>();
//
//            // 存储频谱能量
//            List<double[]> spectrumList = new ArrayList<>();
//
//            dispatcher.addAudioProcessor(mfcc);
//            dispatcher.addAudioProcessor(new AudioProcessor() {
//                @Override
//                public boolean process(AudioEvent audioEvent) {
//                    // 获取MFCC系数
//                    float[] mfccCoeffs = mfcc.getMFCC();
//                    if (mfccCoeffs != null && mfccCoeffs.length >= 3) {
//                        double[] mfccs = new double[mfccCoeffs.length];
//                        for (int i = 0; i < mfccCoeffs.length; i++) {
//                            mfccs[i] = mfccCoeffs[i];
//                        }
//                        mfccList.add(mfccs);
//                    }
//
//                    // 通过FFT计算频谱（使用音频事件的FFT）
//                    float[] audioBuffer = audioEvent.getFloatBuffer().clone();
//                    float[] spectrum = calculateSpectrum(audioBuffer);
//                    if (spectrum != null) {
//                        spectrumList.add(doubleArrayOf(spectrum));
//                    }
//
//                    return true;
//                }
//
//                @Override
//                public void processingFinished() {
//                    if (!mfccList.isEmpty()) {
//                        // 计算平均MFCC系数
//                        double[] avgMfcc = new double[13];
//                        for (double[] mfccs : mfccList) {
//                            for (int i = 0; i < Math.min(13, mfccs.length); i++) {
//                                avgMfcc[i] += mfccs[i];
//                            }
//                        }
//                        for (int i = 0; i < avgMfcc.length; i++) {
//                            avgMfcc[i] /= mfccList.size();
//                        }
//
//                        features.setMfcc1(avgMfcc[0]);
//                        features.setMfcc2(avgMfcc[1]);
//                        features.setMfcc3(avgMfcc[2]);
//                    }
//
//                    if (!spectrumList.isEmpty()) {
//                        // 计算频谱质心
//                        double centroidSum = 0;
//                        for (double[] spectrum : spectrumList) {
//                            centroidSum += calculateSpectralCentroid(spectrum, (float) features.getSampleRate());
//                        }
//                        features.setSpectralCentroid(centroidSum / spectrumList.size());
//
//                        // 计算频谱滚降点
//                        double rolloffSum = 0;
//                        for (double[] spectrum : spectrumList) {
//                            rolloffSum += calculateSpectralRolloff(spectrum, 0.85);
//                        }
//                        features.setSpectralRolloff(rolloffSum / spectrumList.size());
//                    }
//                }
//            });
//
//            dispatcher.run();
//
//        } catch (Exception e) {
//            log.warn("Frequency domain analysis failed: {}", e.getMessage());
//        }
//    }
//
//    /**
//     * 计算频谱（简单FFT实现）
//     */
//    private float[] calculateSpectrum(float[] samples) {
//        int n = samples.length;
//        // 使用实部FFT简化实现
//        float[] spectrum = new float[n / 2];
//
//        // 计算幅度谱（简化版）
//        for (int i = 0; i < spectrum.length; i++) {
//            double sum = 0;
//            for (int j = 0; j < n; j++) {
//                double angle = 2 * Math.PI * i * j / n;
//                sum += samples[j] * Math.cos(angle); // 简化，只取实部
//            }
//            spectrum[i] = (float) Math.abs(sum);
//        }
//
//        return spectrum;
//    }
//
//    /**
//     * 将float数组转换为double数组
//     */
//    private double[] doubleArrayOf(float[] floats) {
//        double[] doubles = new double[floats.length];
//        for (int i = 0; i < floats.length; i++) {
//            doubles[i] = floats[i];
//        }
//        return doubles;
//    }
//
//    /**
//     * 计算频谱质心
//     */
//    private double calculateSpectralCentroid(double[] spectrum, float sampleRate) {
//        double numerator = 0;
//        double denominator = 0;
//
//        for (int i = 0; i < spectrum.length; i++) {
//            double frequency = (double) i * sampleRate / (2 * spectrum.length);
//            numerator += frequency * spectrum[i];
//            denominator += spectrum[i];
//        }
//
//        return denominator > 0 ? numerator / denominator : 0;
//    }
//
//    /**
//     * 计算频谱滚降点
//     */
//    private double calculateSpectralRolloff(double[] spectrum, double percentage) {
//        double totalEnergy = 0;
//        for (double value : spectrum) {
//            totalEnergy += value;
//        }
//
//        double threshold = totalEnergy * percentage;
//        double cumulative = 0;
//
//        for (int i = 0; i < spectrum.length; i++) {
//            cumulative += spectrum[i];
//            if (cumulative >= threshold) {
//                return i;
//            }
//        }
//
//        return spectrum.length - 1;
//    }
//
//    /**
//     * 音高分析
//     */
//    private void analyzePitch(File audioFile, AudioFeatures features) {
//        try {
//            AudioDispatcher dispatcher = AudioDispatcherFactory.fromFile(audioFile, BUFFER_SIZE, 0);
//
//            List<Float> pitches = new ArrayList<>();
//
//            PitchDetectionHandler pitchHandler = (PitchDetectionResult pitchDetectionResult, AudioEvent audioEvent) -> {
//                float pitch = pitchDetectionResult.getPitch();
//                if (pitch > 0) {
//                    pitches.add(pitch);
//                }
//            };
//
//            dispatcher.addAudioProcessor(new PitchProcessor(
//                PitchProcessor.PitchEstimationAlgorithm.YIN,
//                (float) features.getSampleRate(),
//                BUFFER_SIZE,
//                pitchHandler
//            ));
//
//            dispatcher.run();
//
//            if (!pitches.isEmpty()) {
//                double sum = 0;
//                double max = 0;
//                double min = Double.MAX_VALUE;
//
//                for (float pitch : pitches) {
//                    sum += pitch;
//                    max = Math.max(max, pitch);
//                    min = Math.min(min, pitch);
//                }
//
//                double mean = sum / pitches.size();
//
//                // 计算标准差
//                double variance = 0;
//                for (float pitch : pitches) {
//                    variance += Math.pow(pitch - mean, 2);
//                }
//                double std = Math.sqrt(variance / pitches.size());
//
//                features.setMeanPitch(mean);
//                features.setMaxPitch(max);
//                features.setMinPitch(min == Double.MAX_VALUE ? 0 : min);
//                features.setPitchStd(std);
//            }
//
//        } catch (Exception e) {
//            log.warn("Pitch analysis failed: {}", e.getMessage());
//        }
//    }
//
//    /**
//     * 生成频谱图数据 - 修复版本
//     *
//     * @param audioFile 音频文件
//     * @return 频谱数据
//     */
//    public SpectrogramData generateSpectrogram(File audioFile) {
//        log.info("Generating spectrogram: {}", audioFile.getName());
//
//        SpectrogramData data = new SpectrogramData();
//
//        try {
//            AudioDispatcher dispatcher = AudioDispatcherFactory.fromFile(audioFile, BUFFER_SIZE, STEP_SIZE);
//
//            // 使用自定义频谱处理器
//            dispatcher.addAudioProcessor(new AudioProcessor() {
//                @Override
//                public boolean process(AudioEvent audioEvent) {
//                    float[] samples = audioEvent.getFloatBuffer();
//                    float[] spectrum = calculateSpectrum(samples);
//
//                    // 将频谱数据转换为适合可视化的格式
//                    float[] logSpectrum = new float[spectrum.length];
//                    for (int i = 0; i < spectrum.length; i++) {
//                        // 转换为对数刻度，更适合可视化
//                        logSpectrum[i] = (float) (20 * Math.log10(spectrum[i] + 1e-6));
//                    }
//
//                    data.addTimeFrame(logSpectrum);
//                    return true;
//                }
//
//                @Override
//                public void processingFinished() {
//                    log.info("Spectrogram generated with {} frames", data.getFrameCount());
//                }
//            });
//
//            dispatcher.run();
//
//        } catch (Exception e) {
//            log.error("Spectrogram generation failed", e);
//        }
//
//        return data;
//    }
//
//    /**
//     * 频谱图数据类
//     */
//    public static class SpectrogramData {
//        private List<float[]> frames = new ArrayList<>();
//        private long timestamp = System.currentTimeMillis();
//
//        public void addTimeFrame(float[] frame) {
//            frames.add(frame.clone());
//        }
//
//        public List<float[]> getFrames() {
//            return frames;
//        }
//
//        public int getFrameCount() {
//            return frames.size();
//        }
//
//        public int getFrequencyBins() {
//            return frames.isEmpty() ? 0 : frames.get(0).length;
//        }
//
//        public long getTimestamp() {
//            return timestamp;
//        }
//    }
//}
