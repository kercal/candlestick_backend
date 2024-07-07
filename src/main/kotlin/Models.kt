import java.time.Instant
import com.fasterxml.jackson.annotation.JsonFormat

data class InstrumentEvent(val type: Type, val data: Instrument) {
  enum class Type {
    ADD,
    DELETE
  }
}

data class QuoteEvent(val data: Quote)

data class Instrument(val isin: ISIN, val description: String)
typealias ISIN = String

data class Quote(val isin: ISIN, val price: Price)
typealias Price = Double

data class Candlestick(
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
  val openTimestamp: Instant,
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
  var closeTimestamp: Instant,
  val openPrice: Price,
  var highPrice: Price,
  var lowPrice: Price,
  var closingPrice: Price
)

interface CandlestickManager {
  fun getCandlesticks(isin: String): List<Candlestick>
}
