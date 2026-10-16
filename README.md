# yanki-service

Monedero móvil Yanki.

- No se necesita ser un cliente del banco para tener un monedero móvil, solo se necesita un número de documento de identificación (DNI, CEX, Pasaporte), número de celular, el IMEI del celular y correo electrónico.
- El usuario puede recibir y enviar pagos a su monedero con solo su número de celular.
- Puede asociar su monedero a una tarjeta de débito del banco de manera que el saldo sea cargado o acreditado solo a la cuenta principal asociada a la tarjeta de débito.

La comunicación con los demás microservicios es solo por Kafka.

## Tecnologías

- Java 17
- Spring Boot
- Spring Cloud (Config, Eureka)
- RxJava
- Kafka
- Maven
- MongoDB
- Lombok
- Logback
- JUnit, Mockito y JaCoCo
- Docker
