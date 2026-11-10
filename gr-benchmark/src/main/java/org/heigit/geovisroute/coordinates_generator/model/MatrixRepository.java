package org.heigit.geovisroute.coordinates_generator.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MatrixRepository {
    private final Map<String, List<Matrix>> matricesByProfile;
    private final Map<String, Set<Matrix>> uniqueMatricesByProfile;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    protected static final Logger LOGGER = LoggerFactory.getLogger(MatrixRepository.class);

    public MatrixRepository(Set<String> profiles) {
        matricesByProfile = new HashMap<>();
        uniqueMatricesByProfile = new HashMap<>();
        
        for (String profile : profiles) {
            matricesByProfile.put(profile, new ArrayList<>());
            uniqueMatricesByProfile.put(profile, new HashSet<>());
        }
    }
    
    public boolean addMatrix(Matrix matrix) {
        String profile = matrix.getProfile();
        
        lock.writeLock().lock();
        try {
            if (uniqueMatricesByProfile.get(profile).add(matrix)) {
                matricesByProfile.get(profile).add(matrix);
                return true;
            }
            return false;
        } catch (Exception e) {
            LOGGER.error("Error adding matrix: {}", e.getMessage());
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public boolean addMatrixIfUnique(Matrix matrix) {
        return addMatrix(matrix);
    }
    
    public void clear() {
        lock.writeLock().lock();
        try {
            matricesByProfile.values().forEach(List::clear);
            uniqueMatricesByProfile.values().forEach(Set::clear);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public int getMatrixCount(String profile) {
        lock.readLock().lock();
        try {
            return uniqueMatricesByProfile.get(profile).size();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public int getTotalMatrixCount() {
        lock.readLock().lock();
        try {
            return uniqueMatricesByProfile.values().stream()
                    .mapToInt(Set::size)
                    .sum();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public boolean isProfileComplete(String profile, int requiredMatrices) {
        lock.readLock().lock();
        try {
            return uniqueMatricesByProfile.get(profile).size() >= requiredMatrices;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public boolean areAllProfilesComplete(int requiredMatrices) {
        lock.readLock().lock();
        try {
            return uniqueMatricesByProfile.values().stream()
                    .allMatch(matrices -> matrices.size() >= requiredMatrices);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public List<Matrix> getAllMatrices(int limit) {
        lock.readLock().lock();
        try {
            List<Matrix> allMatrices = new ArrayList<>();
            matricesByProfile.values().forEach(matrices ->
                    matrices.stream().limit(limit).forEach(allMatrices::add));
            return allMatrices;
        } finally {
            lock.readLock().unlock();
        }
    }
    /**
     * 将所有按配置方案分组的矩阵对象写入 CSV 文件。
     * 每个矩阵序列化为一行，所有数组字段
     * 转换为字符串表示。该方法包含数据集中的
     * 所有矩阵。
     *
     * CSV 文件将包含以下表头：
     * coordinates,sources,destinations,distances,profile
     *
     * 生成行的示例：
     * "[[8.667542, 49.440851], [8.668100, 49.440900], ....]","[0, 1, 2, 3]","[4, 5]","[[10.5, 20.7, ...], [15.2, 25.4, ...], ...]", "driving-car"
     *
     * 每个字段都用引号包裹，以便安全地包含数组内的逗号。
     * - coordinates：经纬度对的二维数组
     * - sources：起点索引的一维数组
     * - destinations：终点索引的一维数组
     * - distances：距离（米）的二维数组，每个起点对应一个子数组
     * - profile：路径规划配置方案（例如 "driving"、"walking"）
     *
     * @param filePath 要写入 CSV 输出的文件路径
     * @throws FileNotFoundException 如果文件无法创建或打开
     */
    public void writeToCSV(String filePath) throws FileNotFoundException {
        lock.readLock().lock();
        try (PrintWriter pw = new PrintWriter(filePath)) {
            // CSV 表头
            pw.println("coordinates,sources,destinations,distances,profile");

            matricesByProfile.forEach((profile, matrices) ->
                    matrices.forEach(matrix -> {
                        String coordinatesStr = Arrays.deepToString(matrix.getCoordinates());
                        String sourcesStr = Arrays.toString(matrix.getSources());
                        String destinationsStr = Arrays.toString(matrix.getDestinations());
                        String distancesStr = Arrays.deepToString(matrix.getDistances());
                        String profileStr = matrix.getProfile();

                        // 写入用引号包裹的 CSV 行，以处理逗号
                        pw.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n",
                                coordinatesStr,
                                sourcesStr,
                                destinationsStr,
                                distancesStr,
                                profileStr
                        );
                    })
            );
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 将所有按配置方案分组的矩阵对象写入 CSV 文件。
     * 每个矩阵序列化为一行，所有数组字段
     * 转换为字符串表示。该方法限制写入元素的最大数量。
     *
     * CSV 文件将包含以下表头：
     * coordinates,sources,destinations,distances,profile
     *
     * 生成行的示例：
     * "[[8.667542, 49.440851], [8.668100, 49.440900], ....]","[0, 1, 2, 3]","[4, 5]","[[10.5, 20.7, ...], [15.2, 25.4, ...], ...]", "driving-car"
     *
     * 每个字段都用引号包裹，以便安全地包含数组内的逗号。
     * - coordinates：经纬度对的二维数组
     * - sources：起点索引的一维数组
     * - destinations：终点索引的一维数组
     * - distances：距离（米）的二维数组，每个起点对应一个子数组
     * - profile：路径规划配置方案（例如 "driving"、"walking"）
     *
     * @param filePath 要写入 CSV 输出的文件路径
     * @throws FileNotFoundException 如果文件无法创建或打开
     */
    public void writeToCSV(String filePath, int limit) throws FileNotFoundException {
        lock.readLock().lock();
        try (PrintWriter pw = new PrintWriter(filePath)) {
            // CSV 表头
            pw.println("coordinates,sources,destinations,distances,profile");

            matricesByProfile.forEach((profile, matrices) ->
                    matrices.stream().limit(limit).forEach(matrix -> {
                        String coordinatesStr = Arrays.deepToString(matrix.getCoordinates());
                        String sourcesStr = Arrays.toString(matrix.getSources());
                        String destinationsStr = Arrays.toString(matrix.getDestinations());
                        String distancesStr = Arrays.deepToString(matrix.getDistances());
                        String profileStr = matrix.getProfile();

                        pw.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n",
                                coordinatesStr,
                                sourcesStr,
                                destinationsStr,
                                distancesStr,
                                profileStr
                        );
                    })
            );
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public String getProgressMessage() {
        lock.readLock().lock();
        try {
            return uniqueMatricesByProfile.entrySet().stream()
                .map(e -> String.format("%s: %d", e.getKey(), e.getValue().size()))
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
        } finally {
            lock.readLock().unlock();
        }
    }
}
