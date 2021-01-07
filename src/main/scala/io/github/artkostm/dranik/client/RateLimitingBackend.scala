package io.github.artkostm.dranik.client

import io.github.resilience4j.ratelimiter.RateLimiter
import sttp.capabilities.Effect
import sttp.client3.{Request, Response, SttpBackend}
import sttp.monad.MonadError

private[client] class RateLimitingBackend[F[_], P](rateLimiter: RateLimiter, delegate: SttpBackend[F, P])
  extends SttpBackend[F, P] {
  private implicit val monadError: MonadError[F] = delegate.responseMonad

  override def send[T, R >: P with Effect[F]](request: Request[T, R]): F[Response[T]] =
    RateLimitingBackend.decorateF(rateLimiter, delegate.send(request))

  override def close(): F[Unit] = delegate.close()

  override def responseMonad: MonadError[F] = monadError
}

object RateLimitingBackend {

  def apply[F[_], P](rateLimiter: RateLimiter, delegate: SttpBackend[F, P]): SttpBackend[F, P] =
    new RateLimitingBackend[F, P](rateLimiter, delegate)

  def decorateF[F[_], T](rateLimiter: RateLimiter,
                         service: => F[T])(implicit monadError: MonadError[F]): F[T] =
    monadError.flatMap(monadError.unit { () }) { _ =>
      try {
        RateLimiter.waitForPermission(rateLimiter)
        service
      } catch {
        case t: Throwable => monadError.error(t)
      }
    }
}
