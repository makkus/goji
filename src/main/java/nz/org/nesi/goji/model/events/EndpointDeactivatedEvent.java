package nz.org.nesi.goji.model.events;

public class EndpointDeactivatedEvent extends EndpointEvent {

	private final EndpointDeactivatingEvent ev;

	public EndpointDeactivatedEvent(String ep, EndpointDeactivatingEvent ev) {
		super(ep);
		this.ev = ev;
	}

	public EndpointDeactivatingEvent getMatchingDectivatingEvent() {
		return ev;
	}

}
