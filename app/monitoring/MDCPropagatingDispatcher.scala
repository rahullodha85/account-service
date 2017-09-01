package monitoring

import java.util.concurrent.TimeUnit

import akka.dispatch._
import com.typesafe.config.Config
import org.slf4j.MDC

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{ Duration, FiniteDuration }

class MDCPropagatingDispatcherConfigurator(config: Config, prerequisites: DispatcherPrerequisites) extends MessageDispatcherConfigurator(config, prerequisites) {

  private val instance = new MDCPropagatingDispatcher(this, config.getString("id"),
    config.getInt("throughput"),
    FiniteDuration(config.getDuration("throughput-deadline-time", TimeUnit.NANOSECONDS), TimeUnit.NANOSECONDS),
    configureExecutor(),
    FiniteDuration(config.getDuration("shutdown-timeout", TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS))

  override def dispatcher(): MessageDispatcher = instance

}

class MDCPropagatingDispatcher(
    _configurator:                  MessageDispatcherConfigurator,
    id:                             String,
    throughput:                     Int,
    throughputDeadlineTime:         Duration,
    executorServiceFactoryProvider: ExecutorServiceFactoryProvider, shutdownTimeout: FiniteDuration
) extends Dispatcher(_configurator, id, throughput, throughputDeadlineTime, executorServiceFactoryProvider, shutdownTimeout) {

  self =>

  override def prepare(): ExecutionContext = new ExecutionContext {

    // capture the MDC
    val mdcContext = MDC.getCopyOfContextMap

    override def reportFailure(cause: Throwable): Unit = self.reportFailure(cause)

    override def execute(runnable: Runnable): Unit = self.execute(new Runnable {
      def run() = {
        // backup the callee MDC context
        val oldMDCContext = MDC.getCopyOfContextMap

        // Run the runnable with the captured context
        setContextMap(mdcContext)
        try {
          runnable.run()
        } finally {
          // restore the callee MDC context
          setContextMap(oldMDCContext)
        }
      }
    })

    private[this] def setContextMap(context: java.util.Map[String, String]): Unit = {
      if (context == null) {
        MDC.clear()
      } else {
        MDC.setContextMap(context)
      }
    }
  }

}
