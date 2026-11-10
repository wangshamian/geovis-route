package org.heigit.geovisroute.config.profile;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RepoProperties {
    private String repositoryUri;
    private String repositoryName;
    private String repositoryProfileGroup;
    private String graphExtent;
    private String repositoryUser;
    private String repositoryPassword;

    public RepoProperties() {
    }

    public RepoProperties(String ignored) {
        // 该构造函数用于创建一个空对象，以便在 JSON 序列化时忽略它。
    }

    public boolean isEmpty() {
        return repositoryUri == null || repositoryName == null || repositoryProfileGroup == null || graphExtent == null;
    }
}
