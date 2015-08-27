package com.yy.androidlib.util.sdk;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseAdapter<T> extends android.widget.BaseAdapter {

    protected List<T> items = new ArrayList<T>();

    @Override
    public int getCount() {
        if (items == null) {
            return 0;
        } else {
            return items.size();
        }
    }

    @Override
    public T getItem(int i) {
        if (i < 0 || i >= items.size()) {
            return null;
        } else {
            return items.get(i);
        }
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    public void setItems(List<T> data) {
        this.items = new ArrayList<T>(data);
        notifyDataSetChanged();
    }

    public List<T> getItems() {
        return items;
    }
}
