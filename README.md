# ap-fall-16
A collection of the four final projects of the Advanced Programming course of the Computer Science and Networking Master's Degree @ University of Pisa.


The projects 1 and 2 require first to design an abstract model for a Software Defined Network (one switch that communicates with one controller) and then to implement the modules and the communication between them respectively using OCaml and Python. More in details:

- <b>project 1</b>:
It consists in the implementation of a Domain Specific Programming Language for a Software Defined Network. The main parts are an abstract switch module (capable of receiving, processing and forwarding packets), a controller module (capable of creating and modifying the forwarding table and able to manage security policies) and the design and implementation in OCaml of the interpreter for the control language. The interaction between the modules is simulated given a set of packets, the network and the interfaces of input and output of the switch abstract module.


- <b>project 2</b>:
It consists in the implementation of an API for a Software Defined Network. The most important parts are the modules switch and controller (implemented as Python classes) and the API that provides all the functionalities needed to modify and manage the forwarding table and the security policies. The simulation is done both in an automatic (with given data) and in an interactive way. The interactive simulation allows the user to set the interfaces of the switch and to insert the network as a set of IP addresses. Given the initial data both the simulations show the state of the system at each step of the execution: the state of the forwarding table, the active security policies, the packets in the buffers for each interface of the switch (input and output) and the packets that have been dropped and why.

- <b>project 3</b>:


- <b>project 4</b>:
