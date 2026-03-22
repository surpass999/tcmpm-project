package cn.gemrun.base.framework.idempotent.config;

import cn.gemrun.base.framework.idempotent.core.aop.IdempotentAspect;
import cn.gemrun.base.framework.idempotent.core.keyresolver.impl.DefaultIdempotentKeyResolver;
import cn.gemrun.base.framework.idempotent.core.keyresolver.impl.ExpressionIdempotentKeyResolver;
import cn.gemrun.base.framework.idempotent.core.keyresolver.IdempotentKeyResolver;
import cn.gemrun.base.framework.idempotent.core.keyresolver.impl.UserIdempotentKeyResolver;
import cn.gemrun.base.framework.idempotent.core.redis.IdempotentRedisDAO;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import cn.gemrun.base.framework.redis.config.BaseRedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

@AutoConfiguration(after = BaseRedisAutoConfiguration.class)
public class BaseIdempotentConfiguration {

    @Bean
    public IdempotentAspect idempotentAspect(List<IdempotentKeyResolver> keyResolvers, IdempotentRedisDAO idempotentRedisDAO) {
        return new IdempotentAspect(keyResolvers, idempotentRedisDAO);
    }

    @Bean
    public IdempotentRedisDAO idempotentRedisDAO(StringRedisTemplate stringRedisTemplate) {
        return new IdempotentRedisDAO(stringRedisTemplate);
    }

    // ========== 各种 IdempotentKeyResolver Bean ==========

    @Bean
    public DefaultIdempotentKeyResolver defaultIdempotentKeyResolver() {
        return new DefaultIdempotentKeyResolver();
    }

    @Bean
    public UserIdempotentKeyResolver userIdempotentKeyResolver() {
        return new UserIdempotentKeyResolver();
    }

    @Bean
    public ExpressionIdempotentKeyResolver expressionIdempotentKeyResolver() {
        return new ExpressionIdempotentKeyResolver();
    }

}
