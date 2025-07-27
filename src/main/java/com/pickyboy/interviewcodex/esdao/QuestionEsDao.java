package com.pickyboy.interviewcodex.esdao;


import com.pickyboy.interviewcodex.model.dto.question.QuestionEsDTO;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface QuestionEsDao extends ElasticsearchRepository<QuestionEsDTO, Long> {
}
