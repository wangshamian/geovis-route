package org.heigit.geovisroute.config.profile;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.heigit.geovisroute.common.EncoderNameEnum;

import java.nio.file.Path;
import java.util.ArrayList;

import static java.util.Optional.ofNullable;

@Getter
@Setter
public class ProfileProperties {
    @JsonIgnore
    private String profileName;
    @JsonIgnore
    private Boolean enabled;
    @JsonIgnore
    private Path graphPath;
    @JsonProperty("encoder_name")
    private EncoderNameEnum encoderName;
    @JsonProperty("build")
    private BuildProperties build = new BuildProperties();
    @JsonIgnore
    private RepoProperties repo = new RepoProperties();
    @JsonIgnore
    private ServiceProperties service = new ServiceProperties();

    @JsonIgnore
    public ProfileProperties mergeDefaults(ProfileProperties profileDefault, String key) {
        if (profileDefault == null) return this;
        // 将配置名设置为 key
        profileName = ofNullable(profileName).orElse(key);
        // 未设置时从 profileDefault 中取值
        enabled = ofNullable(enabled).orElse(profileDefault.enabled);
        graphPath = ofNullable(graphPath).orElse(profileDefault.graphPath);
        // 与 profileDefault 进行深度合并
        service.merge(profileDefault.service);
        build.merge(profileDefault.build);
        return this;
    }

    @JsonIgnore
    public void mergeLoaded(ProfileProperties loadedProfile) {
        if (loadedProfile == null) return;
        // 复制 build 配置
        build = ofNullable(loadedProfile.build).orElse(build);
        if (loadedProfile.getEncoderName().equals(EncoderNameEnum.PUBLIC_TRANSPORT) && build.getGtfsFile() == null) {
            // 若加载的是公共交通配置，则把 GTFS 文件路径设为任意值以启用 GTFS 存储
            build.setGtfsFile(Path.of("gtfs"));
        }
    }

    @JsonIgnore
    public Integer[] getProfilesTypes() {
        ArrayList<Integer> list = new ArrayList<>();
        // TODO 检查为何此处最初会尝试拆分 encoderName。能否支持多个？
        if (encoderName != null && encoderName != EncoderNameEnum.DEFAULT) {
            list.add(encoderName.getValue());
        }
        return list.toArray(new Integer[0]);
    }
}


