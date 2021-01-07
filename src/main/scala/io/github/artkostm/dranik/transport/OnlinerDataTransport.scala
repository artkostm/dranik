package io.github.artkostm.dranik.transport

import com.github.plokhotnyuk.jsoniter_scala.core._
import io.github.artkostm.dranik.ApplicationConfig.TransportConfig
import io.github.artkostm.dranik.client.OnlinerClient
import io.github.artkostm.dranik.client.OnlinerClient.{ApartmentsResponse, LatLngRequest, PkApiRequest}
import io.github.artkostm.dranik.fs.FS
import zio.Task

import java.text.{DecimalFormat, DecimalFormatSymbols}
import java.time.{Clock, LocalDateTime}
import java.util.UUID

protected[transport] class OnlinerDataTransport(fs: FS.Service, client: OnlinerClient.Service, config: TransportConfig)
  extends DataTransport.Service
  with Windowing
  with Pagination {
  import OnlinerDataTransport._

  override def downloadApartments(): Task[Unit] = {
    val today = LocalDateTime.now(Clock.systemUTC())
    val p     = partition(today, UUID.randomUUID().toString)
    val frame = ((config.leftBoundLatitude, config.leftBoundLongitude), (config.rightBoundLatitude, config.rightBoundLongitude))
    val steps = (config.latitudeStep, config.longitudeStep)
    sliding(frame, steps) {
      case ((startLat, startLng), (endLat, endLng)) =>
        apartmentsPage { page =>
          client
            .getApartments(
              PkApiRequest(page = Some(page),
                           lb = Some(LatLngRequest(startLat, startLng)),
                           rb = Some(LatLngRequest(endLat, endLng)))
            )
            .tap { response =>
              if (response.apartments.nonEmpty)
                fs.write(
                  s"$p/${decimalFormat.format(startLat)}-${decimalFormat.format(startLng)}__${decimalFormat
                    .format(endLat)}-${decimalFormat.format(endLng)}_p${page}_${UUID.randomUUID()}.json",
                  writeToArray[ApartmentsResponse](response)
                )
              else Task.unit
            }
        }
    }
  }
}

object OnlinerDataTransport {
  protected val decimalFormat: DecimalFormat = {
    val decimalFormatSymbols = new DecimalFormatSymbols()
    decimalFormatSymbols.setDecimalSeparator('_')
    decimalFormatSymbols.setGroupingSeparator('_')
    new DecimalFormat("#,###.00", decimalFormatSymbols)
  }

  def partition(ldt: LocalDateTime, id: String): String =
    f"year=${ldt.getYear}/month=${ldt.getMonth.getValue}%02d/day=${ldt.getDayOfMonth}%02d/hour=${ldt.getHour}%02d/etlid=$id"
}