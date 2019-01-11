// pieces from @oyvindberg/binance-scala-api

import $ivy.`com.squareup.okhttp3:okhttp:3.12.1`
import $ivy.`io.circe::circe-core:0.10.0`
import $ivy.`io.circe::circe-parser:0.10.0`
import $ivy.`io.circe::circe-generic:0.10.0`

import io.circe._
import io.circe.generic.semiauto.deriveDecoder
import java.util.Date

// = = = = =  ERROR HANDLING = = = = = //

case class BinanceApiError(code: Int,msg: String)
object BinanceApiError {
  implicit lazy val BinanceApiErrorDecoder: Decoder[BinanceApiError] =
    Decoder.forProduct2("code", "msg")(BinanceApiError.apply)
}
final case class BinanceApiExceptionError(error: BinanceApiError) extends RuntimeException(error.msg)
final case class BinanceApiExceptionMsg(value:   String)          extends RuntimeException(value)
final case class BinanceApiExceptionCause(cause: Throwable)       extends RuntimeException(cause)

// = = = = =  DOMAIN MODEL = = = = = //

case class Instant(millis: Long) {
  def str: String = new Date(millis).toString
}
case class Quantity(value:  BigDecimal)
case class Volume(value:    BigDecimal)
case class Price(value:     BigDecimal)
case class Amount(value:    BigDecimal)
case class Percent(value:   BigDecimal)
case class PriceDiff(value: BigDecimal)
case class Asset(value:   String)
case class Symbol(value:  String)
case class OrderId(value: Long)

trait TradeBase {
  def tradeId:                Long
  def price:                  Price
  def quantity:               Quantity
  def tradeTime:              Option[Instant]
  def isBuyerMaker:           Boolean
  def isBestMatch:            Boolean
}
case class TradeEvent(
    eventType:              String,
    eventTime:              Instant,
    symbol:                 Symbol,
    tradeId:                Long,
    price:                  Price,
    quantity:               Quantity,
    buyerOrderId:           Long,
    sellerOrderId:          Long,
    tradeTime:              Option[Instant],
    isBuyerMaker:           Boolean,
    isBestMatch:            Boolean
) extends TradeBase

trait AggTradeBase {
  def aggregatedTradeId:      Long
  def price:                  Price
  def quantity:               Quantity
  def firstBreakdownTradeId:  Long
  def lastBreakdownTradeId:   Long
  def tradeTime:              Option[Instant]
  def isBuyerMaker:           Boolean
  def wasTradeBestPriceMatch: Boolean
}

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

object Decoders {

  implicit lazy val AmountDecoder:    Decoder[Amount]    = Decoder.decodeBigDecimal.map(Amount.apply)
  implicit lazy val InstantDecoder:   Decoder[Instant]   = Decoder.decodeLong.map(Instant.apply)
  implicit lazy val PriceDecoder:     Decoder[Price]     = Decoder.decodeBigDecimal.map(Price.apply)
  implicit lazy val QuantityDecoder:  Decoder[Quantity]  = Decoder.decodeBigDecimal.map(Quantity.apply)
  implicit lazy val SymbolDecoder:    Decoder[Symbol]    = Decoder.decodeString.map(Symbol.apply)

  implicit lazy val TradeEventDecoder: Decoder[TradeEvent] =
    Decoder.forProduct11("e", "E", "s", "t", "p", "q", "b", "a", "T", "m", "M")(TradeEvent.apply)
  implicit lazy val AggTradeEventDecoder: Decoder[AggTradeEvent] =
    Decoder.forProduct11("e", "E", "s", "a", "p", "q", "f", "l", "T", "m", "M")(AggTradeEvent.apply)
}

// = = = = =  WEB SOCKET = = = = = //
class BinanceApiWebSocketListener extends okhttp3.WebSocketListener {
  import Decoders._ 
  import okhttp3._

  override def onMessage(webSocket: WebSocket, text: String): Unit =
    println("Imcoming: " + text)

  override def onFailure(webSocket: WebSocket, t: Throwable, response: Response): Unit =
    throw BinanceApiExceptionCause(t)
}

class BinanceApiWebSocketClient(streamUrl: String) {

  val client = new okhttp3.OkHttpClient
  
  println("starting web socket client")
  val ws = client.newWebSocket(
    new okhttp3.Request.Builder()
      .url(streamUrl)
      .build,
    new BinanceApiWebSocketListener
  )

  // ws.sendOnOpen("ping", "{"some data":"in JSON format"}");
  // println(ws.getClass.getMethods)
   
  // @throws[java.io.IOException]
  // override def close(): Unit = client.dispatcher.executorService.shutdown
}

