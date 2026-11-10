package org.heigit.geovisroute.exceptions;

import org.heigit.geovisroute.common.StatusCode;
import org.heigit.geovisroute.routing.RoutingErrorCodes;

public class PointNotFoundException extends StatusCodeException {

    public PointNotFoundException(String message) {
        super(StatusCode.NOT_FOUND, RoutingErrorCodes.POINT_NOT_FOUND, message);
    }

    public PointNotFoundException(String message, int errorCode) {
        super(StatusCode.NOT_FOUND, errorCode, message);
    }
}
