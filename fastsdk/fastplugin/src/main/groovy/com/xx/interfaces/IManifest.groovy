package com.xx.interfaces

import com.android.build.gradle.AppExtension
import com.xx.model.RuntimeDataManager
import groovy.xml.StreamingMarkupBuilder
import org.gradle.api.Project

/**
 * 2018-6-5 将集成代码集中在一个区域模块，如果用户进行了移动，下一次Build会将其还原到区域内
 * 2018-6-7 生成apk的时候再添加进Manifest.xml中
 *
 * Created by xievxin on 2018/5/30
 */
abstract class IManifest {

    def static final NAME = "android:name"

    /**
     * 注释节点
     */
    static final String NODE_COMMENT = "gtp_comment"

    /**
     * 输出文本节点，like '\n\r\t'
     */
    static final String NODE_YIELD = "gtp_yield"

    static
    def tabs = ["", "\t", "\t\t", "\t\t\t", "\t\t\t\t", "\t\t\t\t\t", "\t\t\t\t\t\t", "\t\t\t\t\t\t\t", "\t\t\t\t\t\t\t\t"]

    Project project
    def android

    int curTabCount
    def curRoot
    Node compRoot = new Node(null, "compRoot")
    Node pmsRoot = new Node(null, "pmsRoot")

    IManifest() {
        project = RuntimeDataManager.mProject
        android = project.extensions.getByType(AppExtension)
    }

    final void write(File file, String charset) {
        StringBuilder sb = new StringBuilder()
        String content = file.getText(charset)
        int appIndex = content.lastIndexOf("</application>")
        int maniIndex = content.lastIndexOf("</manifest>")
        sb.append(content.substring(0, appIndex))
                .append(addComponentItem())
                .append(content.substring(appIndex, maniIndex))
                .append(addPermissionItem())
                .append(content.substring(maniIndex))
        file.write(sb.toString(), charset)
    }

    private String addComponentItem() {
        curRoot = compRoot
        appendApplicationNodes()
        curTabCount = 2
        return new StreamingMarkupBuilder().bind(result).toString()
                .replace("<manifest>", "")
                .replace("</manifest>", "")
    }

    private String addPermissionItem() {
        curRoot = pmsRoot
        appendPermissionNodes()
        curTabCount = 1
        return new StreamingMarkupBuilder().bind(result).toString()
                .replace("<manifest>", "")
                .replace("</manifest>", "")
    }

    /**
     * 检查用户是否配置了相关信息
     */
    abstract void checkInfo()

    /**
     * application节点下拼接
     * activity、service、receiver、provider等
     * @param root < manifest>
     */
    protected abstract void appendApplicationNodes()

    /**
     * 权限节点拼接
     * @param root < manifest>
     */
    protected abstract void appendPermissionNodes()

    Node appendNode(String name, Map attributes) {
        return _appendNode(name, attributes, new NodeList())
    }

    Node appendNode(String name, Object value) {
        return _appendNode(name, new HashMap(), value)
    }

    /**
     *
     * @param name 节点名
     * @param attributes 属性
     * @param value <>value<\>
     * @return
     */
    private Node _appendNode(String name, Map attributes, Object value) {
        return new Node(curRoot, name, attributes, value)
    }

    /**
     * xml格式规范化
     */
    final def result = {
        manifest() {
            def getAttrs = { Node node, int tabCount ->
                def attrMap = [:]
                int size = node.attributes().size()
                node?.attributes()?.each { key, value ->
                    attrMap.put((size > 1 ? "\n" + tabs[tabCount] : "") + key.toString(), value)
                }
                attrMap
            }
            def parseChild = { Iterator<Node> nodeIt, int deepCount, NodeCallback callback ->
                while (nodeIt.hasNext()) {
                    def nd = nodeIt.next()
                    def name = nd.name()

                    if (name == NODE_COMMENT) {
                        mkp.yield("\n" + tabs[deepCount])
                        mkp.comment(nd.text())
                        continue
                    } else if (name == NODE_YIELD) {
                        mkp.yield("\n" + tabs[deepCount])
                        mkp.yield(nd.text())
                        continue
                    }
                    mkp.yield("\n" + tabs[deepCount])

                    // 没有子节点就以“/>”结尾
                    if (!nd.children()) {
                        "${name}"(getAttrs(nd, deepCount + 1))
                        continue
                    }
                    "${name}"(getAttrs(nd, deepCount + 1)) {
                        callback.onCall(nd, deepCount + 1)
                    }
                }
                mkp.yield("\n" + tabs[deepCount - 1])
            }
            def getChildStr = { Node node ->
                if (node?.children()) {
                    parseChild(node.iterator(), curTabCount, new NodeCallback() {
                        @Override
                        void onCall(Node nd, int curDeepCount) {
                            parseChild(nd.iterator(), curDeepCount, this)
                        }
                    })
                }
            }

            getChildStr(curRoot)
        }
    }

}
