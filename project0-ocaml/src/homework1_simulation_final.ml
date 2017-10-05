(*
  Author: Alessandra Fais
  Mat: 481017
  Computer Science and Networking
  Advanced Programming 2016/17
  Homework 1
*)

(*
open Homework1_DSL_final
open Homework1_SWITCH_final
*)

(* 6 packets are defined for the simulation *)
let payload1 = { protocol=TCP; typeop=Read }
let payload2 = { protocol=UDP; typeop=Write }

(* The switch executes 25 iterations *)

(* Iteration 0: switch requests update -> delete rule *)
(* Iteration 1: switch processes packet 0 *)
let pkt0 = { number = 0; header = { dst_ipaddr = 4; src_ipaddr = 10}; payload = payload1 }
(* Iteration 2: switch requests update -> no updates *)
(* Iteration 3: no packets in the current input interface *)
(* Iteration 4: switch requests update -> delete rule *)
(* Iteration 5: switch processes packet 2 *)
let pkt2 = { number = 2; header = { dst_ipaddr = 20; src_ipaddr = 40}; payload = payload1 }
(* Iteration 6: switch requests update -> suspend rule *)
(* Iteration 7: switch processes packet 1 *)
let pkt1 = { number = 1; header = { dst_ipaddr = 15; src_ipaddr = 30}; payload = payload2 }
(* Iteration 8: switch requests update -> delete rule *)
(* Iteration 9: no packets in the current input interface *)
(* Iteration 10: switch requests update -> activate policies *)
(* Iteration 11: switch processes packet 3 *)
let pkt3 = { number = 3; header = { dst_ipaddr = 35; src_ipaddr = 15}; payload = payload2 }
(* Iteration 12: switch requests update -> delete rule *)
(* Iteration 13: no packets in the current input interface *)
(* Iteration 14: switch requests update -> activate filter BlockTCP *)
(* Iteration 15: no packets in the current input interface *)
(* Iteration 16: switch requests update -> delete rule *)
(* Iteration 17: switch processes packet 4, that is dropped because of the filter on the protocol *)
let pkt4 = { number = 4; header = { dst_ipaddr = 40; src_ipaddr = 25}; payload = payload1 }
(* Iteration 18: switch requests update -> suspend rule *)
(* Iteration 19: no packets in the current input interface *)
(* Iteration 20: switch requests update -> delete rule *)
(* Iteration 21: no packets in the current input interface *)
(* Iteration 22: switch requests update -> no updates *)
(* Iteration 23: switch processes packet 5 *)
let pkt5 = { number = 5; header = { dst_ipaddr = 15; src_ipaddr = 30}; payload = payload2 }
(* Iteration 24: all the input interfaces are empty, the switch processed all the packets *)


(* The input and output interfaces of the switch *)
let inputs = [(12, [pkt0; pkt1]); (34, []); (56, [pkt2; pkt3; pkt4; pkt5])]
let outputs = [(0, []); (1, []); (2, []); (3, []); (4, []); (5, [])]

(* The execution of the switch *)
let result = switch (inputs, outputs)
let expectedoutput = [(0, [pkt3; pkt0]); (1, []); (2, [pkt5; pkt1]); (3, []); (4, []); (5, [pkt2])]
let () = assert (result = expectedoutput);;
