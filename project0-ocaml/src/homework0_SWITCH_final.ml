(*
  Author: Alessandra Fais
  Mat: 481017
  Computer Science and Networking
  Advanced Programming 2016/17
  Homework 1
*)

(*open Homework1_DSL_final*)

(* Auxiliary types and values *)
type interface = int
type ipaddress = int
type flag = Active | Suspended (* Mark a rule in the forward table as active or suspended *)

type rule = {
  src_ipaddr : ipaddress;
  dst_ipaddr : ipaddress;
  dst_port : interface;
  activationflag : flag;
  mutable readscounter : int;
  mutable writescounter : int
}

type forwarding_table = rule list
type filter =
  | BlockTCP
  | BlockUDP
  | BlockIP of int
  | NoFilter

type request_type = (* The type of the requests from the switch to the controller *)
  | RequestTable of interface list
  | RequestRule of interface list * ipaddress
  | RequestUpdate of int

type data_controller = (* The type of the data sent from the controller to the switch *)
  | Table of forwarding_table
  | Rules of forwarding_table
  | Policies of forwarding_table
  | Filter of filter
  | Mobility of forwarding_table
  | NoUpdates

(* Packet structure *)
type pkt_operation_type = Read | Write | OtherOp (* The operation type of the packet *)
type protocol_type = TCP | UDP | OtherProtocol

type header = {
  dst_ipaddr : ipaddress;
  src_ipaddr : ipaddress;
}

type payload = {
  protocol : protocol_type;
  typeop : pkt_operation_type
}

type packet = {
  number : int; (* for test purposes *)
  header : header;
  payload : payload
}

type 'a match_found = No of int | Yes of 'a


(* Auxiliary functions for tests (print functions) *)

let rec printpackets (l:packet list) : unit =
  match l with
  | [] -> Printf.printf "";
  | p::ps -> (Printf.printf "{ ID = %d; header = { dst_ipaddr = %d; src_ipaddr = %d}; payload = { ... } }\n"
              p.number p.header.dst_ipaddr p.header.src_ipaddr;
              printpackets ps)

let rec printpacketlists (l: (interface * packet list) list) : unit =
  match l with
  | [] -> Printf.printf "\n";
  | (i, ps)::is -> (Printf.printf "%d ->\n" i; printpackets ps; printpacketlists is)

let printrule (r:rule) : unit =
  if (r.activationflag=Active) then
    (Printf.printf "{ src_ipaddr = %d; dst_ipaddr = %d; dst_port = %d; activationflag = Active; rcount = %d; wcount = %d}\n"
      r.src_ipaddr r.dst_ipaddr r.dst_port r.readscounter r.writescounter)
  else
    (Printf.printf "{ src_ipaddr = %d; dst_ipaddr = %d; dst_port = %d; activationflag = Suspended; rcount = %d; wcount = %d}\n"
       r.src_ipaddr r.dst_ipaddr r.dst_port r.readscounter r.writescounter)

let rec printtable (t:forwarding_table) : unit =
  match t with
  | [] -> Printf.printf "End routing table\n\n";
  | t1::tl -> printrule t1; printtable tl

(*
  Controller interface module
  param in request: a request for an update from the switch (it can be a request
  for a new forwarding table, for a missing rule, or for a generic update - including
  the security policies.)
*)
(* State of the forwarding table *)
let controller_table : expr ref = ref (Table_e(Nil))
(* Network *)
let controller_network : int list_t ref = ref (Cons(10, Cons(15, Cons(20, Cons(25, Cons(30, Cons(35, Cons(40, Nil))))))))
let controller (request:request_type) : data_controller =

  let network = !controller_network

  (* Auxiliary functions *)

  (*
    Convert an expr of type Table_e in a new data structure
    of type forwarding_table, usable by the switch.
  *)
  in let convertexprtotable (tableexpr:expr) : forwarding_table =
      let rec aux tb (acctable:forwarding_table) =
        match tb with
        | Table_e(t) ->
          (match t with
           | Nil -> acctable
           | Cons(Rule_e(s_ip, d_ip, p, f, cr, cw), tl) ->
             if (f=Active_state)
             then aux (Table_e(tl)) (acctable@[ { src_ipaddr = s_ip;
                                                  dst_ipaddr = d_ip;
                                                  dst_port = p;
                                                  activationflag = Active;
                                                  readscounter = cr;
                                                  writescounter = cw } ])
             else aux (Table_e(tl)) (acctable@[ { src_ipaddr = s_ip;
                                                  dst_ipaddr = d_ip;
                                                  dst_port = p;
                                                  activationflag = Suspended;
                                                  readscounter = cr;
                                                  writescounter = cw } ])
           | _ -> failwith("Type error"))
        | _ -> failwith("Type error")
      in aux tableexpr []

  (*
    Convert a data structure of type list_t in a new one of type
    list (the built-in type list of Ocaml, usable by the switch).
  *)
  in let rec convertlist (l:interface list) : interface list_t =
      match l with
      | [] -> Nil
      | x::xs -> Cons(x, (convertlist xs))

  (*
    Create the forwarding table (call eval of Install) and convert the obtained
    expression of type Table_e in data structure of type forwarding_table, usable
    by the switch. The controller knows the network (the list of IP addresses).
  *)
  in let createtable (outlist:interface list) : data_controller =
      Printf.printf "CONTROLLER: install table (forwarding table created)\n";
      let fwdtable = eval(Install(IPaddresses_e(network), Ports_e(convertlist outlist)))
      in controller_table := fwdtable;
      Table(convertexprtotable fwdtable)

  (*
    Append two lists of type list_t.
  *)
  in let rec append (l1:expr list_t) (l2:expr list_t) : expr list_t =
       match l1 with
       | Nil -> l2
       | Cons(a, b) -> Cons(a, (append b l2))

  (*
    Create a rule and convert the obtained expression of type Rule_e in a data
    structure of type rule, usable by the switch.
  *)
  in let createrule (outlist:interface list) (ipaddr:ipaddress) : data_controller =
      Printf.printf "CONTROLLER: create rules (provide the rules for the IP address %d)\n" ipaddr;
      let rules =
       match (eval(GetRules(ipaddr, IPaddresses_e(network), Ports_e(convertlist outlist)))) with
       | Table_e t -> t
       | _ -> failwith("Type error")
      in let state =
          match !controller_table with
          | Table_e tb -> tb
          | _ -> failwith("Type error")
      in (controller_table := Table_e(append rules state);
        (controller_network := Cons(ipaddr, !controller_network));
        Rules(convertexprtotable (Table_e(rules))))

  (*
    Delete a rule (the first) from the current forwarding table and convert the
    obtained expression of type Table_e in a data structure of type forwarding
    table, usable by the switch.
  *)
  in let deletefirstrule (tb:expr) : data_controller =
      match tb with
      | Table_e(Cons(r, _)) as tb' ->
        Printf.printf "CONTROLLER: delete rule (first rule deleted)\n";
        let t = eval(Delete(r, tb'))
        in controller_table := t;
        Table(convertexprtotable t)
      | _ -> NoUpdates

  (*
    Change the state of a rule (the first) in the current forwarding table and
    convert the obtained expression of type Table_e in a data structure of type
    forwarding table, usable by the switch.
  *)
  in let suspendoractivaterule (tb:expr) : data_controller =
      match tb with
      | Table_e(Cons((Rule_e(_, _, _, f, _, _)) as r, _)) as tb' ->
        let t =
          if (f=Active_state)
          then (Printf.printf "CONTROLLER: suspend rule (first rule suspended)\n"; eval(Suspend(r, tb')))
          else (Printf.printf "CONTROLLER: activate rule (first rule activated)\n"; eval(Activate(r, tb')))
        in controller_table := t;
        Table(convertexprtotable t)
      | _ -> NoUpdates

  (*
    Activate the security policies in the forwarding table and convert the obtained
    expression of type Table_e in data structure of type forwarding_table, usable
    by the switch. The security policy is set as 2 allowed reads and 1 write for
    each destination.
  *)
  in let activatepolicies (tb:expr) : data_controller =
      Printf.printf "CONTROLLER: activate policy (allow only 2 reads and 1 writes)\n";
      let poltable = eval(ActivatePolicies(2, 1, tb))
      in controller_table := poltable;
      Policies(convertexprtotable poltable)

  (*
    Convert an expr of type Filter_e in a data of type filter.
  *)
  in let convertexprtofilter (filterexpr:expr) : filter =
      match filterexpr with
      | Filter_e f ->
       (match f with
        | BlockTCP_f -> BlockTCP
        | BlockUDP_f -> BlockUDP
        | BlockIP_f ip1 -> BlockIP ip1
        | NoFilter_f -> NoFilter)
      | _ -> failwith("Type error")

  (*
    Create a filter.
  *)
  in let createfilter (n:int) : data_controller =
      Printf.printf "CONTROLLER: activate filter of type ";
      let firstel (lis:int list_t) : int =
       match lis with
       | Nil -> failwith("Empty list")
       | Cons(el, _) -> el
      in
      if (((n+1) mod 15) = 0) then (Printf.printf "BlockTCP\n"; Filter(convertexprtofilter (eval(Filter_e(BlockTCP_f)))))
      else if (((n+1) mod 29) = 0) then (Printf.printf "BlockUDP\n"; Filter(convertexprtofilter (eval(Filter_e(BlockUDP_f)))))
      else (let el = firstel network in Printf.printf "BlockIP %d\n" el; Filter(convertexprtofilter (eval(Filter_e(BlockIP_f(el))))))

  (*
    Handle the mobility from the first IP address in the network list to the second one.
  *)
  in let handlemobility (tb:expr) : data_controller =
      Printf.printf "CONTROLLER: handle mobility from the address IP ";
      let (oldip, newip) =
        let pickup (l:int list_t) : (int * int) =
         match l with
           | Cons(a, Cons(b, _)) -> (a, b)
         | _ -> failwith("Mobility not allowed")
        in pickup network
      in let mobtable = eval(HandleMobility(oldip, newip, tb))
      in controller_table := mobtable;
      Printf.printf "%d to %d\n" oldip newip;
      Mobility(convertexprtotable mobtable)
  (*
    When the request received from the switch is the periodic RequestUpdate then
    send a new table obtaied by removing a rule, or a new table with a rule that
    has been activated or suspended, or a new table with new active policies or
    no updates.
  *)
in let createupdate (actualfwdtable:expr) (counter:int) : data_controller =
    if ((counter mod 4) = 0) then deletefirstrule actualfwdtable
    else if ((counter mod 6) = 0) then suspendoractivaterule actualfwdtable
    else if ((counter mod 10) = 0) then activatepolicies actualfwdtable
    else if ((counter mod 14) = 0) then createfilter counter
    else if ((counter mod 16) = 0) then handlemobility actualfwdtable
    else (Printf.printf "CONTROLLER: no updates\n"; NoUpdates)

  in match request with
  | RequestTable(interfacelist) -> createtable interfacelist
  | RequestRule(interfacelist, ipaddress) -> createrule interfacelist ipaddress
  | RequestUpdate(count) -> createupdate !controller_table count


(*
  Switch module
  param inports: list of input interface number * packet list
  param in outports: list of output interface number * packet list
*)
let switch ((inports:(interface * packet list) list), (outports:(interface * packet list) list)) : (interface * packet list) list =

  let inlist, outlist = (* The lists of the input and output interfaces of the switch *)
      List.map fst inports, List.map fst outports

  in let activefilter : filter ref = ref NoFilter

  (* Auxiliary functions *)

  (*
    Insert the packet p in the list of packets of the output interface outp.
  *)
  in let rec putout (p:packet) (outp:interface) (outports:(interface * packet list) list) =
      match outports with
      | [] -> failwith("Interface not found")
      | ((nout, l) as a)::os ->
        if (outp=nout)
        then (nout, p::l)::os
        else a::(putout p outp os)

  (*
    Check if the security policy is active and in that case verify if the operation
    in the packet is permitted (true) or not (false).
  *)
  in let checkpolicy (p:packet) (r:rule) =
      Printf.printf "SWITCH: check policy for packet %d\n" p.number;
      let optype = p.payload.typeop
      in match optype with
      | Read -> if (r.readscounter = -1) then true else
        (if (r.readscounter > 0)
        then (r.readscounter <- r.readscounter - 1; true)
        else false)
      | Write -> if (r.writescounter = -1) then true else
        if (r.writescounter > 0)
        then (r.writescounter <- r.writescounter - 1; true)
        else false
      | OtherOp -> true

  (*
    Find the matching rule inside the forwarding table and return the corresponding
    output interface number or -1 if no rule matches.
  *)
  in let rec findrulematch (p:packet) (table:forwarding_table) : (interface * bool) =
      match table with
      | [] -> (-1, true)
      | r::rs ->
        if (p.header.src_ipaddr=r.src_ipaddr) && (p.header.dst_ipaddr=r.dst_ipaddr) && (r.activationflag=Active)
        then (r.dst_port, (checkpolicy p r))
        else findrulematch p rs

  in let applyfilter (p:packet) =
      match !activefilter with
      | NoFilter -> true
      | BlockTCP ->
        (Printf.printf "SWITCH: apply filter BlockTCP to packet %d\n" p.number;
         if (p.payload.protocol = TCP) then false else true)
      | BlockUDP ->
        (Printf.printf "SWITCH: apply filter BlockUDP to packet %d\n" p.number;
         if (p.payload.protocol = UDP) then false else true)
      | BlockIP ip ->
        (Printf.printf "SWITCH: apply filter BlockIP %d to packet %d\n" ip p.number;
         if (p.header.src_ipaddr = ip) then false else true)

  (*
    Search the route for the packet: if there is a rule in the forwarding table
    then send it in the correct output interface. Otherwise ask for new updates
    (rules) to the controller.
    Return the output interface lists (containing the packet in the right interface
    list or inalterated if the packet is dropped). The packet is dropped due to a
    filter or a security policy.
  *)
  in let rec routepacket (pkt:packet) (table:forwarding_table) (outs:(interface * packet list) list) =
      Printf.printf "SWITCH: in routepacket number %d\n" pkt.number;
      (*Printf.printf "{ ID = %d; header = { dst_ipaddr = %d; src_ipaddr = %d}; payload = { ... } }\n"
                   pkt.number pkt.header.dst_ipaddr pkt.header.src_ipaddr;*)

      if (applyfilter pkt) then
        let (outp, policy) = findrulematch pkt table
        in if (outp = -1)
        then No(pkt.header.dst_ipaddr)
        else (if (policy) then Yes(putout pkt outp outs) else (Printf.printf "SWITCH: Packet %d dropped\n" pkt.number; Yes(outs)))
      else (Printf.printf "SWITCH: Packet %d dropped\n" pkt.number; Yes(outs))

  (*
    Route the first packet inside the input interface. If the interface is empty
    then return the output interfaces unchanged, else return the remained packets
    in the input list and the new output lists (now containing the packet in the
    correct output interface list).
  *)
  in let rec emptyinlist (ins:packet list) (outs:(interface * packet list) list) (tb:forwarding_table) =
      match ins with
      | [] -> Yes([], outs)
      | p::ps ->
        let r = routepacket p tb outs
        in match r with
        | No(ip) -> No(ip)
        | Yes(o) -> Yes(ps, o)

  (*
    Send a request to the controller for a missing rule in the forwarding table
    (the table does not contain an association for the destination IP address searched).
  *)
  in let requestrule (pkt_dst_ipaddr:ipaddress) (table:forwarding_table) =
      Printf.printf "SWITCH: rule not found (request a rule for IP address destination %d)\n" pkt_dst_ipaddr;
      let data = controller(RequestRule(outlist, pkt_dst_ipaddr))
      in match data with
      | Rules r -> r@table
      | _ -> table

  (*
    When the switch receives a table as an update check the differences between the new
    table and the old one in order to save the values of the counters for the policies.
  *)
  in let difftableupdate (table:forwarding_table) (update:forwarding_table) : forwarding_table =
       let rec aux (tb:forwarding_table) (up:forwarding_table) (acc:forwarding_table) =
         match tb, up with
         | _, [] | [], _ -> acc
         | t1::ts, u1::us ->
           if (t1.src_ipaddr=u1.src_ipaddr) && (t1.dst_ipaddr=u1.dst_ipaddr) && (t1.dst_port=u1.dst_port)
           then (if (t1.activationflag=u1.activationflag) then (aux ts us (acc@[t1])) else (aux ts us (acc@[u1])))
           else aux ts up acc
       in aux table update []

  (*
    When the switch receives a table after a mobility update check the differences between the new
    table and the old one in order to save the values of the counters for the policies. If the two
    tables have not the same size then is necessary to fail because the received table is not
    consistent with the old table of the switch.
  *)
  in let difftablemobility (table:forwarding_table) (update:forwarding_table) : forwarding_table =
       let rec aux (tb:forwarding_table) (up:forwarding_table) (acc:forwarding_table) =
         match tb, up with
         | _, [] | [], _ -> acc
         | t1::ts, u1::us ->
           if (t1.src_ipaddr=u1.src_ipaddr) && (t1.dst_ipaddr=u1.dst_ipaddr)
           then (if (t1.dst_port=u1.dst_port) then (aux ts us (acc@[t1])) else (aux ts us (acc@[u1])))
           else failwith("Error during mobility")
       in aux table update []

  (*
    Ask the controller for a periodic update: can be a new rule, or a policy, or
    if there are no new updates the switch maintains the forwarding table unchanged.
  *)
  in let requestupdate (table:forwarding_table) (count:int) =
      Printf.printf "SWITCH: request update\n";
      let data = controller(RequestUpdate(count))
      in match data with
      | NoUpdates -> table
      | Table newtb -> difftableupdate table newtb
      | Policies newt -> newt
      | Filter f -> activefilter := f; table
      | Mobility newt -> difftablemobility table newt
      | _ -> table

  (*
    Check if all the input interfaces are empty.
  *)
  in let rec checkendpackets (inlist:(interface * packet list) list) : bool =
      match inlist with
      | [] -> true
      | (_, [])::is -> true && (checkendpackets is)
      | (_, _)::_ -> false

  (*
    The main function of the loop.
  *)
  in let rec mainloop (inps:(interface * packet list) list) (outps:(interface * packet list) list) (table:forwarding_table) counter =

      Printf.printf "\nSWITCH: mainloop iteration %d\n\n" counter;
      Printf.printf "Input interfaces\n"; printpacketlists inps;
      Printf.printf "Output interfaces\n"; printpacketlists outps;
      (*Printf.printf "Table\n"; printtable table;*)

      if ((checkendpackets inps) = false) then
        if ((counter mod 2) <> 0) then
          match (inps, outps) with
          | ([], _) | (_, []) -> outps
          | ((inum, ipkts)::is as i, _) ->
            let res = emptyinlist ipkts outps table
            in match res with
            | No(ip) -> mainloop i outps (requestrule ip table) counter
            | Yes(inp', outp') -> mainloop (is@[(inum, inp')]) outp' table (counter+1)
        else
          mainloop inps outps (requestupdate table counter) (counter+1)
      else outps

(* MAIN *)

in let fwdtable =
    let table = controller(RequestTable(outlist)) (* Request and obtain the forwarding table *)
    in match table with
    | Table t -> t
    | _ -> failwith("Type error: this is not a forwarding table")

in mainloop inports outports fwdtable 0
