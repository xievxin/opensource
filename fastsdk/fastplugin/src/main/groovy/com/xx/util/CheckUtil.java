package com.xx.util;

import org.gradle.api.Project;

/**
 * Created by xievxin on 2018/6/4
 */
public class CheckUtil {

    /**
     * Plugin version	Required Gradle version
     * 1.0.0 - 1.1.3	2.2.1 - 2.3
     * 1.2.0 - 1.3.1	2.2.1 - 2.9
     * 1.5.0	        2.2.1 - 2.13
     * 2.0.0 - 2.1.2	2.10 - 2.13
     * 2.1.3 - 2.2.3	2.14.1+
     * 2.3.0+	        3.3+
     * 3.0.0+	        4.1+
     * @param project
     * @return
     */
    public static boolean isGradleUper3_0_0(Project project) {
        int verStart = Integer.parseInt(project.getGradle().getGradleVersion().substring(0, 1));
        return verStart>=4;
    }

    public static boolean isEmpty(CharSequence s) {
        if (s == null) {
            return true;
        } else {
            return s.length() == 0;
        }
    }
}
