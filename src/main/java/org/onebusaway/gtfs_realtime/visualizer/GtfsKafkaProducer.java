package org.onebusaway.gtfs_realtime.visualizer;

import com.google.transit.realtime.GtfsRealtime;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import com.google.common.io.ByteStreams;

/**
 * Created by mike on 3/04/17.
 */
public class GtfsKafkaProducer {

    static KafkaProducer<byte[], byte[]> producer = null;
    static String topic;

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        // Bad doc:        http://kafka.apache.org/documentation.html#producerconfigs
        // Incomplete doc: http://kafka.apache.org/documentation.html#newproducerconfigs
        // Java doc:       http://kafka.apache.org/082/javadoc/
        Properties props = new Properties();
        props.put("client.id", "GtfsKafkaProducer");
        // NOTE: that sending host resolver must resolve kafka advertised.host.name ip
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                "localhost:9092"); // apache-kafka:9092
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.ByteArraySerializer");
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.ByteArraySerializer");
        // how many times to retry when produce request fails?
        props.put("retries", "3");
        props.put("linger.ms", 5);
        props.put("max.request.size", 10485760);

        producer = new KafkaProducer<>(props);

        final String optApiKey = "";
        final String optFeed = "bne-bus";
        final String optUrl = "https://gtfsrt.api.translink.com.au/Feed/SEQ";

        // Reading GTFS from a URL
        GtfsRealtime.FeedMessage msg;
        byte[] gtfs;
        try (InputStream in = GetGtfs.feedUrlStream(optApiKey, optFeed, optUrl)) {
            gtfs = ByteStreams.toByteArray(in);
            msg = GtfsRealtime.FeedMessage.parseFrom(gtfs);
        }
        long timestamp;
        if (msg.hasHeader() && msg.getHeader().hasTimestamp()) {
            timestamp = msg.getHeader().getTimestamp();
        } else {
            timestamp = new GregorianCalendar().getTimeInMillis();
            SharedStderrLog.log("Info: GTFS contained no timestamp. Added " +
                    timestamp + " " + formatEpoch(timestamp));
        }

       ProducerRecord<byte[], byte[]> data =
                new ProducerRecord<>("raw-gtfs-" + optFeed, longToBytes(timestamp), gtfs);

        topic = "raw-gtfs-" + optFeed;
        produceSync(longToBytes(timestamp), gtfs);
        //produceAsync("async");
    }


    /* Produce a record and wait for server to reply. Throw an exception if something goes wrong */
    private static void produceSync(byte[] key, byte[] value) throws ExecutionException, InterruptedException {
        ProducerRecord<byte[], byte[]> record = new ProducerRecord<byte[], byte[]>(topic, key, value);
        producer.send(record).get();
    }

    /* Produce a record without waiting for server. This includes a callback that will print an error if something goes wrong */
    private static void produceAsync(byte[] key, byte[] value) {
        ProducerRecord<byte[], byte[]> record = new ProducerRecord<byte[], byte[]>(topic, key, value);
        producer.send(record, new DemoProducerCallback());
    }

    public static String formatEpoch(long epochSeconds) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.");
        return sdf.format(new Date(epochSeconds * 1000));
    }

    public static byte[] longToBytes(long in) {
        byte[] bytes = new byte[8];
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        bb.order(ByteOrder.LITTLE_ENDIAN); // As Protocol Buffers are a Little Endian format.
        bb.putLong(in);
        return bytes;
    }
}

class DemoProducerCallback implements Callback {

    @Override
    public void onCompletion(RecordMetadata recordMetadata, Exception e) {
        if (e != null) {
            System.out.println("Error producing to topic " + recordMetadata.topic());
            e.printStackTrace();
        }
    }
}
