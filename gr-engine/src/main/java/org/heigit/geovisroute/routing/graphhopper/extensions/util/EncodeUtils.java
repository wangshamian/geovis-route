/*  This file is part of Openrouteservice.
 *
 *  Openrouteservice is free software; you can redistribute it and/or modify it under the terms of the
 *  GNU Lesser General Public License as published by the Free Software Foundation; either version 2.1
 *  of the License, or (at your option) any later version.

 *  This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Lesser General Public License for more details.

 *  You should have received a copy of the GNU Lesser General Public License along with this library;
 *  if not, see <https://www.gnu.org/licenses/>.
 */
package org.heigit.geovisroute.routing.graphhopper.extensions.util;

import java.nio.Buffer;
import java.nio.ByteBuffer;

public class EncodeUtils {
    private EncodeUtils() {
    }

    /**
     * 将一个 long 值转换为长度为 8 的字节数组。
     *
     * @param longValue
     * @return 该 long 数值的 8 字节数组表示
     */
    public static byte[] longToByteArray(long longValue) {
        ByteBuffer longToByteBuffer = ByteBuffer.allocate(Long.BYTES);
        longToByteBuffer.putLong(longValue);
        return longToByteBuffer.array();
    }

    /**
     * 将字节数组转换为 long 值表示
     *
     * @param byteArray
     * @return 这些字节对应的 long 数值表示
     */
    public static long byteArrayToLong(byte[] byteArray) {
        ByteBuffer byteToLongBuffer = ByteBuffer.allocate(Long.BYTES);
        // 需要补齐到所需的 8 个字节
        byte[] storageBytes = {0, 0, 0, 0, 0, 0, 0, 0};
        int differenceInSize = storageBytes.length - byteArray.length;

        for (int i = byteArray.length - 1; i >= 0; i--) {
            if (differenceInSize + i >= 0)
                storageBytes[differenceInSize + i] = byteArray[i];
        }

        byteToLongBuffer.put(storageBytes);
        ((Buffer) byteToLongBuffer).flip(); // Java 9 中的变更使得必须强制转换为 Buffer
        return byteToLongBuffer.getLong();
    }
}
