package com.pickyboy.interviewcodex.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import com.pickyboy.interviewcodex.model.dto.questionbank.QuestionBankQueryRequest;
import com.pickyboy.interviewcodex.model.entity.QuestionBank;
import com.pickyboy.interviewcodex.model.vo.QuestionBankVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 题库服务
 *
 * @author pickyboy
 */
public interface QuestionBankService extends IService<QuestionBank> {

    /**
     * 校验数据
     *
     * @param questionBank
     * @param add 对创建的数据进行校验
     */
    void validQuestionBank(QuestionBank questionBank, boolean add);

    /**
     * 获取查询条件
     *
     * @param questionBankQueryRequest
     * @return
     */
    QueryWrapper<QuestionBank> getQueryWrapper(QuestionBankQueryRequest questionBankQueryRequest);

    /**
     * 获取题库封装
     *
     * @param questionBank
     * @param request
     * @return
     */
    QuestionBankVO getQuestionBankVO(QuestionBank questionBank, HttpServletRequest request);

    /**
     * 分页获取题库封装
     *
     * @param questionBankPage
     * @param request
     * @return
     */
    Page<QuestionBankVO> getQuestionBankVOPage(Page<QuestionBank> questionBankPage, HttpServletRequest request);

    /**
     * 获取缓存的题库详情
     * @param id 题库ID
     * @param needQueryQuestionList 是否需要查询题目列表
     * @return 题库详情VO
     */
    QuestionBankVO getCachedQuestionBankVO(Long id, boolean needQueryQuestionList);

    /**
     * 删除题库（带缓存清除）
     * @param id 题库ID
     * @return 是否删除成功
     */
    boolean deleteQuestionBankWithCache(Long id);

    /**
     * 更新题库（带缓存清除）
     * @param questionBank 题库信息
     * @return 是否更新成功
     */
    boolean updateQuestionBankWithCache(QuestionBank questionBank);

    /**
     * 批量删除题库（带缓存清除）
     * @param questionBankIdList 题库ID列表
     */
    void batchDeleteQuestionBanksWithCache(List<Long> questionBankIdList);
}
