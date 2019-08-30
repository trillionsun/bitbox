# bitbox
Structure:
p2p

langauge:
Java and Socket programming

Protocol used:
TCP and UDP support

Properities:
path : path for the  sharing directory
peers : address:port such as :elocalhost:8012
maximumIncommingConnections = 10
blockSize = 2000
syncInterval = 60
mode = .  udp or tcp
udpPort = 8011
MaximumRetry = 10

-> since UDP is not reliable, these are properities to set retry times and timeout 
udpTimeout = 5000
udpRetries = 5



