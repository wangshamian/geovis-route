/*  This file is part of Openrouteservice.
 *
 *  Openrouteservice is free software; you can redistribute it and/or modify it under the terms of the
 *  GNU Lesser General Public License as published by the Free Software Foundation; either version 2.1
 *  of the License, or (at your option) any later version.

 *  This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Lesser General Public License for more details.

 *  You should have received a copy of the GNU Lesser General Public License along with this library;
 *  if not, see <https://www.gnu.org/licenses/>.
 */
package org.heigit.geovisroute.routing.graphhopper.extensions;

import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.storage.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class GRGraph extends BaseGraph {
    private static final Logger LOGGER = LoggerFactory.getLogger(GRGraph.class);
    private final Collection<CHEntry> coreEntries;
    private final Collection<CHEntry> chEntries;

    public GRGraph(Directory dir, boolean withElevation, boolean withTurnCosts, int segmentSize, EncodingManager encodingManager) {
        super(dir, withElevation, withTurnCosts, segmentSize, encodingManager.getBytesForFlags());
        coreEntries = new ArrayList<>();
        chEntries = new ArrayList<>();
        // ORS-GH MOD START - 额外的存储
//        if (encodingManager.hasConditionalAccess()) {
//            this.conditionalAccess = new ConditionalEdges(encodingManager, ConditionalEdges.ACCESS, dir);
//        }
//
//        if (encodingManager.hasConditionalSpeed()) {
//            this.conditionalSpeed = new ConditionalEdges(encodingManager, ConditionalEdges.SPEED, dir);
//        }
    }

    /**
     * 为给定的 {@link CHConfig} 添加一个 {@link CHStorage}。必须在调用 {@link #create(long)}
     * 或 {@link #loadExisting()} 之前调用此方法。
     */
    public GRGraph addCoreGraph(CHConfig chConfig) {
        if (getCoreConfigs().contains(chConfig))
            throw new IllegalArgumentException("For the given CH profile a CHStorage already exists: '" + chConfig.getName() + "'");
        coreEntries.add(createCHEntry(chConfig));
        return this;
    }

    /**
     * @see #addCHGraph(CHConfig)
     */
    public GRGraph addCoreGraphs(List<CHConfig> chConfigs) {
        for (CHConfig chConfig : chConfigs) {
            addCoreGraph(chConfig);
        }
        return this;
    }

    /**
     * @return 指定 profile 名称对应的 {@link CHStorage}，若不存在则返回 null
     */
    public CHStorage getCoreStore(String chName) {
        CHEntry chEntry = getCoreEntry(chName);
        return chEntry == null ? null : chEntry.chStore;
    }

    /**
     * @return 指定 profile 名称对应的 {@link RoutingCHGraph}，若不存在则返回 null
     */
    public RoutingCHGraph getCoreGraph(String chName) {
        CHEntry chEntry = getCoreEntry(chName);
        return chEntry == null ? null : chEntry.chGraph;
    }

    public CHEntry getCoreEntry(String chName) {
        for (CHEntry cg : coreEntries) {
            if (cg.chConfig.getName().equals(chName))
                return cg;
        }
        return null;
    }

    public List<String> getCoreGraphNames() {
        return coreEntries.stream().map(ch -> ch.chConfig.getName()).collect(Collectors.toList());
    }

    public List<CHConfig> getCoreConfigs() {
        return coreEntries.stream().map(c -> c.chConfig).collect(Collectors.toList());
    }

    /**
     * 配置完此存储后，需要显式创建它。
     */
    public GRGraph create(long byteCount) {
        super.create(byteCount);

        coreEntries.forEach(ch -> ch.chStore.create());

        List<CHConfig> coreConfigs = getCoreConfigs();
        List<String> coreProfileNames = new ArrayList<>(coreConfigs.size());
        for (CHConfig chConfig : coreConfigs) {
            coreProfileNames.add(chConfig.getName());
        }
//        getProperties().put("graph.core.profiles", coreProfileNames.toString());
        return this;
    }

    public void loadExistingORS() {
        coreEntries.forEach(cg -> {
            if (!cg.chStore.loadExisting())
                throw new IllegalStateException("Cannot load " + cg);
        });
        if (getExtensions() != null) {
            getExtensions().loadExisting();
        }
    }

    public void flush() {
        super.flush();
        coreEntries.stream().map(ch -> ch.chStore).filter(s -> !s.isClosed()).forEach(CHStorage::flush);
    }

    @Override
    public void close() {
        super.close();
        coreEntries.stream().map(ch -> ch.chStore).filter(s -> !s.isClosed()).forEach(CHStorage::close);
    }

    @Override
    public long getCapacity() {
        return super.getCapacity() + coreEntries.stream().mapToLong(ch -> ch.chStore.getCapacity()).sum();
    }

    /**
     * 避免基础图的边和节点被进一步修改。作为钩子是必要的，例如用于
     * 在其之上的 CH 图进行自身初始化
     */
    public synchronized void freeze() {
        if (isFrozen())
            return;
        super.freeze();
        coreEntries.forEach(ch -> {
            // 这里使用一个较小的值。这可能在后续导致更多次分配，但影响
            // 不会太大。如果使用过大的值，shortcuts 的 DataAccess 最终会
            // 大于所需，因为最后我们并没有做类似 trimToSize 的操作。
            double expectedShortcuts = 0.3 * getBaseGraph().getEdges();
            ch.chStore.init(getBaseGraph().getNodes(), (int) expectedShortcuts);
        });
    }

    @Override
    public String toDetailsString() {
        StringBuilder str = new StringBuilder(super.toDetailsString());
        for (CHEntry ch : coreEntries) {
            str.append(", ").append(ch.chStore.toDetailsString());
        }

        return str.toString();
    }

    // 估计的 core 节点数量，用于 Tarjan 算法中的数组初始化
    public int getCoreNodes() {
        for (CHEntry cg : coreEntries) {
            if (cg.chGraph.getCoreNodes() == -1) continue;
            return cg.chGraph.getCoreNodes();
        }
        throw new IllegalStateException("No prepared core graph was found");
    }
}
