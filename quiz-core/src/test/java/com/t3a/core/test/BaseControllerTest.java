package com.t3a.core.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Base class for all controller tests.
 * Provides common setup and utility methods.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class BaseControllerTest {

    /**
     * MockMvc for performing HTTP requests
     */
    protected MockMvc mockMvc;

    /**
     * ObjectMapper for JSON serialization/deserialization
     */
    protected ObjectMapper objectMapper;

    /**
     * Convert object to JSON string
     *
     * @param obj object to convert
     * @return JSON string
     * @throws Exception if conversion fails
     */
    protected String asJsonString(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }
}
