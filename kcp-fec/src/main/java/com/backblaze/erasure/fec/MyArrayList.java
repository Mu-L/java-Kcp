package com.backblaze.erasure.fec;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/**
 * Created by JinMiao
 * 2020/7/2.
 */
public class MyArrayList<E> extends ObjectArrayList<E> {


    public MyArrayList() {
        super();
    }

    public MyArrayList(int initialCapacity) {
        super(initialCapacity);
    }

    public void removeRange(int fromIndex, int toIndex){
        removeElements(fromIndex, toIndex);
    }
}
