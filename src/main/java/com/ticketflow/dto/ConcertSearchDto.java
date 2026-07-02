package com.ticketflow.dto;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.CompletionField;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.core.suggest.Completion;

// ConcertSearchDto.java
@Getter
@Builder
@Document(indexName = "concerts")
public class ConcertSearchDto {
    @Id
    private String concertId;

    @Field(type = FieldType.Text)
    private String concertName;

    // JsonIgnore를 제거합니다!
    @CompletionField
    private Completion suggest;
}