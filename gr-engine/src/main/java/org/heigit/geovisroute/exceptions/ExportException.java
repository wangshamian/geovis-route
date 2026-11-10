package org.heigit.geovisroute.exceptions;


import org.heigit.geovisroute.common.ServiceRequest;
import org.heigit.geovisroute.common.StatusCode;

/**
 * 当调用了无效的 service/export 组合时，该类负责错误上报。
 *
 * @author Julian Psotta, julian@openrouteservice.org
 */
public class ExportException extends StatusCodeException {
    private static final long serialVersionUID = 59778833258948573L;

    /**
     * @param errorCode         表示 "error_codes.md" 中描述的 error code
     * @param toBeExportedClass 表示具体的 {@link ServiceRequest} 类，例如 geocodingRequest.getClass()
     * @param exportMethod      表示导出格式，类型为 {@link String}
     */
    public ExportException(int errorCode, Class rootClass, Class toBeExportedClass, String exportMethod) {
        super(StatusCode.NOT_IMPLEMENTED, errorCode, toBeExportedClass.getName() + " can not (yet) be exported through " + rootClass + " using export method " + exportMethod);
    }

    /**
     * @param errorCode         表示 "error_codes.md" 中描述的 error code
     * @param toBeExportedClass 表示具体的 {@link ServiceRequest} 类，例如 geocodingRequest.getClass()
     * @param exportMethod      表示导出格式，类型为 {@link String}，例如 "GeoJSON" 或 "JSON"
     */
    public ExportException(int errorCode, Class toBeExportedClass, String exportMethod) {
        super(StatusCode.INTERNAL_SERVER_ERROR, errorCode, "An error accured exporting " + toBeExportedClass.getName() + " using " + exportMethod);
    }
}
