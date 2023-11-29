package com.batch_java.study_spring_batch.batch;

import com.batch_java.study_spring_batch.common.Batch;
import com.batch_java.study_spring_batch.converter.UserConverter;
import com.batch_java.study_spring_batch.model.UserNew;
import com.batch_java.study_spring_batch.model.User;
import jakarta.persistence.EntityManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JpaCursorItemReaderBuilder;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Slf4j
// @Batch
public class DatabaseBatchConfiguration {

    @Bean
    public Job job(
        JobRepository jobRepository,
        Step step
    ) {
        return new JobBuilder("itemReaderJob", jobRepository)
            .incrementer(new RunIdIncrementer())
            .start(step)
            .build();
    }
    
    @Bean
    public Step step(
        JobRepository jobRepository,
        PlatformTransactionManager transactionManager,
        ItemReader<User> jpaCursorItemReader,
        ItemProcessor<User, UserNew> itemProcessor,
        ItemWriter<UserNew> jdbcItemWriter
    ) {
        return new StepBuilder("step", jobRepository)
            .<User, UserNew>chunk(2, transactionManager)
            .reader(jpaCursorItemReader)
            .processor(itemProcessor)
            // .writer(System.out::println)
            .writer(jdbcItemWriter)
            .faultTolerant()
            .skip(DuplicateKeyException.class)
            .skipPolicy(
                (t, skipCount) -> t instanceof DuplicateKeyException && skipCount < Integer.MAX_VALUE
            )
            .build();
    }
    
    @Bean
    public ItemReader<User> jpaPagingItemReader(
        EntityManagerFactory entityManagerFactory
    ) {
        return new JpaPagingItemReaderBuilder<User>()
            .name("jpaPagingItemReader")
            .entityManagerFactory(entityManagerFactory)
            .queryString("SELECT u FROM User u ORDER BY u.id")  // 실제 테이블 명 x, JPA 클래스 이름 사용
            .pageSize(3)
            .build();
    }

    @Bean
    public ItemReader<User> jpaCursorItemReader(
        EntityManagerFactory entityManagerFactory
    ) {
        return new JpaCursorItemReaderBuilder<User>()
            .name("JpaCursorItemReader")
            .entityManagerFactory(entityManagerFactory)
            .queryString("SELECT u FROM User u ORDER BY u.id")
            .build();
    }

    @Bean
    public ItemWriter<UserNew> jpaItemWriter(
        EntityManagerFactory entityManagerFactory
    ) {
        return new JpaItemWriterBuilder<UserNew>()
            .entityManagerFactory(entityManagerFactory)
            .build();
    }
    
    @Bean
    public ItemWriter<UserNew> jdbcItemWriter(
        DataSource dataSource
    ) {
        return new JdbcBatchItemWriterBuilder<UserNew>()
            .dataSource(dataSource)
            .sql("""
                INSERT INTO USER_NEW
                VALUES (:id, :name, :age, :region, :phoneNumber)
                """)
            .beanMapped()
            .build();
    }
    
    @Bean
    public ItemProcessor<User, UserNew> itemProcessor() {
        return user -> {
            user.setAge(user.getAge() * 2);
            return new UserConverter().toNewUser(user);
        };
    }
}