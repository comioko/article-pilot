package github.comioko.articlepilot.config;

import com.mybatisflex.core.FlexGlobalConfig;
import com.mybatisflex.spring.FlexSqlSessionFactoryBean;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;

/**
 * MyBatis-Flex 1.11.1 与 Spring Boot 4.1 不兼容（引用了被搬家的
 * org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration），
 * 这里手动装配 SqlSessionFactory / MapperScannerConfigurer，绕开官方 auto-config。
 */
@Configuration
public class MybatisFlexConfig {

    @Bean
    @ConditionalOnMissingBean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        FlexSqlSessionFactoryBean factory = new FlexSqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setMapperLocations(new PathMatchingResourcePatternResolver()
                .getResources("classpath*:/mapper/**/*.xml"));
        return factory.getObject();
    }
}
