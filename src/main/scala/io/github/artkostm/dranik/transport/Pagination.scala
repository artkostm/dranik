package io.github.artkostm.dranik.transport

import io.github.artkostm.dranik.client.OnlinerClient.ApartmentsResponse
import zio.Task

trait Pagination {

  def apartmentsPage(call: Int => Task[ApartmentsResponse]): Task[Unit] = {
    def loop(page: Int): Task[Unit] =
      call(page).flatMap {
        case response if response.page.last > page => loop(page + 1)
        case _                                     => Task.unit
      }

    loop(1)
  }
}
