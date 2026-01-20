package com.hrconnect.socle.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Classe utilitaire pour créer des configurations Kafka standardisées.
 *
 * <p>Permet de factoriser les paramètres communs et d'assurer la cohérence
 * entre tous les microservices.</p>
 *
 * <p>Exemple d'utilisation :</p>
 * <pre>
 * {@code
 * // Producer
 * Map<String, Object> props = KafkaConfigUtils.producerConfig(bootstrapServers);
 *
 * // Consumer
 * Map<String, Object> props = KafkaConfigUtils.consumerConfig(bootstrapServers, "my-group");
 * }
 * </pre>
 */
public final class KafkaConfigUtils {

    private KafkaConfigUtils() {
        // Utility class
    }

    /**
     * Crée une configuration Producer standard avec :
     * - StringSerializer pour les clés
     * - JsonSerializer pour les valeurs
     * - acks=all, retries=3, idempotence=true
     *
     * @param bootstrapServers Adresse des brokers Kafka
     * @return Configuration du producer
     */
    public static Map<String, Object> producerConfig(String bootstrapServers) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
        return props;
    }

    /**
     * Crée une configuration Producer sans les type headers.
     * Utile quand le consumer n'utilise pas les headers pour la désérialisation.
     *
     * @param bootstrapServers Adresse des brokers Kafka
     * @return Configuration du producer
     */
    public static Map<String, Object> producerConfigWithoutTypeHeaders(String bootstrapServers) {
        Map<String, Object> props = producerConfig(bootstrapServers);
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return props;
    }

    /**
     * Crée une configuration Consumer standard avec :
     * - StringDeserializer pour les clés
     * - auto.offset.reset=earliest
     *
     * @param bootstrapServers Adresse des brokers Kafka
     * @param groupId          ID du groupe de consommateurs
     * @return Configuration du consumer
     */
    public static Map<String, Object> consumerConfig(String bootstrapServers, String groupId) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return props;
    }

    /**
     * Crée une configuration Consumer avec JsonDeserializer pour un type spécifique.
     *
     * @param bootstrapServers Adresse des brokers Kafka
     * @param groupId          ID du groupe de consommateurs
     * @param valueType        Classe du type de valeur à désérialiser
     * @param trustedPackages  Packages autorisés pour la désérialisation
     * @return Configuration du consumer
     */
    public static Map<String, Object> consumerConfig(String bootstrapServers, String groupId,
                                                      Class<?> valueType, String... trustedPackages) {
        Map<String, Object> props = consumerConfig(bootstrapServers, groupId);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, valueType.getName());
        props.put(JsonDeserializer.TRUSTED_PACKAGES, String.join(",", trustedPackages));
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        return props;
    }
}
