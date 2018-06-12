package com.xx.fastsdkimpl

import org.gradle.api.Project

/**
 * 售后
 * Created by xievxin on 2018/6/8
 */
class FastSDKChecker {

    static void notAskJustWaiting(Project project) {
        // 可以拿到extensions来检测，也可以调接口获取对应type来检测
        println("售后检测工具.notAskJustWaiting()...")
    }
}
