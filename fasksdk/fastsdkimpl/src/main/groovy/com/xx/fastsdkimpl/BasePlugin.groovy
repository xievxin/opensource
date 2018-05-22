package com.xx.fastsdkimpl

import org.gradle.api.Plugin
import org.gradle.api.Project

public class BasePlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        System.err.println("BasePlugin start...")
    }
}
