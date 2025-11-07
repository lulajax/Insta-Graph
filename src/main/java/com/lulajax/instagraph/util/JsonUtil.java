package com.lulajax.instagraph.util;

import com.alibaba.fastjson2.JSON;

public class JsonUtil {

    public static <T> T parseObject(String json, Class<T> clazz) {
        return JSON.parseObject(json, clazz);
    }
}
