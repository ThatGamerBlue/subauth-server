package com.thatgamerblue.subauth.server.components.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxProcessor;
import reactor.core.publisher.FluxSink;
import reactor.core.scheduler.Schedulers;
import reactor.util.concurrent.Queues;

@Slf4j
@Component
public class EventBus {
	private final FluxProcessor<Object, Object> processor;
	private final FluxSink<Object> sink;

	public EventBus() {
		this.processor = EmitterProcessor.create(Queues.SMALL_BUFFER_SIZE, false);
		this.sink = processor.sink();
	}

	public <T> Flux<T> onEvent(Class<T> eventType) {
		return processor
			.ofType(eventType)
			.publishOn(Schedulers.boundedElastic())
			.handle((e, sink) -> sink.next(e));
	}

	public void post(Object event) {
		sink.next(event);
	}
}
