package com.pickyboy.interviewcodex.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.pickyboy.interviewcodex.model.dto.question.QuestionQueryRequest;
import com.pickyboy.interviewcodex.model.entity.Question;
import com.pickyboy.interviewcodex.model.vo.QuestionVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 题目服务
 *
 * @author pickyboy
 */
public interface QuestionService extends IService<Question> {

    /**
     * 校验数据
     *
     * @param question
     * @param add 对创建的数据进行校验
     */
    void validQuestion(Question question, boolean add);

    /**
     * 获取查询条件
     *
     * @param questionQueryRequest
     * @return
     */
    QueryWrapper<Question> getQueryWrapper(QuestionQueryRequest questionQueryRequest);

    /**
     * 获取题目封装
     *
     * @param question
     * @param request
     * @return
     */
    QuestionVO getQuestionVO(Question question, HttpServletRequest request);

    /**
     * 分页获取题目封装
     *
     * @param questionPage
     * @param request
     * @return
     */
    Page<QuestionVO> getQuestionVOPage(Page<Question> questionPage, HttpServletRequest request);

    /**
     * 分页获取题目列表（仅管理员可用）
     *
     * @param questionQueryRequest
     * @return
     */
    Page<Question> listQuestionByPage(QuestionQueryRequest questionQueryRequest) ;


    /**
     * 从 ES 查询题目
     *
     * @param questionQueryRequest
     * @return
     */
    Page<Question> searchFromEs(QuestionQueryRequest questionQueryRequest);

    /**
     * 批量删除题目
     * @param questionIdList
     */
    void batchDeleteQuestions(List<Long> questionIdList);

    /**
     * 删除题目（带缓存清除）
     * @param id 题目ID
     * @return 是否删除成功
     */
    boolean deleteQuestionWithCache(Long id);

    /**
     * 更新题目（带缓存清除）
     * @param question 题目信息
     * @return 是否更新成功
     */
    boolean updateQuestionWithCache(Question question);

    /**
     * 批量删除题目（带缓存清除）
     * @param questionIdList 题目ID列表
     */
    void batchDeleteQuestionsWithCache(List<Long> questionIdList);

    /**
     * 获取题目建议
     * @param prefix
     * @return
     */
    List<String> getSuggestions(String prefix);

    List<QuestionVO> getRecommendations(long id);

    /**
     * 获取缓存的题目详情
     * @param id 题目ID
     * @return 题目详情VO
     */
    QuestionVO getCacheQuestionVO(long id);
}
