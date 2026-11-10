package utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;

@Getter(AccessLevel.PUBLIC)
@Setter(AccessLevel.PUBLIC)
@Builder(toBuilder = true, access = AccessLevel.PUBLIC)
public class OrsConfig {
    // 默认配置方案配置
    @Builder.Default
    private String profileDefaultBuildSourceFile = "/home/ors/openrouteservice/files/heidelberg.test.pbf";
    @Builder.Default
    private String profileDefaultGraphPath = "/home/ors/openrouteservice/graphs";
    @Builder.Default
    private boolean profileDefaultEnabled = false;
    @Builder.Default
    private boolean graphManagementEnabled = false;
    @Builder.Default
    private String graphManagementDownloadSchedule = "0/2 * * * * *";
    @Builder.Default
    private String graphManagementActivationSchedule = "0/5 * * * * *";

    // 高程配置
    @Builder.Default
    private String elevationCachePath = "/home/ors/openrouteservice/elevation_cache";

    // GRC 配置
    public String repositoryUri;
    public String repositoryName;
    public String repositoryProfileGroup;
    public String graphExtent;

    @Builder.Default
    private HashMap<String, Boolean> profiles = new HashMap<>();
    @Builder.Default
    private HashMap<String, HashMap<String, Object>> profileConfigs = new HashMap<>();
    @Builder.Default
    private boolean setRepoManagementPerProfile = false;

    public Path toYAML(Path tempDir, String fileName) {
        Path testConfig = tempDir.resolve(fileName);
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode rootNode = mapper.createObjectNode();

        // 创建 profile_default 节点
        ObjectNode profileDefaultNode = mapper.createObjectNode();
        profileDefaultNode.put("enabled", profileDefaultEnabled);
        if (profileDefaultGraphPath != null) {
            profileDefaultNode.put("graph_path", profileDefaultGraphPath);
        }
        // 创建 profile_default 的 build 节点
        ObjectNode profileDefaultBuildNode = mapper.createObjectNode();
        profileDefaultBuildNode.put("source_file", profileDefaultBuildSourceFile);
        profileDefaultNode.set("build", profileDefaultBuildNode);

        // 创建 repo 节点
        ObjectNode repoNode = mapper.createObjectNode();
        repoNode.put("repository_uri", repositoryUri);
        repoNode.put("repository_name", repositoryName);
        repoNode.put("repository_profile_group", repositoryProfileGroup);
        repoNode.put("graph_extent", graphExtent);

        // 创建 profiles 节点
        ObjectNode profilesNode = mapper.createObjectNode();
        ObjectNode profileNode = mapper.createObjectNode();
        for (String profile : profiles.keySet()) {
            // 将每个配置方案添加到 profiles 节点
            profileNode.put("enabled", profiles.get(profile));
            // 查找该配置方案的配置
            HashMap<String, Object> profileConfig = profileConfigs.get(profile);
            if (profileConfig != null) {
                for (String key : profileConfig.keySet()) {
                    profileNode.put(key, profileConfig.get(key).toString());
                }
            }
            if (setRepoManagementPerProfile) {
                profileNode.set("repo", repoNode);
            }
            profilesNode.set(profile, profileNode);
        }

        // 如果 setGraphManagementPerProfile 为 false，则在 profile_default 中设置图管理
        if (!setRepoManagementPerProfile) {
            profileDefaultNode.set("repo", repoNode);
        }

        // 创建 graph_management 节点
        ObjectNode graphManagementNode = mapper.createObjectNode();
        graphManagementNode.put("enabled", graphManagementEnabled);
        graphManagementNode.put("download_schedule", graphManagementDownloadSchedule);
        graphManagementNode.put("activation_schedule", graphManagementActivationSchedule);

        // 创建 elevation 节点
        ObjectNode elevationNode = mapper.createObjectNode();
        elevationNode.put("cache_path", elevationCachePath);

        // 创建 engine 对象
        ObjectNode engineNode = mapper.createObjectNode();
        engineNode.set("profile_default", profileDefaultNode);
        engineNode.set("graph_management", graphManagementNode);
        engineNode.set("profiles", profilesNode);
        engineNode.set("elevation", elevationNode);


        // 将 engine 对象添加到根节点
        rootNode.set("ors", mapper.createObjectNode().set("engine", engineNode));

        // 将 JsonNode 写入 YAML 文件
        YAMLMapper yamlMapper = new YAMLMapper(new YAMLFactory());
        try {
            yamlMapper.writeValue(testConfig.toFile(), rootNode);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return testConfig;
    }

}