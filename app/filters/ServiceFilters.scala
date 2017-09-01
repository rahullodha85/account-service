package filters

import javax.inject.Inject

import play.api.http.DefaultHttpFilters

// common logging and metrics for all requests
class ServiceFilters @Inject() (
  identifyRequestFilter: IdentifyRequestFilter,
  timing:                TimingFilter,
  increment:             IncrementFilter,
  exception:             ExceptionFilter,
  cachingFilter:         CachingFilter
) extends DefaultHttpFilters(identifyRequestFilter, timing, increment, exception, cachingFilter)
