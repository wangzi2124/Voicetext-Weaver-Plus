package org.dromara.common.audio.core;

import org.dromara.common.audio.dsp.AudioFeatures;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * 音频处理核心接口
 * 定义所有音频处理的基本操作
 *
 * @author VoiceText Weaver
 * @since 2026-03-01
 */
public interface AudioProcessor {

    /**
     * 音频格式转换
     *
     * @param sourceFile 源文件
     * @param targetFormat 目标格式 (mp3, wav, m4a, ogg等)
     * @return 转换后的文件
     */
    File convert(File sourceFile, String targetFormat);

    /**
     * 音频格式转换（带参数）
     *
     * @param sourceFile 源文件
     * @param targetFormat 目标格式
     * @param params 转换参数（比特率、采样率等）
     * @return 转换后的文件
     */
    File convert(File sourceFile, String targetFormat, Map<String, Object> params);

    /**
     * 音频裁剪
     *
     * @param sourceFile 源文件
     * @param startTime 开始时间（秒）
     * @param endTime 结束时间（秒）
     * @return 裁剪后的文件
     */
    File trim(File sourceFile, double startTime, double endTime);

    /**
     * 音频合并
     *
     * @param audioFiles 待合并的音频文件列表
     * @return 合并后的文件
     */
    File merge(List<File> audioFiles);

    /**
     * 提取音频特征
     *
     * @param audioFile 音频文件
     * @return 特征对象
     */
    AudioFeatures extractFeatures(File audioFile);

    /**
     * 生成波形图数据
     *
     * @param audioFile 音频文件
     * @param width 宽度（像素）
     * @param height 高度（像素）
     * @return 波形图字节数组（PNG格式）
     */
    byte[] generateWaveform(File audioFile, int width, int height);

    /**
     * 生成波形图数据（JSON格式，用于前端绘制）
     *
     * @param audioFile 音频文件
     * @param points 采样点数
     * @return 波形数据数组
     */
    double[] generateWaveformData(File audioFile, int points);

    /**
     * 获取音频基本信息
     *
     * @param audioFile 音频文件
     * @return 音频信息Map
     */
    Map<String, Object> getAudioInfo(File audioFile);

    /**
     * 检查音频文件是否有效
     *
     * @param audioFile 音频文件
     * @return 是否有效
     */
    boolean validateAudio(File audioFile);

    /**
     * 支持的格式列表
     *
     * @return 格式列表
     */
    List<String> getSupportedFormats();
}
