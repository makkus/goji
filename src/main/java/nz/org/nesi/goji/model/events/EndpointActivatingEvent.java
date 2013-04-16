package nz.org.nesi.goji.model.events;

import grith.jgrith.cred.Cred;

public class EndpointActivatingEvent extends EndpointEvent {

	private final Cred cred;

	public EndpointActivatingEvent(String ep, Cred cred) {
		super(ep);
		this.cred = cred;
	}

	public Cred getCred() {
		return this.cred;
	}

}
