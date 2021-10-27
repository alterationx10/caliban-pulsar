Caliban-Pulsar
==============

This is an example repo of using [Caliban](https://ghostdogpr.github.io/caliban/) subscriptions 
backed by Apache Pulsar.

Pulsar doesn't seem to play nicely on the M1 (Apple Silicon) yet :-( , so the example doesn't work there.

The `Backend` entry point connects a consumer to a **Shared** subscription (competes for messages). When 
a message arrives, it persists some information to redis, and returns an event to another topic with the result
of the operation.

The `Caliban` entry point connects a consumer to an **Exclusive** subscription (no competition), and dumps messages
into a ZIO Hub, which powers a ZStream that feeds a _Subscription_. This pattern allows you to fan out messages to 
multiple instance of Caliban, where you likely don't know which one the target user is subscribed to.

The general capabilities of the schema are:
* Query for an event _by id_
* Mutation for persisting an event: payload is dropped onto a topic (returning a String indicating success of publishing the message)
* Subscription for listening to results of the mutation, filtered by a _target id_

The idea is there might be a case where a client sends a payload that could take time to be processed,
can listen for an event to signal the processes has completed, and then can query for the results.

To spin up the example, run `sbt Docker/stage` to pre-compile everything. Then you can run 
`docker-compose up --build` to spin up the stack. It spins up:
* redis
* pulsar (in standalone mode)
* nginx (proxy for scaling caliban)
* caliban (instance of our graphql server)
* backend (instance of our back-end event processor)

At that point, you should be able to access the graphql api at `http://localhost:8088/api/graphql` and the 
websocket for subscriptions at `ws://localhost:8088/ws/graphqll`. If you've made it that far, you can try scaling up
the number of caliban + backend instances :-)
