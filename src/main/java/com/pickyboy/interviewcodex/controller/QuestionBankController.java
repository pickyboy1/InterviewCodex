package com.pickyboy.interviewcodex.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jd.platform.hotkey.client.callback.JdHotKeyStore;
import com.pickyboy.interviewcodex.annotation.AuthCheck;
import com.pickyboy.interviewcodex.common.BaseResponse;
import com.pickyboy.interviewcodex.common.DeleteRequest;
import com.pickyboy.interviewcodex.common.ErrorCode;
import com.pickyboy.interviewcodex.common.ResultUtils;
import com.pickyboy.interviewcodex.constant.UserConstant;
import com.pickyboy.interviewcodex.exception.BusinessException;
import com.pickyboy.interviewcodex.exception.ThrowUtils;

import com.pickyboy.interviewcodex.model.dto.question.QuestionQueryRequest;
import com.pickyboy.interviewcodex.model.dto.questionbank.QuestionBankAddRequest;
import com.pickyboy.interviewcodex.model.dto.questionbank.QuestionBankEditRequest;
import com.pickyboy.interviewcodex.model.dto.questionbank.QuestionBankQueryRequest;
import com.pickyboy.interviewcodex.model.dto.questionbank.QuestionBankUpdateRequest;
import com.pickyboy.interviewcodex.model.entity.Question;
import com.pickyboy.interviewcodex.model.entity.QuestionBank;
import com.pickyboy.interviewcodex.model.entity.QuestionBankQuestion;
import com.pickyboy.interviewcodex.model.entity.User;
import com.pickyboy.interviewcodex.model.vo.QuestionBankVO;
import com.pickyboy.interviewcodex.service.QuestionBankQuestionService;
import com.pickyboy.interviewcodex.service.QuestionBankService;
import com.pickyboy.interviewcodex.service.QuestionService;
import com.pickyboy.interviewcodex.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 题库接口
 *
 * @author pickyboy
 */
@RestController
@RequestMapping("/questionBank")
@Slf4j
public class QuestionBankController {

    @Resource
    private QuestionBankService questionBankService;

    @Resource
    private UserService userService;

   /* @Resource
     private QuestionService questionService;*/

    // region 基础增删改查

    /**
     * 创建题库
     *
     * @param questionBankAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addQuestionBank(@RequestBody QuestionBankAddRequest questionBankAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(questionBankAddRequest == null, ErrorCode.PARAMS_ERROR);
        // 在此处将实体类和 DTO 进行转换
        QuestionBank questionBank = new QuestionBank();
        BeanUtils.copyProperties(questionBankAddRequest, questionBank);
        // 数据校验
        questionBankService.validQuestionBank(questionBank, true);
        // 填充默认值
        User loginUser = userService.getLoginUser(request);
        questionBank.setUserId(loginUser.getId());
        // 写入数据库
        boolean result = questionBankService.save(questionBank);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        // 返回新写入的数据 id
        long newQuestionBankId = questionBank.getId();
        return ResultUtils.success(newQuestionBankId);
    }



    /**
     * 更新题库（仅管理员可用）
     *
     * @param questionBankUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateQuestionBank(@RequestBody QuestionBankUpdateRequest questionBankUpdateRequest) {
        if (questionBankUpdateRequest == null || questionBankUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //  在此处将实体类和 DTO 进行转换
        QuestionBank questionBank = new QuestionBank();
        BeanUtils.copyProperties(questionBankUpdateRequest, questionBank);
        // 数据校验
        questionBankService.validQuestionBank(questionBank, false);
        // 判断是否存在
        long id = questionBankUpdateRequest.getId();
        QuestionBank oldQuestionBank = questionBankService.getById(id);
        ThrowUtils.throwIf(oldQuestionBank == null, ErrorCode.NOT_FOUND_ERROR);
        // 操作数据库
        boolean result = questionBankService.updateQuestionBankWithCache(questionBank);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }


    /**
     * 编辑题库
     *
     * @param questionBankEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> editQuestionBank(@RequestBody QuestionBankEditRequest questionBankEditRequest, HttpServletRequest request) {
        if (questionBankEditRequest == null || questionBankEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //  在此处将实体类和 DTO 进行转换
        QuestionBank questionBank = new QuestionBank();
        BeanUtils.copyProperties(questionBankEditRequest, questionBank);
        // 数据校验
        questionBankService.validQuestionBank(questionBank, false);
        User loginUser = userService.getLoginUser(request);
        // 判断是否存在
        long id = questionBankEditRequest.getId();
        QuestionBank oldQuestionBank = questionBankService.getById(id);
        ThrowUtils.throwIf(oldQuestionBank == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldQuestionBank.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 操作数据库
        boolean result = questionBankService.updateQuestionBankWithCache(questionBank);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 分页获取当前登录用户创建的题库列表
     *
     * @param questionBankQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<QuestionBankVO>> listMyQuestionBankVOByPage(@RequestBody QuestionBankQueryRequest questionBankQueryRequest,
                                                                         HttpServletRequest request) {
        ThrowUtils.throwIf(questionBankQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 补充查询条件，只查询当前登录用户的数据
        User loginUser = userService.getLoginUser(request);
        questionBankQueryRequest.setUserId(loginUser.getId());
        long current = questionBankQueryRequest.getCurrent();
        long size = questionBankQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Page<QuestionBank> questionBankPage = questionBankService.page(new Page<>(current, size),
                questionBankService.getQueryWrapper(questionBankQueryRequest));
        // 获取封装类
        return ResultUtils.success(questionBankService.getQuestionBankVOPage(questionBankPage, request));
    }

    // endregion

    /**
     * 删除题库
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    // 需要额外删除题库题目关联关系
    @Autowired
    private QuestionBankQuestionService questionBankQuestionService;
    @PostMapping("/delete")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @Transactional
    public BaseResponse<Boolean> deleteQuestionBank(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        QuestionBank oldQuestionBank = questionBankService.getById(id);
        ThrowUtils.throwIf(oldQuestionBank == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldQuestionBank.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 操作数据库
        boolean result = questionBankService.deleteQuestionBankWithCache(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        // 删除题库关联关系（这个操作不需要缓存清除，因为没有缓存关联表）
        questionBankQuestionService.remove(new LambdaQueryWrapper<QuestionBankQuestion>().eq(QuestionBankQuestion::getQuestionBankId, id));
        return ResultUtils.success(true);
    }
    /**
     * 根据 id 获取题库详情（封装类）
     *
     * @param questionBankQueryRequest
     * @return
     */
    // 使用JD HotKey实时发现与缓存热门题库
    // todo: 增加限流
    @GetMapping("/get/vo")
    public BaseResponse<QuestionBankVO> getQuestionBankVOById(QuestionBankQueryRequest questionBankQueryRequest, HttpServletRequest request) {
        // 校验参数
        ThrowUtils.throwIf(questionBankQueryRequest == null, ErrorCode.PARAMS_ERROR);
        Long id = questionBankQueryRequest.getId();
        boolean needQueryQuestionList = questionBankQueryRequest.isNeedQueryQuestionList();
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);

        // 原来实现存在bug
        /*
        存在bug,缓存中的题库不一定带有题目,不能根据isNeedQueryQuestionList的情况实际返回
        // 生成key
        String key  = "bank_detail_" + id;
        log.info("开始处理题库详情请求, key: {}", key);

        // 探测key是否为热点，并上报访问事件
        if (JdHotKeyStore.isHotKey(key)) {
            log.info("Hotkey detected, key: {}", key);
            // 尝试从本地缓存读取
            QuestionBankVO questionBankVO = (QuestionBankVO) JdHotKeyStore.get(key);
            if (questionBankVO != null) {
                log.info("Cache hit for hotkey: {}", key);
                // 缓存命中直接返回
                return ResultUtils.success(questionBankVO);
            }
            log.warn("Cache miss for hotkey: {}. This might happen if the value hasn't been set yet after being marked as hot.", key);
        } else {
            log.debug("Key is not hot yet, key: {}", key);
        }

        // 缓存未命中，查询数据库
        log.info("Cache miss, querying database for key: {}", key);
        QuestionBank questionBank = questionBankService.getById(id);
        ThrowUtils.throwIf(questionBank == null, ErrorCode.NOT_FOUND_ERROR);
        QuestionBankVO questionBankVO = questionBankService.getQuestionBankVO(questionBank, request);

        // 查询题目
        if (needQueryQuestionList) {
            log.info("Querying associated questions for question bank id: {}", id);
            QuestionQueryRequest innerQuestionQueryRequest = new QuestionQueryRequest();
            innerQuestionQueryRequest.setQuestionBankId(id);
            Page<Question> questionPage = questionService.listQuestionByPage(innerQuestionQueryRequest);
            questionBankVO.setQuestionPage(questionPage);
        }

        // 智能缓存：如果是热key,则缓存数据
        log.info("Attempting to smartSet cache for key: {}", key);
        JdHotKeyStore.smartSet(key, questionBankVO);
*/

      // --------------------------------------------
      // region bug修正
        // [BUG修复] 将 needQueryQuestionList 参数加入 key 中，避免缓存污染
        // 这样，带题目列表和不带题目列表的请求将使用不同的缓存key


       /* String key = "bank_detail_" + id + "_questions:" + needQueryQuestionList;
        log.info("开始处理题库详情请求, key: {}", key);

        // 探测key是否为热点，并上报访问事件
        if (JdHotKeyStore.isHotKey(key)) {
            log.info("Hotkey detected, key: {}", key);
            // 尝试从本地缓存读取
            QuestionBankVO questionBankVO = (QuestionBankVO) JdHotKeyStore.get(key);
            if (questionBankVO != null) {
                log.info("Cache hit for hotkey: {}", key);
                // 缓存命中直接返回
                return ResultUtils.success(questionBankVO);
            }
            log.warn("Cache miss for hotkey: {}. This might happen if the value hasn't been set yet after being marked as hot.", key);
        } else {
            log.debug("Key is not hot yet, key: {}", key);
        }

        // 缓存未命中，查询数据库
        log.info("Cache miss, querying database for key: {}", key);
        QuestionBank questionBank = questionBankService.getById(id);
        ThrowUtils.throwIf(questionBank == null, ErrorCode.NOT_FOUND_ERROR);
        QuestionBankVO questionBankVO = questionBankService.getQuestionBankVO(questionBank, request);

        // 查询题目
        if (needQueryQuestionList) {
            log.info("Querying associated questions for question bank id: {}", id);
            QuestionQueryRequest innerQuestionQueryRequest = new QuestionQueryRequest();
            innerQuestionQueryRequest.setQuestionBankId(id);
            Page<Question> questionPage = questionService.listQuestionByPage(innerQuestionQueryRequest);
            questionBankVO.setQuestionPage(questionPage);
        }

        // 智能缓存：如果是热key,则缓存数据
        log.info("Attempting to smartSet cache for key: {}", key);
        JdHotKeyStore.smartSet(key, questionBankVO);*/
        // endregion

        // region 缓存充实方案进行修正
        // [备选方案] 保持 Key 唯一，对缓存进行“充实”
        /*String key = "bank_detail_" + id;
        log.info("开始处理题库详情请求 (缓存充实方案), key: {}", key);

        // 探测key是否为热点，并上报访问事件
        if (JdHotKeyStore.isHotKey(key)) {
            log.info("Hotkey detected, key: {}", key);
            // 尝试从本地缓存读取
            QuestionBankVO questionBankVO = (QuestionBankVO) JdHotKeyStore.get(key);
            if (questionBankVO != null) {
                log.info("Cache hit for hotkey: {}", key);

                // [核心逻辑] 检查缓存内容是否满足当前请求的需求
                if (needQueryQuestionList && questionBankVO.getQuestionPage() == null) {
                    log.info("Cached object is incomplete for key: {}. Fetching questions to enrich it.", key);
                    // 缓存中的对象不完整，需要补充题目列表
                    // ⚠️注意：此处存在并发风险！如果多个请求同时到达这里，会多次查询数据库。
                    // 在生产环境中，这里需要添加额外的锁（如 synchronized）来防止惊群效应。
                    QuestionQueryRequest innerQuestionQueryRequest = new QuestionQueryRequest();
                    innerQuestionQueryRequest.setQuestionBankId(id);
                    Page<Question> questionPage = questionService.listQuestionByPage(innerQuestionQueryRequest);
                    questionBankVO.setQuestionPage(questionPage);

                    // 将补充完整的对象重新写回缓存
                    log.info("Enriched cache object for key: {}. Updating cache.", key);
                    JdHotKeyStore.smartSet(key, questionBankVO);
                }

                // 返回（可能已被充实的）缓存对象
                return ResultUtils.success(questionBankVO);
            }
            log.warn("Cache miss for hotkey: {}. This might happen if the value hasn't been set yet after being marked as hot.", key);
        } else {
            log.debug("Key is not hot yet, key: {}", key);
        }

        // 缓存未命中，查询数据库
        log.info("Cache miss, querying database for key: {}", key);
        QuestionBank questionBank = questionBankService.getById(id);
        ThrowUtils.throwIf(questionBank == null, ErrorCode.NOT_FOUND_ERROR);
        QuestionBankVO questionBankVO = questionBankService.getQuestionBankVO(questionBank, request);

        // 查询题目
        if (needQueryQuestionList) {
            log.info("Querying associated questions for question bank id: {}", id);
            QuestionQueryRequest innerQuestionQueryRequest = new QuestionQueryRequest();
            innerQuestionQueryRequest.setQuestionBankId(id);
            Page<Question> questionPage = questionService.listQuestionByPage(innerQuestionQueryRequest);
            questionBankVO.setQuestionPage(questionPage);
        }

        // 智能缓存：如果是热key,则缓存数据
        log.info("Attempting to smartSet cache for key: {}", key);
        JdHotKeyStore.smartSet(key, questionBankVO);*/
        // 返回结果

        // endregion

      //  log.info("Finished processing request for key: {}", key);

        // 使用自动缓存注解
        // 调用带有缓存的 Service 方法
        QuestionBankVO vo = questionBankService.getCachedQuestionBankVO(id, needQueryQuestionList);
        // 封装并返回
        return ResultUtils.success(vo);
    }


    /**
     * 分页获取题库列表（封装类）
     *
     * @param questionBankQueryRequest
     * @param request
     * @return
     */
    // 通过sentinel限流熔断
    @PostMapping("/list/page/vo")
    @SentinelResource(value = "listQuestionBankVOByPage",
    blockHandler = "handleBlockException",
    fallback = "handleFallback")
    public BaseResponse<Page<QuestionBankVO>> listQuestionBankVOByPage(@RequestBody QuestionBankQueryRequest questionBankQueryRequest,
                                                               HttpServletRequest request) {
        long current = questionBankQueryRequest.getCurrent();
        long size = questionBankQueryRequest.getPageSize();
        log.info("{},{}",size,current);
        // 限制爬虫
        ThrowUtils.throwIf(size > 200, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Page<QuestionBank> questionBankPage = questionBankService.page(new Page<>(current, size),
                questionBankService.getQueryWrapper(questionBankQueryRequest));
        // 获取封装类
        return ResultUtils.success(questionBankService.getQuestionBankVOPage(questionBankPage, request));
    }
    // 处理Sentinel本身抛出的异常,包括blockException和fallback
    public BaseResponse<Page<QuestionBankVO>> handleBlockException(QuestionBankQueryRequest questionBankQueryRequest,
                                                                   HttpServletRequest request, BlockException ex) {
        // 降级处理
        if(ex instanceof DegradeException){
            return handleFallback(questionBankQueryRequest, request, ex);
        }
        log.error("listQuestionBankVOByPage被限流了");
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR,"系统压力过大,请耐心等待");
    }

    // 处理业务异常
    public BaseResponse<Page<QuestionBankVO>> handleFallback(QuestionBankQueryRequest questionBankQueryRequest,
                                                                   HttpServletRequest request, Throwable ex) {
        log.error("listQuestionBankVOByPage被降级了");
        return ResultUtils.success(null);
    }




}
