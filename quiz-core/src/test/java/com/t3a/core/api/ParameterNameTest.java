package com.t3a.core.api;

import com.t3a.core.controller.QuestionBankController;
import com.t3a.core.controller.QuizController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Parameter Name Reflection Test
 * Verifies that the Maven compiler is configured with -parameters flag
 * This test ensures Spring Boot can access parameter names via reflection
 */
class ParameterNameTest {

    @Test
    @DisplayName("QuestionBankController.listBanks - Should have parameter names")
    void questionBankController_listBanks_ShouldHaveParameterNames() throws Exception {
        Method method = QuestionBankController.class.getMethod("listBanks",
                int.class, int.class, Long.class, String.class);

        Parameter[] parameters = method.getParameters();

        assertEquals("pageNum", parameters[0].getName(),
                "Parameter name 'pageNum' should be accessible via reflection. " +
                "Ensure Maven compiler plugin is configured with -parameters flag.");
        assertEquals("pageSize", parameters[1].getName(),
                "Parameter name 'pageSize' should be accessible via reflection. " +
                "Ensure Maven compiler plugin is configured with -parameters flag.");
        assertEquals("creatorId", parameters[2].getName(),
                "Parameter name 'creatorId' should be accessible via reflection. " +
                "Ensure Maven compiler plugin is configured with -parameters flag.");
        assertEquals("category", parameters[3].getName(),
                "Parameter name 'category' should be accessible via reflection. " +
                "Ensure Maven compiler plugin is configured with -parameters flag.");
    }

    @Test
    @DisplayName("QuestionBankController.getBank - Should have parameter names")
    void questionBankController_getBank_ShouldHaveParameterNames() throws Exception {
        Method method = QuestionBankController.class.getMethod("getBank", Long.class);

        Parameter[] parameters = method.getParameters();

        assertEquals("id", parameters[0].getName(),
                "Parameter name 'id' should be accessible via reflection. " +
                "Ensure Maven compiler plugin is configured with -parameters flag.");
    }

    @Test
    @DisplayName("QuestionBankController.createBank - Should have parameter names")
    void questionBankController_createBank_ShouldHaveParameterNames() throws Exception {
        Method method = QuestionBankController.class.getMethod("createBank",
                com.t3a.core.domain.entity.QuestionBank.class);

        Parameter[] parameters = method.getParameters();

        assertEquals("bank", parameters[0].getName(),
                "Parameter name 'bank' should be accessible via reflection. " +
                "Ensure Maven compiler plugin is configured with -parameters flag.");
    }

    @Test
    @DisplayName("QuestionBankController.deleteBank - Should have parameter names")
    void questionBankController_deleteBank_ShouldHaveParameterNames() throws Exception {
        Method method = QuestionBankController.class.getMethod("deleteBank", Long.class);

        Parameter[] parameters = method.getParameters();

        assertEquals("id", parameters[0].getName(),
                "Parameter name 'id' should be accessible via reflection. " +
                "Ensure Maven compiler plugin is configured with -parameters flag.");
    }

    @Test
    @DisplayName("QuizController.getSession - Should have parameter names")
    void quizController_getSession_ShouldHaveParameterNames() throws Exception {
        Method method = QuizController.class.getMethod("getSession", String.class);

        Parameter[] parameters = method.getParameters();

        assertEquals("sessionKey", parameters[0].getName(),
                "Parameter name 'sessionKey' should be accessible via reflection. " +
                "Ensure Maven compiler plugin is configured with -parameters flag.");
    }

    @Test
    @DisplayName("QuizController.getUserHistory - Should have parameter names")
    void quizController_getUserHistory_ShouldHaveParameterNames() throws Exception {
        Method method = QuizController.class.getMethod("getUserHistory", Long.class);

        Parameter[] parameters = method.getParameters();

        assertEquals("userId", parameters[0].getName(),
                "Parameter name 'userId' should be accessible via reflection. " +
                "Ensure Maven compiler plugin is configured with -parameters flag.");
    }

    @Test
    @DisplayName("QuizController.getQuizResult - Should have parameter names")
    void quizController_getQuizResult_ShouldHaveParameterNames() throws Exception {
        Method method = QuizController.class.getMethod("getQuizResult", String.class);

        Parameter[] parameters = method.getParameters();

        assertEquals("sessionKey", parameters[0].getName(),
                "Parameter name 'sessionKey' should be accessible via reflection. " +
                "Ensure Maven compiler plugin is configured with -parameters flag.");
    }

    @Test
    @DisplayName("QuizController.startQuiz - Should have parameter names")
    void quizController_startQuiz_ShouldHaveParameterNames() throws Exception {
        Method method = QuizController.class.getMethod("startQuiz",
                QuizController.StartQuizRequest.class);

        Parameter[] parameters = method.getParameters();

        assertEquals("request", parameters[0].getName(),
                "Parameter name 'request' should be accessible via reflection. " +
                "Ensure Maven compiler plugin is configured with -parameters flag.");
    }

    @Test
    @DisplayName("QuizController.submitQuiz - Should have parameter names")
    void quizController_submitQuiz_ShouldHaveParameterNames() throws Exception {
        Method method = QuizController.class.getMethod("submitQuiz",
                QuizController.SubmitQuizRequest.class);

        Parameter[] parameters = method.getParameters();

        assertEquals("request", parameters[0].getName(),
                "Parameter name 'request' should be accessible via reflection. " +
                "Ensure Maven compiler plugin is configured with -parameters flag.");
    }

    @Test
    @DisplayName("Critical Test - This is the main issue from the bug report")
    void criticalTest_BankListEndpointParameters() throws Exception {
        // This is the exact method that was failing with:
        // "Name for argument of type [int] not specified, and parameter name information
        // not available via reflection. Ensure that the compiler uses the '-parameters' flag."

        Method method = QuestionBankController.class.getMethod("listBanks",
                int.class, int.class, Long.class, String.class);

        Parameter[] parameters = method.getParameters();

        // If this test fails, it means the -parameters flag is not set
        // and the application will return 500 errors for endpoints with @RequestParam

        String param0Name = parameters[0].getName();
        String param1Name = parameters[1].getName();
        String param2Name = parameters[2].getName();
        String param3Name = parameters[3].getName();

        // Verify parameter names are NOT arg0, arg1, arg2, arg3 (which indicates missing -parameters flag)
        assertEquals("pageNum", param0Name,
                "CRITICAL: Parameter name is '" + param0Name + "' instead of 'pageNum'. " +
                "This means the Maven compiler is missing the -parameters flag. " +
                "Add the following to pom.xml:\n" +
                "<compilerArgs>\n" +
                "  <arg>-parameters</arg>\n" +
                "</compilerArgs>");

        assert(!param0Name.startsWith("arg"));
        assert(!param1Name.startsWith("arg"));
        assert(!param2Name.startsWith("arg"));
        assert(!param3Name.startsWith("arg"));
    }
}
