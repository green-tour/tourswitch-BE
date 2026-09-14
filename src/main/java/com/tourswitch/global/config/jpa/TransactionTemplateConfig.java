package com.tourswitch.global.config.jpa;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * TourAPI 호출 등 외부 I/O와 DB 저장을 같은 트랜잭션에 두지 않기 위해(B3 규칙), 서비스가
 * 필요한 구간에만 프로그래밍적으로 트랜잭션을 여는 용도. Spring 프록시 기반 @Transactional은
 * 같은 클래스 내부 self-invocation에 적용되지 않는 문제도 함께 피한다.
 */
@Configuration
public class TransactionTemplateConfig {

    @Bean
    public TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }
}
