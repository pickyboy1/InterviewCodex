package com.pickyboy.interviewcodex.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pickyboy.interviewcodex.cache.AutoCache;
import com.pickyboy.interviewcodex.cache.CacheEvict;
import com.pickyboy.interviewcodex.cache.CacheUtils;
import com.pickyboy.interviewcodex.common.ErrorCode;
import com.pickyboy.interviewcodex.constant.CommonConstant;
import com.pickyboy.interviewcodex.exception.BusinessException;
import com.pickyboy.interviewcodex.exception.ThrowUtils;
import com.pickyboy.interviewcodex.mapper.QuestionBankMapper;

import com.pickyboy.interviewcodex.model.dto.question.QuestionQueryRequest;
import com.pickyboy.interviewcodex.model.dto.questionbank.QuestionBankQueryRequest;
import com.pickyboy.interviewcodex.model.entity.Question;
import com.pickyboy.interviewcodex.model.entity.QuestionBank;
import com.pickyboy.interviewcodex.model.entity.User;
import com.pickyboy.interviewcodex.model.vo.QuestionBankVO;
import com.pickyboy.interviewcodex.model.vo.UserVO;
import com.pickyboy.interviewcodex.service.QuestionBankService;
import com.pickyboy.interviewcodex.service.QuestionService;
import com.pickyboy.interviewcodex.service.UserService;
import com.pickyboy.interviewcodex.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 题库服务实现
 *
 * @author pickyboy
 */
@Service
@Slf4j
public class QuestionBankServiceImpl
        extends ServiceImpl<QuestionBankMapper, QuestionBank>
        implements QuestionBankService {

    @Resource
    private UserService userService;

    @Resource
    private QuestionService questionService;

    @Resource
    private CacheUtils cacheUtils;

    @Override
    public void validQuestionBank(QuestionBank questionBank, boolean add) {
        if (questionBank == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String title = questionBank.getTitle();
        String description = questionBank.getDescription();
        // 创建时，参数不能为空
        if (add) {
            ThrowUtils.throwIf(StringUtils.isAnyBlank(title, description), ErrorCode.PARAMS_ERROR);
        }
        // 有参数则校验
        if (StringUtils.isNotBlank(title) && title.length() > 80) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标题过长");
        }
        if (StringUtils.isNotBlank(description) && description.length() > 8192) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容过长");
        }
    }

    /**
     * 获取查询包装类
     *
     * @param questionBankQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<QuestionBank> getQueryWrapper(QuestionBankQueryRequest questionBankQueryRequest) {
        QueryWrapper<QuestionBank> queryWrapper = new QueryWrapper<>();
        if (questionBankQueryRequest == null) {
            return queryWrapper;
        }

        Long id = questionBankQueryRequest.getId();
        Long notId = questionBankQueryRequest.getNotId();
        String searchText = questionBankQueryRequest.getSearchText();
        String title = questionBankQueryRequest.getTitle();
        String description = questionBankQueryRequest.getDescription();
        String picture = questionBankQueryRequest.getPicture();
        Long userId = questionBankQueryRequest.getUserId();
        String sortField = questionBankQueryRequest.getSortField();
        String sortOrder = questionBankQueryRequest.getSortOrder();

        // 拼接查询条件
        if (StringUtils.isNotBlank(searchText)) {
            queryWrapper.and(qw -> qw.like("title", searchText).or().like("description", searchText));
        }
        queryWrapper.like(StringUtils.isNotBlank(title), "title", title);
        queryWrapper.like(StringUtils.isNotBlank(description), "description", description);
        queryWrapper.like(StringUtils.isNotBlank(picture), "picture", picture);
        queryWrapper.eq(ObjectUtils.isNotEmpty(id), "id", id);
        queryWrapper.ne(ObjectUtils.isNotEmpty(notId), "id", notId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq("isDelete", false);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    @Override
    public QuestionBankVO getQuestionBankVO(QuestionBank questionBank, HttpServletRequest request) {
        QuestionBankVO questionBankVO = QuestionBankVO.objToVo(questionBank);
        // 1. 关联查询用户信息
        Long userId = questionBank.getUserId();
        User user = null;
        if (userId != null && userId > 0) {
            user = userService.getById(userId);
        }
        UserVO userVO = userService.getUserVO(user);
        questionBankVO.setUser(userVO);
        return questionBankVO;
    }

    @Override
    public Page<QuestionBankVO> getQuestionBankVOPage(Page<QuestionBank> questionBankPage, HttpServletRequest request) {
        List<QuestionBank> questionBankList = questionBankPage.getRecords();
        Page<QuestionBankVO> questionBankVOPage = new Page<>(questionBankPage.getCurrent(), questionBankPage.getSize(), questionBankPage.getTotal());
        if (CollUtil.isEmpty(questionBankList)) {
            return questionBankVOPage;
        }
        // 1. 关联查询用户信息
        Set<Long> userIdSet = questionBankList.stream().map(QuestionBank::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));

        // 填充信息
        List<QuestionBankVO> questionBankVOList = questionBankList.stream().map(questionBank -> {
            QuestionBankVO questionBankVO = QuestionBankVO.objToVo(questionBank);
            Long userId = questionBank.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            questionBankVO.setUser(userService.getUserVO(user));
            return questionBankVO;
        }).collect(Collectors.toList());
        questionBankVOPage.setRecords(questionBankVOList);

        return questionBankVOPage;
    }

    @Override
    // 注解只能接受编译时常量，还是只能硬编码场景，不能通过枚举的get方法直接获得，不过枚举可以作为参考
    @AutoCache(scene = "bank_detail", keyExpression = "#id + '_' + #needList")
    public QuestionBankVO getCachedQuestionBankVO(Long id, boolean needList) {
        QuestionBank questionBank = getById(id);
        // 如果题库不存在，返回null，让缓存穿透防护机制处理
        if (questionBank == null) {
            return null;
        }

        // 2. 基础对象转换
        QuestionBankVO questionBankVO = QuestionBankVO.objToVo(questionBank);

        // 3. 关联用户信息
        User user = userService.getById(questionBank.getUserId());
        questionBankVO.setUser(userService.getUserVO(user));

        // 4. 按需查询题目列表
        if (needList) {
            QuestionQueryRequest innerQuestionQueryRequest = new QuestionQueryRequest();
            innerQuestionQueryRequest.setQuestionBankId(id);
            Page<Question> questionPage = questionService.listQuestionByPage(innerQuestionQueryRequest);
            questionBankVO.setQuestionPage(questionPage);
        }

        return questionBankVO;
    }

    @Override
    @CacheEvict(scene = "bank_detail", keyExpression = "#id", statuses = {"true", "false"})
    public boolean deleteQuestionBankWithCache(Long id) {
        return this.removeById(id);
    }

    @Override
    @CacheEvict(scene = "bank_detail", keyExpression = "#questionBank.id", statuses = {"true", "false"})
    public boolean updateQuestionBankWithCache(QuestionBank questionBank) {
        return this.updateById(questionBank);
    }

    @Override
    @CacheEvict(scene = "bank_detail", keyExpression = "#questionBankIdList", isBatch = true, statuses = {"true", "false"})
    public void batchDeleteQuestionBanksWithCache(List<Long> questionBankIdList) {
        // 批量删除题库
        boolean result = this.removeBatchByIds(questionBankIdList);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "批量删除题库失败");
    }

}
