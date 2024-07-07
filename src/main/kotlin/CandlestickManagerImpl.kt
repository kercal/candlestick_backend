import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.function.Supplier
import org.slf4j.LoggerFactory

class CandlestickManagerImpl(private val currentTimeSupplier: Supplier<Instant> = Supplier { Instant.now() }) : CandlestickManager {
    private val logger = LoggerFactory.getLogger(CandlestickManagerImpl::class.java)
    private val candlesticks: MutableMap<String, MutableList<Candlestick>> = mutableMapOf()

    override fun getCandlesticks(isin: String): List<Candlestick> {
        logger.info("Fetching candlesticks for ISIN: $isin")

        val currentTime = currentTimeSupplier.get().truncatedTo(ChronoUnit.MINUTES)
        val startTime = currentTime.minus(30, ChronoUnit.MINUTES)

        val candleList = candlesticks[isin] ?: return emptyList()
        val result = mutableListOf<Candlestick>()

        var lastCandle: Candlestick? = null
        var time = startTime
        while (time.isBefore(currentTime) || time == currentTime) {
            val candle = candleList.find { it.openTimestamp == time }
            if (candle != null) {
                result.add(candle)
                lastCandle = candle
            } else if (lastCandle != null) {
                // Reuse the last candlestick if no new quotes were received
                result.add(lastCandle.copy(openTimestamp = time, closeTimestamp = time.plus(1, ChronoUnit.MINUTES)))
            }
            time = time.plus(1, ChronoUnit.MINUTES)
        }

        logger.info("Candlesticks fetched: $result")
        return result
    }

    fun addQuote(isin: String, quote: Quote) {
        logger.info("Adding quote for ISIN: $isin, Quote: $quote")
        val currentTimestamp = currentTimeSupplier.get().truncatedTo(ChronoUnit.MINUTES)
        val candleList = candlesticks.getOrPut(isin) { mutableListOf() }
        val lastCandle = candleList.lastOrNull()

        if (lastCandle == null || lastCandle.closeTimestamp <= currentTimestamp) {
            // Create a new candlestick if there's no existing one or the last one is closed
            val newCandle = Candlestick(
                openTimestamp = currentTimestamp,
                closeTimestamp = currentTimestamp.plus(1, ChronoUnit.MINUTES),
                openPrice = quote.price,
                highPrice = quote.price,
                lowPrice = quote.price,
                closingPrice = quote.price
            )
            candleList.add(newCandle)
            logger.info("Created new candlestick: $newCandle")
        } else {
            // Update the existing candlestick with new price information
            lastCandle.apply {
                closingPrice = quote.price
                highPrice = maxOf(highPrice, quote.price)
                lowPrice = minOf(lowPrice, quote.price)
            }
            logger.info("Updated existing candlestick: $lastCandle")
        }

        // Clean up old candlesticks beyond the 30-minute window
        val thirtyMinutesAgo = currentTimestamp.minus(30, ChronoUnit.MINUTES)
        candlesticks[isin] = candleList.filter { it.openTimestamp.isAfter(thirtyMinutesAgo) }.toMutableList()
    }
}
