package org.heigit.geovisroute.routing.graphhopper.extensions.parsers;

/**
 * 将所有出现的 EncodedValueOld 替换为常规的 EncodedValue
 */
public class EncodedValueOld {
    protected final long shift;
    protected final long mask;
    protected final double factor;
    protected final long defaultValue;
    private final String name;
    private final long maxValue;
    private final boolean allowZero;
    private final int bits;

    /**
     * 定义一个按位编码的值
     * <p>
     *
     * @param name         用于调试的描述
     * @param shift        该值的位索引
     * @param bits         保留的位数
     * @param factor       存储值的缩放因子
     * @param defaultValue 默认值
     * @param maxValue     默认最大值
     */
    public EncodedValueOld(String name, int shift, int bits, double factor, long defaultValue, int maxValue) {
        this(name, shift, bits, factor, defaultValue, maxValue, true);
    }

    public EncodedValueOld(String name, int shift, int bits, double factor, long defaultValue, int maxValue, boolean allowZero) {
        this.name = name;
        this.shift = shift;
        this.factor = factor;
        this.defaultValue = defaultValue;
        this.bits = bits;
        long tmpMask = (1L << bits) - 1;
        this.maxValue = Math.min(maxValue, Math.round(tmpMask * factor));
        if (maxValue > this.maxValue)
            throw new IllegalStateException(name + " -> maxValue " + maxValue + " is too large for " + bits + " bits");

        double factorDivision = maxValue / factor;
        if (factorDivision != (int) factorDivision) {
            throw new IllegalStateException("MaxValue needs to be divisible by factor without remainder");
        }

        mask = tmpMask << shift;
        this.allowZero = allowZero;
    }

    protected void checkValue(long value) {
        if (value > maxValue)
            throw new IllegalArgumentException(name + " value too large for encoding: " + value + ", maxValue:" + maxValue);
        if (value < 0)
            throw new IllegalArgumentException("negative " + name + " value not allowed! " + value);
        if (!allowZero && value == 0)
            throw new IllegalArgumentException("zero " + name + " value not allowed! " + value);
    }

    public long setValue(long flags, long value) {
        // 缩放值
        value = Math.round(value / factor);
        checkValue((long) (value * factor));
        value <<= shift;

        // 清除值对应的位
        flags &= ~mask;

        // 设置值
        return flags | value;
    }

    public String getName() {
        return name;
    }

    public long getValue(long flags) {
        // 查找值
        flags &= mask;
        flags >>>= shift;
        return Math.round(flags * factor);
    }

    public int getBits() {
        return bits;
    }

    public double getFactor() {
        return factor;
    }

    public long setDefaultValue(long flags) {
        return setValue(flags, defaultValue);
    }
}
