package com.hrconnect.socle.kafka;

import io.micrometer.observation.ObservationRegistry;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.Map;

/**
 * Factory pour créer facilement des ConsumerFactory et ListenerContainerFactory typés.
 *
 * <p>Simplifie la création de consumers Kafka avec une configuration standardisée.</p>
 *
 * <p>Exemple d'utilisation :</p>
 * <pre>
 * {@code
 * @Configuration
 * public class KafkaConfig {
 *
 *     @Value("${spring.kafka.bootstrap-servers}")
 *     private String bootstrapServers;
 *
 *     @Bean
 *     public ConsumerFactory<String, EmployeeState> employeeConsumerFactory() {
 *         return KafkaConsumerFactoryBuilder.create(EmployeeState.class)
 *             .bootstrapServers(bootstrapServers)
 *             .groupId("my-service")
 *             .trustedPackages("com.hrconnect.employee.contract")
 *             .build();
 *     }
 *
 *     @Bean
 *     public ConcurrentKafkaListenerContainerFactory<String, EmployeeState> employeeKafkaListenerContainerFactory() {
 *         return KafkaConsumerFactoryBuilder.listenerFactory(employeeConsumerFactory());
 *     }
 * }
 * }
 * </pre>
 *
 * @param <T> Type de la valeur consommée
 */
public class KafkaConsumerFactoryBuilder<T> {

    private final Class<T> valueType;
    private String bootstrapServers;
    private String groupId;
    private String[] trustedPackages = {"com.hrconnect.*"};
    private boolean useTypeHeaders = false;

    private KafkaConsumerFactoryBuilder(Class<T> valueType) {
        this.valueType = valueType;
    }

    /**
     * Crée un nouveau builder pour le type spécifié.
     *
     * @param valueType Classe du type de valeur à désérialiser
     * @param <T>       Type de la valeur
     * @return Builder configuré
     */
    public static <T> KafkaConsumerFactoryBuilder<T> create(Class<T> valueType) {
        return new KafkaConsumerFactoryBuilder<>(valueType);
    }

    /**
     * Crée rapidement un ListenerContainerFactory à partir d'un ConsumerFactory.
     * Cette méthode ne configure PAS l'observation (tracing).
     *
     * @param consumerFactory ConsumerFactory à utiliser
     * @param <T>             Type de la valeur
     * @return ListenerContainerFactory configuré
     * @deprecated Utiliser {@link #listenerFactory(ConsumerFactory, ObservationRegistry)} pour le tracing
     */
    @Deprecated
    public static <T> ConcurrentKafkaListenerContainerFactory<String, T> listenerFactory(
            ConsumerFactory<String, T> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, T> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        return factory;
    }

    /**
     * Crée un ListenerContainerFactory avec observation activée pour le tracing distribué.
     * Cette méthode permet la propagation automatique du traceId via Kafka.
     *
     * @param consumerFactory     ConsumerFactory à utiliser
     * @param observationRegistry ObservationRegistry pour le tracing (non utilisé directement, mais garde la signature pour injection)
     * @param <T>                 Type de la valeur
     * @return ListenerContainerFactory configuré avec observation
     */
    @SuppressWarnings("unused")
    public static <T> ConcurrentKafkaListenerContainerFactory<String, T> listenerFactory(
            ConsumerFactory<String, T> consumerFactory,
            ObservationRegistry observationRegistry) {
        ConcurrentKafkaListenerContainerFactory<String, T> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        // Active l'observation pour propager le contexte de trace (traceId/spanId)
        factory.getContainerProperties().setObservationEnabled(true);
        return factory;
    }

    /**
     * Définit l'adresse des brokers Kafka.
     */
    public KafkaConsumerFactoryBuilder<T> bootstrapServers(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
        return this;
    }

    /**
     * Définit l'ID du groupe de consommateurs.
     */
    public KafkaConsumerFactoryBuilder<T> groupId(String groupId) {
        this.groupId = groupId;
        return this;
    }

    /**
     * Définit les packages autorisés pour la désérialisation.
     * Par défaut : "com.hrconnect.*"
     */
    public KafkaConsumerFactoryBuilder<T> trustedPackages(String... packages) {
        this.trustedPackages = packages;
        return this;
    }

    /**
     * Active l'utilisation des type headers pour la désérialisation.
     * Par défaut : false (utilise le type par défaut)
     */
    public KafkaConsumerFactoryBuilder<T> useTypeHeaders(boolean useTypeHeaders) {
        this.useTypeHeaders = useTypeHeaders;
        return this;
    }

    /**
     * Construit le ConsumerFactory.
     *
     * @return ConsumerFactory configuré
     */
    public ConsumerFactory<String, T> build() {
        if (bootstrapServers == null) {
            throw new IllegalStateException("bootstrapServers must be set");
        }
        if (groupId == null) {
            throw new IllegalStateException("groupId must be set");
        }

        Map<String, Object> props = KafkaConfigUtils.consumerConfig(bootstrapServers, groupId);

        JsonDeserializer<T> deserializer = new JsonDeserializer<>(valueType);
        deserializer.addTrustedPackages(trustedPackages);
        deserializer.setUseTypeMapperForKey(false);

        if (!useTypeHeaders) {
            deserializer.ignoreTypeHeaders();
        }

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }
}
