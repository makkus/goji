package nz.org.nesi.goji.model.events;

public class EndpointCreatedEvent extends EndpointEvent {

	private final EndpointCreatingEvent ev;

	public EndpointCreatedEvent(String ep, EndpointCreatingEvent ev) {
		super(ep);
		this.ev = ev;
	}

	public EndpointCreatingEvent getEndpointCreatingEvent() {
		return this.ev;
	}

}
