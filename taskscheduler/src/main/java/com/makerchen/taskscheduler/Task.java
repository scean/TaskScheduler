package com.makerchen.taskscheduler;

import java.util.Random;
import java.util.concurrent.CountDownLatch;

/**
 * @author MakerChen
 * @date 2017/4/21
 * @see
 */
public abstract class Task implements Comparable<Task> {

    public static final int MAX_PRIORITY = 10;
    public static final int MIN_PRIORITY = 1;
    public static final int NORM_PRIORITY = 5;

    private static final int THREAD_NEW = 1;
    private static final int THREAD_MAIN = 1 << 1;

    private int mDefaultThread = THREAD_NEW ;
    /**
     * 任务名
     */
    private String taskName = String.valueOf(new Random().nextInt(Integer.MAX_VALUE)) ;

    /**
     * 任务的优先级，范围1~10。10的优先级最高，1的优先级最低。
     */
    int mPriority = NORM_PRIORITY;

    /**
     * 可用作同一批任务执行的通知
     */
    CountDownLatch mCountDownLatch ;

    public Task () {
    }

    public Task ( int priority ) {
        initPriority(priority);
    }

    public Task ( CountDownLatch countDownLatch ) {
        this.mCountDownLatch = countDownLatch ;
    }

    public Task(int priority, CountDownLatch countDownLatch ) {
        this.mCountDownLatch = countDownLatch ;
        initPriority(priority);
    }

    /**
     * 执行具体任务
     */
    public abstract void executeTask() throws Exception ;

    boolean isDoneInMainThread () {
        return ( mDefaultThread & THREAD_MAIN ) != 0 ;
    }

    /**
     * 设置主线程中进行异步回调
     */
    public void setDoneInUIThread () {
        mDefaultThread = THREAD_MAIN ;
    }

    /**
     * 任务执行完毕
     */
    public abstract void done () throws Exception ;

    private void initPriority( int priority ) {
        if ( priority < MIN_PRIORITY ) {
            priority = MIN_PRIORITY ;
        }
        if ( priority > MAX_PRIORITY ) {
            priority = MAX_PRIORITY ;
        }
        this.mPriority = priority ;
    }

    public int getPriority() {
        return mPriority;
    }

    public void setPriority(int priority) {
        initPriority(priority);
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public CountDownLatch getCountDownLatch() {
        return mCountDownLatch;
    }

    public void setCountDownLatch(CountDownLatch mCountDownLatch) {
        this.mCountDownLatch = mCountDownLatch;
    }

    @Override
    public int compareTo(Task task) {
        return this.mPriority > task.mPriority ? -1 : this.mPriority < task.mPriority ? 1 : 0 ;
    }

    @Override
    public String toString() {
        return "Task{" +
                "mPriority=" + mPriority +
                ", taskName='" + taskName + '\'' +
                '}';
    }

}