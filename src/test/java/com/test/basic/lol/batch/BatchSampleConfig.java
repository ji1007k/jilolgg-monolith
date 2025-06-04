package com.test.basic.lol.batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;


/**
 * 실행흐름
 * - JobLauncher가 Job을 실행
 * - Job이 여러 Step으로 나뉘어 순차 실행
 * - 각 Step은 ItemReader → ItemProcessor → ItemWriter 순으로 처리
 * - 처리 중 예외 발생 시 설정에 따라 재시도, 스킵, 실패 처리
 * - 처리 정보는 JobRepository에 저장됨
 *
 * 일반적인 배치 Job 구성에는 DSL 방식이 빠르고 선언적으로 코드를 작성할 수 있어 추천됩니다.
 *      > JobBuilderFactory, StepBuilderFactory, JobRepository 등을 추상화해서 자동으로 연결
 * 복잡한 Job 흐름 (ex: 조건 분기, 재시작 설정, 실패 처리, FlowJob 등)이 필요한 경우 기존 Builder 방식이 더 유연합니다.
 */
@Configuration
// Job, Step, JobLauncher, JobRepository 등의 필수 컴포넌트를 자동으로 설정
@EnableBatchProcessing
@Profile("test")
public class BatchSampleConfig {

    private Queue<String> dataQueue = new LinkedList<>(List.of("one", "two", "three", "four", "five"));

    // JobBuilder와 StepBuilder를 직접 new
    // DSL 사용 시 JobRepository와 PlatformTransactionManager는 Spring Batch가 자동 등록
    @Bean
    public DataSourceInitializer batchDataSourceInitializer(DataSource dataSource) {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource("org/springframework/batch/core/schema-h2.sql"));

        // 에러 무시 설정 추가 (EX. 이미 관련 테이블 생성된 경우)
        populator.setContinueOnError(true); // SQL 실행 중 에러가 나도 계속 실행
        populator.setIgnoreFailedDrops(true); // DROP 구문 실패 무시 (선택적)

        DataSourceInitializer initializer = new DataSourceInitializer();
        initializer.setDataSource(dataSource);
        initializer.setDatabasePopulator(populator);
        return initializer;
    }

    // 하나의 배치 작업 단위. 여러 Step으로 구성
    @Bean
    public Job exampleJob(JobRepository jobRepository, Step exampleStep) {
        return new JobBuilder("exampleJob", jobRepository)
                .start(exampleStep)
                .build();
    }

    // Step: Job을 구성하는 단위 작업.
    //  chunk 기반 처리 방식(ItemReader → ItemProcessor → ItemWriter 흐름)
    //  -> Spring Batch 5 이상에서 가장 권장됨
    @Bean
    public Step exampleStep(JobRepository jobRepository,
                            PlatformTransactionManager transactionManager) {
        return new StepBuilder("exampleStep", jobRepository)
                .<String, String>chunk(3, transactionManager)   // 3개씩 읽고 처리 후 쓰기
                .reader(itemReader())
                .processor(itemProcessor())
                .writer(itemWriter())
                .build();
    }


    // 1) 데이터 입력 (e.g. DB, CSV, API 등)
    //  데이터를 한 건씩 읽어 리턴, 더 이상 없으면 null
    @Bean
    public ItemReader<String> itemReader() {
        // 1) 기본 형식
        /*return new ItemReader<String>() {
            @Override
            public String read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
                return dataQueue.poll();
            }
        };*/

        // 2) 람다 형식
//        return () -> dataQueue.poll();
        
        // 3) 메서드 참조 형식
        return dataQueue::poll;

        // ListItemReader: 메모리 리스트 순회용 구현체
        //  메모리 내 리스트를 단순히 Iterator로 순회하며 하나씩 반환
        /*List<String> items = Arrays.asList("one", "two", "three", "four", "five");
        return new ListItemReader<>(items);*/
    }

    // 2) 데이터 가공/처리
    //  O process(I item) throws Exception;
    //  입력값을 가공하여 출력.
    @Bean
    public ItemProcessor<String, String> itemProcessor() {
        // 1) 기본 형식
       /* return new ItemProcessor<String, String>() {
            @Override
            public String process(String item) throws Exception {
                return item.toUpperCase();
            }
        };*/

        // 2) 람다 형식
//        return item -> item.toUpperCase();

        // 3) 메서드 참조 형식.
        //     람다에서 넘기던 매개변수(item)가 내부적으로 자동으로 전달됨
        return String::toUpperCase;
    }

    // 3) 처리된 데이터를 출력 (e.g. DB 저장 등)
    // Chunk<T>는 내부적으로 List<T>를 래핑(wrapping)한 객체. 처리 상태(rollback 등)나 메타데이터 등을 함께 전달
    // Iterable<T>를 구현하고 있어서 for-each로 순회 가능하며 chunk.getItems()로 내부 List 가져오기 가능
    @Bean
    public ItemWriter<String> itemWriter() {
        
        // 1) 기본 형식
        /*return new ItemWriter<String>() {
            // Spring Batch 5부터 List가 아닌 Chunk로 인자를 받음
            @Override
            public void write(Chunk<? extends String> chunk) throws Exception {
                System.out.println("Writing chunk:" + chunk);
            }
        };*/
        
        // 2) 람다 형식
        // 2-1)
        /*return chunk -> {
            for (String item : chunk) {
                System.out.println("Writing item: " + item);
            }
        };*/
        // 2-2)
//        return chunk -> chunk.forEach(item -> System.out.println("Writing item: " + item));

        // 3) 람다 + 메서드 참조 형식
        return chunk -> chunk.forEach(System.out::println);
    }

// -----------------------------------------------------------------------------
    @Bean
    public Job simpleJob(JobRepository jobRepository, Step simpleStep) {
        return new JobBuilder("simpleJob", jobRepository)
                .start(simpleStep)
                .build();
    }

    @Bean
    public Step simpleStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("simpleStep", jobRepository)
                .tasklet(new Tasklet() {
                    @Override
                    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
                        System.out.println("Tasklet 실행됨");
                        return RepeatStatus.FINISHED;
                    }
                }, transactionManager)
                .build();
    }



// ------------------------------------------------------------------------------
    // Spring Batch 5 이전 버전 방식. ***5 버전에서 deprecated.***
    // Tasklet: 간단한 하나의 작업(단일 Step)을 처리할 때 사용하는 인터페이스
    //  ex) 로그 출력, DB 초기화, 파일 삭제, 외부 API 호출
    public Tasklet simpleTask() {
        // 1) 기본 형식 (위와 동일한 기능)
       /* @Override
        public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
            System.out.println("Tasklet 실행됨");
            return RepeatStatus.FINISHED;
        }*/

        // 2) 람다 형식
        //  Tasklet은 함수형 인터페이스(메서드가 하나만 존재 → 람다로 표현 가능)
        //  매개변수는 execute() 메서드의 시그니처에 맞춰 자동으로 바인딩
        //  결국엔 Tasklet을 익명 구현 객체로 만들어 줌
        return (contribution, chunkContext) -> {
            System.out.println("Tasklet 실행됨");
            return RepeatStatus.FINISHED;   // RepeatStatus.CONTINUABLE: 반복 실행되도록 함 (루프 필요 시)
        };
    }
}
