package com.zzx.flowerfieldfrienddisk.model.enums;

/**
 * 队伍状态枚举
 */
public enum FieldStatusEnum {

    PUBLIC(0, "公开"),
    PRIVATE(1, "私有"),
    SECRET(2, "加密");

    private int value;

    private String text;

    public static FieldStatusEnum getEnumByValue(Integer value) {
        if (value == null) {
            return null;
        }
        FieldStatusEnum[] values = FieldStatusEnum.values();
        for (FieldStatusEnum FieldStatusEnum : values) {
            if (FieldStatusEnum.getValue() == value) {
                return FieldStatusEnum;
            }
        }
        return null;
    }

    FieldStatusEnum(int value, String text) {
        this.value = value;
        this.text = text;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}