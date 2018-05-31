package com.xx.model

class FastXmlNamespaceReader {
    Map read(File file) {
        FileReader fileReader = new FileReader(file)

        StringBuffer sb = new StringBuffer()

        boolean _xml_end = false
        int len
        char c
        while ((len = fileReader.read()) != -1) {
            c = (char) len
            if (c == '<') {
                continue
            } else if (c == '>') {
                if (!_xml_end) {
                    _xml_end = true
                    continue
                }
                break
            } else {
                if (_xml_end) {
                    sb.append(c)
                }
            }
        }
        fileReader.close()
        println("FastXmlNamespaceReader.read() : " + sb.toString() + "\n-------------\n")

        def xmlnsMap = [:]
        def attrs = sb.toString().trim().split("\\s+")
        attrs.each {
            if (it.toString().startsWith("xmlns:")) {
                def str = it.toString().replaceFirst("xmlns:", "").split("=")
                xmlnsMap.put(str[0], str[1].replaceAll("\"", ""))
            }
        }

//        xmlnsMap.each {key,value->
//            println(key+"_"+value)
//        }
        xmlnsMap
    }
}
