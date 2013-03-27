package nz.org.nesi.goji.model.events;

public class EndpointActivatedEvent extends EndpointEvent {

	private final EndpointActivatingEvent ev;

	public EndpointActivatedEvent(String ep, EndpointActivatingEvent ev) {
		super(ep);
		this.ev = ev;
	}

	public EndpointActivatingEvent getMatchingActivatingEvent() {
		return ev;
	}

}
