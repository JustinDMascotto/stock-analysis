package org.bde.stock.datasource.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.bde.stock.common.message.AssetCandleMessageKey;
import org.bde.stock.common.message.AssetCandleMessageValue;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.PreDestroy;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;


@Slf4j
@Service
public class DataFetcherService
{
    private KafkaTemplate<AssetCandleMessageKey, AssetCandleMessageValue> kafkaTemplate;

    private RestTemplate restTemplate;

    private UriComponentsBuilder uriBuilder;

    private Integer intervalMinutes;

    private String symbol;

    private Long initialStartTime;


    @Autowired
    public DataFetcherService( final KafkaTemplate<AssetCandleMessageKey, AssetCandleMessageValue> kafkaTemplate,
                               final RestTemplate restTemplate,
                               @Value( "${org.bde.stock.datasource.candle.interval-minutes}" ) final Integer interval,
                               @Value( "${org.bde.stock.datasource.symbol}" ) final String symbol,
                               @Value( "${org.bde.stock.datasource.startTime}" ) final Long startTime )
    {
        this.kafkaTemplate = kafkaTemplate;
        this.restTemplate = restTemplate;
        this.intervalMinutes = interval;
        this.symbol = symbol;
        this.initialStartTime = startTime;
        final String binanceInterval = translateInterval( this.intervalMinutes );
        this.uriBuilder = UriComponentsBuilder.fromUriString( String.format( BASE_URL + "/api/v3/klines?symbol=%s&interval=%s&startTime={startTime}&endTime={endTime}&limit=1000", this.symbol, binanceInterval ) );
    }


    private Instant retryAfter = null;

    private Long previousEndTime = null;

    private static final String BASE_URL = "https://api.binance.com";



    //    @Scheduled( cron = "0 0 * * ? *" )
    @Scheduled( cron = "${org.bde.stock.datasource.fetch-interval.cron}" )
    public void fetchData()
    {
        if ( retryAfter == null ||
             Instant.now().isAfter( retryAfter ) )
        {
            try
            {
                final var startTime = determineStartTime( initialStartTime, previousEndTime );
                final var endTime = determineEndTime( startTime );
                final var params = new HashMap<String,Long>();
                params.put( "startTime", startTime );
                params.put( "endTime", endTime );
                var requestUriComponents = uriBuilder.buildAndExpand( params );
                final var response =
                      restTemplate.exchange( requestUriComponents.toUri(),
                                             HttpMethod.GET,
                                             HttpEntity.EMPTY,
                                             new ParameterizedTypeReference<List<List<Object>>>() {} );
                final var candleKeyAndMessages = translateToKeyAndMessage( Objects.requireNonNull( response.getBody() ) );

                //sort so we send the earliest candle first / most resent last
                candleKeyAndMessages.sort( ( o1, o2 ) -> o1.getValue().getTimestamp().isBefore( o2.getValue().getTimestamp() ) ? -1 : 1 );

                sendMessages( candleKeyAndMessages );

                logLastCandlesTimestamp();
            }
            catch ( HttpStatusCodeException exception )
            {
                if ( exception.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS )
                {
                    retryAfter = Instant.ofEpochMilli( Long.parseLong( Objects.requireNonNull( exception.getResponseHeaders().getFirst( "Retry-After" ) ) ) );
                }
                else if ( exception.getStatusCode() == HttpStatus.I_AM_A_TEAPOT )
                {
                    log.error( "IP address banned" );
                    System.exit( -1 );
                }
            }
        }
    }


    private Long determineStartTime( final Long initialStartTime,
                                     final Long previousEndTime )
    {
        if( previousEndTime == null )
        {
            return initialStartTime;
        }
        else
        {
            return previousEndTime + 1L;
        }
    }


    private Long determineEndTime( final Long startTime )
    {
        final Long diff = Instant.now().toEpochMilli() - startTime;
        final Long intervalInMilli = intervalMinutes * 60L * 1000L;
        if ( diff / intervalInMilli > 1000 )
        {
            return startTime + 1000 * intervalInMilli;
        }
        else
        {
            return Instant.now().toEpochMilli();
        }
    }


    private void sendMessages( final List<Pair<AssetCandleMessageKey, AssetCandleMessageValue>> messages )
    {
        messages.forEach( keyValue -> kafkaTemplate.send( "asset-candle", keyValue.getKey(), keyValue.getValue() )
                                                   .addCallback( new ListenableFutureCallback<>()
                                                   {
                                                       @Override
                                                       public void onFailure( final Throwable ex )
                                                       {
                                                           log.error( "Failed to send candle", ex );
                                                       }


                                                       @Override
                                                       public void onSuccess( final SendResult<AssetCandleMessageKey, AssetCandleMessageValue> result )
                                                       {
                                                           log.debug( "Sent candle with timestamp " + result.getProducerRecord().value().getTimestamp() );
                                                           previousEndTime = result.getProducerRecord().value().getTimestamp().toInstant( ZoneOffset.UTC ).toEpochMilli();
                                                       }
                                                   } ) );
    }


    private List<Pair<AssetCandleMessageKey, AssetCandleMessageValue>> translateToKeyAndMessage( final List<List<Object>> binanceResponse )
    {
        return binanceResponse.stream().map( candle -> {
            final var openTime = Instant.ofEpochMilli( (Long) candle.get( 0 ) ).atOffset( ZoneOffset.UTC ).toLocalDateTime();
            final var open = Double.valueOf( (String) candle.get( 1 ) );
            final var high = Double.valueOf( (String) candle.get( 2 ) );
            final var low = Double.valueOf( (String) candle.get( 3 ) );
            final var close = Double.valueOf( (String) candle.get( 4 ) );
            final var volume = Double.valueOf( (String) candle.get( 5 ) );
            final var closeTime = Instant.ofEpochMilli( (Long) candle.get( 6 ) ).atOffset( ZoneOffset.UTC ).toLocalDateTime();
            final var quoteAssetVolume = Double.valueOf( (String) candle.get( 7 ) );
            final var numberOfTrades = (Integer) candle.get( 8 );
            final var takerBuyBaseAssetVolume = Double.valueOf( (String) candle.get( 9 ) );
            final var takerBuyQuoteAssetVolume = Double.valueOf( (String) candle.get( 10 ) );

            final var candleKey = AssetCandleMessageKey.builder()
                                                       .symbol( symbol )
                                                       .interval( intervalMinutes ).build();

            final var candleValue = AssetCandleMessageValue.builder()
                                                           .open( open )
                                                           .high( high )
                                                           .low( low )
                                                           .close( close )
                                                           .volume( volume )
                                                           .interval( intervalMinutes )
                                                           .timestamp( openTime ).build();

            return Pair.of( candleKey, candleValue );
        } ).collect( Collectors.toList() );
    }


    private String translateInterval( final Integer integerMinutes )
    {
        if ( integerMinutes >= 60 )
        {
            return integerMinutes / 60 + "h";
        }
        else
        {
            return integerMinutes + "m";
        }
    }


    @PreDestroy
    public void logLastCandlesTimestamp()
    {
        log.info( "Last message sent with timestamp: " + Optional.ofNullable( previousEndTime ).orElse( 0L ) );
    }
}
