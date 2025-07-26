package com.pickyboy.interviewcodex.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pickyboy.interviewcodex.model.entity.Question;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;

/**
* @author pickyboy
* @description 针对表【question(题目)】的数据库操作Mapper
* @createDate 2025-07-15 15:38:44
* @Entity generator.domain.Question
*/
public interface QuestionMapper extends BaseMapper<Question> {

    @Select("SELECT * from question where updateTime >= #{fiveMinutesAgoDate}")
    List<Question> listQuestionWithDelete(Date fiveMinutesAgoDate);
}




