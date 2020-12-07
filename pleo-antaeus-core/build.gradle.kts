plugins {
    kotlin("jvm")
}

kotlinProject()

dependencies {
    implementation(project(":pleo-antaeus-data"))
    // Cron Scheduler
    implementation("org.quartz-scheduler:quartz:2.3.0")
    api(project(":pleo-antaeus-models"))
}