package io.psyllabus.platform.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class RequestIdFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_HEADER = "X-Trace-ID";
    private static final String MDC_TRACE_ID = "traceId";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException
    {
        try{
            String traceId = request.getHeader(TRACE_ID_HEADER);

            if (traceId == null || traceId.isBlank()){
                traceId = UUID.randomUUID().toString();
            }

            MDC.put(MDC_TRACE_ID, traceId);
            response.setHeader(TRACE_ID_HEADER, traceId);

            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
