package com.t3a.core.controller;

import com.t3a.common.domain.Result;
import com.t3a.core.domain.entity.Question;
import com.t3a.core.service.QuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 题目管理接口
 */
@Slf4j
@RestController
@RequestMapping("/question")
@RequiredArgsConstructor
@Tag(name = "Question Management", description = "题目管理接口")
public class QuestionController {

    private final QuestionService questionService;

    @Operation(summary = "获取题库的所有题目")
    @GetMapping("/list")
    public Result<List<Question>> listQuestions(@RequestParam Long bankId) {
        log.info("获取题目列表: bankId={}", bankId);
        List<Question> questions = questionService.listByBankId(bankId);
        return Result.success(questions);
    }

    @Operation(summary = "创建题目")
    @PostMapping("/create")
    public Result<Question> createQuestion(@RequestBody Question question) {
        log.info("创建题目: {}", question);
        Question created = questionService.createQuestion(question);
        return Result.success("题目创建成功", created);
    }

    @Operation(summary = "批量创建题目")
    @PostMapping("/batch")
    public Result<Void> batchCreateQuestions(@RequestBody List<Question> questions) {
        log.info("批量创建题目: count={}", questions.size());
        questionService.batchCreate(questions);
        return Result.success("批量创建成功", null);
    }

    @Operation(summary = "随机获取测验题目")
    @GetMapping("/random")
    public Result<List<Question>> getRandomQuestions(
            @RequestParam Long bankId,
            @RequestParam(defaultValue = "10") Integer count) {
        log.info("随机获取题目: bankId={}, count={}", bankId, count);
        List<Question> questions = questionService.getRandomQuestions(bankId, count);
        return Result.success(questions);
    }

    @Operation(summary = "获取题目详情")
    @GetMapping("/{id}")
    public Result<Question> getQuestion(@PathVariable Long id) {
        Question question = questionService.getById(id);
        if (question == null) {
            return Result.error("题目不存在");
        }
        return Result.success(question);
    }

    @Operation(summary = "更新题目")
    @PutMapping("/update")
    public Result<Void> updateQuestion(@RequestBody Question question) {
        log.info("更新题目: {}", question);
        questionService.updateQuestion(question);
        return Result.success("题目更新成功", null);
    }

    @Operation(summary = "删除题目")
    @DeleteMapping("/{id}")
    public Result<Void> deleteQuestion(@PathVariable Long id) {
        log.info("删除题目: id={}", id);
        questionService.deleteQuestion(id);
        return Result.success("题目删除成功", null);
    }
}
