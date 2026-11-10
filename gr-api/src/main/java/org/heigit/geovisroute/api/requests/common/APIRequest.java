package org.heigit.geovisroute.api.requests.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.heigit.geovisroute.api.APIEnums;

public class APIRequest {
    public static final String PARAM_ID = "id";
    public static final String PARAM_PROFILE = "profile";
    public static final String PARAM_PROFILE_NAME = "profile_name";

    @Schema(name = PARAM_ID, description = "请求的任意标识字符串，会反映在 meta 信息中。",
            example = "my_request")
    @JsonProperty(PARAM_ID)
    protected String id;
    @JsonIgnore
    private boolean hasId = false;

    @Schema(name = PARAM_PROFILE, hidden = true)
    protected APIEnums.Profile profile;

    @Schema(name = PARAM_PROFILE_NAME, hidden = true)
    protected String profileName;

    public boolean hasId() {
        return hasId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
        this.hasId = true;
    }

    public APIEnums.Profile getProfile() {
        return profile;
    }

    public void setProfile(APIEnums.Profile profile) {
        this.profile = profile;
    }

    public String getProfileName() {
        return profileName;
    }

    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }
}
