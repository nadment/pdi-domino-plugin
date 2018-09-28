# PDI Domino Plugin

## Overview

This plugin for PDI provide 
* __Database connection__ 
* __Domino input__ step to import domino data from a view or formula.
* __Domino run agent__ job entry to run domino agent.

Support MetaData Injection (MDI)

## How to install

#### System Requirements

Pentaho Data Integration 8.0 or above

The classpath must include Notes.jar (local) or NCSO.jar (remote)
- Notes.jar can be found in the program directory of any Notes/Domino installation. 
- NCSO.jar can be found in the domino\java directory under the data directory in Domino Designer or the Domino server.

The DIIOP (Domino IIOP) task on the server must be running for remote calls. 

#### Using Pentaho Marketplace ##

1. Find the plugin in the [Pentaho Marketplace](http://www.pentaho.com/marketplace) and click Install
2. Restart Spoon

#### Manual Install ##

1. Place the “pdi-domino-plugin” folder in the ${DI\_HOME}/plugins/ directory
2. Restart Spoon

## Documentation

[See Plugin Wiki](https://github.com/nadment/pdi-domino-plugin/wiki)
 

## License

Licensed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).


