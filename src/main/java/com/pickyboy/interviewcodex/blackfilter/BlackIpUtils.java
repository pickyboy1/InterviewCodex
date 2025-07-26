package com.pickyboy.interviewcodex.blackfilter;

import cn.hutool.bloomfilter.BitMapBloomFilter;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import org.yaml.snakeyaml.Yaml;

import java.util.List;
import java.util.Map;

/**
 * 黑名单过滤工具类
 */
public class BlackIpUtils {

    private static BitMapBloomFilter bloomFilter;

    public  static boolean isBlackIp(String ip) {
        return bloomFilter.contains(ip);
    }

    // 重建ip黑名单
    public static void rebuildBlackIp(String configInfo) {
        if(StrUtil.isBlank(configInfo)){
            configInfo = "{}";
        }
        // 解析yaml文件
        Yaml yaml = new Yaml();
        Map map = yaml.loadAs(configInfo, Map.class);
        List<String> blackIpList = (List<String>)map.get("blackIpList");

        //加锁防止并发
        synchronized (BlackIpUtils.class) {
            if (CollUtil.isNotEmpty(blackIpList)) {
                BitMapBloomFilter bitMapBloomFilter = new BitMapBloomFilter(958506);
                for (String ip : blackIpList) {
                    bitMapBloomFilter.add(ip);
                }
                bloomFilter = bitMapBloomFilter;
            } else {
                bloomFilter = new BitMapBloomFilter(100);
            }
        }
    }
}
