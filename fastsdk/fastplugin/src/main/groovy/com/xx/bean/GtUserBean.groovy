package com.xx.bean

class GtUserBean {

    String APP_ID
    String APP_KEY
    String APP_SECRET
    boolean skipNetCheck = false


    @Override
    public String toString() {
        return "GtUserBean{" +
                "APP_ID='" + APP_ID + '\'' +
                ", APP_KEY='" + APP_KEY + '\'' +
                ", APP_SECRET='" + APP_SECRET + '\'' +
                ", skipNetCheck=" + skipNetCheck +
                '}';
    }
}
