package com.pickyboy.interviewcodex.constant;



/**
 * 缓存场景枚举
 * <p>
 * 它定义了所有业务缓存的前缀，并提供 getScene() 方法供注解使用。
 *
 * @author pickyboy
 */
public enum CacheSceneEnum {

    BANK_DETAIL("bank_detail"),
    QUESTION_DETAIL("question_detail");

    private final String scene;
    public String getScene() {
        return scene;
    }
    CacheSceneEnum(String scene) {
        this.scene = scene;
    }
}