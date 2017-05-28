package com.makerchen.taskscheduler;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 负责各种任务的调度
 *
 * @author MakerChen
 * @date 2017/5/3
 * @see
 */
public class TaskScheduler {

    private static TaskScheduler instance = null;

    public static TaskScheduler getInstance() {
        if (null == instance) {
            synchronized (TaskScheduler.class) {
                if (null == instance) {
                    instance = new TaskScheduler();
                }
            }
        }
        return instance;
    }

    /**
     * 待办任务队列。调用方添加的任务会在该队列中进行优先级排序。
     * 完成排序后，会送到长度为3的执行任务队列，交由线程池处理。
     */
    private PriorityBlockingQueue<Task> mTodoQueue = new PriorityBlockingQueue<>();
    private ExecutorService mExecutorService;
    private LinkedBlockingDeque<Task> mDoingQueue = null;

    /**
     * 当前异步任务调度状态
     */
    private int mCurrentStatus = TASK_STATUS_NOT_START;
    private static final int TASK_STATUS_NOT_START = 1; //未开始
    private static final int TASK_STATUS_RUNNING = 1 << 1; //任务执行中
    private static final int TASK_STATUS_EXCEPTION = 1 << 2; //任务出现异常
    private static final int TASK_STATUS_FINISH = 1 << 3; //任务结束

    private static final int DEFAULT_CAPACITY = 3;

    private int mCapacity = DEFAULT_CAPACITY;

    private AtomicInteger atomicInteger = new AtomicInteger(0);

    private TaskScheduler() {

    }

    private Object lock = new Object();

    /**
     * 将完成排序的队列，送至执行队列中。
     */
    private Runnable rScheduleQueue = new Runnable() {
        @Override
        public void run() {
            try {
                while ((mCurrentStatus & TASK_STATUS_RUNNING) != 0) {
                    if (mTodoQueue.isEmpty()) {//队列是空的
                        L.d("mTodoQueue is waiting.");
                        synchronized (lock) {
                            lock.wait();
                        }
                    } else {
                        if (mDoingQueue.size() >= (mCapacity + atomicInteger.get())) {//正在执行的任务超过了最大容量,当存在BatchTask的时候需同步扩展执行任务队列的长度
                            L.d("mDoingQueue is full. size = " + (mCapacity + atomicInteger.get()));
                            synchronized (lock) {
                                lock.wait();
                            }
                        } else {
                            final Task task = mTodoQueue.take();
                            mDoingQueue.put(task);//向执行队列添加任务。put如果容量超限会等待释放之后继续添加。
                            L.d("mDoingQueue's size is = " + mDoingQueue.size());
                            mExecutorService.execute(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        try {
                                            task.executeTask();//执行具体的异步任务
                                        } catch (Exception e) {
                                            L.d(e);
                                        }

                                        if (task.isDoneInMainThread()) {
                                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                                @Override
                                                public void run() {
                                                    try {
                                                        task.done();
                                                    } catch (Exception e) {
                                                        L.d(e);
                                                    }
                                                }
                                            });
                                        } else {
                                            try {
                                                task.done();
                                            } catch (Exception e) {
                                                L.d(e);
                                            }
                                        }

                                        if (task.mCountDownLatch != null && !(task instanceof TaskBatch)) {//判断非done
                                            task.mCountDownLatch.countDown();
                                        } else if (task.mCountDownLatch != null && (task instanceof TaskBatch)) {
                                            atomicInteger.decrementAndGet();
                                        }

                                        mDoingQueue.take();//从执行任务队列中移除任务
                                    } catch (InterruptedException e) {
                                        L.d(e);
                                    }
                                    synchronized (lock) {
                                        lock.notify();
                                    }
                                }
                            });
                            L.d("mExecutorService's size is = " + ((ThreadPoolExecutor) mExecutorService).getPoolSize());
                        }
                    }
                }
                L.d("end!");
            } catch (InterruptedException e) {
                L.d(null, e);
                mCurrentStatus = TASK_STATUS_EXCEPTION;
            }
        }
    };

    /**
     * 执行一般的异步任务
     *
     * @param task
     */
    public void executeTask(Task task) {
        tryRestart();
        mTodoQueue.put(task);
        synchronized (lock) {
            lock.notify(); //通知排序队列继续输送待执行任务
        }
    }

    private void tryRestart() {
        if ((mCurrentStatus & TASK_STATUS_RUNNING) == 0 && (mCurrentStatus & TASK_STATUS_FINISH) == 0) {
            //非running状态且不是结束状态，尝试重启异步任务队列
            restart();
        }
    }

    /**
     * 执行一般的异步任务
     *
     * @param tasks
     */
    public void executeTask(Task... tasks) {
        if (checkAndRestart(tasks)) return;

        for (Task task : tasks) {
            mTodoQueue.put(task);
        }
        synchronized (lock) {
            lock.notify(); //通知排序队列继续输送待执行任务
        }
    }

    private boolean checkAndRestart(Task[] tasks) {
        if (tasks == null || tasks.length == 0) return true;

        tryRestart();
        return false;
    }

    /**
     * 批量执行无序异步任务，待所有任务完成后通知出来。
     *
     * @param tasks
     */
    public void executeBatchTaskUnordered(DoneListener doneListener, Task... tasks) {
        if (checkAndRestart(tasks)) return;

        setBatchTask(doneListener, tasks);

        for (Task task : tasks) {
            mTodoQueue.put(task);
        }

        synchronized (lock) {
            lock.notify(); //通知排序队列继续输送待执行任务
        }

    }

    private void setBatchTask(DoneListener doneListener, Task[] tasks) {
        int length = tasks.length;
        final CountDownLatch countDownLatch = new CountDownLatch(length);

        TaskBatch taskBatch = new TaskBatch(countDownLatch, doneListener);
        mTodoQueue.put(taskBatch);

        atomicInteger.incrementAndGet();

        for (int i = 0; i < length; i++) {
            Task task = tasks[i];
            task.setTaskName(taskBatch.getTaskName() + SPILT + task.getTaskName());
            task.setCountDownLatch(countDownLatch);
        }
    }

    private static final String SPILT = "::";

    /**
     * 批量执行有序异步任务，待所有任务完成后通知出来。
     *
     * @param tasks
     */
    public void executeBatchTaskOrdered(DoneListener doneListener, Task... tasks) {

        if (checkAndRestart(tasks)) return;

        setBatchTask(doneListener, tasks);

        int length = tasks.length;
        for (int i = 0; i < length; i++) {
            Task task = tasks[i];
            task.mPriority = length - i;//TODO:可优化
            mTodoQueue.put(task);
        }

        synchronized (lock) {
            lock.notify(); //通知排序队列继续输送待执行任务
        }

    }

    /**
     * 重启异步任务
     */
    public void restart() {
        if ((mCurrentStatus & TASK_STATUS_EXCEPTION) != 0) {
            mCurrentStatus = TASK_STATUS_RUNNING;

            mExecutorService.execute(rScheduleQueue);
        }
    }

    /**
     * 清理现场
     */
    public void finish() {
        mCurrentStatus = TASK_STATUS_FINISH;

        if (mExecutorService != null) {
            mExecutorService.shutdown();
            mExecutorService = null;
        }

    }

    /**
     * 设置同时处理异步任务的容量
     *
     * @param capacity
     * @return
     */
    public TaskScheduler buildCapacity(int capacity) {
        this.mCapacity = capacity;
        return this;
    }

    public void init() {
        mExecutorService = Executors.newCachedThreadPool();
        mDoingQueue = new LinkedBlockingDeque();
        start();
    }

    private void start() {
        mCurrentStatus = TASK_STATUS_RUNNING;
        mExecutorService.execute(rScheduleQueue);
    }

}
