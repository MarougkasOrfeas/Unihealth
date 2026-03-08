package gr.uniwa.unihealth.backend.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.deser.jdk.StringDeserializer;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.StdSerializer;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.text.Normalizer.Form;

@Configuration
public class JacksonConfig {

  @Bean
  public JacksonJsonHttpMessageConverter jacksonJsonHttpMessageConverter() {
    return new JacksonJsonHttpMessageConverter(jsonMapper());
  }

  @Bean
  @Primary
  public JsonMapper jsonMapper() {
    SimpleModule customModule = new SimpleModule()
        .addDeserializer(String.class, new StdDeserializer<String>(String.class) {

          @Override
          public String deserialize(JsonParser parser, DeserializationContext context) {
            String result = StringDeserializer.instance.deserialize(parser, context);
            return StringUtils.isBlank(result) ? null
                : Normalizer.normalize(result, Form.NFC).trim();
          }
        }).addDeserializer(BigDecimal.class, new StdDeserializer<BigDecimal>(BigDecimal.class) {

          @Override
          public BigDecimal deserialize(JsonParser parser, DeserializationContext context) {
            String value = parser.getValueAsString();
            return !StringUtils.isBlank(value) ? new BigDecimal(value).stripTrailingZeros() : null;
          }
        }).addSerializer(BigDecimal.class, new StdSerializer<BigDecimal>(BigDecimal.class) {

          @Override
          public void serialize(BigDecimal value, JsonGenerator gen,
              SerializationContext provider) {
            gen.writeString(value != null ? value.stripTrailingZeros().toPlainString() : null);
          }
        });

    return JsonMapper.builder()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .changeDefaultPropertyInclusion(v -> v.withValueInclusion(JsonInclude.Include.NON_EMPTY))
        .addModule(customModule)
        .build();
  }
}
