package com.makerchen.taskscheduler;

import java.util.concurrent.CountDownLatch;

/**
 * 用于给批量异步任务完成之后的通知
 * @author MakerChen
 * @date 2017/5/5
 * @see
 */
class TaskBatch extends Task {

    private DoneListener mDoneListener;

    public TaskBatch(CountDownLatch countDownLatch , DoneListener doneListener ) {
        super(countDownLatch);
        this.mDoneListener = doneListener ;
        this.mPriority = Integer.MAX_VALUE ;//haha~
    }

    @Override
    public void executeTask() {
        if ( mCountDownLatch != null ) {
            try {
                L.d("TaskBatch " + getTaskName() + " is awaiting...");
                mCountDownLatch.await();
            } catch (InterruptedException e) {
                L.d(e);
            }
        }
    }

    @Override
    public void done() {
        if ( mDoneListener != null ) {
            L.d("TaskBatch " + getTaskName() + " has done . ");
            mDoneListener.done();
        }
    }

}