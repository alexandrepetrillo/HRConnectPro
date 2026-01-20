package com.hrconnect.socle.kafka;

import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.Map;

/**
 * Factory pour créer facilement des ProducerFactory et KafkaTemplate typés.
 *
 * <p>Simplifie la création de producers Kafka avec une configuration standardisée.</p>
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
 *     public ProducerFactory<String, InterviewState> producerFactory() {
 *         return KafkaProducerFactoryBuilder.<InterviewState>create()
 *             .bootstrapServers(bootstrapServers)
 *             .build();
 *     }
 *
 *     @Bean
 *     public KafkaTemplate<String, InterviewState> kafkaTemplate() {
 *         return KafkaProducerFactoryBuilder.kafkaTemplate(producerFactory());
 *     }
 * }
 * }
 * </pre>
 *
 * @param <T> Type de la valeur produite
 */
public class KafkaProducerFactoryBuilder<T> {

    private String bootstrapServers;
    private boolean addTypeHeaders = false;

    private KafkaProducerFactoryBuilder() {
    }

    /**
     * Crée un nouveau builder.
     *
     * @param <T> Type de la valeur
     * @return Builder configuré
     */
    public static <T> KafkaProducerFactoryBuilder<T> create() {
        return new KafkaProducerFactoryBuilder<>();
    }

    /**
     * Crée rapidement un KafkaTemplate à partir d'un ProducerFactory.
     *
     * @param producerFactory ProducerFactory à utiliser
     * @param <T>             Type de la valeur
     * @return KafkaTemplate configuré
     */
    public static <T> KafkaTemplate<String, T> kafkaTemplate(ProducerFactory<String, T> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    /**
     * Définit l'adresse des brokers Kafka.
     */
    public KafkaProducerFactoryBuilder<T> bootstrapServers(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
        return this;
    }

    /**
     * Active l'ajout des type headers dans les messages.
     * Par défaut : false
     */
    public KafkaProducerFactoryBuilder<T> addTypeHeaders(boolean addTypeHeaders) {
        this.addTypeHeaders = addTypeHeaders;
        return this;
    }

    /**
     * Construit le ProducerFactory.
     *
     * @return ProducerFactory configuré
     */
    public ProducerFactory<String, T> build() {
        if (bootstrapServers == null) {
            throw new IllegalStateException("bootstrapServers must be set");
        }

        Map<String, Object> props = addTypeHeaders
                ? KafkaConfigUtils.producerConfig(bootstrapServers)
                : KafkaConfigUtils.producerConfigWithoutTypeHeaders(bootstrapServers);

        return new DefaultKafkaProducerFactory<>(props);
    }
}
