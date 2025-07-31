package com.pickyboy.interviewcodex.job.cycle;

import cn.hutool.core.collection.CollUtil;
import com.pickyboy.interviewcodex.esdao.QuestionEsDao;
import com.pickyboy.interviewcodex.lock.DistributeLock;
import com.pickyboy.interviewcodex.mapper.QuestionMapper;
import com.pickyboy.interviewcodex.model.dto.question.QuestionEsDTO;
import com.pickyboy.interviewcodex.model.entity.Question;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 增量同步题目到 es
 *
 * @author <a href="https://github.com/liyupi">pickyboy</a>
 */

@Component
@Slf4j
public class IncSyncQuestionToEs {

    @Resource
    private QuestionMapper questionMapper;

    @Resource
    private QuestionEsDao questionEsDao;

    /**
     * 每分钟执行一次
     */
    @Scheduled(fixedRate = 60 * 1000)
    @DistributeLock(
            scene = "sync:question:es",
            key = "'singleton'", // 对于单例任务，提供一个固定的静态key即可
            waitTime = 0,        // 推荐配置：0秒等待
            expireTime = 55000   // 推荐配置：略小于任务执行间隔
    )
    public void run() {
        // 查询近 5 分钟内的数据
        Date fiveMinutesAgoDate = new Date(new Date().getTime() - 5 * 60 * 1000L);
        List<Question> questionList = questionMapper.listQuestionWithDelete(fiveMinutesAgoDate);
        if (CollUtil.isEmpty(questionList)) {
            log.info("no inc question");
            return;
        }
        List<QuestionEsDTO> questionEsDTOList = questionList.stream()
                .map(QuestionEsDTO::objToDto)
                .collect(Collectors.toList());
        final int pageSize = 500;
        int total = questionEsDTOList.size();
        log.info("IncSyncQuestionToEs start, total {}", total);
        for (int i = 0; i < total; i += pageSize) {
            int end = Math.min(i + pageSize, total);
            log.info("sync from {} to {}", i, end);
            questionEsDao.saveAll(questionEsDTOList.subList(i, end));
        }
        log.info("IncSyncQuestionToEs end, total {}", total);
    }
}
