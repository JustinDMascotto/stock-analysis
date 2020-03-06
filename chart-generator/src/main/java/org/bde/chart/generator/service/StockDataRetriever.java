package org.bde.chart.generator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bde.chart.generator.entity.StockCandleEntity;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import org.bde.chart.generator.repository.StockCandleRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


@Service
public class StockDataRetriever
{

    private static final String URL_TEMPLATE = "https://www.alphavantage.co/query?function=TIME_SERIES_INTRADAY&symbol=%s&interval=%smin&outputsize=full&apikey=XLE8ORJ43RYJC2TV";

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern( "yyyy-MM-dd HH:mm:ss" );

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final RestTemplate restTemplate;

    private final StockCandleRepository repo;


    public StockDataRetriever( final StockCandleRepository repo )
    {
        List<HttpMessageConverter<?>> messageConverters = new ArrayList<HttpMessageConverter<?>>();
        messageConverters.add( new ByteArrayHttpMessageConverter() );
        messageConverters.add( new StringHttpMessageConverter() );
        restTemplate = new RestTemplate( messageConverters );
        this.repo = repo;
    }


    public void maybeRetrieveData( final String ticker,
                                   final Integer interval )
          throws Exception
    {
        final var httpHeaders = new HttpHeaders();
        httpHeaders.setAccept( Arrays.asList( MediaType.APPLICATION_JSON ) );

        final var entity = new HttpEntity<>( httpHeaders );

        final List<StockCandleEntity> latestDatesCandles = new ArrayList<>();
        var searchDate = LocalDate.now();

        do
        {
            latestDatesCandles.addAll( repo.findByTimestampAndTicker( searchDate,
                                                                      ticker ) );
            searchDate = searchDate.minusDays( 1 );
        }
        while ( latestDatesCandles.isEmpty() &&
                !searchDate.isBefore( LocalDate.now().minusDays( 7 ) ) );

        if ( latestDatesCandles.isEmpty() ||
             latestDatesCandles.get( 0 ).getTimestamp().isBefore( LocalDateTime.of( LocalDate.now(), LocalTime.of( 14, 0, 0 ) ) ) )

        {
            final var response = restTemplate.exchange( buildUrl( ticker, interval ),
                                                        HttpMethod.GET,
                                                        entity,
                                                        String.class );

            final var mapOfCandles = parseResponse( response.getBody(), interval, ticker );

            repo.saveAll( mapOfCandles.entrySet()
                                      .stream()
                                      .filter( entry -> latestDatesCandles.isEmpty() ||
                                                        entry.getKey().isAfter( latestDatesCandles.get( 0 ).getTimestamp() ) )
                                      .map( Map.Entry::getValue )
                                      .collect( Collectors.toList() ) );
        }
    }


    private String buildUrl( final String ticker,
                             final Integer interval )
    {
        return String.format( URL_TEMPLATE, ticker, interval.toString() );
    }


    private Map<LocalDateTime, StockCandleEntity> parseResponse( final String jsonBody,
                                                                 final Integer interval,
                                                                 final String ticker )
          throws Exception
    {
        final var node = MAPPER.readTree( jsonBody );
        final var timeSeries = node.get( "Time Series (5min)" );
        return IntStream.range( 0, 10000 )
                        .mapToObj( i -> {
                            final var candleTime = LocalDateTime.of( LocalDate.now( ZoneId.of( "America/New_York" ) ), LocalTime.of( 14, 0, 0 ) ).minusMinutes( 5 * i );
                            return Optional.ofNullable( timeSeries.get( candleTime.format( DATE_TIME_FORMATTER ) ) )
                                           .map( candle -> StockCandleEntity.builder()
                                                                            .timestamp( candleTime )
                                                                            .ticker( ticker )
                                                                            .interval( interval )
                                                                            .open( candle.get( "1. open" ).asDouble() )
                                                                            .high( candle.get( "2. high" ).asDouble() )
                                                                            .low( candle.get( "3. low" ).asDouble() )
                                                                            .close( candle.get( "4. close" ).asDouble() )
                                                                            .volume( candle.get( "5. volume" ).asInt() ).build() )
                                           .orElse( null );
                        } )
                        .filter( Objects::nonNull )
                        .collect( Collectors.toMap( StockCandleEntity::getTimestamp, c -> c ) );
    }
}
