// to run: amm -s --no-remote-logging -p BinanceApi.sc Reader.sc
import $ivy.`com.typesafe.akka::akka-actor:2.5.16`
import $ivy.`com.typesafe.akka::akka-stream:2.5.16`
import $ivy.`com.typesafe.akka::akka-cluster:2.5.16`

import scala.util.Properties
import scala.concurrent.{ Await, Future, Promise }
import scala.concurrent.duration._
import akka.{ Done, NotUsed }
import akka.stream.scaladsl._

val defaultMarkets = "BNBBTC,BTCUSDT"
val endpoint = "wss://stream.binance.com:9443"
val markets = Properties.envOrElse("MARKETS", defaultMarkets).split(",").toList

val wsUrl = endpoint + "/stream?streams=" + markets.
   map(_.toLowerCase).
   map{ case x => s"$x@trade" }.
   mkString("/")

// val wsUrl = endpoint + "/ws/bnbbtc@trade"
implicit val system = akka.actor.ActorSystem()
implicit val materializer = akka.stream.ActorMaterializer()
import system.dispatcher

println(s"connecting to $wsUrl")
// readLine
val client = new BinanceApiWebSocketClient(wsUrl)

Await.ready( Future {
    Thread.sleep(999999)
    0
}, Duration.Inf)
