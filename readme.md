### Goal
*Write an Round Robin API which receives HTTP POSTS and routes them to one of a list of Application APIs*

**Language Opted**: Java
**Frameworks Used**: Spring-boot web with Lombok and JPA (with MySQL JDBC connector)
**Necessary Tools**: MySQL UI Workbench, Jmeter, Postman

### The Simple API

This has a bare minimum Spring-boot web POST API that receives a request and maps it to response and returns it.

Sample CURL:

```
curl --location 'http://localhost:8080/simple/api' \
--header 'Content-Type: application/json' \
--data '{
    "game": "Mobile Legends",
    "gamer_id": "GYUTDTE",
    "points": 20
}'
```

Sample Response:

```
{
    "game": "Mobile Legends",
    "points": 20,
    "gamer_id": "GYUTDTE"
}
```

It also has a GET health API that can be used to get the current server status.

```
curl --location 'http://localhost:8080/health'
```

Sample Response:

```
{
    "status": "OK"
}
```


### Round Robin Routing API

This API hosts the routing service. It balances the server load to the simple API in a round robin fashion. It hosts 2 basic services:

#### The routing service

When the routing service receives a request for the simple POST API (mentioned above), it fetches the next available server from the heartbeat service and routes the request to the server.

![[Routing Service.png]]

Sample Routing CURL:
```
curl --location 'http://localhost:8080/route/simple/api' \
--header 'Content-Type: application/json' \
--data '{
    "game": "Mobile Legends",
    "gamer_id": "GYUTDTE",
    "points": 20
}'
```

Sample Response:
```
{
    "game": "Mobile Legends",
    "points": 20,
    "gamer_id": "GYUTDTE"
}
```

Database Snapshot:
![[Database snapshot.png]]

#### The heartbeat service

As the application starts, the servers.yml is scanned for all the configured servers. Each alive server is added to active server set while each inactive server is added to the stale server list.
A server is considered alive if the health API configured returns OK status.

![[Start up.png]]

Every second the active server set is scanned and checked if all the servers are alive. If not they are added to the stale set.

![[Active Server scheduler.png]]

Every 5 seconds the stale server set is scanned to check if any of the servers are back alive. Each alive server is then put back in the active server list.

![[Stale Server scheduler.png]]

A server status API is exposed by this layer to return the current status of all the configured servers.

Sample Server Status CURL:
```
curl --location 'http://localhost:8080/route/servers/status'
```
Sample Response:
```
{
    "server_queue": [
        {
            "instanceId": "b91ab238-41d6-4601-835c-fc768a96cdde",
            "uri": "http://localhost:8081",
            "healthApiPath": "/health",
            "resourceApiPath": "/simple/api"
        },
        {
            "instanceId": "faefa484-d88b-483b-b2ae-31e142551fbb",
            "uri": "http://localhost:8082",
            "healthApiPath": "/health",
            "resourceApiPath": "/simple/api"
        },
        {
            "instanceId": "89d5e2c5-6ca8-4141-ba3b-b09f6fd00773",
            "uri": "http://localhost:8083",
            "healthApiPath": "/health",
            "resourceApiPath": "/simple/api"
        }
    ],
    "active_servers": [
        {
            "instanceId": "b91ab238-41d6-4601-835c-fc768a96cdde",
            "uri": "http://localhost:8081",
            "healthApiPath": "/health",
            "resourceApiPath": "/simple/api"
        },
        {
            "instanceId": "faefa484-d88b-483b-b2ae-31e142551fbb",
            "uri": "http://localhost:8082",
            "healthApiPath": "/health",
            "resourceApiPath": "/simple/api"
        },
        {
            "instanceId": "89d5e2c5-6ca8-4141-ba3b-b09f6fd00773",
            "uri": "http://localhost:8083",
            "healthApiPath": "/health",
            "resourceApiPath": "/simple/api"
        }
    ],
    "stale_servers": []
}
```


### Testing Steps

1. The simple API application is ran on 3 different ports: 8081, 8082 and 8083. Sample command:
```
java -jar -Dserver.port=8081 codapayments-simple-api-1.0-SNAPSHOT.jar > /Users/ayankumarsaha/Desktop/server-log/server-1.log &

java -jar -Dserver.port=8082 codapayments-simple-api-1.0-SNAPSHOT.jar > /Users/ayankumarsaha/Desktop/server-log/server-2.log &

java -jar -Dserver.port=8083 codapayments-simple-api-1.0-SNAPSHOT.jar > /Users/ayankumarsaha/Desktop/server-log/server-3.log &
```

2. The routing API application server is started at 8080.
3. The routing API is hit from Postman:
   ![[postman routing api.png]]
4. One of the simple api servers is killed.

```
   kill -9 [process-id]
```

5. The routing server status api is hit. Since the server sets are rebalanced, it will show one stale and 2 active servers:
```
{
    "server_queue": [
        {
            "instanceId": "ba689476-4386-4e88-beaa-9e6ece60d465",
            "uri": "http://localhost:8082",
            "healthApiPath": "/health",
            "resourceApiPath": "/simple/api"
        },
        {
            "instanceId": "ad6e1858-48bc-47e0-9f33-55908db14ce9",
            "uri": "http://localhost:8083",
            "healthApiPath": "/health",
            "resourceApiPath": "/simple/api"
        }
    ],
    "active_servers": [
        {
            "instanceId": "ba689476-4386-4e88-beaa-9e6ece60d465",
            "uri": "http://localhost:8082",
            "healthApiPath": "/health",
            "resourceApiPath": "/simple/api"
        },
        {
            "instanceId": "ad6e1858-48bc-47e0-9f33-55908db14ce9",
            "uri": "http://localhost:8083",
            "healthApiPath": "/health",
            "resourceApiPath": "/simple/api"
        }
    ],
    "stale_servers": [
        {
            "instanceId": "21d27939-86f7-4030-a05b-da0760534053",
            "uri": "http://localhost:8081",
            "healthApiPath": "/health",
            "resourceApiPath": "/simple/api"
        }
    ]
}
```

6. On hitting the routing service, it will only direct traffic to the active servers in round robin fashion.
7. On re-running the stopped simple api application server, the active and stale server set will be rebalanced.