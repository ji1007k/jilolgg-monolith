package com.test.basic.lol.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SimpleRetryListener implements RetryListener {
    @Override
    public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
        log.warn("재시도 발생 - 현재 횟수: {}, 예외: {}",
                context.getRetryCount(),
                throwable.getClass().getSimpleName()
        );
    }
}
