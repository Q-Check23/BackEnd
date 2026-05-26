package team23.q_check.common.log;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 요청·응답의 헤더와 바디를 DEBUG 레벨로 찍는다. 평소엔 꺼두고 인시던트 시 actuator 로 런타임 토글:
 *   curl -X POST http://localhost:8081/actuator/loggers/http.exchange \
 *        -H 'Content-Type: application/json' -d '{"configuredLevel":"DEBUG"}'
 *
 * 안전장치:
 *  - 민감 헤더(Authorization, Cookie, Set-Cookie, X-USER-ID 등) 값 마스킹
 *  - 본문은 4KB 까지만 (초과분은 ...truncated 표시)
 *  - multipart/image/octet-stream 등 바이너리 본문은 사이즈만 표시
 *  - DEBUG 비활성 시 ContentCaching 래핑 비용 자체를 안 치름 (shouldNotFilter)
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class HttpExchangeLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger("http.exchange");

    private static final int MAX_BODY_BYTES = 4 * 1024;
    private static final Set<String> SENSITIVE_HEADERS = Set.of(
            "authorization", "cookie", "set-cookie", "proxy-authorization", "x-user-id"
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !log.isDebugEnabled();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        ContentCachingRequestWrapper req = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper res = new ContentCachingResponseWrapper(response);

        try {
            chain.doFilter(req, res);
        } finally {
            try {
                logExchange(req, res);
            } finally {
                res.copyBodyToResponse();
            }
        }
    }

    private void logExchange(ContentCachingRequestWrapper req, ContentCachingResponseWrapper res) {
        log.debug("http.exchange method={} path={} reqHeaders={} reqBody={} status={} resHeaders={} resBody={}",
                req.getMethod(),
                req.getRequestURI(),
                collectRequestHeaders(req),
                extractBody(req.getContentAsByteArray(), req.getCharacterEncoding(), req.getContentType()),
                res.getStatus(),
                collectResponseHeaders(res),
                extractBody(res.getContentAsByteArray(), res.getCharacterEncoding(), res.getContentType()));
    }

    private Map<String, String> collectRequestHeaders(HttpServletRequest req) {
        Map<String, String> headers = new LinkedHashMap<>();
        Enumeration<String> names = req.getHeaderNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            headers.put(name, mask(name, req.getHeader(name)));
        }
        return headers;
    }

    private Map<String, String> collectResponseHeaders(HttpServletResponse res) {
        Map<String, String> headers = new LinkedHashMap<>();
        for (String name : res.getHeaderNames()) {
            headers.put(name, mask(name, res.getHeader(name)));
        }
        return headers;
    }

    private String mask(String headerName, String value) {
        if (value == null) {
            return null;
        }
        if (SENSITIVE_HEADERS.contains(headerName.toLowerCase(Locale.ROOT))) {
            return "***";
        }
        return value;
    }

    private String extractBody(byte[] bytes, String encoding, String contentType) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        if (isBinary(contentType)) {
            return "<binary " + bytes.length + "B>";
        }
        Charset cs = resolveCharset(encoding);
        int len = Math.min(bytes.length, MAX_BODY_BYTES);
        String body = new String(bytes, 0, len, cs);
        return bytes.length > MAX_BODY_BYTES
                ? body + "...(truncated " + (bytes.length - MAX_BODY_BYTES) + "B)"
                : body;
    }

    private Charset resolveCharset(String encoding) {
        if (encoding == null || encoding.isBlank()) {
            return StandardCharsets.UTF_8;
        }
        try {
            return Charset.forName(encoding);
        } catch (Exception e) {
            return StandardCharsets.UTF_8;
        }
    }

    private boolean isBinary(String contentType) {
        if (contentType == null) {
            return false;
        }
        String ct = contentType.toLowerCase(Locale.ROOT);
        return ct.startsWith("multipart/")
                || ct.startsWith("image/")
                || ct.startsWith("audio/")
                || ct.startsWith("video/")
                || ct.contains("octet-stream");
    }
}
