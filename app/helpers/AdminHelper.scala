package helpers

import play.api.libs.json.{ Json, JsValue }
import scala.collection.mutable
import java.lang.management.{ BufferPoolMXBean, ManagementFactory }

object AdminHelper {
  def extractJvmStats(): JsValue = {
    import scala.collection.JavaConverters._

    val out: mutable.Map[String, Double] = mutable.Map.empty

    val mem = ManagementFactory.getMemoryMXBean

    val heap = mem.getHeapMemoryUsage
    out += ("jvm_heap_committed" -> heap.getCommitted)
    out += ("jvm_heap_max" -> heap.getMax)
    out += ("jvm_heap_used" -> heap.getUsed)

    val nonheap = mem.getNonHeapMemoryUsage
    out += ("jvm_nonheap_committed" -> nonheap.getCommitted)
    out += ("jvm_nonheap_max" -> nonheap.getMax)
    out += ("jvm_nonheap_used" -> nonheap.getUsed)

    val threads = ManagementFactory.getThreadMXBean
    out += ("jvm_thread_daemon_count" -> threads.getDaemonThreadCount.toLong)
    out += ("jvm_thread_count" -> threads.getThreadCount.toLong)
    out += ("jvm_thread_peak_count" -> threads.getPeakThreadCount.toLong)

    val runtime = ManagementFactory.getRuntimeMXBean
    out += ("jvm_start_time" -> runtime.getStartTime)
    out += ("jvm_uptime" -> runtime.getUptime)

    val os = ManagementFactory.getOperatingSystemMXBean
    out += ("jvm_num_cpus" -> os.getAvailableProcessors.toLong)
    os match {
      case unix: com.sun.management.UnixOperatingSystemMXBean =>
        out += ("jvm_fd_count" -> unix.getOpenFileDescriptorCount)
        out += ("jvm_fd_limit" -> unix.getMaxFileDescriptorCount)
      case _ => // ew, Windows... or something
    }

    val compilation = ManagementFactory.getCompilationMXBean
    out += ("jvm_compilation_time_msec" -> compilation.getTotalCompilationTime)

    val classes = ManagementFactory.getClassLoadingMXBean
    out += ("jvm_classes_total_loaded" -> classes.getTotalLoadedClassCount)
    out += ("jvm_classes_total_unloaded" -> classes.getUnloadedClassCount)
    out += ("jvm_classes_current_loaded" -> classes.getLoadedClassCount.toLong)

    var postGCTotalUsage = 0L
    var currentTotalUsage = 0L
    ManagementFactory.getMemoryPoolMXBeans.asScala.foreach { pool =>
      val name = pool.getName
      Option(pool.getCollectionUsage).foreach { usage =>
        out += ("jvm_post_gc_" + name + "_used" -> usage.getUsed)
        postGCTotalUsage += usage.getUsed
        out += ("jvm_post_gc_" + name + "_max" -> usage.getMax)
      }
      Option(pool.getUsage) foreach { usage =>
        out += ("jvm_current_mem_" + name + "_used" -> usage.getUsed)
        currentTotalUsage += usage.getUsed
        out += ("jvm_current_mem_" + name + "_max" -> usage.getMax)
      }
    }
    out += ("jvm_post_gc_used" -> postGCTotalUsage)
    out += ("jvm_current_mem_used" -> currentTotalUsage)

    ManagementFactory.getPlatformMXBeans(classOf[BufferPoolMXBean]).asScala.foreach { bp =>
      val name = bp.getName
      out += ("jvm_buffer_" + name + "_count" -> bp.getCount)
      out += ("jvm_buffer_" + name + "_used" -> bp.getMemoryUsed)
      out += ("jvm_buffer_" + name + "_max" -> bp.getTotalCapacity)
    }

    Json.toJson(out.toMap)
  }
}
