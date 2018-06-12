package com.xx.impl.geshu

import com.android.build.gradle.AppExtension
import com.xx.bean.UserBean
import com.xx.interfaces.IManifest

/**
 * 个数SDK集成模块
 * Created by xievxin on 2018/6/11
 */
class GeshuManifest extends IManifest {

    @Override
    void checkInfo() {
        def android = project.extensions.findByType(AppExtension)
        def usr = project.extensions.findByType(UserBean)

        def placeHolders = android.defaultConfig.manifestPlaceholders
        placeHolders.put("GS_APPID", usr.gs_APP_ID)
        placeHolders.put("GT_INSTALL_CHANNEL", usr.gs_INSTALL_CHANNEL)

        if (!usr.gs_APP_ID) {
            System.err.println("gs_APP_ID not found")
        }
        if (!usr.gs_INSTALL_CHANNEL) {
            System.err.println("gs_INSTALL_CHANNEL not found")
        }
        println("个数Manifest.checkInfo() end")
    }

    @Override
    protected void appendApplicationNodes() {

    }

    @Override
    protected void appendPermissionNodes() {

    }
}
