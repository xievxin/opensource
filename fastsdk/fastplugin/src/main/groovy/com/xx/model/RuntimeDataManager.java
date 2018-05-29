package com.xx.model;

import org.gradle.api.Project;

import java.io.File;

/**
 * Created by xievxin on 2018/5/22
 */
public class RuntimeDataManager {
    public String pluginDir;
    public String zipSDKPath;

    public static Project mProject = null;
    private static RuntimeDataManager mInstance;

    public static synchronized RuntimeDataManager getInstance() {
        if (mInstance == null) {
            mInstance = new RuntimeDataManager();
        }
        return mInstance;
    }

    public RuntimeDataManager() {
        pluginDir = mProject.getBuildDir().getAbsolutePath() + File.separator + "gtPlugins";
        zipSDKPath = pluginDir + File.separator + "libSDK.zip";
    }
}
