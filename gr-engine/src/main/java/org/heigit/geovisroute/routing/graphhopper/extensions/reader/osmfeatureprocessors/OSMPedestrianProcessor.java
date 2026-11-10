package org.heigit.geovisroute.routing.graphhopper.extensions.reader.osmfeatureprocessors;

import com.graphhopper.reader.ReaderWay;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OSMPedestrianProcessor {
    private final List<String> allowed;

    public OSMPedestrianProcessor() {
        allowed = new ArrayList<>();
        allowed.add("yes");
        allowed.addAll(Arrays.asList("yes", "designated", "permissive", "destination"));
    }

    /**
     * 判断该 way 是否为步行化道路，即行人应能徒步通行。
     *
     * @param way
     * @return
     */
    public boolean isPedestrianisedWay(ReaderWay way) {

        boolean isPedestrian = false;

        if (way.hasTag("highway")) {
            String highwayType = way.getTag("highway");
            switch (highwayType) {
                case "footway", "living_street", "pedestrian", "path", "track" -> isPedestrian = true;
                default -> {
                }
            }
        }
        if (way.hasTag("public_transport") && way.getTag("public_transport").equals("platform"))
            isPedestrian = true;

        if (way.hasTag("foot")) {
            String footTag = way.getTag("foot");
            if (allowed.contains(footTag)) {
                isPedestrian = true;
            }
        }

        return isPedestrian;
    }
}
