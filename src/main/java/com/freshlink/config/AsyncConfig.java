package com.freshlink.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

	/**
	 * Pool for outbound notifications.
	 *
	 * Deliberately small and bounded: sending email is slow and entirely
	 * non-critical, so it must never be able to exhaust threads or memory that
	 * request handling depends on.
	 *
	 * CallerRunsPolicy on saturation is the wrong default here - it would push
	 * SMTP latency back onto whichever thread published the event. Dropping the
	 * notification is the better trade: the order is already committed, and a
	 * missed courtesy email beats a stalled request thread.
	 */
	@Bean("notificationExecutor")
	public Executor notificationExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(2);
		executor.setMaxPoolSize(4);
		executor.setQueueCapacity(100);
		executor.setThreadNamePrefix("notify-");
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
		// Let in-flight mail finish on shutdown rather than cutting it off.
		executor.setWaitForTasksToCompleteOnShutdown(true);
		executor.setAwaitTerminationSeconds(20);
		executor.initialize();
		return executor;
	}
}
