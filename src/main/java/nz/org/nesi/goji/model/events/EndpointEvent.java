package nz.org.nesi.goji.model.events;

import java.util.Date;

public abstract class EndpointEvent {

	private final String endpoint;
	private final Date time;

	public EndpointEvent(String ep) {
		this.endpoint = ep;
		time = new Date();
	}

	public String getEndpoint() {
		return endpoint;
	}

	public Date getTime() {
		return time;
	}

}
