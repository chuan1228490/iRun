package com.ikeu.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.io.Serializable;
import java.util.List;

@Data
@Schema(description = "修改评价请求DTO")
public class ReviewUpdateDTO implements Serializable {
    @Min(1) @Max(5)
    private Integer rating;

    private String content;
    private List<String> tags;
}