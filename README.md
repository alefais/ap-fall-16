# ap-fall-16
A collection of projects of the Advanced Programming course of the Computer Science and Networking Master's Degree @ University of Pisa.

<b>Note for projects 1 and 2:</b>
The abstract model for the Software Defined Network of projects 1 and 2 contains one switch that communicates with one controller: it is required the implementation of both the modules and of the communication between them.

| <b>Project number</b> | <b>Language</b> | <b>Description</b> |
| ---------- | ----------------------- | ----------- |
| 1 | Ocaml | The implementation of a Domain Specific Programming Language for a Software Defined Network model and a simulation of the behaviour of the network modeled. The main parts are an abstract switch module (capable of receiving, processing and forwarding packets) and a controller module (capable of creating and modifying the forwarding table and able to manage security policies) that uses the designed control language. The interpreter for the control language has been written in OCaml. The interaction between the modules is simulated given a set of packets, the network and the interfaces of input and output of the switch module. |
| 2 | Python | The implementation of an API for a Software Defined Network model and two step by step simulations of the behaviour and state of the network modeled. The main parts of the project are the modules switch and controller (implemented as Python classes) and the API that provides all the functionalities needed to modify and manage the forwarding table and the security policies. The first simulation is automatic and executes on given data. The second simulation is interactive and allows the user to set the interfaces of the switch and to insert the network as a set of IP addresses. Given the initial data both the simulations show the state of the system at each step of the execution: forwarding table, active security policies, packets queued in the buffer of each interface of the switch (input and output) and packets that have been dropped (with the reason of their discard). |
| 3 | Java |  |
| 4 | Scala |  |
