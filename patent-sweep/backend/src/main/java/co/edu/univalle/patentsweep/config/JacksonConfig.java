package co.edu.univalle.patentsweep.config;

import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import java.time.format.DateTimeFormatter;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Formato de fechas del contrato REST: dd/mm/aaaa hh:mm:ss para fecha y hora, y dd/mm/aaaa
 * para fechas (PB, todas las HU). Se aplica a la API y a los clientes de integración.
 */
@Configuration
public class JacksonConfig {

    public static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    public static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer formatoFechas() {
        return builder -> builder
                .serializers(new LocalDateTimeSerializer(FORMATO_FECHA_HORA), new LocalDateSerializer(FORMATO_FECHA))
                .deserializers(new LocalDateTimeDeserializer(FORMATO_FECHA_HORA),
                        new LocalDateDeserializer(FORMATO_FECHA));
    }
}
