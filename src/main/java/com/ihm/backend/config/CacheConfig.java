package cm.ihm.backend.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration du système de cache avec Redis (WebFlux avec Lettuce)
 * Utilisé pour améliorer les performances et réduire la charge sur la BDD
 */
@Configuration
@EnableCaching
@RequiredArgsConstructor
@Slf4j
public class CacheConfig implements CachingConfigurer {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.cache.redis.time-to-live:3600000}")
    private long defaultTtl;

    /**
     * Configuration de la connexion Redis avec Lettuce (compatible WebFlux)
     */
    @Bean
    public LettuceConnectionFactory lettuceConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisHost);
        config.setPort(redisPort);
        
        if (redisPassword != null && !redisPassword.isEmpty()) {
            config.setPassword(redisPassword);
        }
        
        LettuceConnectionFactory factory = new LettuceConnectionFactory(config);
        log.info("Configuration Redis avec Lettuce: {}:{}", redisHost, redisPort);
        return factory;
    }

    /**
     * Template Redis pour opérations manuelles (synchrone)
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Sérialisation des clés en String
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // Sérialisation des valeurs en JSON
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper());
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        
        template.afterPropertiesSet();
        return template;
    }

    /**
     * Template Redis réactif pour opérations WebFlux
     */
    @Bean
    public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate(LettuceConnectionFactory connectionFactory) {
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer valueSerializer = new GenericJackson2JsonRedisSerializer(objectMapper());
        
        RedisSerializationContext<String, Object> serializationContext = RedisSerializationContext
            .<String, Object>newSerializationContext()
            .key(keySerializer)
            .value(valueSerializer)
            .hashKey(keySerializer)
            .hashValue(valueSerializer)
            .build();
        
        return new ReactiveRedisTemplate<>(connectionFactory, serializationContext);
    }

    /**
     * ObjectMapper personnalisé pour la sérialisation JSON dans Redis
     */
    private ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.activateDefaultTyping(
            mapper.getPolymorphicTypeValidator(),
            ObjectMapper.DefaultTyping.NON_FINAL
        );
        return mapper;
    }

    /**
     * Configuration du gestionnaire de cache Redis
     */
    @Bean
    @Override
    public CacheManager cacheManager() {
        // Configuration par défaut
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMillis(defaultTtl))
                .serializeKeysWith(
                    RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())
                )
                .serializeValuesWith(
                    RedisSerializationContext.SerializationPair.fromSerializer(
                        new GenericJackson2JsonRedisSerializer(objectMapper())
                    )
                )
                .disableCachingNullValues();

        // Configurations spécifiques par cache
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // Cache utilisateurs - 1 heure
        cacheConfigurations.put("users", defaultConfig.entryTtl(Duration.ofHours(1)));
        
        // Cache compositions - 30 minutes
        cacheConfigurations.put("compositions", defaultConfig.entryTtl(Duration.ofMinutes(30)));
        
        // Cache cours publics - 2 heures (changent moins souvent)
        cacheConfigurations.put("courses", defaultConfig.entryTtl(Duration.ofHours(2)));
        
        // Cache granules - 1 heure
        cacheConfigurations.put("granules", defaultConfig.entryTtl(Duration.ofHours(1)));
        
        // Cache statistiques - 5 minutes (données fréquemment mises à jour)
        cacheConfigurations.put("statistics", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        
        // Cache recherche - 15 minutes
        cacheConfigurations.put("search", defaultConfig.entryTtl(Duration.ofMinutes(15)));
        
        // Cache fichiers - 1 heure
        cacheConfigurations.put("files", defaultConfig.entryTtl(Duration.ofHours(1)));

        return RedisCacheManager.builder(lettuceConnectionFactory())
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }

    /**
     * Générateur de clés personnalisé pour le cache
     * Format: nomClasse:nomMethode:param1_param2_...
     */
    @Bean
    @Override
    public KeyGenerator keyGenerator() {
        return (target, method, params) -> {
            StringBuilder key = new StringBuilder();
            key.append(target.getClass().getSimpleName());
            key.append(":");
            key.append(method.getName());
            
            if (params.length > 0) {
                key.append(":");
                for (Object param : params) {
                    if (param != null) {
                        key.append(param.toString()).append("_");
                    }
                }
                // Supprimer le dernier underscore
                if (key.charAt(key.length() - 1) == '_') {
                    key.setLength(key.length() - 1);
                }
            }
            
            return key.toString();
        };
    }

    /**
     * Gestionnaire d'erreurs de cache
     * Continue le traitement même si Redis est indisponible
     */
    @Bean
    @Override
    public CacheErrorHandler errorHandler() {
        return new CustomCacheErrorHandler();
    }

    /**
     * Gestionnaire d'erreurs personnalisé qui log mais ne bloque pas l'application
     */
    public static class CustomCacheErrorHandler extends SimpleCacheErrorHandler {
        
        @Override
        public void handleCacheGetError(RuntimeException exception, 
                                       org.springframework.cache.Cache cache, 
                                       Object key) {
            log.error("Erreur lors de la récupération du cache '{}' pour la clé '{}': {}", 
                    cache.getName(), key, exception.getMessage());
            // Ne pas propager l'exception - continuer sans cache
        }

        @Override
        public void handleCachePutError(RuntimeException exception, 
                                       org.springframework.cache.Cache cache, 
                                       Object key, 
                                       Object value) {
            log.error("Erreur lors de la mise en cache '{}' pour la clé '{}': {}", 
                    cache.getName(), key, exception.getMessage());
            // Ne pas propager l'exception
        }

        @Override
        public void handleCacheEvictError(RuntimeException exception, 
                                         org.springframework.cache.Cache cache, 
                                         Object key) {
            log.error("Erreur lors de l'éviction du cache '{}' pour la clé '{}': {}", 
                    cache.getName(), key, exception.getMessage());
            // Ne pas propager l'exception
        }

        @Override
        public void handleCacheClearError(RuntimeException exception, 
                                         org.springframework.cache.Cache cache) {
            log.error("Erreur lors du nettoyage du cache '{}': {}", 
                    cache.getName(), exception.getMessage());
            // Ne pas propager l'exception
        }
    }
