package com.voicetext.common.audios.voiceclone;
//
//import lombok.extern.slf4j.Slf4j;
//import com.voicetext.common.audio.config.AudioProperties;
//import com.voicetext.common.audio.voiceclone.model.TrainTask;
//import com.voicetext.common.audio.voiceclone.model.VoiceModel;
//import org.springframework.data.redis.core.RedisTemplate;
//
//import java.io.File;
//import java.util.*;
//import java.util.concurrent.TimeUnit;
//
///**
// * 声音模型管理器
// */
//@Slf4j
//public class VoiceModelManager {
//
//    private final AudioProperties properties;
//    private final RedisTemplate<String, Object> redisTemplate;
//    private final Map<String, VoiceModel> modelCache = new HashMap<>();
//
//    // Redis键前缀
//    private static final String MODEL_KEY_PREFIX = "voice:model:";
//    private static final String TENANT_MODELS_KEY = "voice:tenant:%d:models";
//
//    public VoiceModelManager(AudioProperties properties,
//                             RedisTemplate<String, Object> redisTemplate) {
//        this.properties = properties;
//        this.redisTemplate = redisTemplate;
//    }
//
//    /**
//     * 保存模型信息
//     */
//    public void saveModel(TrainTask task, String modelId) {
//        VoiceModel model = VoiceModel.builder()
//            .modelId(modelId)
//            .modelName(task.getModelName())
//            .tenantId(task.getTenantId())
//            .creatorId(task.getCreatorId())
//            .modelType(task.getModelType())
//            .modelPath(getModelPath(modelId))
//            .trainDuration(calculateTrainDuration(task))
//            .status("active")
//            .createTime(new Date())
//            .build();
//
//        // 保存到Redis
//        String key = MODEL_KEY_PREFIX + modelId;
//        redisTemplate.opsForValue().set(key, model, 30, TimeUnit.DAYS);
//
//        // 添加到租户模型列表
//        String tenantKey = String.format(TENANT_MODELS_KEY, task.getTenantId());
//        redisTemplate.opsForList().leftPush(tenantKey, modelId);
//        redisTemplate.expire(tenantKey, 30, TimeUnit.DAYS);
//
//        // 缓存到本地
//        modelCache.put(modelId, model);
//    }
//
//    /**
//     * 获取模型信息
//     */
//    public VoiceModel getModel(String modelId) {
//        // 先查本地缓存
//        VoiceModel model = modelCache.get(modelId);
//        if (model != null) {
//            return model;
//        }
//
//        // 查Redis
//        String key = MODEL_KEY_PREFIX + modelId;
//        model = (VoiceModel) redisTemplate.opsForValue().get(key);
//
//        if (model != null) {
//            modelCache.put(modelId, model);
//        }
//
//        return model;
//    }
//
//    /**
//     * 获取租户的所有模型
//     */
//    public List<VoiceModel> getTenantModels(Long tenantId) {
//        String tenantKey = String.format(TENANT_MODELS_KEY, tenantId);
//        List<Object> modelIds = redisTemplate.opsForList().range(tenantKey, 0, -1);
//
//        if (modelIds == null || modelIds.isEmpty()) {
//            return Collections.emptyList();
//        }
//
//        List<VoiceModel> models = new ArrayList<>();
//        for (Object modelId : modelIds) {
//            VoiceModel model = getModel(modelId.toString());
//            if (model != null) {
//                models.add(model);
//            }
//        }
//
//        return models;
//    }
//
//    /**
//     * 检查配额
//     */
//    public boolean checkQuota(Long tenantId) {
//        List<VoiceModel> models = getTenantModels(tenantId);
//        int quota = properties.getVoiceClone().getModelQuota();
//        return models.size() < quota;
//    }
//
//    /**
//     * 删除模型
//     */
//    public void deleteModel(String modelId) {
//        VoiceModel model = getModel(modelId);
//        if (model == null) {
//            return;
//        }
//
//        // 从Redis删除
//        String key = MODEL_KEY_PREFIX + modelId;
//        redisTemplate.delete(key);
//
//        // 从租户列表移除
//        String tenantKey = String.format(TENANT_MODELS_KEY, model.getTenantId());
//        redisTemplate.opsForList().remove(tenantKey, 0, modelId);
//
//        // 删除本地缓存
//        modelCache.remove(modelId);
//
//        // 删除模型文件
//        File modelFile = new File(model.getModelPath());
//        if (modelFile.exists()) {
//            modelFile.delete();
//        }
//    }
//
//    /**
//     * 设置默认模型
//     */
//    public void setDefaultModel(Long tenantId, String modelId) {
//        List<VoiceModel> models = getTenantModels(tenantId);
//        for (VoiceModel model : models) {
//            if (model.getModelId().equals(modelId)) {
//                model.setDefault(true);
//            } else {
//                model.setDefault(false);
//            }
//            // 更新到Redis
//            String key = MODEL_KEY_PREFIX + model.getModelId();
//            redisTemplate.opsForValue().set(key, model);
//        }
//    }
//
//    /**
//     * 获取默认模型
//     */
//    public VoiceModel getDefaultModel(Long tenantId) {
//        List<VoiceModel> models = getTenantModels(tenantId);
//        return models.stream()
//            .filter(VoiceModel::isDefault)
//            .findFirst()
//            .orElse(models.isEmpty() ? null : models.get(0));
//    }
//
//    /**
//     * 获取模型文件路径
//     */
//    private String getModelPath(String modelId) {
//        return properties.getVoiceClone().getModelRoot() +
//               File.separator + modelId;
//    }
//
//    /**
//     * 计算训练时长
//     */
//    private int calculateTrainDuration(TrainTask task) {
//        // TODO: 计算实际训练时长
//        return 0;
//    }
//}
