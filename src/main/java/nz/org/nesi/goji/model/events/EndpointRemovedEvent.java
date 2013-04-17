package nz.org.nesi.goji.model.events;

public class EndpointRemovedEvent extends EndpointEvent {

	private final EndpointRemovingEvent ere;

	public EndpointRemovedEvent(String ep, EndpointRemovingEvent ev) {
		super(ep);
		this.ere = ev;
	}

	public EndpointRemovingEvent getEndpointRemovingEvent() {
		return this.ere;
	}

}
