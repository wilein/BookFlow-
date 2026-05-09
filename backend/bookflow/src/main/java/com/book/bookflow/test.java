package com.book.bookflow;

import org.springframework.data.redis.connection.FutureResult;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

public class test {
    public static void main(String[] args) throws IOException {
        PipedInputStream pipedInputStream = new PipedInputStream();
        PipedOutputStream pipedOutputStream = new PipedOutputStream();
        pipedInputStream.connect(pipedOutputStream);
        Thread product = new Thread(() -> {
            try {
            for (int i = 0; i < 5; i++){

                    pipedOutputStream.write(i);


                System.out.println(Thread.currentThread().getName() + "写入数据" + i);
                Thread.sleep(2000);
            }
            pipedOutputStream.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        Thread consumer = new Thread(() -> {
            try {
                while (true){
                    int read = pipedInputStream.read();
                    if(read != -1){
                        System.out.println(Thread.currentThread().getName() + "读取数据" + read);
                    }else {
                        break;
                    }
                    Thread.sleep(1000);
                }
                pipedInputStream.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        product.start();
        consumer.start();

    }


}
class MyCallable implements Callable<String>{

    @Override
    public String call() throws Exception {
        return Thread.currentThread().getName() + "线程启动";
    }
}
class MyRunnable implements Runnable{

    @Override
    public void run() {
        System.out.println(Thread.currentThread().getName() + "线程启动");
    }
}
class MyThread extends Thread{
    @Override
    public void run() {
        System.out.println(Thread.currentThread().getName() + "线程启动");
    }
}
