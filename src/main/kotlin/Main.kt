import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("MainKt")

fun main() {
  logger.info("starting up")

  val server = Server()
  val instrumentStream = InstrumentStream()
  val quoteStream = QuoteStream()
  val candlestickManager = CandlestickManagerImpl()

  // Set to keep track of active instruments
  val activeInstruments: MutableSet<String> = mutableSetOf()

  // Connect to the instrument stream
  instrumentStream.connect { event ->
    when (event.type) {
      InstrumentEvent.Type.ADD -> {
        activeInstruments.add(event.data.isin)
        logger.info("Added instrument: ${event.data.isin}")
      }
      InstrumentEvent.Type.DELETE -> {
        activeInstruments.remove(event.data.isin)
        logger.info("Deleted instrument: ${event.data.isin}")
      }
    }
  }

  // Connect to the quote stream
  quoteStream.connect { event ->
    if (activeInstruments.contains(event.data.isin)) {
      candlestickManager.addQuote(event.data.isin, event.data)
      logger.info("Updated quote for instrument: ${event.data.isin}")
    }
  }

  // Set the candlestick manager service in the server
  server.service = candlestickManager
  server.start()
}

// Jackson ObjectMapper for JSON serialization and deserialization
val jackson: ObjectMapper =
  jacksonObjectMapper()
    .registerModule(JavaTimeModule())
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
