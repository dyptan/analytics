package com.dyptan.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class SizeEstimator {
    public static int countBytesOf(Object obj) {
        ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = null;
        try {
            objectOutputStream = new ObjectOutputStream(byteOutputStream);
            if (obj != null) { // Add null check here
                objectOutputStream.writeObject(obj);
                objectOutputStream.flush();
            }
            objectOutputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return byteOutputStream.toByteArray().length;
    }
}
