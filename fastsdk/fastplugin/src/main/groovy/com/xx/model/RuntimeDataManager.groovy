package com.xx.model

import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import org.gradle.api.Project

/**
 * Created by xievxin on 2018/5/22
 */
class RuntimeDataManager {

    public String pluginDir
    public String zipSDKPath

    public static Project mProject = null
    private static RuntimeDataManager mInstance
    private def android

    static synchronized RuntimeDataManager getInstance() {
        if (mInstance == null) {
            mInstance = new RuntimeDataManager()
        }
        return mInstance
    }

    private RuntimeDataManager() {
        pluginDir = mProject.getBuildDir().getAbsolutePath() + File.separator + "gtPlugins"
        zipSDKPath = pluginDir + File.separator + "libSDK.zip"
    }

}
