package com.zzx.flowerfieldfrienddisk.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;

/**
 * 文件类型枚举（按文件后缀归类，对应 file 表的 format 字段）
 */
@Getter
public enum FileTypeEnum {

    IMAGE("图片", "image", Arrays.asList("jpg", "jpeg", "png", "webp")),

    VIDEO("视频", "video", Arrays.asList("mp4", "webm")),

    AUDIO("音频", "audio", Arrays.asList("mp3", "m4a"));

    private final String text;

    private final String value;

    /**
     * 该类型包含的文件后缀（不含点，小写）
     */
    private final List<String> formats;

    FileTypeEnum(String text, String value, List<String> formats) {
        this.text = text;
        this.value = value;
        this.formats = formats;
    }

    /**
     * 根据 value 获取枚举
     */
    public static FileTypeEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (FileTypeEnum fileTypeEnum : FileTypeEnum.values()) {
            if (fileTypeEnum.value.equals(value)) {
                return fileTypeEnum;
            }
        }
        return null;
    }
}
