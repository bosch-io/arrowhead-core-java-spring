package eu.arrowhead.core.qos.service.ping.monitor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit4.SpringRunner;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.dto.shared.EventDTO;
import eu.arrowhead.common.dto.shared.QosMonitorEventType;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.core.qos.QosMonitorConstants;
import eu.arrowhead.core.qos.dto.event.EventDTOConverter;

@RunWith(SpringRunner.class)
public class PingEventProcessorTest {

	//=================================================================================================
	// members
	@InjectMocks
	private final PingEventProcessor pingEventProcessor = new PingEventProcessor();

	@Mock
	private ConcurrentHashMap<UUID, PingEventBufferElement> eventBuffer;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test(expected = IllegalArgumentException.class)
	public void testProcessEventsProcessIdNull() {

		final UUID processId = null;
		final long measurementExpiryTime = 1L;

		when(eventBuffer.get(any())).thenReturn(null);

		pingEventProcessor.processEvents(processId, measurementExpiryTime);

		verify(eventBuffer,  never()).get(any());
	}

	//-------------------------------------------------------------------------------------------------
	@Test(expected = IllegalArgumentException.class)
	public void testProcessEventsMeasurementExpiryTimeIsLessThenOne() {

		final UUID processId = UUID.randomUUID();
		final long measurementExpiryTime = 0L;

		pingEventProcessor.processEvents(processId, measurementExpiryTime);

		verify(eventBuffer, never()).get(any());
	}

	//-------------------------------------------------------------------------------------------------
	@Test(expected = IllegalArgumentException.class)
	public void testProcessEventsMeasurementExpiryTimeIsInNotInTheFuture() {

		final UUID processId = UUID.randomUUID();
		final long measurementExpiryTime = System.currentTimeMillis();

		pingEventProcessor.processEvents(processId, measurementExpiryTime);

		verify(eventBuffer, never()).get(any());
	}

	//-------------------------------------------------------------------------------------------------
	@Test(expected = ArrowheadException.class)
	public void testProcessEventsNoEventWithinMeasurementExpiryTime() {

		final UUID processId = UUID.randomUUID();
		final long measurementExpiryTime = System.currentTimeMillis() + 10000;

		final PingEventBufferElement bufferElement = null;

		when(eventBuffer.get(any())).thenReturn(bufferElement);

		pingEventProcessor.processEvents(processId, measurementExpiryTime);

		verify(eventBuffer, atLeastOnce()).get(any());
		verify(eventBuffer, never()).remove(any());
	}

	//-------------------------------------------------------------------------------------------------
	@Test(expected = ArrowheadException.class)
	public void testProcessEventsInterrruptedEventPresent() {

		final UUID processId = UUID.randomUUID();
		final long measurementExpiryTime = System.currentTimeMillis() + 10000;

		final PingEventBufferElement bufferElement = new PingEventBufferElement(processId);
		bufferElement.addEvent(
				QosMonitorConstants.INTERRUPTED_MONITORING_MEASUREMENT_EVENT_POSITION,
				EventDTOConverter.convertToInterruptedMonitoringMeasurementEvent(getValidInterruptedDTOForTest()));

		when(eventBuffer.get(any())).thenReturn(bufferElement);

		pingEventProcessor.processEvents(processId, measurementExpiryTime);

		verify(eventBuffer, times(1)).get(any());
		verify(eventBuffer, times(1)).remove(any());
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private EventDTO getInValidEventTypeEventDTOForTest() {

		final EventDTO event = new EventDTO();
		event.setEventType("UNKNOWN_MEAUSREMENT_EVENT");
		event.setMetaData(getValidMeasuermentEventDTOMetadtaProcessIdForTest());
		event.setPayload(getValidMeasuermentEventDTOEmptyPayloadForTest());
		event.setTimeStamp(Utilities.convertZonedDateTimeToUTCString(ZonedDateTime.now()));

		return event;
	}

	//-------------------------------------------------------------------------------------------------
	private EventDTO getValidReceivedMeasurementRequestEventDTOForTest() {

		final EventDTO event = new EventDTO();
		event.setEventType(QosMonitorEventType.RECEIVED_MONITORING_REQUEST.name());
		event.setMetaData(getValidMeasuermentEventDTOMetadtaProcessIdForTest());
		event.setPayload(getValidMeasuermentEventDTOEmptyPayloadForTest());
		event.setTimeStamp(Utilities.convertZonedDateTimeToUTCString(ZonedDateTime.now()));

		return event;
	}

	//-------------------------------------------------------------------------------------------------
	private EventDTO getValidStartEventDTOForTest() {

		final EventDTO event = new EventDTO();
		event.setEventType(QosMonitorEventType.STARTED_MONITORING_MEASUREMENT.name());
		event.setMetaData(getValidMeasuermentEventDTOMetadtaProcessIdForTest());
		event.setPayload(getValidMeasuermentEventDTOEmptyPayloadForTest());
		event.setTimeStamp(Utilities.convertZonedDateTimeToUTCString(ZonedDateTime.now()));

		return event;
	}

	//-------------------------------------------------------------------------------------------------
	private EventDTO getValidFinishedEventDTOForTest() {

		final EventDTO event = new EventDTO();
		event.setEventType(QosMonitorEventType.FINISHED_MONITORING_MEASUREMENT.name());
		event.setMetaData(getValidMeasuermentEventDTOMetadtaProcessIdForTest());
		event.setPayload(getValidMeasuermentEventDTOEmptyPayloadForTest());
		event.setTimeStamp(Utilities.convertZonedDateTimeToUTCString(ZonedDateTime.now()));

		return event;
	}

	//-------------------------------------------------------------------------------------------------
	private EventDTO getValidInterruptedDTOForTest() {

		final EventDTO event = new EventDTO();
		event.setEventType(QosMonitorEventType.INTERRUPTED_MONITORING_MEASUREMENT.name());
		event.setMetaData(getValidMeasuermentEventDTOMetadtaProcessIdForTest());
		event.setPayload(getValidMeasuermentEventDTOEmptyPayloadForTest());
		event.setTimeStamp(Utilities.convertZonedDateTimeToUTCString(ZonedDateTime.now()));

		return event;
	}

	//-------------------------------------------------------------------------------------------------
	private Map<String, String> getValidMeasuermentEventDTOMetadtaProcessIdForTest() {

		return Map.of(QosMonitorConstants.PROCESS_ID_KEY, UUID.randomUUID().toString());

	}

	//-------------------------------------------------------------------------------------------------
	private String getValidMeasuermentEventDTOEmptyPayloadForTest() {

		return "[]";

	}
}
