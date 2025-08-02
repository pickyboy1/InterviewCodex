package com.pickyboy.interviewcodex.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Lists;
import com.pickyboy.interviewcodex.cache.CacheUtils;
import com.pickyboy.interviewcodex.common.ErrorCode;
import com.pickyboy.interviewcodex.constant.CommonConstant;
import com.pickyboy.interviewcodex.exception.BusinessException;
import com.pickyboy.interviewcodex.exception.ThrowUtils;
import com.pickyboy.interviewcodex.lock.DistributeLock;
import com.pickyboy.interviewcodex.mapper.QuestionBankQuestionMapper;

import com.pickyboy.interviewcodex.model.dto.questionbankquestion.QuestionBankQuestionQueryRequest;
import com.pickyboy.interviewcodex.model.entity.Question;
import com.pickyboy.interviewcodex.model.entity.QuestionBank;
import com.pickyboy.interviewcodex.model.entity.QuestionBankQuestion;
import com.pickyboy.interviewcodex.model.entity.User;
import com.pickyboy.interviewcodex.model.vo.QuestionBankQuestionVO;
import com.pickyboy.interviewcodex.model.vo.UserVO;
import com.pickyboy.interviewcodex.service.QuestionBankQuestionService;
import com.pickyboy.interviewcodex.service.QuestionBankService;
import com.pickyboy.interviewcodex.service.QuestionService;
import com.pickyboy.interviewcodex.service.UserService;
import com.pickyboy.interviewcodex.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


/**
 * 题库题目关联服务实现
 *
 * @author pickyboy
 */
@Service
@Slf4j
public class QuestionBankQuestionServiceImpl extends ServiceImpl<QuestionBankQuestionMapper, QuestionBankQuestion> implements QuestionBankQuestionService {

    @Resource
    private UserService userService;
    @Resource
    @Lazy
    private QuestionService questionService;

    @Resource
    @Lazy
    private QuestionBankService questionBankService;

    @Autowired
    @Qualifier("batchExecutor")
    private ThreadPoolExecutor executor;

    @Resource
    private RedissonClient redissonClient;

    @Autowired
    private CacheUtils cacheUtils;

    /**
     * 校验数据
     *
     * @param questionBankQuestion
     * @param add      对创建的数据进行校验
     */
    @Override
    public void validQuestionBankQuestion(QuestionBankQuestion questionBankQuestion, boolean add) {
        ThrowUtils.throwIf(questionBankQuestion == null, ErrorCode.PARAMS_ERROR);
        Long questionBankId = questionBankQuestion.getQuestionBankId();
        if (questionBankId != null) {
            QuestionBank questionBank = questionBankService.getById(questionBankId);
            ThrowUtils.throwIf(questionBank == null, ErrorCode.NOT_FOUND_ERROR,"题库不存在");
        }
        Long questionId = questionBankQuestion.getQuestionId();
        if (questionId != null) {
            Question question = questionService.getById(questionId);
            ThrowUtils.throwIf(question == null, ErrorCode.NOT_FOUND_ERROR,"题目不存在");
        }
        // todo 补充校验规则
//        if (StringUtils.isNotBlank(title)) {
//            ThrowUtils.throwIf(title.length() > 80, ErrorCode.PARAMS_ERROR, "标题过长");
//        }
    }

    /**
     * 获取查询条件
     *
     * @param questionBankQuestionQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<QuestionBankQuestion> getQueryWrapper(QuestionBankQuestionQueryRequest questionBankQuestionQueryRequest) {
        QueryWrapper<QuestionBankQuestion> queryWrapper = new QueryWrapper<>();
        if (questionBankQuestionQueryRequest == null) {
            return queryWrapper;
        }
        //  从对象中取值
        Long id = questionBankQuestionQueryRequest.getId();
        Long notId = questionBankQuestionQueryRequest.getNotId();
        String sortField = questionBankQuestionQueryRequest.getSortField();
        String sortOrder = questionBankQuestionQueryRequest.getSortOrder();
        Long questionBankId = questionBankQuestionQueryRequest.getQuestionBankId();
        Long questionId = questionBankQuestionQueryRequest.getQuestionId();
        Long userId = questionBankQuestionQueryRequest.getUserId();
        // 补充需要的查询条件

        // 模糊查询

        // JSON 数组查询

        // 精确查询
        queryWrapper.ne(ObjectUtils.isNotEmpty(notId), "id", notId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(questionBankId), "questionBankId", questionBankId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(questionId), "questionId", questionId);
        // 排序规则
        queryWrapper.orderBy(SqlUtils.validSortField(sortField),
                sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    /**
     * 获取题库题目关联封装
     *
     * @param questionBankQuestion
     * @param request
     * @return
     */
    @Override
    public QuestionBankQuestionVO getQuestionBankQuestionVO(QuestionBankQuestion questionBankQuestion, HttpServletRequest request) {
        // 对象转封装类
        QuestionBankQuestionVO questionBankQuestionVO = QuestionBankQuestionVO.objToVo(questionBankQuestion);

        //  可以根据需要为封装对象补充值，不需要的内容可以删除
        // region 可选
        // 1. 关联查询用户信息
        Long userId = questionBankQuestion.getUserId();
        User user = null;
        if (userId != null && userId > 0) {
            user = userService.getById(userId);
        }
        UserVO userVO = userService.getUserVO(user);
        questionBankQuestionVO.setUser(userVO);

        // endregion

        return questionBankQuestionVO;
    }

    /**
     * 分页获取题库题目关联封装
     *
     * @param questionBankQuestionPage
     * @param request
     * @return
     */
    @Override
    public Page<QuestionBankQuestionVO> getQuestionBankQuestionVOPage(Page<QuestionBankQuestion> questionBankQuestionPage, HttpServletRequest request) {
        List<QuestionBankQuestion> questionBankQuestionList = questionBankQuestionPage.getRecords();
        Page<QuestionBankQuestionVO> questionBankQuestionVOPage = new Page<>(questionBankQuestionPage.getCurrent(), questionBankQuestionPage.getSize(), questionBankQuestionPage.getTotal());
        if (CollUtil.isEmpty(questionBankQuestionList)) {
            return questionBankQuestionVOPage;
        }
        // 对象列表 => 封装对象列表
        List<QuestionBankQuestionVO> questionBankQuestionVOList = questionBankQuestionList.stream().map(questionBankQuestion -> {
            return QuestionBankQuestionVO.objToVo(questionBankQuestion);
        }).collect(Collectors.toList());

        //  可以根据需要为封装对象补充值，不需要的内容可以删除
        // region 可选
        // 1. 关联查询用户信息
        Set<Long> userIdSet = questionBankQuestionList.stream().map(QuestionBankQuestion::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));

        // 填充信息
        questionBankQuestionVOList.forEach(questionBankQuestionVO -> {
            Long userId = questionBankQuestionVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            questionBankQuestionVO.setUser(userService.getUserVO(user));
               });
        // endregion

        questionBankQuestionVOPage.setRecords(questionBankQuestionVOList);
        return questionBankQuestionVOPage;
    }

    /**
     * 批量添加题目到题库
     * @param questionIdList
     * @param questionBankId
     * @param loginUser
     */
    @Override
    @DistributeLock(scene="batchAddQuestionsToBank", keyExpression = "#questionBankId")
    public void batchAddQuestionsToBank(List<Long> questionIdList, Long questionBankId, User loginUser) {
        // 参数校验
        ThrowUtils.throwIf(CollUtil.isEmpty(questionIdList), ErrorCode.PARAMS_ERROR, "题目列表不能为空");
        ThrowUtils.throwIf(questionBankId<=0, ErrorCode.PARAMS_ERROR, "题库id非法");
        ThrowUtils.throwIf(loginUser==null, ErrorCode.NOT_LOGIN_ERROR);
        // 题目存在性校验
        // 合法的题目id列表
        List<Long> validQuestionIdList = questionService.listObjs(new LambdaQueryWrapper<Question>()
                .select(Question::getId)
                .in(Question::getId, questionIdList),obj->Long.parseLong(obj.toString()));
        ThrowUtils.throwIf(CollUtil.isEmpty(validQuestionIdList), ErrorCode.PARAMS_ERROR, "合法题目为空");

        // 题库存在性校验
        QuestionBank questionBank = questionBankService.getById(questionBankId);
        ThrowUtils.throwIf(questionBank==null, ErrorCode.NOT_FOUND_ERROR, "题库不存在");
        // 过滤已经添加过的题目
        Set<Long> alreadyAdded = new HashSet<>(this.listObjs(new LambdaQueryWrapper<QuestionBankQuestion>()
                .select(QuestionBankQuestion::getQuestionId)
                .eq(QuestionBankQuestion::getQuestionBankId, questionBankId)
                .in(QuestionBankQuestion::getQuestionId, validQuestionIdList), obj -> Long.parseLong(obj.toString())));
        validQuestionIdList.removeAll(alreadyAdded);

        // 转换为数据库实体类
        List<QuestionBankQuestion> toInsert = new ArrayList<>();
        for (Long questionId : validQuestionIdList) {
            QuestionBankQuestion questionBankQuestion = new QuestionBankQuestion();
            questionBankQuestion.setQuestionBankId(questionBankId);
            questionBankQuestion.setQuestionId(questionId);
            questionBankQuestion.setUserId(loginUser.getId());
            toInsert.add(questionBankQuestion);
        }
       // 分批处理添加
        int batchSize = 1000;
        List<List<QuestionBankQuestion>> partition = Lists.partition(toInsert, batchSize);
        // 获取代理
        QuestionBankQuestionService proxy = (QuestionBankQuestionService) AopContext.currentProxy();
        // 批量异步添加
        List<CompletableFuture<Void>> future = partition.stream()
                .map(list -> CompletableFuture.runAsync(() -> proxy.batchAddQuestionsToBankInner(list), executor)).toList();
        CompletableFuture.allOf(future.toArray(new CompletableFuture[0])).join();
    }

    /**
     * 批量添加题目到题库,供内部调用的事务方法
     * @param questionBankQuestions
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void batchAddQuestionsToBankInner(List<QuestionBankQuestion> questionBankQuestions){
        try {
            boolean success = this.saveBatch(questionBankQuestions);
            ThrowUtils.throwIf(!success, ErrorCode.OPERATION_ERROR,"插入失败");

        }catch (DataIntegrityViolationException e){
            log.error("批量添加题目到题库违反完整性约束");
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "题目已经存在于题库,无法添加");
        }
        catch (DataAccessException e){
            log.error("数据库连接问题,事务问题导致操作失败");
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "数据库操作失败");
        }
        catch (Exception e){
            log.error("未知错误, 错误信息: {}",e.getMessage());
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未知错误,添加失败");
        }
    }

    /**
     * 批量移除题库题目
     * @param questionIdList
     * @param questionBankId
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchRemoveQuestionsFromBank(List<Long> questionIdList, Long questionBankId) {
        // 参数校验
        ThrowUtils.throwIf(CollUtil.isEmpty(questionIdList), ErrorCode.PARAMS_ERROR, "题目列表不能为空");
        ThrowUtils.throwIf(questionBankId<=0, ErrorCode.PARAMS_ERROR, "题库id非法");

        // 移除
        boolean success = this.remove(new LambdaQueryWrapper<QuestionBankQuestion>()
                .eq(QuestionBankQuestion::getQuestionBankId, questionBankId).in(QuestionBankQuestion::getQuestionId, questionIdList));
        ThrowUtils.throwIf(!success, ErrorCode.OPERATION_ERROR,"移除失败");
    }

    @Override
    public void batchAddQuestionsToBankWithCache(List<Long> questionIdList, Long questionBankId, User loginUser) {
        // 调用原有方法
        this.batchAddQuestionsToBank(questionIdList, questionBankId, loginUser);
        // 清除题库缓存
        clearQuestionBankCache(questionBankId);
    }

    @Override
    public void batchRemoveQuestionsFromBankWithCache(List<Long> questionIdList, Long questionBankId) {
        // 调用原有方法
        this.batchRemoveQuestionsFromBank(questionIdList, questionBankId);
        // 清除题库缓存
        clearQuestionBankCache(questionBankId);
    }

    /**
     * 清除题库缓存（两种key格式）
     * @param questionBankId 题库ID
     */
    private void clearQuestionBankCache(Long questionBankId) {
        if (questionBankId != null) {
            cacheUtils.evictCacheWithStatus("bank_detail", questionBankId, Arrays.asList("true", "false"));
        }
    }

}
