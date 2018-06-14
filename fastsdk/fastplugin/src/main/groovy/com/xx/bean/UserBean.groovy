package com.xx.bean

class UserBean {

    String getui_APP_ID
    String getui_APP_KEY
    String getui_APP_SECRET

    String gs_APP_ID
    String gs_INSTALL_CHANNEL


    @Override
    public String toString() {
        return "UserBean{" +
                "getui_APP_ID='" + getui_APP_ID + '\'' +
                ", getui_APP_KEY='" + getui_APP_KEY + '\'' +
                ", getui_APP_SECRET='" + getui_APP_SECRET + '\'' +
                ", gs_APP_ID='" + gs_APP_ID + '\'' +
                ", gs_INSTALL_CHANNEL='" + gs_INSTALL_CHANNEL + '\'' +
                '}';
    }
}
