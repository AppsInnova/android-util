package com.yy.androidlib.util.apache;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CollectionUtils {

    public static boolean containsAll(Map map, List keys) {
        for (Object key : keys) {
            if (!map.containsKey(key)) {
                return false;
            }
        }
        return true;
    }

    public static List findAll(Map map, List keys) {
        List list = new ArrayList();
        for (Object key : keys) {
            Object value = map.get(key);
            if (value != null) {
                list.add(value);
            }
        }
        return list;
    }
}
