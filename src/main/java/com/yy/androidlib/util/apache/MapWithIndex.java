package com.yy.androidlib.util.apache;

import java.util.HashMap;

public class MapWithIndex<K, V> extends HashMap<K, V> {
    public K keyAt(int index) {
        int i = 0;
        for (K key : keySet()) {
            if (i == index) {
                return key;
            }
            ++i;
        }
        return null;
    }

    public V valueAt(int index) {
        return get(keyAt(index));
    }
}
