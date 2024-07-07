import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.function.Supplier
import kotlin.test.assertEquals

class MainTest {

  private lateinit var candlestickManager: CandlestickManager
  private lateinit var server: Server

  @BeforeEach
  fun setUp() {
    candlestickManager = mockk()
    server = Server()
    server.service = candlestickManager
  }

  @Test
  fun `should return candlesticks for valid ISIN`() {
    val isin = "HM7113S18007"
    val candlesticks = listOf(
      Candlestick(
        openTimestamp = Instant.parse("2024-07-07T13:00:00Z"),
        closeTimestamp = Instant.parse("2024-07-07T13:01:00Z"),
        openPrice = 100.0,
        highPrice = 110.0,
        lowPrice = 90.0,
        closingPrice = 105.0
      )
    )
    every { candlestickManager.getCandlesticks(isin) } returns candlesticks

    val request = Request(Method.GET, "/candlesticks").query("isin", isin)
    val response = server.getRoutes()(request)

    assertEquals(Status.OK, response.status)
    assertEquals(jackson.writeValueAsString(candlesticks), response.bodyString())
  }

  @Test
  fun `should return bad request for missing ISIN`() {
    val request = Request(Method.GET, "/candlesticks")
    val response = server.getRoutes()(request)

    assertEquals(Status.BAD_REQUEST, response.status)
    assertEquals("{'reason': 'missing_isin'}", response.bodyString())
  }

  @Test
  fun `should create new candlestick if no existing candlestick for the minute`() {
    val isin = "HM7113S18007"
    val quote = Quote(isin, 100.0)
    val currentTimeSupplier = mockk<Supplier<Instant>>()
    val now = Instant.now().truncatedTo(ChronoUnit.MINUTES)
    every { currentTimeSupplier.get() } returns now
    val candlestickManagerImpl = CandlestickManagerImpl(currentTimeSupplier)
    val initialCandlestickCount = candlestickManagerImpl.getCandlesticks(isin).size

    candlestickManagerImpl.addQuote(isin, quote)

    val updatedCandlestickCount = candlestickManagerImpl.getCandlesticks(isin).size
    assertEquals(initialCandlestickCount + 1, updatedCandlestickCount)
  }

  @Test
  fun `should update existing candlestick with new quote`() {
    val isin = "HM7113S18007"
    val currentTimeSupplier = mockk<Supplier<Instant>>()
    val now = Instant.now().truncatedTo(ChronoUnit.MINUTES)
    every { currentTimeSupplier.get() } returns now
    val candlestickManagerImpl = CandlestickManagerImpl(currentTimeSupplier)
    val initialQuote = Quote(isin, 100.0)
    candlestickManagerImpl.addQuote(isin, initialQuote)

    val newQuote = Quote(isin, 120.0)
    candlestickManagerImpl.addQuote(isin, newQuote)

    val candlesticks = candlestickManagerImpl.getCandlesticks(isin)
    val updatedCandlestick = candlesticks.last()

    assertEquals(100.0, updatedCandlestick.openPrice)
    assertEquals(120.0, updatedCandlestick.highPrice)
    assertEquals(100.0, updatedCandlestick.lowPrice)
    assertEquals(120.0, updatedCandlestick.closingPrice)
  }

  @Test
  fun `should reuse last candlestick if no new quotes received`() {
    val isin = "HM7113S18007"
    val currentTimeSupplier = mockk<Supplier<Instant>>()
    val now = Instant.now().truncatedTo(ChronoUnit.MINUTES)
    val later = now.plus(2, ChronoUnit.MINUTES)
    every { currentTimeSupplier.get() } returns now andThen later
    val candlestickManagerImpl = CandlestickManagerImpl(currentTimeSupplier)
    val initialQuote = Quote(isin, 100.0)
    candlestickManagerImpl.addQuote(isin, initialQuote)

    val reusedCandlesticks = candlestickManagerImpl.getCandlesticks(isin)
    val lastCandlestick = reusedCandlesticks.last()

    assertEquals(initialQuote.price, lastCandlestick.openPrice)
    assertEquals(initialQuote.price, lastCandlestick.highPrice)
    assertEquals(initialQuote.price, lastCandlestick.lowPrice)
    assertEquals(initialQuote.price, lastCandlestick.closingPrice)
  }
}
