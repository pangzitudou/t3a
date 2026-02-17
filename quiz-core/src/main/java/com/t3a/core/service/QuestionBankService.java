package com.t3a.core.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.t3a.core.domain.entity.QuestionBank;
import com.t3a.core.mapper.QuestionBankMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 题库服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionBankService {

    private final QuestionBankMapper questionBankMapper;
    private final QuestionService questionService;

    /**
     * 创建题库
     */
    @Transactional(rollbackFor = Exception.class)
    public QuestionBank createBank(QuestionBank bank) {
        log.info("创建题库: name={}", bank.getName());
        bank.setDeleted(0);
        questionBankMapper.insert(bank);
        return bank;
    }

    /**
     * 根据ID查询题库
     */
    public QuestionBank getById(Long id) {
        QuestionBank bank = questionBankMapper.selectOne(
                new LambdaQueryWrapper<QuestionBank>()
                        .eq(QuestionBank::getId, id)
                        .eq(QuestionBank::getDeleted, 0)
        );
        if (bank != null) {
            bank.setQuestionCount(questionService.countByBankId(bank.getId()));
        }
        return bank;
    }

    /**
     * 分页查询题库列表
     */
    public Page<QuestionBank> listBanks(int pageNum, int pageSize, Long creatorId, String category) {
        LambdaQueryWrapper<QuestionBank> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(QuestionBank::getDeleted, 0);

        if (creatorId != null) {
            wrapper.eq(QuestionBank::getCreatorId, creatorId);
        }

        if (category != null && !category.isEmpty()) {
            wrapper.eq(QuestionBank::getCategory, category);
        }

        wrapper.orderByDesc(QuestionBank::getCreateTime);

        Page<QuestionBank> page = new Page<>(pageNum, pageSize);
        Page<QuestionBank> resultPage = questionBankMapper.selectPage(page, wrapper);
        resultPage.getRecords().forEach(bank -> bank.setQuestionCount(questionService.countByBankId(bank.getId())));
        return resultPage;
    }

    /**
     * 查询公开题库
     */
    public List<QuestionBank> listPublicBanks() {
        List<QuestionBank> banks = questionBankMapper.selectList(
                new LambdaQueryWrapper<QuestionBank>()
                        .eq(QuestionBank::getIsPublic, true)
                        .eq(QuestionBank::getDeleted, 0)
                        .orderByDesc(QuestionBank::getCreateTime)
        );
        banks.forEach(bank -> bank.setQuestionCount(questionService.countByBankId(bank.getId())));
        return banks;
    }

    /**
     * 更新题库
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateBank(QuestionBank bank) {
        log.info("更新题库: id={}", bank.getId());
        questionBankMapper.updateById(bank);
    }

    /**
     * 删除题库（逻辑删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteBank(Long id) {
        log.info("删除题库: id={}", id);
        // Leverage MyBatis-Plus logic delete to avoid generating empty UPDATE SET SQL.
        questionBankMapper.deleteById(id);
    }
}
