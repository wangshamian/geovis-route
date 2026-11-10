package org.heigit.geovisroute.export;

public class ExportWarning {
    public static final int EMPTY_BBOX = 1;

    private int warningCode = 0;
    private String warningMessage = "";

    /**
     * 生成警告对象，并根据传入的警告码初始化消息
     *
     * @param warning 需要生成的警告对应的警告码
     */
    public ExportWarning(int warning) {
        warningCode = warning;
        if (warning == EMPTY_BBOX) {
            warningMessage = "The specified bbox doesn't contain any nodes.";
        } else {
            warningMessage = "Unknown error";
        }
    }

    public int getWarningCode() {
        return warningCode;
    }

    public String getWarningMessage() {
        return warningMessage;
    }
}
