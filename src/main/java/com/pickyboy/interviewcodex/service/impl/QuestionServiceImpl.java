package com.pickyboy.interviewcodex.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pickyboy.interviewcodex.annotation.AuthCheck;
import com.pickyboy.interviewcodex.common.BaseResponse;
import com.pickyboy.interviewcodex.common.ErrorCode;
import com.pickyboy.interviewcodex.common.ResultUtils;
import com.pickyboy.interviewcodex.constant.CommonConstant;
import com.pickyboy.interviewcodex.constant.UserConstant;
import com.pickyboy.interviewcodex.exception.ThrowUtils;
import com.pickyboy.interviewcodex.mapper.QuestionMapper;
import com.pickyboy.interviewcodex.model.dto.post.PostQueryRequest;
import com.pickyboy.interviewcodex.model.dto.question.QuestionEsDTO;
import com.pickyboy.interviewcodex.model.dto.question.QuestionQueryRequest;
import com.pickyboy.interviewcodex.model.dto.question.QuestionQueryRequest;
import com.pickyboy.interviewcodex.model.entity.Question;
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
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;

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
        // todo 从对象中取值
        String title = question.getTitle();
        // 创建数据时，参数不能为空
        if (add) {
            // todo 补充校验规则
            ThrowUtils.throwIf(StringUtils.isBlank(title), ErrorCode.PARAMS_ERROR);
        }
        // 修改数据时，有参数则校验
        // todo 补充校验规则
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
        // todo 从对象中取值
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
        // todo 补充需要的查询条件
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

        // todo 可以根据需要为封装对象补充值，不需要的内容可以删除
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

        // todo 可以根据需要为封装对象补充值，不需要的内容可以删除
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


    @Override
    public Page<Question> searchFromEs(QuestionQueryRequest questionQueryRequest) {
            Long id = questionQueryRequest.getId();
            Long notId = questionQueryRequest.getNotId();
            String searchText = questionQueryRequest.getSearchText();
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
     * 批量删除题目
     * @param questionIdList
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteQuestions(List<Long> questionIdList) {
        ThrowUtils.throwIf(CollUtil.isEmpty(questionIdList), ErrorCode.PARAMS_ERROR,"题目列表不能为空");
        boolean result = this.removeBatchByIds(questionIdList);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "批量删除题目失败");
        // 移除关联关系
        questionBankQuestionService.remove(new LambdaQueryWrapper<QuestionBankQuestion>()
                .in(QuestionBankQuestion::getQuestionId, questionIdList));
    }
}
