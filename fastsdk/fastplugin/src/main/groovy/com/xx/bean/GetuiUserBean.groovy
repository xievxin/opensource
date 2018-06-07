package com.xx.bean

class GetuiUserBean {

    String getui_APP_ID
    String getui_APP_KEY
    String getui_APP_SECRET
    boolean skipNetCheck = false


    @Override
    public String toString() {
        return "GetuiUserBean{" +
                "getui_APP_ID='" + getui_APP_ID + '\'' +
                ", getui_APP_KEY='" + getui_APP_KEY + '\'' +
                ", getui_APP_SECRET='" + getui_APP_SECRET + '\'' +
                ", skipNetCheck=" + skipNetCheck +
                '}';
    }
}
