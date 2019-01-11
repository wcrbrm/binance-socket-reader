// to run: amm -s --no-remote-logging -p BinanceApi.sc Reader.sc

import $ivy.`com.typesafe.akka::akka-actor:2.5.16`
import $ivy.`com.typesafe.akka::akka-stream:2.5.16`
import $ivy.`com.typesafe.akka::akka-cluster:2.5.16`
import $ivy.`com.typesafe::ssl-config-akka:0.2.2`
import $ivy.`com.squareup.okhttp3:okhttp:3.12.1`
import $ivy.`io.circe::circe-core:0.10.0`
import $ivy.`io.circe::circe-parser:0.10.0`
import $ivy.`io.circe::circe-generic:0.10.0`

import scala.util.Properties
import scala.concurrent.{ Await, Future, Promise }
import scala.concurrent.duration._
import akka.{ Done, NotUsed }
import akka.stream.scaladsl._
import com.typesafe.sslconfig.akka.AkkaSSLConfig
import io.circe._
import java.io.{Closeable, IOException}
import okhttp3.{OkHttpClient, Request, Response, WebSocket, WebSocketListener}


val defaultMarkets = "BNBBTC,BTCUSDT"
val endpoint = "wss://stream.binance.com:9443"
val markets = Properties.envOrElse("MARKETS", defaultMarkets).split(",").toList

// val wsUrl = endpoint + "/stream?streams=" + markets.
   // map(_.toLowerCase).
   // map{ case x => s"$x@trade/$x@aggTrade" }.
   // mkString("/")

val wsUrl = endpoint + "/ws/bnbbtc@trade"

implicit val system = akka.actor.ActorSystem()
implicit val materializer = akka.stream.ActorMaterializer()
import system.dispatcher

println(s"connecting to $wsUrl")


/**
  * An aggregated trade event for a symbol.
  */
case class AggTradeEvent(
    eventType:              String,
    eventTime:              Instant,
    symbol:                 Symbol,
    aggregatedTradeId:      Long,
    price:                  Price,
    quantity:               Quantity,
    firstBreakdownTradeId:  Long,
    lastBreakdownTradeId:   Long,
    tradeTime:              Option[Instant],
    isBuyerMaker:           Boolean,
    wasTradeBestPriceMatch: Boolean
) extends AggTradeBase

/**
  * Binance API WebSocket listener.
  */
class BinanceApiWebSocketListener[T: Decoder](var callback: T => Unit) extends WebSocketListener {
  override def onMessage(webSocket: WebSocket, text: String): Unit =
    try {
      parser.decode[T](text) match {
        case Right(event) => callback(event)
        case Left(error)  => throw BinanceApiExceptionMsg(s"Couldn't decode $text: $error")
      }
    } catch {
      case e: IOException =>
        throw BinanceApiExceptionCause(e)
    }

  override def onFailure(webSocket: WebSocket, t: Throwable, response: Response): Unit =
    throw BinanceApiExceptionCause(t)
}

/**
  * Binance API WebSocket client implementation using OkHttp.
  */
class BinanceApiWebSocketClientImpl() extends BinanceApiWebSocketClient with Closeable {
  private val client = new OkHttpClient

  override def onAggTradeEvent(symbol: Symbol)(callback: AggTradeEvent => Unit): Unit =
    createNewWebSocket(s"${symbol.value.toLowerCase}@aggTrade",
                       new BinanceApiWebSocketListener[AggTradeEvent](callback))
  override def onTradeEvent(symbol: Symbol)(callback: TradeEvent => Unit): Unit =
    createNewWebSocket(s"${symbol.value.toLowerCase}@trade",
                       new BinanceApiWebSocketListener[TradeEvent](callback))
}


readLine