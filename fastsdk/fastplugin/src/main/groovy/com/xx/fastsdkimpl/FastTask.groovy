package com.xx.fastsdkimpl

import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.tasks.TaskAction

/**
 * 预留给后期干  售后
 * Created by xievxin on 2018/6/1
 */
class FastTask extends DefaultTask {

    @Override
    Task doFirst(Closure action) {
        Task task = super.doFirst(action)
        println("FastTask.doFirst()...")
        return task
    }

    @TaskAction
    void ya() {
        println("yayayayaya")
    }
}
