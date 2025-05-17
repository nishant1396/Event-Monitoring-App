package com.event;

import com.event.model.Event;
import com.event.model.EventStatusRequest;
import com.event.service.EventService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@EmbeddedKafka(partitions = 1, topics = {"live-sports-events"})
@ActiveProfiles("test")
public class EventMonitoringIntegrationTest {

	@LocalServerPort
	private int port;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private EventService eventService;

	@Autowired
	private EmbeddedKafkaBroker embeddedKafkaBroker;

	@Value("${kafka.topic.events}")
	private String topicName;

	// Mock the RestTemplate to avoid actual HTTP calls
	@MockitoBean
	private RestTemplate restTemplate;

	private Consumer<String, Event> consumer;

	/**
	 * Configure test properties to use embedded Kafka
	 */
	@DynamicPropertySource
	static void registerKafkaProperties(DynamicPropertyRegistry registry) {
		registry.add("kafka.bootstrap-servers", () -> "${spring.embedded.kafka.brokers}");
		// Setting a shorter polling interval for faster test execution
		registry.add("event.polling.interval", () -> "1");
		registry.add("event.initial.delay", () -> "1");
	}

	@BeforeEach
	void setUp() {
		// Configure Kafka consumer for test
		Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
				"test-group-id", "true", embeddedKafkaBroker);
		consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
		consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.event.model");

		DefaultKafkaConsumerFactory<String, Event> consumerFactory =
				new DefaultKafkaConsumerFactory<>(consumerProps, new StringDeserializer(),
												  new JsonDeserializer<>(Event.class, false));

		consumer = consumerFactory.createConsumer();
		consumer.subscribe(Collections.singletonList(topicName));

		// Clear any existing messages
		consumer.poll(Duration.ofMillis(100));

		System.out.println("==========================================================");
		System.out.println("STARTING EVENT MONITORING INTEGRATION TEST");
		System.out.println("==========================================================");
		System.out.println("Using port: " + port);

		// Update dynamic server port in application context
		System.setProperty("server.port", String.valueOf(port));
		System.setProperty("event.api.mock-endpoint", "http://localhost:" + port + "/mock-api/events/{eventId}");
	}

	@AfterEach
	void tearDown() {
		if (consumer != null) {
			consumer.close();
		}

		System.out.println("==========================================================");
		System.out.println("COMPLETED EVENT MONITORING INTEGRATION TEST");
		System.out.println("==========================================================");
	}

	@Test
	void testFullEventMonitoringFlow() throws Exception {
		// Set up mock for the RestTemplate to return a mock response
		Map<String, String> mockResponse = Map.of(
				"eventId", "test-event-123",
				"currentScore", "0"
		);

		// Mock the RestTemplate exchange method to return our mock response
		when(restTemplate.exchange(
				anyString(),
				any(),
				any(),
				any(org.springframework.core.ParameterizedTypeReference.class)))
				.thenReturn(org.springframework.http.ResponseEntity.ok(mockResponse));

		// Test #1: Check Kafka Health
		System.out.println("\n----- Testing Kafka Health -----");
		String healthResponse = mockMvc.perform(get("/kafka-health"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"))
				.andExpect(jsonPath("$.kafka").value("UP"))
				.andReturn().getResponse().getContentAsString();

		System.out.println("Kafka Health Response: " + healthResponse);

		// Test #2: Activate Event Monitoring
		System.out.println("\n----- Testing Event Activation -----");
		String eventId = "test-event-" + System.currentTimeMillis();
		EventStatusRequest request = new EventStatusRequest();
		request.setEventId(eventId);
		request.setStatus(true);

		mockMvc.perform(post("/events/status")
								.contentType(MediaType.APPLICATION_JSON)
								.content(objectMapper.writeValueAsString(request)))
				.andDo(print())
				.andExpect(status().isOk());

		System.out.println("Activated event: " + eventId);

		// Test #3: Verify direct API call to Mock API works
		System.out.println("\n----- Testing Mock API directly -----");
		mockMvc.perform(get("/mock-api/events/{eventId}", eventId))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.eventId").value(eventId))
				.andExpect(jsonPath("$.currentScore").exists());

		// Test #4: Wait for Event Updates in Kafka
		System.out.println("\n----- Waiting for Event Updates in Kafka -----");
		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			ConsumerRecords<String, Event> records = consumer.poll(Duration.ofMillis(1000));

			for (ConsumerRecord<String, Event> record : records) {
				Event event = record.value();
				System.out.println("Received Kafka message: " + event);

				if (eventId.equals(event.getEventId())) {
					System.out.println("Found matching event: " + event);
					assertThat(event.isLive()).isTrue();
					assertThat(event.getCurrentScore()).isNotNull();
				}
			}
		});

		// Test #5: Check All Events
		System.out.println("\n----- Testing Get All Events -----");
		String allEventsResponse = mockMvc.perform(get("/events"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$." + eventId).exists())
				.andExpect(jsonPath("$." + eventId + ".live").value(true))
				.andReturn().getResponse().getContentAsString();

		System.out.println("All Events Response: " + allEventsResponse);

		// Test #6: Deactivate Event
		System.out.println("\n----- Testing Event Deactivation -----");
		request.setStatus(false);

		mockMvc.perform(post("/events/status")
								.contentType(MediaType.APPLICATION_JSON)
								.content(objectMapper.writeValueAsString(request)))
				.andDo(print())
				.andExpect(status().isOk());

		System.out.println("Deactivated event: " + eventId);

		// Test #7: Verify Event Is Inactive
		System.out.println("\n----- Verifying Event Is Inactive -----");
		await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
			String verifyResponse = mockMvc.perform(get("/events"))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();

			Map<String, Event> events = objectMapper.readValue(verifyResponse,
															   objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Event.class));

			Event event = events.get(eventId);
			System.out.println("Final event state: " + event);
			assertThat(event.isLive()).isFalse();
		});

		System.out.println("\n----- All Tests Passed Successfully -----");
	}
}