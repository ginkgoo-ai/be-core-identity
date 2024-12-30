package com.benwk.ginkgoocoreidentity.config.cache;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

import java.io.IOException;


@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Create serializer
        GenericJackson2JsonRedisSerializer serializer = createGenericJackson2JsonRedisSerializer();

        // Set serializers
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);

        return template;
    }

    private GenericJackson2JsonRedisSerializer createGenericJackson2JsonRedisSerializer() {
        // Create ObjectMapper with custom configuration
        ObjectMapper objectMapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .addModule(createOAuth2Module())
                .activateDefaultTyping(
                        BasicPolymorphicTypeValidator.builder()
                                .allowIfBaseType(Object.class)
                                .build(),
                        ObjectMapper.DefaultTyping.NON_FINAL
                )
                .build();

        return new GenericJackson2JsonRedisSerializer(objectMapper);
    }

    private SimpleModule createOAuth2Module() {
        SimpleModule module = new SimpleModule("OAuth2Module");

        // AuthorizationGrantType serializer/deserializer
        module.addSerializer(AuthorizationGrantType.class, new JsonSerializer<AuthorizationGrantType>() {
            @Override
            public void serialize(AuthorizationGrantType value, JsonGenerator gen, SerializerProvider provider)
                    throws IOException {
                gen.writeStartObject();
                gen.writeStringField("value", value.getValue());
                gen.writeEndObject();
            }
        });

        module.addDeserializer(AuthorizationGrantType.class, new JsonDeserializer<AuthorizationGrantType>() {
            @Override
            public AuthorizationGrantType deserialize(JsonParser p, DeserializationContext ctxt)
                    throws IOException {
                JsonNode node = p.getCodec().readTree(p);
                String value = node.get("value").asText();
                return new AuthorizationGrantType(value);
            }
        });

        return module;
    }
}
