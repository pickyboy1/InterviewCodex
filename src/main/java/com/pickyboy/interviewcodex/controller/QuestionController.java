package com.pickyboy.interviewcodex.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.Tracer;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pickyboy.guardian.anotation.GuardianCheck;
import com.pickyboy.guardian.anotation.Rule;
import com.pickyboy.guardian.model.constant.GuardianConstants;
import com.pickyboy.interviewcodex.annotation.AuthCheck;
import com.pickyboy.interviewcodex.common.BaseResponse;
import com.pickyboy.interviewcodex.common.DeleteRequest;
import com.pickyboy.interviewcodex.common.ErrorCode;
import com.pickyboy.interviewcodex.common.ResultUtils;
import com.pickyboy.interviewcodex.constant.UserConstant;
import com.pickyboy.interviewcodex.exception.BusinessException;
import com.pickyboy.interviewcodex.exception.ThrowUtils;
import com.pickyboy.interviewcodex.model.dto.question.*;
import com.pickyboy.interviewcodex.model.entity.Question;
import com.pickyboy.interviewcodex.model.entity.QuestionBank;
import com.pickyboy.interviewcodex.model.entity.QuestionBankQuestion;
import com.pickyboy.interviewcodex.model.entity.User;
import com.pickyboy.interviewcodex.model.vo.QuestionVO;
import com.pickyboy.interviewcodex.service.QuestionBankQuestionService;
import com.pickyboy.interviewcodex.service.QuestionService;
import com.pickyboy.interviewcodex.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 题目接口
 *
 * @author pickyboy
 */
@RestController
@RequestMapping("/question")
@Slf4j
public class QuestionController {

    @Resource
    private QuestionService questionService;

    @Resource
    private UserService userService;

    @Resource
    private QuestionBankQuestionService questionBankQuestionService;


    // region 基础增删改查

    /**
     * 创建题目
     *
     * @param questionAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addQuestion(@RequestBody QuestionAddRequest questionAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(questionAddRequest == null, ErrorCode.PARAMS_ERROR);
        // 将实体类和 DTO 进行转换
        Question question = new Question();
        BeanUtils.copyProperties(questionAddRequest, question);
        List<String> tags = questionAddRequest.getTags();
        if (tags != null)  {
            question.setTags(JSONUtil.toJsonStr(tags));
        }
        // 数据校验
        questionService.validQuestion(question, true);
        // 填充默认值
        User loginUser = userService.getLoginUser(request);
        question.setUserId(loginUser.getId());
        // 写入数据库
        boolean result = questionService.save(question);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        // 返回新写入的数据 id
        long newQuestionId = question.getId();
        return ResultUtils.success(newQuestionId);
    }

    /**
     * 删除题目
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteQuestion(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        Question oldQuestion = questionService.getById(id);
        ThrowUtils.throwIf(oldQuestion == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldQuestion.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 操作数据库
        boolean result = questionService.deleteQuestionWithCache(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 更新题目（仅管理员可用）
     *
     * @param questionUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateQuestion(@RequestBody QuestionUpdateRequest questionUpdateRequest) {
        if (questionUpdateRequest == null || questionUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //  在此处将实体类和 DTO 进行转换
        Question question = new Question();
        BeanUtils.copyProperties(questionUpdateRequest, question);
        List<String> tags = questionUpdateRequest.getTags();
        if (tags != null)  {
            question.setTags(JSONUtil.toJsonStr(tags));
        }
        // 数据校验
        questionService.validQuestion(question, false);
        // 判断是否存在
        long id = questionUpdateRequest.getId();
        Question oldQuestion = questionService.getById(id);
        ThrowUtils.throwIf(oldQuestion == null, ErrorCode.NOT_FOUND_ERROR);
        // 操作数据库
        boolean result = questionService.updateQuestionWithCache(question);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 编辑题目（给用户使用）
     *
     * @param questionEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editQuestion(@RequestBody QuestionEditRequest questionEditRequest, HttpServletRequest request) {
        if (questionEditRequest == null || questionEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //  在此处将实体类和 DTO 进行转换
        Question question = new Question();
        BeanUtils.copyProperties(questionEditRequest, question);
        List<String> tags = questionEditRequest.getTags();
        if (tags != null)  {
            question.setTags(JSONUtil.toJsonStr(tags));
        }
        // 数据校验
        questionService.validQuestion(question, false);
        // 判断是否存在
        long id = questionEditRequest.getId();
        Question oldQuestion = questionService.getById(id);
        ThrowUtils.throwIf(oldQuestion == null, ErrorCode.NOT_FOUND_ERROR);
        User user = userService.getLoginUser(request);
        // 仅本人或管理员可编辑
        if (!oldQuestion.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 操作数据库
        boolean result = questionService.updateQuestionWithCache(question);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    // endregion

    /**
     * 判断检测爬虫方法
     * 使用redis计数器,判断一定时间内的访问次数
     * @param loginUserId
     */
// 已迁移为Guardian自定义框架
/*    private void crawlerDetect(long loginUserId){
        final int WARN_COUNT = 10;
        final int BAN_COUNT = 20;
        String key = String.format("user:access:%s",loginUserId);
        // 统计一分钟内访问次数,180秒过期
       long count =  counterManager.incrAndGetCounter(key,1, TimeUnit.MINUTES,180);

        if(count > BAN_COUNT){
            StpUtil.kickout(loginUserId);
            // 封号
            User updateUser = new User();
            updateUser.setId(loginUserId);
            updateUser.setUserRole("ban");
            userService.updateById(updateUser);
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "访问次数过多,已封号");
        }
        if(count == WARN_COUNT){
            // 可以向管理员发邮件告警
        }
    }*/
    /**
     * 根据 id 获取题目（封装类）
     *
     * @param id
     * @return
     */

    @GuardianCheck(scene = "question", key = "userId", windowSize = 60, timeUnit = TimeUnit.SECONDS, rules = {
            @Rule(count = 10, level = GuardianConstants.ALERT_LEVEL_INFO, strategy = {GuardianConstants.STRATEGY_TYPE_LOG_ONLY}, description = "测试"),
            @Rule(count = 20, level = GuardianConstants.ALERT_LEVEL_WARNING, strategy = {GuardianConstants.STRATEGY_TYPE_REJECT}, description = "测试",continuous = true),
            @Rule(count = 30, level = GuardianConstants.ALERT_LEVEL_CRITICAL, strategy = {GuardianConstants.STRATEGY_TYPE_BAN}, description = "测试")
    },errorMessage = "访问频率过高,恶意刷取将封号")
    @GetMapping("/get/vo")
    public BaseResponse<QuestionVO> getQuestionVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        QuestionVO question = questionService.getCacheQuestionVO(id);
        ThrowUtils.throwIf(question == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类
        return ResultUtils.success(question);
    }

    /**
     * 分页获取题目列表（仅管理员可用）
     *
     * @param questionQueryRequest
     * @return
     */
    // todo: 管理员也可以接入Es进行搜索
    @PostMapping("/list/page")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Question>> listQuestionByPage(@RequestBody QuestionQueryRequest questionQueryRequest) {
        ThrowUtils.throwIf(questionQueryRequest == null, ErrorCode.PARAMS_ERROR);
        Page<Question> questionPage = questionService.listQuestionByPage(questionQueryRequest);
        return ResultUtils.success(questionPage);
    }

    /**
     * 分页搜索题目列表（封装类）普通搜索
     *
     * @param questionQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<QuestionVO>> listQuestionVOByPage(@RequestBody QuestionQueryRequest questionQueryRequest,
                                                               HttpServletRequest request) {
        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Page<Question> questionPage = questionService.page(new Page<>(current, size),
                questionService.getQueryWrapper(questionQueryRequest));
        // 获取封装类
        return ResultUtils.success(questionService.getQuestionVOPage(questionPage, request));
    }

    /**
     * 分页搜索题目列表（限流版本）
     *
     * @param questionQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo/sentinel")
    public BaseResponse<Page<QuestionVO>> listQuestionVOByPageSentinel(@RequestBody QuestionQueryRequest questionQueryRequest,
                                                               HttpServletRequest request) {
        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 基于ip限流
        String remoteAddr = request.getRemoteAddr();
        Entry entry = null;
        // 编码方式声明资源,sentinel会自动统计访问次数,超过阈值会触发熔断降级
        try {
            entry = SphU.entry("listQuestionVOByPageSentinel", EntryType.IN,1,remoteAddr);
            // 被保护的资源
            // 查询数据库
            Page<Question> questionPage = questionService.page(new Page<>(current, size),
                    questionService.getQueryWrapper(questionQueryRequest));
            // 获取封装类
            return ResultUtils.success(questionService.getQuestionVOPage(questionPage, request));
        }
        catch(Throwable e){
            // 非注解方式声明资源,业务自身异常需要手动上报给sentinel,触发熔断降级
            if(!BlockException.isBlockException( e)){
                Tracer.trace(e);
                return ResultUtils.error(ErrorCode.SYSTEM_ERROR,"系统错误");
            }
            if(e instanceof DegradeException){
                return handleFallback(questionQueryRequest, request, e);
            }
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR,"访问过于频繁,请稍后再试");
        }
        finally {
            if (entry != null) {
                entry.exit(1,remoteAddr);
            }
        }
    }

    // 降级策略,直接返回空
    public BaseResponse<Page<QuestionVO>> handleFallback(QuestionQueryRequest questionQueryRequest,
                                                                   HttpServletRequest request, Throwable ex) {
        return ResultUtils.success( null);
    }

    /**
     * 分页获取当前登录用户创建的题目列表
     *
     * @param questionQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<QuestionVO>> listMyQuestionVOByPage(@RequestBody QuestionQueryRequest questionQueryRequest,
                                                                 HttpServletRequest request) {
        ThrowUtils.throwIf(questionQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 补充查询条件，只查询当前登录用户的数据
        User loginUser = userService.getLoginUser(request);
        questionQueryRequest.setUserId(loginUser.getId());
        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Page<Question> questionPage = questionService.page(new Page<>(current, size),
                questionService.getQueryWrapper(questionQueryRequest));
        // 获取封装类
        return ResultUtils.success(questionService.getQuestionVOPage(questionPage, request));
    }




    /*
    *  es搜索题目
    * */
    @PostMapping("/search/page/vo")
    public BaseResponse<Page<QuestionVO>> searchQuestionByPage(@RequestBody QuestionQueryRequest questionQueryRequest
    , HttpServletRequest request) {

        long size = questionQueryRequest.getPageSize();
        ThrowUtils.throwIf(size > 200, ErrorCode.PARAMS_ERROR);
        Page<Question> questionPage = questionService.searchFromEs(questionQueryRequest);
        return ResultUtils.success(questionService.getQuestionVOPage(questionPage, request));
    }

    /**
     * 根据题目ID获取相关推荐题目
     *
     * @param id 当前题目的ID
     * @return
     */
    @GetMapping("/recommend")
    public BaseResponse<List<QuestionVO>> getRecommendations(@RequestParam long id) {
        List<QuestionVO> recommendations = questionService.getRecommendations(id);
        return ResultUtils.success(recommendations);
    }

    /**
     * 批量删除题目(及对应关联关系)
     */
    @PostMapping("/delete/batch")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteBatchQuestions(@RequestBody QuestionBatchDeleteRequest questionBatchDeleteRequest) {
        ThrowUtils.throwIf(questionBatchDeleteRequest==null, ErrorCode.PARAMS_ERROR, "题目列表不能为空");
        questionService.batchDeleteQuestions(questionBatchDeleteRequest.getQuestionIdList());
        return ResultUtils.success(true);
    }

    // todo: 可以添加批量添加题目接口,用于AI生成题目的添加



}
