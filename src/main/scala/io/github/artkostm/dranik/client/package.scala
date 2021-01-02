package io.github.artkostm.dranik

import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import sttp.capabilities.WebSockets
import sttp.capabilities.zio.ZioStreams
import sttp.client3.SttpBackend
import zio.{Has, Task, ZLayer}

import scala.concurrent.duration._
import scala.language.postfixOps

package object client {
  type OnlinerClient  = Has[OnlinerClient.Service]
  type OnlinerBackend = SttpBackend[Task, ZioStreams with WebSockets]

  final case class RetryConfig(times: Int, every: Duration)

  final case class ClientConfig(url: String,
                                auth: Option[Any] = None,
                                retry: RetryConfig = RetryConfig(0, 0 seconds))

  object OnlinerClient {

    lazy val live = ZLayer.fromServices[OnlinerBackend, ClientConfig, OnlinerClient.Service] {
      (backend, config) =>
        new SttpOnlinerClient(config)(backend)
    }

    trait Service {
      def getApartments(request: PkApiRequest): Task[ApartmentsResponse]
    }

    // request
    final case class LatLngRequest(lat: BigDecimal, lng: BigDecimal)
    final case class PkApiRequest(page: Option[Int] = Some(1),
                                  lb: Option[LatLngRequest],
                                  rb: Option[LatLngRequest])
    // response
    final case class PageResponse(limit: Int, items: Int, current: Int, last: Int)
    final case class LocationResponse(address: String,
                                      user_address: String,
                                      latitude: Double,
                                      longitude: Double)
    final case class ConversionResponse(amount: String, currency: String)
    final case class PriceResponse(amount: String,
                                   currency: String,
                                   converted: Map[String, ConversionResponse])
    final case class SellerResponse(`type`: String)
    final case class ApartmentResponse(id: Long,
                                       author_id: Long,
                                       location: LocationResponse,
                                       price: PriceResponse,
                                       resale: Boolean,
                                       number_of_rooms: Int,
                                       floor: Int,
                                       number_of_floors: Int,
                                       area: Map[String, Option[Double]],
                                       photo: String,
                                       seller: SellerResponse,
                                       created_at: String,
                                       last_time_up: String,
                                       up_available_in: Long,
                                       url: String,
                                       auction_bid: Map[String, String])
    final case class ApartmentsResponse(apartments: Seq[ApartmentResponse],
                                        total: Int,
                                        page: PageResponse)

    object ApartmentsResponse {
      implicit val apartmentsCodec: JsonValueCodec[ApartmentsResponse] = JsonCodecMaker.make
    }
  }
}
