
Razie's Workflow Vision: Gremlins
=================================

When I think "workflow" I imagine a graph of activities of different kinds, through which a processing
engine carves a path. As opposed to just being there for fun, these nodes actually do stuff, so they look 
much like a bunch of (more or less chaotic) Gremlins, chewing away at a piece of a problem.

Some activities carry out actual processing or communication while others merely figure out what path to
follow next and yet others are there just for structure. Some complete immediately while some take their 
time. Some are on the same sequence while some are completed in parallel and on different engines/servers.

The Gremlins image is more obvious if you start to think outside the box. Literally, Gremlins can go into 
other systems and applications and do stuff. This may sound non-sensical but for any B2B developer it
sounds right: tracking/coordinating sub-processes in other parties - you'd *love* to get visibility and 
control into those, wouldn't you? You could cascade a suspend or roll-up some intermediary states...

The basic processing engine is thus nothing more than a graph traversal / visitor. However - forget the 
visitor pattern, these nodes are object-oriented and encapsulate their own logic.


Workflow patterns and building blocks
-------------------------------------

Common patterns are expressed in terms of the basic constructs, to create new, higher-level logic and 
structures.

One difference from other frameworks will be that of dissolving the barriers between concurrent programming,
distribution and workflows. I see all preceeding techniques as steps, identifying patterns and leading us 
here. 
The only programming model worth learning is the workflow...the others are good maybe for a history class. 
Disclaimer: although there is none, no false modesty was wasted in the making of the above paragraph :))


Design
======

So, what we need is:

* generic graph-based processing engine
Classes like <code>WA</code> and <code>WL</code> describe this level of the workflow.
* engine cooperation API (so remote engines can coordinate sub-workflows)
* graph template-ing engine
* basic activity library
* default/standard embedable container
* default agent structure for distribution

That's it! 

Well, then there's

* content migration API (script/workflow/code)
* modularity (OSGI)

   
Context, environment
--------------------

All nodes in a workflow share the same context. Each node can modify the common context.

TODO need to figure out exaclty how sub-workflow contexts interact.


Actions and Actionables
-----------------------

This is the basic concept of an action/actionable. All we do is a set of steps/actions on different objects
in an environment. 
Whether, being a developer, you're thinking about <code>console.logging(a message)</code> or sending a 
message to an external system, making a WS call etc


Concurrency
-----------

Concurrent programming takes into account that most of the time, the same resource is shared for different  
purposes, often at the same time. For instance, fighter jet computers tracking multiple bogies or amazon's
ordering system handling multiple carts depleting the same book shelves, not to mention Google's robots 
reading a gazillion pages from a gazillion servers with different response times.

Things get pretty complicated very quickly when trying to express this concurrency.

The "standard" approaches are:

- shared state
The classic model where processing threads share some state (like list of users). All kinds of constructs
are used to manage concurrent access to state/resources. A Monitor could wrap the logging system and lock 
it for the duration of different calls, while a Barrier can wait for all missiles to hit their targets before
assesment...
- message passing
Independent stateful or stateless "actors" that can respond to messages. All state is encapsulated in the 
messages queued for all actors at any one time... 
An actor could wrap the logging system and process log requests as they arrive as messages. If there would
be any result, it would send that as a message to another actor.

While the first does have some visualization options, the second can at best produce an incoherent 
abstract image in my head...although one can appreciate the late-binding flexibility of an actor system - 
their communication paths could be re-wired at runtime.

Also - concurrency also describes processes that occur concurrently, like the CCS where you have two processes
in parallel P | Q...in this case, the shared resource is simply the time ;)


### How does our workflow model accomodate concurrency? 

Well, for one, there's multiple worflows than run at the same time. 
How many, for whom and what they do is unknown and as irrelevant as the number of threads.
Inside the same workflow, a few execution threads can exist at the same time. Simply activating more links 
from any node will spawn a few threads and when these meet in special "join" nodes, the threads merge 
again.

If you have shared resources, you have the option of using any concurrent technique you like:

- in the classic model you would use semaphores, locks, barriers and other such contraptions - 
I recommend you use distributed locks not just local, since workflows can migrate all over the place...
We will provide some simple implementations
- in the actor model, you would simply use long-running actor workflows, which just listen for incoming 
messages and process them

While the shared context makes this entire model resemble the classic concurrency model, message 
passing is also possible
between wofklows or between sub-workflows and especially between workflows and the outside environment.

However, the workflow enforces a shift from thinking in semaphores to thinking in transactions. 
TODO will need to elaborate on that.


### Split/join

NOTE there are two options I'm considering:

1. special "split" node that will spawn threads and all other nodes can only activate 1 link at a time
1. any node could spawn multiple links, automatically trigerring thread spawns

Since we have to use a special "join" node to merge threads, there is a symetrical beauty to having a 
"split"...


Distribution
------------

Distributed programming deals with the distribution of resources and programs all over a network. 
The major difference is that communication is no longer guaranteed and response times vary.

The main concept to master at this point is "asynchronous". The actor model described above naturally 
resembles a distributed system and is very apt at being distributed, because of the embedded asynchronicity.

Asynchronous communication implies extremely late binding, flexibility and less depdencies between the
interacting parties.

TODO document how we're addressing distribution:

- remote gremlins
- gremlin API
- message passing primitives
- sync/async or async/sync bridges


Workflow
--------


How the Gremlins take over the world
====================================
   
If all software components out there start to implement the GremlinsAPI for sub-process management, the
gremlins will spread like wildfire and take over the world. Then they process themselves into oblivion.


Wait, there's more Gremlins
===========================

I noticed there's at least another project on github called "Gremlin" and interestingly, it deals with 
graphs and programming as well...it is probable that we both have the same vivid imagination :)

I've always thought of this distributed workflow as a bunch of gremlins, replicating themselves and 
chewing different problems up all over the place and I don't want to change it just because of a name, 
so, as hard as it may turn out to be, I'll keep this name, invoking the power of visualization.
   

Why, oh why?
============

While different functionalities like Web Services Choreography can be grafted onto existing frameworks 
like adding libraries to Java, I feel that a new framework of concepts and constructs is needed, to 
become the fabric for all the new constructs. 

While the BPM guys claim "we've got it" they're right. But so did BPEL, XPDL, YAWL etc....YET 
we're left to pick up the pieces in all and any language. 
There is no light workflow that we can use and 
all we can do is use all kinds of graphic designers, design all kinds of XML representation and interact 
with 3rd party engines which require yet more integration to interact with 3rd party systems, while at 
every step, simplicity is sacrificed on the altar of...well, I don't know!

At each step you bump into incompatible interfaces, incomprehensible states, yet another standard, 
n hundred page specifications, umpteen thousand attributes and so on.

There is an inner beauty to the simplicity of Lisp, where everything is a function...or Smalltalk where
everything is an object (except when there's turtles, you see).

That's where Scala comes in: as a natively scalable language, it gives us the chance to setup this 
gremlins fabric where we can write both small (1 liner) as well as big (inter-enterprise) workflows...
and focus on learning a small set of patterns and use a lot of built-in functionality that the 
fabric has to offer.

Hence this project. Workflow is a big part of it.


Preemptive answers to questions I made up
-----------------------------------------

### Why not use scripts?

Q: Well - John can script to add the just-downloaded episode to an RSS and notify TVersity to pick it up,
while Jane will just write Scala code for that...

Well - yeah, but John can't see where his "flow" hit a snag, can't debug it and can't nicely discuss it, 
share it, pause it or manage it remotely, can he?

Jane is even worse off - she can't even run her code remotely unless the recompiled thing is manually 
uploaded remotely...


