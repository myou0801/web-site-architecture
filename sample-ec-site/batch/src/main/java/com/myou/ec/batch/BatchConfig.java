package com.myou.ec.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.BatchObservabilityBeanPostProcessor;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration(proxyBeanMethods = false)
public class BatchConfig {

    @Bean
    static BatchObservabilityBeanPostProcessor batchObservabilityBeanPostProcessor() {
        return new BatchObservabilityBeanPostProcessor();
    }

    @Bean
    Job job01(JobRepository jobRepository, PlatformTransactionManager transactionManager, Step step01) {
        return new JobBuilder("job01", jobRepository)
                .start(step01)
                .build();
    }

    @Bean
    Step step01(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("step01", jobRepository)
                .tasklet((StepContribution contribution, ChunkContext chunkContext) -> {
                    Logger logger = LoggerFactory.getLogger("sample");
                    logger.info("test!!");
                    return RepeatStatus.FINISHED;
                }, transactionManager).build();
    }

}
