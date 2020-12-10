package io.pleo.antaeus.core.scheduler

import io.pleo.antaeus.core.services.BillingService
import org.quartz.*
import org.quartz.CronScheduleBuilder.cronSchedule
import org.quartz.JobBuilder.newJob
import org.quartz.TriggerBuilder.newTrigger
import org.quartz.impl.StdSchedulerFactory
import java.nio.file.Path
import java.nio.file.Paths
import org.quartz.Job

class InvoiceJob : Job {
    override fun execute(context: JobExecutionContext) {

        val dataMap = context.jobDetail.jobDataMap
        val billingService:BillingService = dataMap["billingService"] as BillingService

        billingService.processPendingInvoices()

    }
}

class InvoiceJobScheduler(private val billingService: BillingService) {
    fun scheduleJob() {
        try {
            val sf = StdSchedulerFactory()
            val path: Path = Paths.get("","gradle","quartz.properties")
            sf.initialize(path.toAbsolutePath().toString())

            val scheduler:Scheduler =  sf.scheduler

            val jobMap = JobDataMap()
            jobMap["billingService"] = billingService

            val processInvoiceJob: JobDetail = newJob(InvoiceJob::class.java)
                    .withIdentity("invoiceJob", "g1")
                    .usingJobData(jobMap)
                    .build()

            val processInvoiceTrigger = newTrigger()
                    .withIdentity("processInvoiceTrigger", "g1")
                    .startNow()
                    .withSchedule(cronSchedule("0 0 0 1 *"))
                    .build()

            scheduler.scheduleJob(processInvoiceJob, processInvoiceTrigger)
            scheduler.start()
        } catch (se: SchedulerException) {
            se.printStackTrace()
        }
    }
}

