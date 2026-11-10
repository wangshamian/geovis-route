package integrationtests;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.junit.jupiter.TestcontainersExtension;

import java.io.IOException;

import static utils.ContainerInitializer.buildSharedLayersAndGraphs;

/**
 * 此测试函数是一个辅助函数，用于为测试场景构建所需的容器镜像。
 * 它应在 CI 中使用。
 */
@ExtendWith(TestcontainersExtension.class)
@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
@EnabledIfEnvironmentVariable(named = "ONE_SHOT_GRAPH_BUILDER", matches = "true")
class OneShotGraphBuilderTest {

    @Test
    void oneShotGraphBuilder() throws IOException, InterruptedException {
        buildSharedLayersAndGraphs();
        Assertions.assertTrue(true);
    }
}
