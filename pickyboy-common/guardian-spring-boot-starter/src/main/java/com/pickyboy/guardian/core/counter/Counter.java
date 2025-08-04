package com.pickyboy.guardian.core.counter;

import com.pickyboy.guardian.model.counter.CounterParam;

public interface Counter {
        /**
         * 递增计数并返回当前值
         *
         * @param config 计数配置
         * @return 当前计数值
         */
        long incrementAndGet(CounterParam config);

        /**
         * 获取当前计数值
         */
        long getCurrentCount(CounterParam config);

        /**
         * 重置计数器
         */
        void reset(CounterParam config);

            /**
     * 计数器类型标识
     */
    String getType();


}
