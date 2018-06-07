package com.xx.impl.getui

import com.xx.bean.GetuiUserBean
import com.xx.interfaces.IManifest

/**
 * 个推SDK集成模块
 * Created by xievxin on 2018/5/31
 */
class GetuiManifest extends IManifest {

    @Override
    void checkInfo() {
        def usr = project.extensions.findByType(GetuiUserBean)

        if (!usr.getui_APP_ID) {
            System.err.println("getui_APP_ID not found")
        }
        if (!usr.getui_APP_KEY) {
            System.err.println("getui_APP_KEY not found")
        }
        if (!usr.getui_APP_SECRET) {
            System.err.println("getui_APP_SECRET not found")
        }
        println("GetuiManifest.checkInfo() end")
    }

    @Override
    protected void appendApplicationNodes() {
        def usr = project.extensions.findByType(GetuiUserBean)
        appendNode(NODE_COMMENT, "个推SDK配置开始")
        appendNode(IManifest.NODE_COMMENT, "配置的第三方参数属性")
        appendNode(IManifest.NODE_COMMENT, "插件标识，请勿删除")
        appendNode("meta-data", ["android:name": "PUSH_FLAG", "android:value": ""])
        appendNode("meta-data", ["android:name": "PUSH_APPID", "android:value": "${usr.getui_APP_ID}"])
        appendNode("meta-data", ["android:name": "PUSH_APPKEY", "android:value": "${usr.getui_APP_KEY}"])
        appendNode("meta-data", ["android:name": "PUSH_APPSECRET", "android:value": "${usr.getui_APP_SECRET}}"])
        appendNode(IManifest.NODE_COMMENT, "配置SDK核心服务")
        appendNode("service", [
                "android:name"    : "com.igexin.sdk.PushService",
                "android:exported": "true",
                "android:label"   : "NotificationCenter",
                "android:process" : ":pushservice"
        ]).appendNode("intent-filter")
                .appendNode("action", ["android:name": "com.igexin.sdk.action.service.message"])
        appendNode("receiver", ["android:name": "com.igexin.sdk.PushReceiver"])
                .appendNode("intent-filter")
                .appendNode("action", ["android:name": "android.intent.action.BOOT_COMPLETED"]).parent()
                .appendNode("action", ["android:name": "android.net.conn.CONNECTIVITY_CHANGE"]).parent()
                .appendNode("action", ["android:name": "android.intent.action.USER_PRESENT"]).parent()
                .appendNode("action", ["android:name": "com.igexin.sdk.action.refreshls"]).parent()
                .appendNode(IManifest.NODE_COMMENT, "以下三项为可选的action声明，可大大提高service存活率和消息到达速度").parent()
                .appendNode("action", ["android:name": "android.intent.action.MEDIA_MOUNTED"]).parent()
                .appendNode("action", ["android:name": "android.intent.action.ACTION_POWER_CONNECTED"]).parent()
                .appendNode("action", ["android:name": "android.intent.action.ACTION_POWER_DISCONNECTED"]).parent()

        appendNode("activity", [
                "android:name"              : "com.igexin.sdk.PushActivity",
                "android:excludeFromRecents": "true",
                "android:exported"          : "false",
                "android:process"           : ":pushservice",
                "android:taskAffinity"      : "com.igexin.sdk.PushActivityTask",
                "android:theme"             : "@android:style/Theme.Translucent.NoTitleBar"
        ])
        appendNode("activity", [
                "android:name"              : "com.igexin.sdk.GActivity",
                "android:excludeFromRecents": "true",
                "android:exported"          : "true",
                "android:process"           : ":pushservice",
                "android:taskAffinity"      : "com.igexin.sdk.PushActivityTask",
                "android:theme"             : "@android:style/Theme.Translucent.NoTitleBar"
        ])
        appendNode(IManifest.NODE_COMMENT, "个推SDK配置结束")
    }

    @Override
    protected void appendPermissionNodes() {
        def applicationId = android.defaultConfig.applicationId
        appendNode("uses-permission", ["android:name": "android.permission.INTERNET"])
        appendNode("uses-permission", ["android:name": "android.permission.READ_PHONE_STATE"])
        appendNode("uses-permission", ["android:name": "android.permission.ACCESS_NETWORK_STATE"])
        appendNode("uses-permission", ["android:name": "android.permission.CHANGE_WIFI_STATE"])
        appendNode("uses-permission", ["android:name": "android.permission.ACCESS_WIFI_STATE"])
        appendNode("uses-permission", ["android:name": "android.permission.WAKE_LOCK"])
        appendNode("uses-permission", ["android:name": "android.permission.RECEIVE_BOOT_COMPLETED"])
        appendNode("uses-permission", ["android:name": "android.permission.WRITE_EXTERNAL_STORAGE"])
        appendNode("uses-permission", ["android:name": "android.permission.VIBRATE"])
        appendNode("uses-permission", ["android:name": "android.permission.GET_TASKS"])
        appendNode(IManifest.NODE_COMMENT, "支持iBeancon 需要蓝牙权限")
        appendNode("uses-permission", ["android:name": "android.permission.BLUETOOTH"])
        appendNode("uses-permission", ["android:name": "android.permission.BLUETOOTH_ADMIN"])
        appendNode(IManifest.NODE_COMMENT, "支持个推3.0 电子围栏功能")
        appendNode("uses-permission", ["android:name": "android.permission.ACCESS_FINE_LOCATION"])
        appendNode("uses-permission", ["android:name": "android.permission.ACCESS_COARSE_LOCATION"])
        appendNode(IManifest.NODE_COMMENT, "浮动通知权限")
        appendNode("uses-permission", ["android:name": "android.permission.SYSTEM_ALERT_WINDOW"])
        appendNode(IManifest.NODE_COMMENT, "自定义权限")
        appendNode("uses-permission", ["android:name": "getui.permission.GetuiService.${applicationId}"])
        appendNode("permission", [
                "android:name"           : "getui.permission.GetuiService.${applicationId}",
                "android:protectionLevel": "normal"
        ])
    }

}
