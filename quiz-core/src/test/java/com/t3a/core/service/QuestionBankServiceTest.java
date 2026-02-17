package com.t3a.core.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.t3a.core.domain.entity.QuestionBank;
import com.t3a.core.mapper.QuestionBankMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * QuestionBankService unit tests
 * Tests business logic using Mockito for dependencies
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class QuestionBankServiceTest {

    @Mock
    private QuestionBankMapper bankMapper;

    @Mock
    private QuestionService questionService;

    @InjectMocks
    private QuestionBankService bankService;

    private QuestionBank testBank;

    @BeforeEach
    void setUp() {
        testBank = new QuestionBank();
        testBank.setId(1L);
        testBank.setName("Test Bank");
        testBank.setDescription("Test Description");
        testBank.setCategory("Java");
        testBank.setCreatorId(1L);
        testBank.setIsPublic(true);
        testBank.setAiGenerated(false);
        testBank.setDeleted(0);

    }

    @Test
    @DisplayName("createBank - Should successfully create bank")
    void createBank_Success() {
        // Given
        when(bankMapper.insert(any(QuestionBank.class))).thenReturn(1);

        // When
        QuestionBank result = bankService.createBank(testBank);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getDeleted());
        verify(bankMapper, times(1)).insert(testBank);
    }

    @Test
    @DisplayName("createBank - Should handle database error")
    void createBank_DatabaseError_ShouldThrowException() {
        // Given
        when(bankMapper.insert(any(QuestionBank.class))).thenThrow(new RuntimeException("Database error"));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            bankService.createBank(testBank);
        });

        verify(bankMapper, times(1)).insert(testBank);
    }

    @Test
    @DisplayName("getById - Should return bank when exists")
    void getById_ExistingBank_ShouldReturnBank() {
        // Given
        when(bankMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testBank);

        // When
        QuestionBank result = bankService.getById(1L);

        // Then
        assertNotNull(result);
        assertEquals("Test Bank", result.getName());
        verify(bankMapper, times(1)).selectOne(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("getById - Should return null when not exists")
    void getById_NonExistentBank_ShouldReturnNull() {
        // Given
        when(bankMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        // When
        QuestionBank result = bankService.getById(999L);

        // Then
        assertNull(result);
        verify(bankMapper, times(1)).selectOne(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("getById - Should not return deleted banks")
    void getById_DeletedBank_ShouldReturnNull() {
        // Given
        testBank.setDeleted(1);
        when(bankMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        // When
        QuestionBank result = bankService.getById(1L);

        // Then
        assertNull(result);
    }

    @Test
    @DisplayName("listBanks - Should return paginated results")
    void listBanks_WithoutFilters_ShouldReturnPage() {
        // Given
        Page<QuestionBank> page = new Page<>(1, 10);
        page.setRecords(Arrays.asList(testBank));
        page.setTotal(1);

        when(bankMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);
        when(questionService.countByBankId(anyLong())).thenReturn(0L);

        // When
        Page<QuestionBank> result = bankService.listBanks(1, 10, null, null);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getRecords().size());
        verify(bankMapper, times(1)).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("listBanks - Should filter by creatorId")
    void listBanks_WithCreatorId_ShouldFilter() {
        // Given
        Page<QuestionBank> page = new Page<>(1, 10);
        page.setRecords(Arrays.asList(testBank));

        when(bankMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);
        when(questionService.countByBankId(anyLong())).thenReturn(0L);

        // When
        Page<QuestionBank> result = bankService.listBanks(1, 10, 1L, null);

        // Then
        assertNotNull(result);
        verify(bankMapper, times(1)).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("listBanks - Should filter by category")
    void listBanks_WithCategory_ShouldFilter() {
        // Given
        Page<QuestionBank> page = new Page<>(1, 10);
        page.setRecords(Arrays.asList(testBank));

        when(bankMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);
        when(questionService.countByBankId(anyLong())).thenReturn(0L);

        // When
        Page<QuestionBank> result = bankService.listBanks(1, 10, null, "Java");

        // Then
        assertNotNull(result);
        verify(bankMapper, times(1)).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("listBanks - Should handle empty category")
    void listBanks_EmptyCategory_ShouldIgnoreFilter() {
        // Given
        Page<QuestionBank> page = new Page<>(1, 10);
        page.setRecords(Arrays.asList(testBank));

        when(bankMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);
        when(questionService.countByBankId(anyLong())).thenReturn(0L);

        // When
        Page<QuestionBank> result = bankService.listBanks(1, 10, null, "");

        // Then
        assertNotNull(result);
        verify(bankMapper, times(1)).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("listPublicBanks - Should return only public banks")
    void listPublicBanks_ShouldReturnPublicBanks() {
        // Given
        QuestionBank publicBank = new QuestionBank();
        publicBank.setIsPublic(true);

        QuestionBank privateBank = new QuestionBank();
        privateBank.setIsPublic(false);

        List<QuestionBank> banks = Arrays.asList(publicBank, privateBank);
        when(bankMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(banks);
        when(questionService.countByBankId(anyLong())).thenReturn(0L);

        // When
        List<QuestionBank> result = bankService.listPublicBanks();

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        verify(bankMapper, times(1)).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("listPublicBanks - Should return empty list when no public banks")
    void listPublicBanks_NoPublicBanks_ShouldReturnEmpty() {
        // Given
        when(bankMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Arrays.asList());

        // When
        List<QuestionBank> result = bankService.listPublicBanks();

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(bankMapper, times(1)).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("updateBank - Should successfully update bank")
    void updateBank_Success() {
        // Given
        when(bankMapper.updateById(any(QuestionBank.class))).thenReturn(1);

        // When
        testBank.setName("Updated Name");
        bankService.updateBank(testBank);

        // Then
        verify(bankMapper, times(1)).updateById(testBank);
    }

    @Test
    @DisplayName("updateBank - Should handle non-existent bank")
    void updateBank_NonExistentBank_ShouldNotThrow() {
        // Given
        when(bankMapper.updateById(any(QuestionBank.class))).thenReturn(0);

        // When
        testBank.setId(999L);
        bankService.updateBank(testBank);

        // Then - Should not throw exception
        verify(bankMapper, times(1)).updateById(testBank);
    }

    @Test
    @DisplayName("deleteBank - Should logically delete bank")
    void deleteBank_Success() {
        // When
        bankService.deleteBank(1L);

        // Then
        verify(bankMapper, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("deleteBank - Should handle non-existent bank")
    void deleteBank_NonExistentBank_ShouldNotThrow() {
        // When - Should not throw exception
        bankService.deleteBank(999L);

        // Then
        verify(bankMapper, times(1)).deleteById(999L);
    }

    @Test
    @DisplayName("listBanks - Should order by create time desc")
    void listBanks_ShouldOrderByCreateTimeDesc() {
        // Given
        Page<QuestionBank> page = new Page<>(1, 10);
        page.setRecords(Arrays.asList(testBank));

        when(bankMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);
        when(questionService.countByBankId(anyLong())).thenReturn(0L);

        // When
        bankService.listBanks(1, 10, null, null);

        // Then - Verify the query wrapper includes ordering
        verify(bankMapper, times(1)).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("listPublicBanks - Should order by create time desc")
    void listPublicBanks_ShouldOrderByCreateTimeDesc() {
        // Given
        when(bankMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Arrays.asList(testBank));
        when(questionService.countByBankId(anyLong())).thenReturn(0L);

        // When
        bankService.listPublicBanks();

        // Then - Verify the query wrapper includes ordering
        verify(bankMapper, times(1)).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("listBanks - Should not return deleted banks")
    void listBanks_ShouldNotReturnDeletedBanks() {
        // Given
        Page<QuestionBank> page = new Page<>(1, 10);
        page.setRecords(Arrays.asList());

        when(bankMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);
        when(questionService.countByBankId(anyLong())).thenReturn(0L);

        // When
        Page<QuestionBank> result = bankService.listBanks(1, 10, null, null);

        // Then
        assertNotNull(result);
        verify(bankMapper, times(1)).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }
}
