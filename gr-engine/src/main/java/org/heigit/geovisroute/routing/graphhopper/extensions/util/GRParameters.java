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
package org.heigit.geovisroute.routing.graphhopper.extensions.util;

/**
 * This code is based on that from GraphHopper GmbH.
 *
 * @author Peter Karich
 * @author Hendrik Leuschner
 */
public class GRParameters {
    private GRParameters() {
    }

    /* 带 'INIT' 前缀的参数用作默认值，和/或在启动时进行配置。*/
    static final String ROUTING_INIT_PREFIX = "routing.";

    /**
     * 可作为 hints 传入并按请求影响路径计算的参数。
     */
    public static final class Weighting {
        private Weighting() {
        }

        public static final String TIME_DEPENDENT_SPEED_OR_ACCESS = "time_dependent_speed_or_access";
    }


    /**
     * 使用 contraction hierarchies 加速的路径计算相关属性
     */
    public static final class Core {
        private Core() {
        }

        public static final String PREPARE = "prepare.core.";
        /**
         * HintsMap 中的该属性名用于在运行时配置是否应忽略 CH 路径计算。
         */
        public static final String DISABLE = "core.disable";
        /**
         * HintsMap 中的属性名，用于指定是否应忽略可能出现的路径计算错误而将 heading 用于 CH。
         */
        public static final String FORCE_HEADING = "core.force_heading";
    }

    /**
     * 使用 landmark 加速的路径计算相关属性
     */
    public static final class CoreLandmark {
        private CoreLandmark() {
        }

        public static final String PREPARE = "prepare.corelm.";
        /**
         * 指定 landmark 集合。
         */
        public static final String LMSETS = PREPARE + "lmsets";
        /**
         * HintsMap 中的该属性名用于在运行时配置是否应忽略 CH 路径计算。
         */
        public static final String DISABLE = "corelm.disable";
        /**
         * 指定路径计算时应使用多少个活跃 landmark
         */
        public static final String ACTIVE_COUNT = "corelm.active_landmarks";
        /**
         * 活跃数量的默认值
         */
        public static final String ACTIVE_COUNT_DEFAULT = ROUTING_INIT_PREFIX + ACTIVE_COUNT;
        /**
         * 指定应创建多少个 landmark
         */
        public static final String COUNT = PREPARE + "landmarks";
    }

    /**
     * 分区预处理相关属性
     */
    public static final class FastIsochrone {
        private FastIsochrone() {
        }

        public static final String PREPARE = "prepare.fastisochrone.";
        /**
         * HintsMap 中的该属性名用于在运行时配置是否应忽略 CH 路径计算。
         */
        public static final String PROFILE = "fastisochrone.profile";
        /**
         * HintsMap 中的属性名，用于指定是否应忽略可能出现的路径计算错误而将 heading 用于 CH。
         */
        public static final String FORCE_HEADING = "fastisochrone.force_heading";
    }


}
