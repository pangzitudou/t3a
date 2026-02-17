package com.t3a.core.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.t3a.common.domain.Result;
import com.t3a.core.domain.entity.QuestionBank;
import com.t3a.core.service.QuestionBankService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 题库管理接口
 */
@Slf4j
@RestController
@RequestMapping("/bank")
@RequiredArgsConstructor
@Tag(name = "Question Bank", description = "题库管理接口")
public class QuestionBankController {

    private final QuestionBankService bankService;

    @Operation(summary = "创建题库")
    @PostMapping("/create")
    public Result<QuestionBank> createBank(@RequestBody QuestionBank bank) {
        log.info("创建题库: {}", bank);
        QuestionBank created = bankService.createBank(bank);
        return Result.success("题库创建成功", created);
    }

    @Operation(summary = "获取题库详情")
    @GetMapping("/{id}")
    public Result<QuestionBank> getBank(@PathVariable Long id) {
        QuestionBank bank = bankService.getById(id);
        if (bank == null) {
            return Result.error("题库不存在");
        }
        return Result.success(bank);
    }

    @Operation(summary = "分页查询题库列表")
    @GetMapping("/list")
    public Result<Page<QuestionBank>> listBanks(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long creatorId,
            @RequestParam(required = false) String category) {

        Page<QuestionBank> page = bankService.listBanks(pageNum, pageSize, creatorId, category);
        return Result.success(page);
    }

    @Operation(summary = "获取公开题库列表")
    @GetMapping("/public")
    public Result<List<QuestionBank>> listPublicBanks() {
        List<QuestionBank> banks = bankService.listPublicBanks();
        return Result.success(banks);
    }

    @Operation(summary = "更新题库")
    @PutMapping("/update")
    public Result<Void> updateBank(@RequestBody QuestionBank bank) {
        log.info("更新题库: {}", bank);
        bankService.updateBank(bank);
        return Result.success("题库更新成功", null);
    }

    @Operation(summary = "删除题库")
    @DeleteMapping("/{id}")
    public Result<Void> deleteBank(@PathVariable Long id) {
        log.info("删除题库: id={}", id);
        bankService.deleteBank(id);
        return Result.success("题库删除成功", null);
    }
}
