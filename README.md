# ipnExample
Example Spring Boot App (Java) for IPN (Instant Payment Notification) from Paypal handling. 

## Requirments
- Java JDK 8
- Maven

## Run
- Run `mvn spring-boot:run` as user *root* because of IPN listener should listen at port 80. 
- Send IPN via POST to http://<yourhost>/ipn or test app via Paypal IPN simulator





