package com.makerchen.sample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.makerchen.taskscheduler.DoneListener;
import com.makerchen.taskscheduler.L;
import com.makerchen.taskscheduler.Task;
import com.makerchen.taskscheduler.TaskScheduler;

import java.util.Enumeration;
import java.util.Random;
import java.util.Vector;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TaskScheduler.getInstance().buildCapacity(3).init();

    }

    public void executeStandard(View view) {
        executeTask(produceTask(10));
    }

    public void executeAppend(View view) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                executeTask(produceTask(10));
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                L.d("---------------------------new task start . ----------------------------");
                executeTask(produceTask(10));
                L.d("---------------------------new task end . ----------------------------");
            }
        }).start();
    }

    public void executeBatchUnordered(View view) {
        Vector<Task> tasks = produceTask(4);
        Task[] taskArray = tasks.toArray(new Task[1]);
        TaskScheduler.getInstance().executeBatchTaskUnordered(new DoneListener() {
            @Override
            public void done() {
                L.d("batch task done!");
            }
        }, taskArray);
    }

    public void executeBatchOrdered(View view) {
        Vector<Task> tasks = produceTask(10);
        Task[] taskArray = tasks.toArray(new Task[1]);
        TaskScheduler.getInstance().executeBatchTaskOrdered(new DoneListener() {
            @Override
            public void done() {
                L.d("batch task done!");
            }
        }, taskArray);
    }

    public void stop(View view) {
        TaskScheduler.getInstance().finish();
    }

    private void executeTask(Vector<Task> tasks) {
        Enumeration<Task> enu = tasks.elements();
        while (enu.hasMoreElements()) {
            TaskScheduler.getInstance().executeTask(enu.nextElement());
        }
    }

    private Vector<Task> produceTask(int taskNum) {
        final Vector<Task> tasks = new Vector<>();
        final Random r = new Random();
        for (int i = 0; i < taskNum; i++) {
            Task task = new Task(r.nextInt(10) + 1) {
                @Override
                public void executeTask() {
                    L.d("execute task = " + this);
                    try {
                        Thread.sleep(r.nextInt(2000));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void done() {
                    L.d("done task = " + this + " , thread = " + Thread.currentThread() );
                }

            };
            task.setTaskName("taskName" + i);
            tasks.add(task);
        }
        return tasks;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        TaskScheduler.getInstance().finish();
    }

}
