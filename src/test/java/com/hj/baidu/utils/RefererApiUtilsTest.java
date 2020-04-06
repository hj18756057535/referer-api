package com.hj.baidu.utils;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import com.hj.baidu.RefererApiApplicationTests;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.*;

@Slf4j
class RefererApiUtilsTest extends RefererApiApplicationTests {
    @Autowired
    RefererApiUtils refererApiUtils;

    @Test
    void getEqid() {
        Date date = new Date();
        System.out.println(refererApiUtils.getEqid("dc9d297d0005b4f9000000045e81617c"));
        Date date1 = new Date();
        System.out.println((date1.getTime() - date.getTime()));
    }


    // 总的请求个数
    private static final int requestTotal = 30;
    // 同一时刻最大的并发线程的个数
    private static final int concurrentThreadNum = 30;

    @Test
    void test() throws InterruptedException {
        BlockingQueue<String> arrayBlockingQueue = new ArrayBlockingQueue<>(requestTotal + 10);
        try (InputStream fileInputStream = new FileInputStream("C:\\Users\\jie hong\\Documents\\WXWork\\1688850110893419\\Cache\\File\\2020-03\\eqid.csv");
             CSVReader reader = new CSVReader(new InputStreamReader(fileInputStream, StandardCharsets.UTF_8),
                     CSVWriter.DEFAULT_SEPARATOR,
                     CSVWriter.DEFAULT_QUOTE_CHARACTER)) {
            String[] line;
            for (int i = 0; i < requestTotal; i++) {
                line = reader.readNext();
                String s = line[0];
                arrayBlockingQueue.offer(s);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        ExecutorService executorService = Executors.newCachedThreadPool();
        CountDownLatch countDownLatch = new CountDownLatch(requestTotal);
        Semaphore semaphore = new Semaphore(concurrentThreadNum);
        Date date = new Date();
        for (int i = 0; i < requestTotal; i++) {
            executorService.execute(() -> {
                try {
                    semaphore.acquire();
                    String result = refererApiUtils.getEqid(arrayBlockingQueue.poll());
                    log.info("result:{}.", result);
                    semaphore.release();
                } catch (InterruptedException e) {
                    log.error("exception", e);
                }
                countDownLatch.countDown();
            });
        }
        countDownLatch.await();
        executorService.shutdown();
        Date date1 = new Date();
        log.info("请求完成");
        log.info("请求完成时间{}", date1.getTime() - date.getTime());
    }

}