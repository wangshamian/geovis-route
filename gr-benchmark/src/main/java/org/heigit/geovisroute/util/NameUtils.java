package org.heigit.geovisroute.util;

import java.io.File;

public class NameUtils {
    private NameUtils() {
        // 隐藏隐式构造函数
    }

    public static String getFileNameWithoutExtension(String filePath) {
        if (filePath == null) {
            return null;
        }
        String fileName = new File(filePath).getName();
        int lastDotIndex = fileName.lastIndexOf('.');
        return lastDotIndex > 0 ? fileName.substring(0, lastDotIndex) : fileName;
    }
}
