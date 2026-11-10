package org.heigit.geovisroute.coordinates_generator.generators;

import me.tongfei.progressbar.DelegatingProgressBarConsumer;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.heigit.geovisroute.coordinates_generator.model.Matrix;
import org.heigit.geovisroute.coordinates_generator.model.MatrixRepository;
import org.heigit.geovisroute.coordinates_generator.service.MatrixCalculator;
import org.heigit.geovisroute.coordinates_generator.service.CoordinateSnapper;
import org.heigit.geovisroute.util.CoordinateGeneratorHelper;
import org.heigit.geovisroute.util.ProgressBarLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class CoordinateGeneratorMatrix extends AbstractCoordinateGenerator {
    public record MatrixDimensions(int numRows, int numCols) {
    }

    protected static final Logger LOGGER = LoggerFactory.getLogger(CoordinateGeneratorMatrix.class);

    private static final int DEFAULT_THREAD_COUNT = Runtime.getRuntime().availableProcessors();
    private static final String LOCATION_KEY = "location";

    private final int numMatrices;
    private final Map<String, Double> maxDistanceByProfile;
    private final int numRows;
    private final int numCols;
    private final MatrixRepository matrixRepository;
    private final CoordinateSnapper coordinateSnapper;
    private final MatrixCalculator matrixCalculator;
    private final int numThreads;
    private final CloseableHttpClient httpClient;

    protected CoordinateGeneratorMatrix(int numMatrices, double[] extent, String[] profiles, String baseUrl,
            Map<String, Double> maxDistanceByProfile,
            MatrixDimensions matrixDimensions) {
        this(numMatrices, extent, profiles, baseUrl, maxDistanceByProfile, matrixDimensions, DEFAULT_THREAD_COUNT);
    }

    public CoordinateGeneratorMatrix(int numMatrices, double[] extent, String[] profiles, String baseUrl,
            Map<String, Double> maxDistanceByProfile,
            MatrixDimensions matrixDimensions, int numThreads) {
        super(extent, profiles, baseUrl, "matrix");
        validateInputs(numMatrices, numThreads);

        this.numMatrices = numMatrices;
        this.maxDistanceByProfile = normalizeMaxDistances(profiles, maxDistanceByProfile);
        this.numRows = matrixDimensions.numRows;
        this.numCols = matrixDimensions.numCols;
        this.numThreads = numThreads;
        this.matrixRepository = new MatrixRepository(Set.of(profiles));
        this.httpClient = createHttpClient();

        Function<HttpPost, String> requestExecutor = request -> {
            try {
                return httpClient.execute(request, this::processResponse);
            } catch (IOException e) {
                LOGGER.error("Error executing request: {}", e.getMessage());
                return null;
            }
        };

        Map<String, String> headers = createHeaders();
        this.coordinateSnapper = new CoordinateSnapper(baseUrl, headers, mapper, requestExecutor);
        this.matrixCalculator = new MatrixCalculator(baseUrl, headers, mapper, requestExecutor);
    }

    @Override
    protected CloseableHttpClient createHttpClient() {
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(numThreads * 2); // 每个线程允许 2 个连接
        cm.setDefaultMaxPerRoute(numThreads); // 限制每个主机的连接数
        ConnectionConfig connConfig = ConnectionConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(10))
                .build();
        cm.setDefaultConnectionConfig(connConfig);

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofSeconds(5))
                .setResponseTimeout(Timeout.ofSeconds(30))
                .build();

        return HttpClients.custom()
                .setConnectionManager(cm)
                .setDefaultRequestConfig(requestConfig)
                .build();
    }

    private void validateInputs(int numRoutes, int numThreads) {
        if (numRoutes <= 0)
            throw new IllegalArgumentException("Number of routes must be positive");
        if (numThreads <= 0)
            throw new IllegalArgumentException("Number of threads must be positive");
    }

    private Map<String, Double> normalizeMaxDistances(String[] profiles, Map<String, Double> maxDistanceByProfile) {
        Map<String, Double> normalized = new HashMap<>();
        for (String profile : profiles) {
            normalized.put(profile, maxDistanceByProfile.getOrDefault(profile, Double.MAX_VALUE));
        }
        return normalized;
    }

    public void generateMatrices() {
        generate(DEFAULT_MAX_ATTEMPTS);
    }

    @Override
    public void generate(int maxAttempts) {
        LOGGER.info("Starting route generation with {} threads and max attempts: {}", numThreads, maxAttempts);
        initializeCollections();

        ExecutorService executor = null;
        AtomicBoolean shouldContinue = new AtomicBoolean(true);
        AtomicInteger consecutiveFailedAttempts = new AtomicInteger(0);

        try {
            executor = Executors.newFixedThreadPool(numThreads);
            executeMatrixGeneration(executor, maxAttempts, shouldContinue, consecutiveFailedAttempts);
        } catch (Exception e) {
            LOGGER.error("Error generating routes: ", e);
        } finally {
            shutdownExecutor(executor);
        }
    }

    private void executeMatrixGeneration(ExecutorService executor, int maxAttempts,
            AtomicBoolean shouldContinue, AtomicInteger consecutiveFailedAttempts) {
        try (ProgressBar pb = createProgressBar()) {
            while (!matrixRepository.areAllProfilesComplete(numMatrices) &&
                    consecutiveFailedAttempts.get() < maxAttempts &&
                    shouldContinue.get()) {

                int initialTotalMatrices = matrixRepository.getTotalMatrixCount();
                List<Future<Boolean>> futures = submitTasks(executor, shouldContinue);

                // 等待所有任务完成
                for (Future<Boolean> future : futures) {
                    try {
                        future.get();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        shouldContinue.set(false);
                        LOGGER.warn("Route generation interrupted");
                    } catch (ExecutionException e) {
                        LOGGER.error("Error in worker task: {}", e.getCause().getMessage());
                    }
                }

                updateProgressBar(pb);

                // 检查本次迭代是否取得进展
                if (matrixRepository.getTotalMatrixCount() == initialTotalMatrices) {
                    int attempts = consecutiveFailedAttempts.incrementAndGet();
                    pb.setExtraMessage(String.format("Attempt %d/%d - No new routes", attempts, maxAttempts));
                } else {
                    consecutiveFailedAttempts.set(0);
                }
            }

            finalizeProgress(pb, maxAttempts, consecutiveFailedAttempts.get());
        }
    }

    private ProgressBar createProgressBar() {
        return new ProgressBarBuilder()
                .setStyle(ProgressBarStyle.COLORFUL_UNICODE_BAR)
                .setUpdateIntervalMillis(2000)
                .setTaskName("Generating routes")
                .setInitialMax((long) numMatrices * profiles.length)
                .setConsumer(new DelegatingProgressBarConsumer(ProgressBarLogger.getLogger()::info))
                .build();
    }

    private List<Future<Boolean>> submitTasks(ExecutorService executor, AtomicBoolean shouldContinue) {
        List<Future<Boolean>> futures = new ArrayList<>();

        for (String profile : profiles) {
            if (!matrixRepository.isProfileComplete(profile, numMatrices)) {
                int tasksPerProfile = Math.max(1, numThreads / profiles.length);
                for (int i = 0; i < tasksPerProfile && shouldContinue.get(); i++) {
                    futures.add(executor.submit(new MatrixGenerationTask(profile, shouldContinue)));
                }
            }
        }

        return futures;
    }

    private void updateProgressBar(ProgressBar pb) {
        int totalRoutes = matrixRepository.getTotalMatrixCount();
        pb.stepTo(totalRoutes);
        pb.setExtraMessage(matrixRepository.getProgressMessage());
    }

    private void finalizeProgress(ProgressBar pb, int maxAttempts, int attempts) {
        pb.stepTo(matrixRepository.getTotalMatrixCount());
        if (attempts >= maxAttempts) {
            LOGGER.warn("Stopped route generation after {} attempts. Routes per profile: {}",
                    maxAttempts, matrixRepository.getProgressMessage());
        }
    }

    private void shutdownExecutor(ExecutorService executor) {
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                executor.shutdownNow();
            }
        }

        // 关闭 HTTP 客户端
        try {
            httpClient.close();
        } catch (IOException e) {
            LOGGER.error("Error closing HTTP client", e);
        }
    }

    /**
     * 我们希望生成的矩阵是完全连通的，
     * 即所有 (X/Y) 对都能正向和反向到达。
     * 示例表格如下：
     *
     * <table border="1" cellpadding="4">
     *   <tr>
     *     <th></th><th>A</th><th>B</th><th>C</th><th>D</th>
     *   </tr>
     *   <tr>
     *     <th>1</th>
     *     <td>1-&gt;A<br/>A-&gt;1</td>
     *     <td>1-&gt;B<br/>B-&gt;1</td>
     *     <td>1-&gt;C<br/>C-&gt;1</td>
     *     <td>1-&gt;D<br/>D-&gt;1</td>
     *   </tr>
     *   <tr>
     *     <th>2</th>
     *     <td>2-&gt;A<br/>A-&gt;2</td>
     *     <td>2-&gt;B<br/>B-&gt;2</td>
     *     <td>2-&gt;C<br/>C-&gt;2</td>
     *     <td>2-&gt;D<br/>D-&gt;2</td>
     *   </tr>
     *   <tr>
     *     <th>3</th>
     *     <td>3-&gt;A<br/>A-&gt;3</td>
     *     <td>3-&gt;B<br/>B-&gt;3</td>
     *     <td>3-&gt;C<br/>C-&gt;3</td>
     *     <td>3-&gt;D<br/>D-&gt;3</td>
     *   </tr>
     *   <tr>
     *     <th>4</th>
     *     <td>4-&gt;A<br/>A-&gt;4</td>
     *     <td>4-&gt;B<br/>B-&gt;4</td>
     *     <td>4-&gt;C<br/>C-&gt;4</td>
     *     <td>4-&gt;D<br/>D-&gt;4</td>
     *   </tr>
     * </table>
     *
     * 在暴力方法中，这意味着 O(2 * X * Y) 次路径规划检查。
     * 我们改用链式方法，可将复杂度降低到 O(X + Y)：
     * 若所有列点都可到达 (A-&gt;B-&gt;C-&gt;D)
     * 且所有行点可反向到达 (4-&gt;3-&gt;2-&gt;1)
     * 并找到一条连接 (1-&gt;A)，
     * 则根据传递性，所有组合都被覆盖。
     * 对于反向情况，我们只需将最后一列连接到最后一个行项。
     */
    private class MatrixGenerationTask implements Callable<Boolean> {
        private final String profile;
        private final AtomicBoolean shouldContinue;

        public MatrixGenerationTask(String profile, AtomicBoolean shouldContinue) {
            this.profile = profile;
            this.shouldContinue = shouldContinue;
        }

        @Override
        public Boolean call() {
            if (!shouldContinue.get() || matrixRepository.isProfileComplete(profile, numMatrices)) {
                return false;
            }

            try {
                return generateMatricesForProfile();
            } catch (Exception e) {
                LOGGER.error("Error generating routes for profile {}: {}", profile, e.getMessage());
                return false;
            }
        }

        /**
         * 生成若干矩阵的主方法
         * 
         * @return 是否成功将矩阵添加到存储库
         */
        private boolean generateMatricesForProfile() {
            // 获取该配置方案的最大距离
            double effectiveMaxDistance = maxDistanceByProfile.getOrDefault(profile, Double.MAX_VALUE);

            // 这里有两道统计关卡
            // 1. 需要找到 numRows + numCols 个可吸附的坐标
            // 2. 需要确保这些坐标满足上述链式
            // 关系的可达性
            // 我们在单个任务内解决问题 1，将问题 2 留待
            // 任务的重复执行来解决

            int targetNum = numCols + numRows;
            int maxRetries = 20 * targetNum; // 这是随机的上限；可以改进
            int currRetry = 0;
            List<double[]> snappedCoordinates = new ArrayList<>();

            while (snappedCoordinates.size() < targetNum && currRetry < maxRetries) {
                List<double[]> randomCoordinate = CoordinateGeneratorHelper.randomCoordinatesInExtent(1,
                        extent);

                // 将坐标吸附到路网
                List<double[]> snappedCoordinate = coordinateSnapper.snapCoordinates(randomCoordinate, profile);
                snappedCoordinates.addAll(snappedCoordinate);
                currRetry++;
            }

            if (snappedCoordinates.size() < targetNum) {
                // 未能找到足够多的吸附坐标
                return false;
            }

            return processSnappedCoordinates(snappedCoordinates, effectiveMaxDistance);
        }

        private boolean processSnappedCoordinates(List<double[]> snappedCoordinates, double maxDistance) {
            boolean addedNewMatrix = false;
            try {
                addedNewMatrix = processCoordinatePairs(snappedCoordinates, maxDistance);
            } catch (Exception e) {
                LOGGER.error("Error processing snapped coordinates: {}", e.getMessage());
            }
            return addedNewMatrix;
        }

        /**
         * 通过校验距离和可达性约束来处理一组吸附后的坐标，
         * 并可选地计算和存储得到的距离矩阵。
         * <p>
         * 该方法检查：
         * <ul>
         * <li>行坐标与列坐标之间允许的最大距离</li>
         * <li>行元素之间、列元素之间的正向可达性</li>
         * <li>行与列之间的连接，以形成环形可达性</li>
         * <li>在满足所有条件时计算非对称矩阵</li>
         * </ul>
         *
         * @param snappedCoordinates 坐标列表，其中前
         *                           {@code numRows} 个条目视为行，
         *                           接下来的 {@code numCols} 个视为列
         * @param maxDistance        每个行列坐标对之间要求的最小距离
         * @return 若矩阵成功计算并处理则返回 {@code true}；
         *         否则返回 {@code false}
         */
        private boolean processCoordinatePairs(List<double[]> snappedCoordinates, double maxDistance) {
            List<double[]> row = snappedCoordinates.subList(0, numRows);
            List<double[]> col = snappedCoordinates.subList(numRows, numRows + numCols);

            if (!hasNearbyPoints(row, col, maxDistance))
                return false;
            if (!rowsColsRouteable(row, col))
                return false;
            if (!isRouteable(row.get(0), col.get(0)))
                return false;
            if (!isRouteable(col.get(numCols - 1), row.get(numRows - 1)))
                return false;

            return computeAndProcessMatrix(snappedCoordinates);
        }

        /**
         * 检查行与列之间是否存在任何坐标对处于指定的最大距离之内。
         *
         * @param row         行坐标列表
         * @param col         列坐标列表
         * @param maxDistance 允许的最大接近距离（米）
         * @return 若存在过于接近的行列坐标对则返回 {@code true}；
         *         否则返回 {@code false}
         */
        private boolean hasNearbyPoints(List<double[]> row, List<double[]> col, double maxDistance) {
            for (double[] rowCoord : row) {
                for (double[] colCoord : col) {
                    double distance = CoordinateGeneratorHelper.calculateHaversineDistance(rowCoord, colCoord);
                    if (distance <= maxDistance)
                        return true;
                }
            }
            return false;
        }

        /**
         * 校验行元素与列元素之间的正向可达性：
         * <ul>
         * <li>沿行坐标反向遍历（即从最后一个到第一个）</li>
         * <li>沿列坐标正向遍历</li>
         * </ul>
         *
         * @param row 行坐标列表
         * @param col 列坐标列表
         * @return 若正向方向的所有可达性条件均满足则返回 {@code true}
         */
        private boolean rowsColsRouteable(List<double[]> row, List<double[]> col) {
            for (int i = numRows - 1; i > 0; i--) {
                if (!isRouteable(row.get(i), row.get(i - 1)))
                    return false;
            }

            for (int i = 0; i < numCols - 1; i++) {
                if (!isRouteable(col.get(i), col.get(i + 1)))
                    return false;
            }

            return true;
        }

        /**
         * 使用提供的坐标计算非对称距离矩阵，
         * 若结果可用则进行处理。
         *
         * @param snappedCoordinates 行坐标与列坐标的合并列表
         * @return 若矩阵成功计算并传递给 {@code processMatrixResult} 则返回 {@code true}；否则返回 {@code false}
         */
        private boolean computeAndProcessMatrix(List<double[]> snappedCoordinates) {
            int[] sources = java.util.stream.IntStream.range(0, numRows).toArray();
            int[] destinations = java.util.stream.IntStream.range(0, numCols).toArray();

            Optional<MatrixCalculator.MatrixResult> fullMatrixResult = matrixCalculator.calculateAsymmetricMatrix(
                    snappedCoordinates, sources, destinations, profile);

            return fullMatrixResult.map(this::processMatrixResult).orElse(false);
        }

        /**
         * 检查两点之间是否正向连通
         * 
         * @param from 起始坐标对
         * @param to   目标坐标对
         * @return 若矩阵成功计算且两点可达则返回 true，
         *         否则返回 false
         */
        private boolean isRouteable(double[] from, double[] to) {
            Optional<MatrixCalculator.MatrixResult> matrixResultOpt = matrixCalculator.calculateMatrix(
                    List.of(from, to), profile);
            if (matrixResultOpt.isEmpty()) {
                return false;
            }
            MatrixCalculator.MatrixResult result = matrixResultOpt.get();
            return result.getSources().get(0) != null && result.getDestinations().get(0) != null;
        }

        /**
         * 处理 MatrixResult，将起点和终点位置转换为所需的矩阵格式，
         * 并尝试将其添加到矩阵存储库。
         *
         * <p>
         * 它将起点和终点坐标展平为单个坐标数组，
         * 为起点和终点生成索引数组，将距离数据转换为 double[][]，
         * 并构造 Matrix 对象。若矩阵唯一，则将其添加到
         * 存储库。
         * </p>
         *
         * @param result 矩阵计算的结果
         * @return 若矩阵成功添加（即唯一）则返回 true，否则返回 false
         */
        private boolean processMatrixResult(MatrixCalculator.MatrixResult result) {
            List<Map<String, Object>> rawSources = result.getSources();
            List<Map<String, Object>> rawDestinations = result.getDestinations();
            List<List<Double>> rawDistances = result.getDistances();

            int sourceCount = rawSources.size();
            int destCount = rawDestinations.size();
            int totalCount = sourceCount + destCount;

            double[][] coordinates = new double[totalCount][2];
            int[] sourceIndices = new int[sourceCount];
            int[] destinationIndices = new int[destCount];

            // 填充起点坐标和索引
            for (int i = 0; i < sourceCount; i++) {
                List<Double> loc = (List<Double>) rawSources.get(i).get(LOCATION_KEY);
                coordinates[i][0] = loc.get(0);
                coordinates[i][1] = loc.get(1);
                sourceIndices[i] = i;
            }

            // 填充终点坐标和索引
            for (int i = 0; i < destCount; i++) {
                List<Double> loc = (List<Double>) rawDestinations.get(i).get(LOCATION_KEY);
                int index = sourceCount + i;
                coordinates[index][0] = loc.get(0);
                coordinates[index][1] = loc.get(1);
                destinationIndices[i] = index;
            }

            // 将距离列表转换为二维数组
            double[][] distances = rawDistances.stream()
                    .map(row -> row.stream().mapToDouble(Double::doubleValue).toArray())
                    .toArray(double[][]::new);

            try {
                Matrix matrix = new Matrix(coordinates, sourceIndices, destinationIndices, distances, profile);
                boolean added = matrixRepository.addMatrixIfUnique(matrix);

                if (added) {
                    LOGGER.debug("Added new unique route for profile: {}", profile);
                } else {
                    LOGGER.debug("Skipped duplicate route for profile: {}", profile);
                }

                return added;

            } catch (Exception e) {
                LOGGER.error("Error adding route: {}", e.getMessage(), e);
                return false;
            }
        }
    }

    @Override
    protected void initializeCollections() {
        matrixRepository.clear();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Matrix> getResult() {
        return matrixRepository.getAllMatrices(numMatrices);
    }

    @Override
    public void writeToCSV(String filePath) throws FileNotFoundException {
        matrixRepository.writeToCSV(filePath, numMatrices);
    }

    public int getNumMatrices() {
        return numMatrices;
    }
}
