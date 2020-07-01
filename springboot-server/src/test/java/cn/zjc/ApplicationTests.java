package cn.zjc;

import cn.zjc.rabbitmq.sender.direct.DirectSender;
import cn.zjc.rabbitmq.sender.topic.TopicSender;
import cn.zjc.server.util.IdGeneratorService;
import cn.zjc.test.DistributedTest;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = ApplicationTests.class)
@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class})
@ComponentScan(basePackages = "cn.zjc")
@PropertySource({"classpath:redis.properties", "classpath:zk.properties", "classpath:datasources.properties", "classpath:rabbitmq.properties"})
@EnableAsync
@EnableScheduling
@Slf4j
public class ApplicationTests {

    private static final int SLICE_SIZE = 5000;
    @Resource
    private DistributedTest distributedTest;
    @Resource
    private DirectSender directSender;
    @Resource
    private TopicSender topicSender;
    @Resource
    private IdGeneratorService idGeneratorService;

    @Test
    public void secKill() throws InterruptedException {
        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                try {
                    distributedTest.getGoods("123456", 111111);
                    Thread.sleep(10000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
            Thread.sleep(1000);
        }
        TimeUnit.SECONDS.sleep(3);
    }

    @Test
    public void send() {
        String zjc000 = idGeneratorService.generateAlipayOrderNumber("zjc000");
        String zjc001 = idGeneratorService.generateTMallOrderNumber("zjc001");

        System.out.println(zjc000);
        System.out.println(zjc001);
    }

    /**
     * 线程池例子
     *
     * @param now
     */
    public void threadTest(Timestamp now) {
        //使用guava定义ThreadFactory,禁止使用Executors的静态方法创建线程池
        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("urgeDispatcher-pool-%d").build();
        ExecutorService executorService = new ThreadPoolExecutor(5, 50, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(1024), namedThreadFactory, new ThreadPoolExecutor.AbortPolicy());
        try {
            long count = 10000;
            int group = Long.valueOf((count + SLICE_SIZE) / SLICE_SIZE).intValue();

            List<Integer> list = Lists.newArrayList();
            for (int i = 0; i < group; i++) {
                list.add(i);
            }

            final CountDownLatch cd = new CountDownLatch(list.size());
            for (final Integer index : list) {
                executorService.execute(() -> {
                    try {
                        System.out.println("==========");
                    } catch (Exception e0) {
                        log.error("===========", e0);
                    } finally {
                        cd.countDown();
                    }
                });
            }
            cd.await();
        } catch (Exception e) {
            log.error("==============", e);
        } finally {
            //优雅地关闭线程池
            executorService.shutdown();
        }
    }
}