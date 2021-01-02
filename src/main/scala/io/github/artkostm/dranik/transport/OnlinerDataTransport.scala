package io.github.artkostm.dranik.transport

import com.github.plokhotnyuk.jsoniter_scala.core._
import io.github.artkostm.dranik.client.OnlinerClient
import io.github.artkostm.dranik.client.OnlinerClient.{ApartmentsResponse, LatLngRequest, PkApiRequest}
import io.github.artkostm.dranik.fs.FS
import zio.Task

import java.text.{DecimalFormat, DecimalFormatSymbols}
import java.util.UUID

protected[transport] class OnlinerDataTransport(fs: FS.Service, client: OnlinerClient.Service) extends DataTransport.Service with MapCrawler with Pagination {
  private val decimalFormatSymbols = {
    val x = new DecimalFormatSymbols()
    x.setDecimalSeparator('_')
    x.setGroupingSeparator('_')
    x
  }

  private val df = new DecimalFormat("#,###.00", decimalFormatSymbols)

  override def downloadApartments(): Task[Unit] =
    sliding {
      case ((startLat, startLng), (endLat, endLng)) =>
        apartmentsPage { page =>
          client.getApartments(PkApiRequest(
            page = Some(page),
            lb = Some(LatLngRequest(startLat, startLng)),
            rb = Some(LatLngRequest(endLat, endLng))))
            .tap { response =>
              if(response.apartments.nonEmpty) fs.write(s"p${page}_${df.format(startLat)}-${df.format(startLng)}__${df.format(endLat)}-${df.format(endLng)}_${UUID.randomUUID()}.json", writeToArray[ApartmentsResponse](response))
              else Task.unit
            }
        }
    }
}
