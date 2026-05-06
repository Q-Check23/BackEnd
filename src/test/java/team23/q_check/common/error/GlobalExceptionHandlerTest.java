package team23.q_check.common.error;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ExceptionTestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void handleAppException_returnsMappedStatusAndBody() throws Exception {
        mockMvc.perform(get("/test/errors/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("test not found"));
    }

    @Test
    void handleInvalidRequest_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/test/errors/invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void missingAuthorizationHeader_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/test/errors/with-auth"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void missingNonAuthHeader_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/test/errors/with-custom-header"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void missingRequiredQueryParam_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/test/errors/with-param"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void malformedJsonBody_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/test/errors/with-body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not valid json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @RestController
    static class ExceptionTestController {
        @GetMapping("/test/errors/not-found")
        public void throwNotFound() {
            throw new AppException(ErrorCode.NOT_FOUND, "test not found");
        }

        @GetMapping("/test/errors/invalid")
        public void throwInvalid() {
            throw new IllegalArgumentException("invalid");
        }

        @GetMapping("/test/errors/with-auth")
        public void requireAuth(@RequestHeader("Authorization") String auth) {
        }

        @GetMapping("/test/errors/with-custom-header")
        public void requireCustomHeader(@RequestHeader("X-Custom") String custom) {
        }

        @GetMapping("/test/errors/with-param")
        public void requireParam(@RequestParam("q") String q) {
        }

        @PostMapping("/test/errors/with-body")
        public void requireBody(@RequestBody TestPayload body) {
        }

        record TestPayload(String name) {}
    }
}
