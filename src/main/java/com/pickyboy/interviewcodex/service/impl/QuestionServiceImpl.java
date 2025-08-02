package com.pickyboy.interviewcodex.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pickyboy.interviewcodex.cache.AutoCache;
import com.pickyboy.interviewcodex.cache.CacheEvict;
import com.pickyboy.interviewcodex.common.ErrorCode;
import com.pickyboy.interviewcodex.constant.CommonConstant;
import com.pickyboy.interviewcodex.exception.BusinessException;
import com.pickyboy.interviewcodex.exception.ThrowUtils;
import com.pickyboy.interviewcodex.mapper.QuestionMapper;
import com.pickyboy.interviewcodex.model.dto.question.QuestionEsDTO;
import com.pickyboy.interviewcodex.model.dto.question.QuestionQueryRequest;
import com.pickyboy.interviewcodex.model.entity.Question;
import com.pickyboy.interviewcodex.model.entity.QuestionBankQuestion;
import com.pickyboy.interviewcodex.model.entity.User;
import com.pickyboy.interviewcodex.model.vo.QuestionVO;
import com.pickyboy.interviewcodex.model.vo.UserVO;
import com.pickyboy.interviewcodex.service.QuestionBankQuestionService;
import com.pickyboy.interviewcodex.service.QuestionService;
import com.pickyboy.interviewcodex.service.UserService;
import com.pickyboy.interviewcodex.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MoreLikeThisQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.completion.CompletionSuggestionBuilder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.suggest.response.CompletionSuggestion;
import org.springframework.data.elasticsearch.core.suggest.response.Suggest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 题目服务实现
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://www.code-nav.cn">编程导航学习圈</a>
 */
@Service
@Slf4j
public class QuestionServiceImpl extends ServiceImpl<QuestionMapper, Question> implements QuestionService {

    @Resource
    private UserService userService;

    @Resource
    private QuestionBankQuestionService questionBankQuestionService;

    @Resource
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    /**
     * 校验数据
     *
     * @param question
     * @param add      对创建的数据进行校验
     */
    @Override
    public void validQuestion(Question question, boolean add) {
        ThrowUtils.throwIf(question == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        String title = question.getTitle();
        // 创建数据时，参数不能为空
        if (add) {
            // 补充校验规则
            ThrowUtils.throwIf(StringUtils.isBlank(title), ErrorCode.PARAMS_ERROR);
        }
        // 修改数据时，有参数则校验
        // 补充校验规则
        if (StringUtils.isNotBlank(title)) {
            ThrowUtils.throwIf(title.length() > 80, ErrorCode.PARAMS_ERROR, "标题过长");
        }
    }

    /**
     * 获取查询条件
     *
     * @param questionQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<Question> getQueryWrapper(QuestionQueryRequest questionQueryRequest) {
        QueryWrapper<Question> queryWrapper = new QueryWrapper<>();
        if (questionQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = questionQueryRequest.getId();
        Long notId = questionQueryRequest.getNotId();
        String title = questionQueryRequest.getTitle();
        String content = questionQueryRequest.getContent();
        String searchText = questionQueryRequest.getSearchText();
        String sortField = questionQueryRequest.getSortField();
        String sortOrder = questionQueryRequest.getSortOrder();
        List<String> tagList = questionQueryRequest.getTags();
        Long userId = questionQueryRequest.getUserId();
        String anwser = questionQueryRequest.getAnswer();
        // 补充需要的查询条件
        // 从多字段中搜索
        if (StringUtils.isNotBlank(searchText)) {
            // 需要拼接查询条件
            queryWrapper.and(qw -> qw.like("title", searchText).or().like("content", searchText));
        }
        // 模糊查询
        queryWrapper.like(StringUtils.isNotBlank(title), "title", title);
        queryWrapper.like(StringUtils.isNotBlank(content), "content", content);
        queryWrapper.like(StringUtils.isNotBlank(anwser), "answer", anwser);
        // JSON 数组查询
        if (CollUtil.isNotEmpty(tagList)) {
            for (String tag : tagList) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        // 精确查询
        queryWrapper.ne(ObjectUtils.isNotEmpty(notId), "id", notId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        // 排序规则

        if(StringUtils.equals(sortField,"error")){
            sortField = null;
        }
        queryWrapper.orderBy(SqlUtils.validSortField(sortField),
                sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    /**
     * 获取题目封装
     *
     * @param question
     * @param request
     * @return
     */
    @Override
    public QuestionVO getQuestionVO(Question question, HttpServletRequest request) {
        // 对象转封装类
        QuestionVO questionVO = QuestionVO.objToVo(question);

        //  可以根据需要为封装对象补充值，不需要的内容可以删除
        // region 可选
        // 1. 关联查询用户信息
        Long userId = question.getUserId();
        User user = null;
        if (userId != null && userId > 0) {
            user = userService.getById(userId);
        }
        UserVO userVO = userService.getUserVO(user);
        questionVO.setUser(userVO);

        // endregion

        return questionVO;
    }

    /**
     * 分页获取题目封装
     *
     * @param questionPage
     * @param request
     * @return
     */
    @Override
    public Page<QuestionVO> getQuestionVOPage(Page<Question> questionPage, HttpServletRequest request) {
        List<Question> questionList = questionPage.getRecords();
        Page<QuestionVO> questionVOPage = new Page<>(questionPage.getCurrent(), questionPage.getSize(), questionPage.getTotal());
        if (CollUtil.isEmpty(questionList)) {
            return questionVOPage;
        }
        // 对象列表 => 封装对象列表
        List<QuestionVO> questionVOList = questionList.stream().map(question -> {
            return QuestionVO.objToVo(question);
        }).collect(Collectors.toList());

        //  可以根据需要为封装对象补充值，不需要的内容可以删除
        // region 可选
        // 1. 关联查询用户信息
        Set<Long> userIdSet = questionList.stream().map(Question::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));

        // 填充信息
        questionVOList.forEach(questionVO -> {
            Long userId = questionVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            questionVO.setUser(userService.getUserVO(user));
        });
        // endregion

        questionVOPage.setRecords(questionVOList);
        return questionVOPage;
    }

    /**
     * 分页获取题目列表（仅管理员可用）
     *
     * @param questionQueryRequest
     * @return
     */
    public Page<Question> listQuestionByPage(QuestionQueryRequest questionQueryRequest) {
        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();

        QueryWrapper<Question> queryWrapper = this.getQueryWrapper(questionQueryRequest);

        // 根据题库id查询题目列表接口
        Long questionBankId = questionQueryRequest.getQuestionBankId();
        if (questionBankId != null) {
            List<Long> questionIds = questionBankQuestionService
                    .listObjs(new LambdaQueryWrapper<QuestionBankQuestion>()
                            .select(QuestionBankQuestion::getQuestionId)
                            .eq(QuestionBankQuestion::getQuestionBankId, questionBankId),obj->Long.parseLong(obj.toString()));
            if(CollUtil.isEmpty(questionIds)){
                return new Page<>(current,size);
            }

            if(CollUtil.isNotEmpty(questionIds)) {
                queryWrapper.in("id", questionIds);
            }
        }
        // 查询数据库
        Page<Question> questionPage = this.page(new Page<>(current, size),
                queryWrapper);
        return questionPage;
    }


    /**
     * 从数据库分页搜索
     * @param questionQueryRequest
     * @return
     */
    private Page<Question> searchFromDb(QuestionQueryRequest questionQueryRequest) {
        QueryWrapper<Question> wrapper = getQueryWrapper(questionQueryRequest);
        Page<Question> page = page(new Page<>(questionQueryRequest.getCurrent(), questionQueryRequest.getPageSize()
        ),wrapper);
        return page;
    }

    /**
     * 通过ES搜索题目列表
     * @param questionQueryRequest
     * @return
     */
    @Override
    @SentinelResource(
            value = "searchFromEs", // 资源名，在 Sentinel 控制台配置规则时使用
            blockHandler = "handleSearchBlock", // 指定流控降级后的处理方法
            fallback = "handleSearchFallback"  // 指定熔断降级后的处理方法
    )
    public Page<Question> searchFromEs(QuestionQueryRequest questionQueryRequest) {

            Long id = questionQueryRequest.getId();
            Long notId = questionQueryRequest.getNotId();
            String searchText = questionQueryRequest.getSearchText();
            if(searchText.equals("error")){
                throw new BusinessException(123,"测试Es错误降级");
            }
            Long questionBankId = questionQueryRequest.getQuestionBankId();
            List<String> tagList = questionQueryRequest.getTags();
            Long userId = questionQueryRequest.getUserId();
            // es 起始页为 0
            long current = questionQueryRequest.getCurrent() - 1;
            long pageSize = questionQueryRequest.getPageSize();
            String sortField = questionQueryRequest.getSortField();
            String sortOrder = questionQueryRequest.getSortOrder();
            // 构造查询条件
            BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
            // 过滤
            boolQueryBuilder.filter(QueryBuilders.termQuery("isDelete", 0));
            if (id != null) {
                boolQueryBuilder.filter(QueryBuilders.termQuery("id", id));
            }
            if (notId != null) {
                boolQueryBuilder.mustNot(QueryBuilders.termQuery("id", notId));
            }
            if (userId != null) {
                boolQueryBuilder.filter(QueryBuilders.termQuery("userId", userId));
            }
            if(questionBankId!= null){
                boolQueryBuilder.filter(QueryBuilders.termQuery("questionBankId", questionBankId));
            }
            // 必须包含所有标签
            if (CollUtil.isNotEmpty(tagList)) {
                for (String tag : tagList) {
                    boolQueryBuilder.filter(QueryBuilders.termQuery("tags", tag));
                }
            }

            // 按关键词检索
            if (StringUtils.isNotBlank(searchText)) {
                boolQueryBuilder.should(QueryBuilders.matchQuery("title", searchText));
                boolQueryBuilder.should(QueryBuilders.matchQuery("description", searchText));
                boolQueryBuilder.should(QueryBuilders.matchQuery("content", searchText));
                // 满足一个即可
                boolQueryBuilder.minimumShouldMatch(1);
            }

            // 排序
            SortBuilder<?> sortBuilder = SortBuilders.scoreSort();
            if (StringUtils.isNotBlank(sortField)) {
                sortBuilder = SortBuilders.fieldSort(sortField);
                sortBuilder.order(CommonConstant.SORT_ORDER_ASC.equals(sortOrder) ? SortOrder.ASC : SortOrder.DESC);
            }
            // 分页
            PageRequest pageRequest = PageRequest.of((int) current, (int) pageSize);

            // 构造查询
            NativeSearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(boolQueryBuilder)
                    .withPageable(pageRequest).withSorts(sortBuilder).build();
            // 查询
               SearchHits<QuestionEsDTO> searchHits = elasticsearchRestTemplate.search(searchQuery, QuestionEsDTO.class);

               Page<Question> page = new Page<>();
               page.setTotal(searchHits.getTotalHits());
               List<Question> resourceList = new ArrayList<>();
               // 构造返回结果
               if (searchHits.hasSearchHits()) {
                   List<SearchHit<QuestionEsDTO>> searchHitList = searchHits.getSearchHits();
                   searchHitList.forEach(searchHit -> {
                       resourceList.add(QuestionEsDTO.dtoToObj(searchHit.getContent()));
                   });
               }
               page.setRecords(resourceList);
               return page;

    }

    /**
     * Sentinel blockHandler 方法
     * 当 searchFromEs 资源被流控或熔断时调用此方法。
     * @param request
     * @param ex
     * @return
     */
    public Page<Question> handleSearchBlock(QuestionQueryRequest request, BlockException ex) {
        log.warn("Sentinel blocked the request for searchFromEs. Falling back.", ex);
        // Case 1: 如果是流控异常 (FlowException)，说明系统访问量过大
        // 策略：快速失败，直接返回“系统繁忙”提示，不降级到数据库，避免压力传导。
        if (ex instanceof FlowException) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST);
        }

        // Case 2: 如果是熔断降级异常 (DegradeException)，说明 ES 服务本身出现问题
        // 策略：执行降级逻辑，尝试从数据库查询。
        if (ex instanceof DegradeException) {
            return finalFallback(request);
        }

        // 其他 BlockException (如系统负载保护、权限不通过等)，默认也进行快速失败处理
        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "请求被限制，请稍后重试");
    }

    /**
     * Sentinel fallback 方法
     * 当 searchFromEs 方法内部抛出任何未被捕获的异常时调用此方法。
     * @param request
     * @param t
     * @return
     */
    public Page<Question> handleSearchFallback(QuestionQueryRequest request, Throwable t) {
        log.error("Exception in searchFromEs, Sentinel fallback triggered.", t);
        // 查询ES出异常统一走最终降级逻辑
        return finalFallback(request);
    }

    /**
     * 统一的、健壮的降级方法。
     * 它首先尝试从数据库查询，如果数据库也失败，则返回一个安全的空结果。
     * @param request
     * @return
     */
    private Page<Question> finalFallback(QuestionQueryRequest request) {
        try {
            log.info("Executing fallback: trying to search from database.");
            return searchFromDb(request);
        } catch (Exception dbException) {
            log.error("Database search also failed during fallback. Returning empty result.", dbException);
            // 最终兜底策略：返回一个空的分页对象，保证接口不会异常，对前端友好。
            Page<Question> emptyPage = new Page<>(request.getCurrent(), request.getPageSize(), 0);
            emptyPage.setRecords(Collections.emptyList());
            return emptyPage;
        }
    }


    @Override
    public List<QuestionVO> getRecommendations(long id) {
        // 1. 定义用于相似度计算的字段
        // 我们希望根据题目的标题、内容和标签来查找相似项
        // String[] fields = {"title", "content", "tags"};
        // 关键优化：根据反馈，移除 content 字段，仅根据题目的标题和标签来查找相似项，以提高推荐的精准度
        String[] fields = {"title", "tags"};

        // 2. 创建 MoreLikeThisQueryBuilder
        MoreLikeThisQueryBuilder moreLikeThisQueryBuilder = QueryBuilders.moreLikeThisQuery(
                        fields, // 用于比较的字段
                        null,   // likeTexts, 这里我们不用原始文本，而是用已存在的文档
                        new MoreLikeThisQueryBuilder.Item[]{
                                // 指定示例文档，index为别名"question"，id为当前题目ID
                                new MoreLikeThisQueryBuilder.Item("question", String.valueOf(id))
                        }
                )
                .minTermFreq(1) // 词条在示例文档中至少出现1次
                .minDocFreq(1)  // 词条在索引中至少出现在1个文档中
                .maxQueryTerms(12); // 最多使用12个词条来生成查询

        // 3. 构建布尔查询，组合 MLT 查询并排除自身
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery()
                .must(moreLikeThisQueryBuilder) // 必须满足 MLT 的相似度条件
                .mustNot(QueryBuilders.termQuery("id", id)); // 必须不是当前题目自身

        // 4. 构建最终的 NativeSearchQuery
        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(boolQueryBuilder)
                .withPageable(org.springframework.data.domain.PageRequest.of(0, 10)) // 获取前10条推荐
                .build();

        // 5. 执行查询
        SearchHits<QuestionEsDTO> searchHits = elasticsearchRestTemplate.search(searchQuery, QuestionEsDTO.class);

        // 6. 处理并返回结果
        // 此处为了简化，直接返回了 Question 对象，您可以按需转换为 QuestionVO
        return searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(QuestionEsDTO::dtoToObj)
                .map(
                    QuestionVO::objToVo
                ).collect(Collectors.toList());
    }

    @Override
    @AutoCache(scene = "question_detail",keyExpression = "#id")
    public QuestionVO getCacheQuestionVO(long id) {
// 1. 核心数据查询
        Question question = this.getById(id);
        ThrowUtils.throwIf(question == null, ErrorCode.NOT_FOUND_ERROR);

        // 2. 转换为 VO 并填充关联信息
        QuestionVO questionVO = QuestionVO.objToVo(question);
        User user = userService.getById(question.getUserId());
        questionVO.setUser(userService.getUserVO(user));

        return questionVO;
    }

    @Override
    @CacheEvict(scene = "question_detail", keyExpression = "#id")
    public boolean deleteQuestionWithCache(Long id) {
        return this.removeById(id);
    }

    @Override
    @CacheEvict(scene = "question_detail", keyExpression = "#question.id")
    public boolean updateQuestionWithCache(Question question) {
        return this.updateById(question);
    }

    @Override
    @CacheEvict(scene = "question_detail", keyExpression = "#questionIdList", isBatch = true)
    public void batchDeleteQuestionsWithCache(List<Long> questionIdList) {
        this.batchDeleteQuestions(questionIdList);
    }

    /*
    获取搜索建议
     */
    @Override
    public List<String> getSuggestions(String prefix) {
        String suggestionName = "question_title_suggestion";

        // 注意：这里查询的字段名是 DTO 中的字段名 "titleSuggest"
        CompletionSuggestionBuilder suggestionBuilder = new CompletionSuggestionBuilder("titleSuggest")
                .prefix(prefix)
                .size(10);

        SuggestBuilder suggestBuilder = new SuggestBuilder().addSuggestion(suggestionName, suggestionBuilder);

        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withSuggestBuilder(suggestBuilder)
                .build();

        SearchHits<QuestionEsDTO> searchHits = elasticsearchRestTemplate.search(searchQuery, QuestionEsDTO.class);

        List<String> suggestionList = new ArrayList<>();
        Suggest suggest = searchHits.getSuggest();
        if (suggest != null) {
            // 关键改动：直接处理返回的原始 Suggest.Suggestion 对象
            Suggest.Suggestion<? extends Suggest.Suggestion.Entry<? extends Suggest.Suggestion.Entry.Option>> suggestionResult =
                    suggest.getSuggestion(suggestionName);
            if (suggestionResult != null) {
                for (Suggest.Suggestion.Entry<? extends Suggest.Suggestion.Entry.Option> entry : suggestionResult.getEntries()) {
                    for (Suggest.Suggestion.Entry.Option option : entry.getOptions()) {
                        suggestionList.add(option.getText());
                    }
                }
            }
        }
        return suggestionList;
    }

    /**
     * 批量删除题目
     * @param questionIdList
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(scene = "question_detail", keyExpression = "#questionIdList", isBatch = true)
    public void batchDeleteQuestions(List<Long> questionIdList) {
        ThrowUtils.throwIf(CollUtil.isEmpty(questionIdList), ErrorCode.PARAMS_ERROR,"题目列表不能为空");
        boolean result = this.removeBatchByIds(questionIdList);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "批量删除题目失败");
        // 移除关联关系
        questionBankQuestionService.remove(new LambdaQueryWrapper<QuestionBankQuestion>()
                .in(QuestionBankQuestion::getQuestionId, questionIdList));
    }

}
