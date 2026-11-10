package org.heigit.geovisroute.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class NameUtilsTest {

    @ParameterizedTest(name = "{index}: {0} -> {1}")
    @MethodSource("fileNameTestCases")
    @DisplayName("Test getFileNameWithoutExtension with various inputs")
    void testGetFileNameWithoutExtension(String filePath, String expected) {
        String actual = NameUtils.getFileNameWithoutExtension(filePath);
        assertEquals(expected, actual);
    }

    @SuppressWarnings("unused")
    private static Stream<Arguments> fileNameTestCases() {
        return Stream.of(
            // 普通文件路径
            arguments("/path/to/file.txt", "file"),
            // 文件名中包含多个点
            arguments("/path/to/file.name.txt", "file.name"),
            // 无扩展名
            arguments("/path/to/file", "file"),
            // 以点结尾
            arguments("/path/to/file.", "file"),
            // 仅文件名带扩展名
            arguments("file.txt", "file"),
            // 仅文件名不带扩展名
            arguments("file", "file"),
            // 空字符串
            arguments("", ""),
            // null
            arguments(null, null)
        );
    }
}
