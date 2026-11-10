package org.heigit.geovisroute.fastisochrones.partitioning;

import com.carrotsearch.hppc.IntArrayList;

import java.util.*;
import java.util.Map.Entry;

/**
 * 排序辅助类。
 * <p>
 *
 * @author Hendrik Leuschner
 */
public class Sort {
    //按值对 ID 列表排序
    public IntArrayList sortByValueReturnList(Integer[] ids, Double[] values) {
        ArrayIndexComparator comparator = new ArrayIndexComparator(values);
        Integer[] indices = comparator.createIndexArray();
        //并行排序第一个单元数组
        Arrays.sort(indices, comparator);

        IntArrayList result = new IntArrayList(indices.length);
        for (int entry : indices) {
            result.add(ids[entry]);
        }

        return result;
    }

    public <K, V extends Comparable<? super V>> List<K> sortByValueReturnList(Map<K, V> map, boolean ascending) {
        List<Entry<K, V>> list = new ArrayList<>(map.entrySet());
        list.sort(Entry.comparingByValue());
        if (!ascending) {
            Collections.reverse(list);
        }
        List<K> result = new ArrayList<>(list.size());
        for (Entry<K, V> entry : list) {
            result.add(entry.getKey());
        }

        return result;
    }

    private static class ArrayIndexComparator implements Comparator<Integer> {
        private final Double[] array;

        public ArrayIndexComparator(Double[] array) {
            this.array = array;
        }

        public Integer[] createIndexArray() {
            Integer[] indexes = new Integer[array.length];
            for (int i = 0; i < array.length; i++) {
                indexes[i] = i; // 自动装箱
            }
            return indexes;
        }

        @Override
        public int compare(Integer index1, Integer index2) {
            // 从 Integer 自动拆箱为 int，以用作数组索引
            return array[index1].compareTo(array[index2]);
        }
    }
}
