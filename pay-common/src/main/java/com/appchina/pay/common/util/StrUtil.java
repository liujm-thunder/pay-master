package com.appchina.pay.common.util;


public class StrUtil {

    public static String toString(Object obj) {
        return obj == null?"":obj.toString();
    }

    public static String toString(Object obj, String nullStr) {
        return obj == null?nullStr:obj.toString();
    }

}
