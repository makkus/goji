Goji - Globus Online Java API
==========================

Goji is based on [JGO](http://confluence.globus.org/display/~neillm/JGOClient+Homepage) written by Neill Miller (University of Chicago). 

Purpose
-------

Goji has 2 goals:

1) make the GlobusOnline REST API available via an easy-to-use Java library

2) integrate VOMS and an Information sytem provider into this Java Library so that GlobusOnline can be used in conjunction with those technologies


Building Goji
-------------

In order to build and run Grisu from the git sources, you need: 

- Sun Java Development Kit (version ≥ 6)
- [git](http://git-scm.com) 
- [Apache Maven](http://maven.apache.org) (version >=2)

- the [gd_bundle.crt](http://www.mcs.anl.gov/~neillm/esg/gd_bundle.crt) file in $HOME/.globus/certificates

Checkout Goji via github:

    git clone git://github.com/makkus/goji.git
    
Then build using maven:

    cd goji
    mvn clean install
    
Usage examples 
---------------

### Create a session

If you have a (valid) proxy credential in the default location (e.g. /tmp/x509... in Linux), you can create a session object easily via:

    GlobusOnlineSession session = new GlobusOnlineSession(go_user);
    
If you want Goji to use a proxy out of your local x509 certificate (in  $HOME/.globus/usercert.pem), you need to create it first:

	Credential cred = new Credential("yourCertPassword").toCharArray());
	GlobusOnlineSession session = new GlobusOnlineSession(go_user, cred);
		
Or, if you have a proxy credential on a MyProxy server somewhere, you could use that:

    Credential cred = new Credential("myProxyUsername", "myProxyPassword".toCharArray(), "myproxyHost", 7512)
    GlobusOnlineSession session = new GlobusOnlineSession(go_user, cred);

### List all GlobusOnline endpoints

    System.out.println("List of endpoints:");
    for (Endpoint e : session.getAllEndpoints()) {
       System.out.println(e.getName());
    }
    
### Activate all your endpoints (using the default session credential)

    session.activateAllUserEndpoints();
    
Please note, this only works if all your endpoints (the ones which start with [yourGOusername]#...) can be activated with the default session credential.
If that is not the case, you'll need to...

### Activate endpoints manually

    // Creating a voms proxy out of the default proxy (need that for my
	// endpoints since I don't have
	// any filesystems I can access with a "plain", non-voms proxy.
	// You might not need it...
	Credential nz_nesi = session.getCredential().createVomsCredential("/nz/nesi");
	// making sure that the proxy is accessible for GlobusOnline via MyProxy
	// internally, Goji creates a random username/password combination for
	// the proxy. Might change that later on...
	nz_nesi.uploadMyProxy();

	// activate the endpoint with the newly created voms proxy
	session.activateEndpoint("gram5_ceres_auckland_ac_nz--nz_nesi", nz_nesi);

### Display your endpoint information

    for (Endpoint e : session.getAllUserEndpoints()) {
		System.out.println("User endpoint: " + e.getName());
		System.out.println("Expires: " + e.getExpires());
		session.list(e.getName(), "/~/");
	}
	
### List a directory

    Set<GFile> files = session.list("gram5_ceres_auckland_ac_nz--nz_nesi", "/~/");
	for (GFile f : files) {
	   System.out.println(f.getName());
	}
	
Note: your endpoint needs to be activated for this

### Transfer a file and wait for the transfer to finish

    Transfer t = session.transfer("gram5_ceres_auckland_ac_nz--nz_nesi/~/testfile.result.txt",
				"go#ep1/~/testfile.result.txt");

	t.waitForTransferToFinish();

Note: both endpoints need to be activated for this

### More examples

For more examples and fully working code examples, please browse: https://github.com/makkus/goji/tree/master/src/main/java/nz/org/nesi/goji/examples
I'll add more example code here once I find some time...
