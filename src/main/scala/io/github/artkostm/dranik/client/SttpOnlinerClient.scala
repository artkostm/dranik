package io.github.artkostm.dranik.client

import com.github.plokhotnyuk.jsoniter_scala.core._
import io.github.artkostm.dranik.client.OnlinerClient.{ApartmentsResponse, PkApiRequest}
import sttp.client3._
import sttp.model.{Header, MediaType}
import zio.clock.Clock
import zio.duration.Duration
import zio.{Has, Schedule, Task}

class SttpOnlinerClient(config: ClientConfig)(backend: OnlinerBackend)
  extends OnlinerClient.Service {

  override def getApartments(r: PkApiRequest): Task[ApartmentsResponse] =
    basicRequest
      .get(
        uri"${config.url}/search/apartments?page=${r.page}&bounds[lb][lat]=${r.lb.map(_.lat)}&bounds[lb][long]=${r.lb
          .map(_.lng)}&bounds[rt][lat]=${r.rb.map(_.lat)}&bounds[rt][long]=${r.rb.map(_.lng)}"
      )
      .header(Header.accept(MediaType.ApplicationJson))
      .header(Header.userAgent("PostmanRuntime/7.26.8"))
      .response(asByteArray.getRight.map(bytes => readFromArray[ApartmentsResponse](bytes))) // todo: abstract using protocols, avoid using jsoniter here
      .send(backend)
      .map(_.body)
      .retry(
        Schedule.recurs(config.retry.times) && Schedule
          .spaced(Duration.fromScala(config.retry.every))
      )
      .provide(Has(Clock.Service.live))

}
