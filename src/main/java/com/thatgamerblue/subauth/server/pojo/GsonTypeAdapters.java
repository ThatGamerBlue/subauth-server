package com.thatgamerblue.subauth.server.pojo;

import com.google.gson.TypeAdapterFactory;
import com.thatgamerblue.subauth.server.components.websocket.messages.WSMessage;
import com.thatgamerblue.subauth.server.util.RuntimeTypeAdapterFactory;
import java.util.Collection;

public class GsonTypeAdapters {
	public static <T> TypeAdapterFactory createFactory(Class<T> baseClass, Collection<Class<? extends T>> messages) {
		RuntimeTypeAdapterFactory<T> factory = RuntimeTypeAdapterFactory.of(baseClass);
		messages.forEach(factory::registerSubtype);
		return factory;
	}
}
