package org.peercentrum;

import org.junit.Test;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerFactory;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

public class QuartzSpike implements Job {
  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    System.out.println("Hello world "+context.getFireInstanceId()); 
  }
  
  @Test
  public void test() throws Exception {
    System.setProperty("org.terracotta.quartz.skipUpdateCheck", "true");

    SchedulerFactory schedFact = new StdSchedulerFactory();
    Scheduler sched = schedFact.getScheduler();
    sched.start();

    JobDetail job = JobBuilder.newJob(QuartzSpike.class)
        .withIdentity("myJob", "group1")
        .build();

    // Trigger the job to run now, and then every 40 seconds
    Trigger trigger = TriggerBuilder.newTrigger()
        .withIdentity("myTrigger", "group1")
        .startNow()
        .withSchedule(SimpleScheduleBuilder.simpleSchedule()
            .withIntervalInSeconds(1)
            .repeatForever())
            .build();

    // Tell quartz to schedule the job using our trigger
    sched.scheduleJob(job, trigger);

    Thread.sleep(100_000);
  }

}
