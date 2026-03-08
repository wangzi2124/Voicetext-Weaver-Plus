package com.voicetext.common.audios.core;

//import com.voicetext.common.audios.dsp.model.AudioSegment;
//
//import java.io.File;
//import java.util.List;
//import java.util.Map;
//
///**
// * 音频处理器接口
// * 定义所有音频处理的核心操作
// */
//public interface AudioProcessor {
//
//    /**
//     * 获取音频信息
//     *
//     * @param audioFile 音频文件
//     * @return 音频信息(时长、采样率、声道等)
//     */
//    Map<String, Object> getAudioInfo(File audioFile);
//
//    /**
//     * 格式转换
//     *
//     * @param inputFile  输入文件
//     * @param targetFormat 目标格式(mp3/wav/flac/aac)
//     * @param options    转换选项(采样率、比特率等)
//     * @return 转换后的文件
//     */
//    File convertFormat(File inputFile, String targetFormat, Map<String, Object> options);
//
//    /**
//     * 裁剪音频
//     *
//     * @param inputFile 输入文件
//     * @param startTime 开始时间(秒)
//     * @param endTime   结束时间(秒)
//     * @return 裁剪后的文件
//     */
//    File trim(File inputFile, double startTime, double endTime);
//
//    /**
//     * 拼接音频
//     *
//     * @param audioFiles 音频文件列表
//     * @param crossFade  交叉淡变时长(秒)
//     * @return 拼接后的文件
//     */
//    File concat(List<File> audioFiles, double crossFade);
//
//    /**
//     * 调整音量
//     *
//     * @param inputFile 输入文件
//     * @param ratio     音量系数(0.0-2.0)
//     * @return 调整后的文件
//     */
//    File adjustVolume(File inputFile, double ratio);
//
//    /**
//     * 音量归一化
//     *
//     * @param inputFile 输入文件
//     * @param targetDb  目标音量(dB)
//     * @return 归一化后的文件
//     */
//    File normalize(File inputFile, double targetDb);
//
//    /**
//     * 添加淡入淡出
//     *
//     * @param inputFile 输入文件
//     * @param fadeIn    淡入时长(秒)
//     * @param fadeOut   淡出时长(秒)
//     * @return 处理后的文件
//     */
//    File addFade(File inputFile, double fadeIn, double fadeOut);
//
//    /**
//     * 提取音频片段
//     *
//     * @param inputFile 输入文件
//     * @param segments  片段列表
//     * @return 包含所有片段的文件
//     */
//    File extractSegments(File inputFile, List<AudioSegment> segments);
//
//    /**
//     * 生成波形数据
//     *
//     * @param inputFile    输入文件
//     * @param samplesCount 采样点数
//     * @return 波形数据(归一化到-1到1)
//     */
//    float[] generateWaveform(File inputFile, int samplesCount);
//
//    /**
//     * 获取音频指纹
//     *
//     * @param inputFile 输入文件
//     * @return 音频指纹(用于识别)
//     */
//    String generateFingerprint(File inputFile);
//
//    /**
//     * 检测音频格式是否支持
//     *
//     * @param format 格式名称
//     * @return 是否支持
//     */
//    boolean isFormatSupported(String format);
//}
