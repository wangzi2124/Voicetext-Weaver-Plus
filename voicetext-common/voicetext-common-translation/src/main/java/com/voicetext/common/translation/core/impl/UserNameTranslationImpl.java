package com.voicetext.common.translation.core.impl;

import cn.hutool.core.convert.Convert;
import com.voicetext.common.core.service.UserService;
import com.voicetext.common.translation.annotation.TranslationType;
import com.voicetext.common.translation.constant.TransConstant;
import com.voicetext.common.translation.core.TranslationInterface;
import lombok.AllArgsConstructor;

/**
 * 用户名翻译实现
 *
 * @author Lion Li
 */
@AllArgsConstructor
@TranslationType(type = TransConstant.USER_ID_TO_NAME)
public class UserNameTranslationImpl implements TranslationInterface<String> {

    private final UserService userService;

    @Override
    public String translation(Object key, String other) {
        return userService.selectUserNameById(Convert.toLong(key));
    }
}
