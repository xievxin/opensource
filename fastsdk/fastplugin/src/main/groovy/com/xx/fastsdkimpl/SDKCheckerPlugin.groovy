package com.xx.fastsdkimpl

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.project.DefaultProject
import org.gradle.api.internal.tasks.DefaultTaskContainer

import java.lang.reflect.Method

class SDKCheckerPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.afterEvaluate {
            FastSDKChecker.notAskJustWaiting(project)
        }
    }

    void addTask() {
        try {
            def taskContainerField = DefaultProject.class.getDeclaredField("taskContainer")
            taskContainerField.setAccessible(true)
            def taskContainer = taskContainerField.get(project)

            // public <T extends Task> T create(String name, Class<T> type) {
            Method createMhd = DefaultTaskContainer.class.getDeclaredMethod("create", String.class, Class.class)
            createMhd.invoke(taskContainer, "fastsdk", FastSDKChecker.class)
            println("addTask 'fastsdk'")

//            project.getTasksByName("fastsdk", false).each {Task task->
//                if(task.getClass().simpleName.startsWith("FastSDKChecker")) {
//                    task.doFirst {}
//                }
//            }
        } catch (Exception e) {
            e.printStackTrace()
        }
    }
}
