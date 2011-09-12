Goja - Globus Online Java API
==========================

This one is heavily based on [JGO](http://confluence.globus.org/display/~neillm/JGOClient+Homepage) written by Neill Miller. All of the original code is copyrighted by the University of Chicago. It's written to support GO support for the [Grisu framework](https://github.com/grisu/grisu).

Most of the changes to the **JGO** code are refactorings to have a more object-oriented GO client library, which can be used in a threadsafe way within the Grisu framework.

Also, I converted it into a maven project since that is what Grisu uses for dependency management.

Purpose
----

Goji tries to integrate GlobusOnline with VOMS and mds information. Internally it keeps
a list of endpoints synchronized with all available endpoints for a user who is member of a certain
set of VOs. 

Endpoint management
--------------------

Endpoints are managed using information from an information source. At the moment, only a config-file
based information source is supported, but it will be linked to mds information in the
near future.
Endpoints are named in a unique way, in order to keep them synchronized without too much
overhead:

nz#[hostname]--[group]
(with '.' and '/' characters replaced to '_')

