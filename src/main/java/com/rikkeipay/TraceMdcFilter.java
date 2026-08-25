package com.rikkeipay;

import io.opentelemetry.api.trace.Span;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class TraceMdcFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_KEY = "trace_id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        // Trích xuất traceId từ OpenTelemetry
        String traceId = Span.current().getSpanContext().getTraceId();
        
        try {
            // Nạp traceId vào SLF4J MDC
            if (traceId != null && !traceId.isEmpty() && !traceId.equals("00000000000000000000000000000000")) {
                MDC.put(TRACE_ID_KEY, traceId);
            }
            
            // Tiếp tục chuỗi filter và xử lý request
            filterChain.doFilter(request, response);
            
        } finally {
            // Bắt buộc dọn dẹp MDC sau khi xử lý xong request
            MDC.remove(TRACE_ID_KEY);
            // Hoặc MDC.clear() nếu muốn xóa sạch toàn bộ context
        }
    }
}
