package com.xx.model

import org.gradle.api.Project

/**
 * Created by xievxin on 2018/5/22
 */
class RuntimeDataManager {

    public String pluginDir

    public static Project mProject = null
    private static RuntimeDataManager mInstance

    static synchronized RuntimeDataManager getInstance() {
        if (mInstance == null) {
            mInstance = new RuntimeDataManager()
        }
        return mInstance
    }

    private RuntimeDataManager() {
        pluginDir = mProject.getBuildDir().getAbsolutePath() + File.separator + "gtPlugins"
    }

}
