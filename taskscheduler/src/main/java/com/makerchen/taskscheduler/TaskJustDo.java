package com.makerchen.taskscheduler;

/**
 * 仅完成任务的执行。
 * @author MakerChen
 * @date 2017/5/9
 * @see
 */
public abstract class TaskJustDo extends Task {

    @Override
    public final void done() {
        L.d(this + " , Task Just Do has done !");
    }

}