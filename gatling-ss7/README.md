How to create a basic (manual) setup to test STP using Gatling-SIGTRAN plugin

- Clone the Jss7 repository

````
git clone https://github.com/RestComm/jss7.git
````

- Download the _jss7.patch_ and apply it to the jss7 repo and install

````
git apply ./jss7.patch
mvn install 
```` 

- Clone the public snice-gatling repo with the ss7 plugin

````
git clone -branch ss7 https://github.com/eleonoracau/snice-gatling.git
```` 

- Update the ss7 configuration of the remote and local Ip addresses of the hosts that will be used for the load test. The config is located in
_gatling-ss7/src/main/scala/io/snice/gatling/ss7/protocol/Ss7Config.scala_
````
  // M3UA details
  val LOCAL_IP = "172.22.211.112" // Private Ip of the Gatling host
  val LOCAL_PORT = 2905

  val REMOTE_IP = "172.22.138.96" // Private Ip of the STP host
  val REMOTE_PORT = 2905
```` 

- Install and copy the fat jar to the target host in `dev` (just clone a visited-plmn-lte instance)

````
mvn install
owl scp ./gatling-engine/target/snice-gatling-engine-0.0.5-SNAPSHOT.jar wireless-mobilecore-visited-plmn-lte:/tmp
````

- Ensure the target STP host has the Gatling Peer properly configured in the _stp.xml_. If not, add it with the Private Ip of the Gatling host and restart the STP
````
    <Peer
          name="Gatling-TEST"
          role="wireless-mobilecore-visited-plmn-lte"
          type="sctp_passive"
          acceptor_id="1"
          remote_addr="172.22.211.112" // Private Ip of the Gatling host
          remote_port="2905"
          origination_point_code="50"
          destination_point_code="60"
          routing_context="50"
          network_indicator="3"
          message_priority="0"
          signalling_link_selection="255"
          enable_rkm="0"
          ipx_id="test-ipx"
          metric="100"
          weight="100"
    />
````

- ssh into the machine and make sure the host runs with `java8`
````
owl ssh wireless-mobilecore-visited-plmn-lte
java -version
yum install java-1.8.0-openjdk
update-alternatives --config java
```` 

- Start Gatling 
````
cd /tmp
java -jar snice-gatling-engine-0.0.5-SNAPSHOT.jar
````



