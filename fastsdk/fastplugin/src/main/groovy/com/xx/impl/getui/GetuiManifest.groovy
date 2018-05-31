package com.xx.impl.getui

import com.xx.interfaces.IManifest

class GetuiManifest extends IManifest {

    @Override
    protected void appendApplicationNodes(def mkp, Node root) {
        root.appendNode(IManifest.NODE_COMMENT, "个推SDK配置开始")
        root.appendNode(IManifest.NODE_COMMENT, "配置的第三方参数属性")
        root.appendNode(IManifest.NODE_COMMENT, "插件标识，请勿删除")
        root.appendNode("meta-data", ["android:name": "PUSH_FLAG", "android:value": ""])
        root.appendNode("meta-data", ["android:name": "PUSH_APPID", "android:value": "${project.gtUser.APP_ID}"])
        root.appendNode("meta-data", ["android:name": "PUSH_APPKEY", "android:value": "${project.gtUser.APP_KEY}"])
        root.appendNode("meta-data", ["android:name": "PUSH_APPSECRET", "android:value": "${project.gtUser.APP_SECRET}}"])
        root.appendNode(IManifest.NODE_COMMENT, "配置SDK核心服务")
        root.appendNode("service", [
                "android:name"    : "com.igexin.sdk.PushService",
                "android:exported": "true",
                "android:label"   : "NotificationCenter",
                "android:process" : ":pushservice"
        ]).appendNode("intent-filter")
                .appendNode("action", ["android:name": "com.igexin.sdk.action.service.message"])
        root.appendNode("receiver", ["android:name": "com.igexin.sdk.PushReceiver"])
                .appendNode("intent-filter")
                .appendNode("action", ["android:name": "android.intent.action.BOOT_COMPLETED"]).parent()
                .appendNode("action", ["android:name": "android.net.conn.CONNECTIVITY_CHANGE"]).parent()
                .appendNode("action", ["android:name": "android.intent.action.USER_PRESENT"]).parent()
                .appendNode("action", ["android:name": "com.igexin.sdk.action.refreshls"]).parent()
                .appendNode(IManifest.NODE_COMMENT, "以下三项为可选的action声明，可大大提高service存活率和消息到达速度").parent()
                .appendNode("action", ["android:name": "android.intent.action.MEDIA_MOUNTED"]).parent()
                .appendNode("action", ["android:name": "android.intent.action.ACTION_POWER_CONNECTED"]).parent()
                .appendNode("action", ["android:name": "android.intent.action.ACTION_POWER_DISCONNECTED"]).parent()
        root.appendNode("activity", [
                "android:name": "com.igexin.sdk.PushActivity",
                "android:excludeFromRecents": "true",
                "android:exported": "false",
                "android:process": ":pushservice",
                "android:taskAffinity": "com.igexin.sdk.PushActivityTask",
                "android:theme": "@android:style/Theme.Translucent.NoTitleBar"
        ])
        root.appendNode("activity", [
                "android:name": "com.igexin.sdk.GActivity",
                "android:excludeFromRecents": "true",
                "android:exported": "true",
                "android:process": ":pushservice",
                "android:taskAffinity": "com.igexin.sdk.PushActivityTask",
                "android:theme": "@android:style/Theme.Translucent.NoTitleBar"
        ])
        root.appendNode(IManifest.NODE_COMMENT, "个推SDK配置结束")
    }

    @Override
    protected void appendPermissionNodes(def mkp, Node root) {
        root.appendNode("uses-permission", ["android:name": "android.permission.INTERNET"])
        root.appendNode("uses-permission", ["android:name": "android.permission.READ_PHONE_STATE"])
        root.appendNode("uses-permission", ["android:name": "android.permission.ACCESS_NETWORK_STATE"])
        root.appendNode("uses-permission", ["android:name": "android.permission.CHANGE_WIFI_STATE"])
        root.appendNode("uses-permission", ["android:name": "android.permission.ACCESS_WIFI_STATE"])
        root.appendNode("uses-permission", ["android:name": "android.permission.WAKE_LOCK"])
        root.appendNode("uses-permission", ["android:name": "android.permission.RECEIVE_BOOT_COMPLETED"])
        root.appendNode("uses-permission", ["android:name": "android.permission.WRITE_EXTERNAL_STORAGE"])
        root.appendNode("uses-permission", ["android:name": "android.permission.VIBRATE"])
        root.appendNode("uses-permission", ["android:name": "android.permission.GET_TASKS"])
        root.appendNode(IManifest.NODE_COMMENT, "支持iBeancon 需要蓝牙权限")
        root.appendNode("uses-permission", ["android:name": "android.permission.BLUETOOTH"])
        root.appendNode("uses-permission", ["android:name": "android.permission.BLUETOOTH_ADMIN"])
        root.appendNode(IManifest.NODE_COMMENT, "支持个推3.0 电子围栏功能")
        root.appendNode("uses-permission", ["android:name": "android.permission.ACCESS_FINE_LOCATION"])
        root.appendNode("uses-permission", ["android:name": "android.permission.ACCESS_COARSE_LOCATION"])
        root.appendNode(IManifest.NODE_COMMENT, "浮动通知权限")
        root.appendNode("uses-permission", ["android:name": "android.permission.SYSTEM_ALERT_WINDOW"])
        root.appendNode(IManifest.NODE_COMMENT, "自定义权限")
        root.appendNode("uses-permission", ["android:name": "getui.permission.GetuiService.\${applicationId}"])
        root.appendNode("permission",
                ["android:name": "getui.permission.GetuiService.\${applicationId}", "android:protectionLevel": "normal"])
        mkp.yield "\n"
    }
}
