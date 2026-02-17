package com.t3a.core.controller;

import com.t3a.common.domain.Result;
import com.t3a.core.domain.entity.QuizSession;
import com.t3a.core.domain.entity.User;
import com.t3a.core.mapper.QuizSessionMapper;
import com.t3a.core.service.QuestionBankService;
import com.t3a.core.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardControllerLogicTest {

    @Mock
    private UserService userService;

    @Mock
    private QuizSessionMapper quizSessionMapper;

    @Mock
    private QuestionBankService questionBankService;

    @InjectMocks
    private DashboardController dashboardController;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getDashboard_ShouldHandleSessionWithoutBankId() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", "N/A")
        );

        User user = new User();
        user.setId(1L);
        user.setUsername("admin");
        when(userService.findByUsername("admin")).thenReturn(user);

        QuizSession session = new QuizSession();
        session.setSessionKey("s-1");
        session.setUserId(1L);
        session.setBankId(null);
        session.setDeleted(0);
        session.setStatus("COMPLETED");
        session.setTotalScore(10);
        session.setUserScore(BigDecimal.valueOf(6));
        session.setTimeSpent(120);
        session.setSubmitTime(LocalDateTime.now());
        when(quizSessionMapper.selectList(any())).thenReturn(List.of(session));

        Result<DashboardController.DashboardStats> result = dashboardController.getDashboard();
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().getQuizzesCompleted());
        assertEquals(1, result.getData().getRecentQuizzes().size());
        assertEquals("Unknown Bank", result.getData().getRecentQuizzes().get(0).getBankName());
    }
}
