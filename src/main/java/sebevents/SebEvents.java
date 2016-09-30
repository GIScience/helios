package sebevents;

import java.util.ArrayList;
import java.util.HashMap;

public class SebEvents {

	public static SebEvents events = new SebEvents();
	
	HashMap<String, ArrayList<EventListener>> listeners = new HashMap<>();
	
	public void fire(String eventName, Object payload) {
		
		if (listeners.get(eventName) == null) {
			return;
		}
		
		for(EventListener listener : listeners.get(eventName)) {
			listener.handleEvent(eventName, payload);
		}
	}
	
	public void addListener(String eventName, EventListener listener) {
		
		if (!listeners.containsKey(eventName)) {
			listeners.put(eventName, new ArrayList<EventListener>());
		}
		
		if (!listeners.get(eventName).contains(listener)) {
			listeners.get(eventName).add(listener);
		}		
	}
}
