package com.egg.commerce.quartz.util;

import org.quartz.JobExecutionContext;
import com.egg.commerce.quartz.domain.SysJob;

/**
 * 定时任务处理（允许并发执行）
 * 
 * @author egg
 *
 */
public class QuartzJobExecution extends AbstractQuartzJob
{
    @Override
    protected void doExecute(JobExecutionContext context, SysJob sysJob) throws Exception
    {
        JobInvokeUtil.invokeMethod(sysJob);
    }
}
