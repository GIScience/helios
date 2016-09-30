package sebevents;

public interface EventListener {

	void handleEvent(String eventName, Object payload);
}
