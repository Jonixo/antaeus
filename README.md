## Antaeus

Antaeus (/ænˈtiːəs/), in Greek mythology, a giant of Libya, the son of the sea god Poseidon and the Earth goddess Gaia. He compelled all strangers who were passing through the country to wrestle with him. Whenever Antaeus touched the Earth (his mother), his strength was renewed, so that even if thrown to the ground, he was invincible. Heracles, in combat with him, discovered the source of his strength and, lifting him up from Earth, crushed him to death.

Welcome to our challenge.

## Architecture Changes

- `AntauesDAL` should be divided into two, `InvoiceDAL` and `CustomerDAL`.
- Add `FAILED` invoice status to model.
- Implemented Quartz Scheduler for cron job.
- Added option to trigger invoice processing by POST request.
- Added mocked Notification Service and Currency Exchange Provider.

## Services

### Customer Service

The customer service contains methods to fetch customers. No changes made here.

### Invoice Service

The invoice service contains methods to fetch or update invoices. It has been 
extended to fetch invoices by status and update the invoices. 

### Billing Service

The billing service contains methods to process pending invoices. It is the core of Antheus.

### Scheduler

Scheduler uses Quartz cron job to start the process of pending invoices.

### Notification Service

The notification service can send an e-mail or sms message to invoice owner about success
or failure of the process. It has not been implemented.

### Currency Exchange Provider

The currency exchange provider is an external service. It handles currency exchanges in case of a 
currency mismatch. It is a mocked service.

### Payment Provider 

The payment provider is an external service. It handles payment requests. It is a mocked service. 

## General Overview of the Process

The process of pending invoices can be triggered by cron job trigger, or a POST request.
Either way will not change how the process works. In order;

- Billing Service will get all of the `PENDING` invoices from the database in a list.
- Each invoice will be sent one by one to the payment process.
- In this process the invoice will be checked if it's `PAID` or `FAILED` already.
- Then It will be forwarded to the `Payment Provider`.
- At this point there are 4 possible outcomes;
  - Payment will be successful, return true.
  - Customer not found, log the error, return false.
  - Network error, log the issue, try 4 more times (total of 5) with 5 seconds between each try.
  - Currency mismatch, log the issue, try exchange.
    - Exchange successful, update invoice currency to required currency, return updated invoice.
    - Customer not found, log the error, return null.
    - Currency not found, log the error, return null.
    - Network error, log the issue, try 4 more times (total of 5) with 5 seconds between each try.
- Notify the customer about success or failure of the payment.

This process will regularly happen at the start of any month without any outside trigger due to cron job setup.

## Summary and Shortcomings

This version of Antaeus is really basic, it is still viable, but It needs important improvements 
to be actually used in real life. 

### Scalability

Antaeus is not currently scalable. Due to the way Antaeus getting and processing pending invoices
if we run two of them they would do the same query and work through pending invoices one by one at the same time
and brick the whole process. To fix this issue I would use a service to query the invoices, send 
invoices to Kafka topic or RabbitMQ queue. Then if we have multiple payment services up, they can 
pick the next invoice from the queue and process it. Lastly we can use k8s to load balance.

### Database

If the amount of invoices is in hundreds of thousands, I would recommend switching to ElasticSearch.
Traditional databases are much slower compared to ES and it can be used to save logs.

### User Experience and Realism

Instead of processing invoices at the start of each month, I would change the system, so the payment process 
will start every day and pay the invoices on their deadline. This would make sure the invoices
aren't piled up and make sure no invoices left unprocessed before their deadline.
To improve the user experience, I would check the customer balance 2 days before deadline and
inform users if their balance lacks funds to pay upcoming invoices.

## Last Thoughts 

It was a much better challenge than solving some hackerrank questions. I never used kotlin before
and I am so used to using Spring annotations, I forgot Quartz existed. It was hard for me to find quality
time to spend on this task, so It took me longer then I wanted, sorry about that.

Total time spent;
- One evening learning Kotlin, Quartz Scheduler and designing the solution.
- Around 12 hours coding.
- A couple more hours for README.md + cleaning up.

10/10, I would do another one. 
Have a nice day!

### Building

```
./gradlew build
```

### Running

There are 2 options for running Anteus. You either need libsqlite3 or docker. Docker is easier but requires some docker knowledge. We do recommend docker though.

*Running Natively*

Native java with sqlite (requires libsqlite3):

If you use homebrew on MacOS `brew install sqlite`.

```
./gradlew run
```

*Running through docker*

Install docker for your platform

```
docker build -t antaeus
docker run antaeus
```

### App Structure
The code given is structured as follows. Feel free however to modify the structure to fit your needs.
```
├── buildSrc
|  | gradle build scripts and project wide dependency declarations
|  └ src/main/kotlin/utils.kt 
|      Dependencies
|
├── pleo-antaeus-app
|       main() & initialization
|
├── pleo-antaeus-core
|       This is probably where you will introduce most of your new code.
|       Pay attention to the PaymentProvider and BillingService class.
|
├── pleo-antaeus-data
|       Module interfacing with the database. Contains the database 
|       models, mappings and access layer.
|
├── pleo-antaeus-models
|       Definition of the Internal and API models used throughout the
|       application.
|
└── pleo-antaeus-rest
        Entry point for HTTP REST API. This is where the routes are defined.
```

### Main Libraries and dependencies
* [Exposed](https://github.com/JetBrains/Exposed) - DSL for type-safe SQL
* [Javalin](https://javalin.io/) - Simple web framework (for REST)
* [kotlin-logging](https://github.com/MicroUtils/kotlin-logging) - Simple logging framework for Kotlin
* [JUnit 5](https://junit.org/junit5/) - Testing framework
* [Mockk](https://mockk.io/) - Mocking library
* [Sqlite3](https://sqlite.org/index.html) - Database storage engine

Happy hacking 😁!
