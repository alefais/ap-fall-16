(*
  Author: Alessandra Fais
  Mat: 481017
  Computer Science and Networking
  Advanced Programming 2016/17
  Homework 1
*)

type binaryflag = Active_state | Suspended_state
type 'a list_t = Nil | Cons of 'a * 'a list_t
type filter_t =
  | BlockTCP_f
  | BlockUDP_f
  | BlockIP_f of int
  | NoFilter_f

(* Expressions of the domain specific language *)
type expr =
  | Ports_e of int list_t
  | IPaddresses_e of int list_t
  | Rule_e of int * int * int * binaryflag * int * int (* src_IPaddress * dst_IPaddress * dst_port * activeflag * counters *)
  | Table_e of expr list_t
  | Install of expr * expr
  | GetRules of int * expr * expr
  | Delete of expr * expr
  | Suspend of expr * expr
  | Activate of expr * expr
  | ActivatePolicies of int * int * expr
  | Filter_e of filter_t
  | HandleMobility of int * int * expr

(* Evaluation of the expressions *)
let rec eval (e : expr) : expr =

  (* Auxiliary functions *)

  (*
    Check if the forwarding table is well structured (must be a list of expr
    of type Rule_e).
  *)
  let rec checkfwdtable (e:expr list_t) =
    match e with
    | Nil as t -> t
    | Cons(e1, es) ->
      (match (eval e1) with
       | Rule_e _ -> Cons(e1, checkfwdtable es)
       | _ -> failwith("Type error")
      )

  (*
    Check if two rules are equals.
  *)
  in let comparerules (e1:expr) (e2:expr) =
       match e1, e2 with
       | Rule_e(s_ip, d_ip, p, f, cr, cw), Rule_e(s_ip', d_ip', p', f', cr', cw') ->
         (s_ip = s_ip') && (d_ip = d_ip') && (p = p')
       | _ -> failwith("Type error")

  (*
    Delete the rule r from the forwarding table t and return the new table.
  *)
  in let rec del (r:expr) (t:expr list_t) =
       match t with
       | Nil -> Nil
       | Cons(r1, t1) ->
         if (comparerules r r1)
         then del r t1
         else Cons(r1, (del r t1))

  (*
    Suspend the rule r inside the forwarding table t (if present) and return
    the new table.
  *)
  in let rec susp (r:expr) (t:expr list_t) =
       match t with
       | Nil -> Nil
       | Cons(Rule_e(s_ip, d_ip, p, f, cr, cw) as r1, t1) ->
         if (comparerules r r1)
         then Cons(Rule_e(s_ip, d_ip, p, Suspended_state, cr, cw), t1)
         else Cons(r1, (susp r t1))
       | _ -> failwith("Type error")

   (*
     Activate the rule r inside the forwarding table t (if present) and return
     the new table.
   *)
  in let rec act (r:expr) (t:expr list_t) =
       match t with
       | Nil -> Nil
       | Cons(Rule_e(s_ip, d_ip, p, f, cr, cw) as r1, t1) ->
         if (comparerules r r1)
         then Cons(Rule_e(s_ip, d_ip, p, Active_state, cr, cw), t1)
         else Cons(r1, (act r t1))
       | _ -> failwith("Type error")

  (*
    Append two lists of type list_t.
  *)
  in let rec append (l1:expr list_t) (l2:expr list_t) : expr list_t =
       match l1 with
       | Nil -> l2
       | Cons(a, b) -> Cons(a, (append b l2))

  (*
   Create a sequence of rules from the whole network to the same dest IP address
   (the output interface is the same for all the rules).
  *)
  in let ruleswholenetwork_fornewtable (addr:int) (port:int) (l:int list_t) =
     let rec acctableaux addr' port' l' acc =
      match l' with
      | Nil -> acc
      | Cons(n, ns) ->
        let r1 = Rule_e(n, addr', port', Active_state, -1, -1)
        in (acctableaux addr' port' ns (append acc (Cons(r1, Nil))))
     in acctableaux addr port l Nil

  (*
    Create a sequence of rules from the whole network to the same dest IP address
    (the output interface is the same for all the rules).
  *)
  in let ruleswholenetwork_fornewrules (addr:int) (port:int) (l:int list_t) =
      let rec acctableaux addr' port' l' acc =
       match l' with
       | Nil -> acc
       | Cons(n, ns) ->
         let r1 = Rule_e(n, addr', port', Active_state, -1, -1)
         in let r2 = Rule_e(addr', n, port', Active_state, -1, -1)
         in (acctableaux addr' port' ns (append acc (Cons(r1, Cons(r2, Nil)))))
      in acctableaux addr port l Nil

  (*
    Given a list of IP addresses (the network) and a list of interfaces (the output
    interfaces of the switch) create a new forwarding table. The correpsondance
    between address and output interface is decided by using a simple hash function.
  *)
  in let createtable (e1:int list_t) (e2:int list_t) : expr list_t =
      let hashfunction addr nports =
      (((addr / 2)) + 7) mod nports

      in let rec listlength l =
          match l with
          | Nil -> 0
          | Cons(_, l') -> 1 + listlength l'

      in let rec getelementat (pos:int) (l:int list_t) : int =
          match l with
          | Nil -> failwith("No output interfaces")
          | Cons(p, ps) -> if (pos = 0)
            then p
            else getelementat (pos-1) ps

      in let len = listlength e2

      (*
        For each element in the address list compute index=hash(addr, number_of_out_ports)
        and create a new rule for the table with the association between the addr and
        the out_port in position index of the list of ports.
       *)
      in let rec acctable (acc:expr list_t) (network:int list_t) (wholenetwork:int list_t) =
          if len = 0 then acc
          else
            match network with
            | Nil -> acc
            | Cons(addr, tl) -> (
                let position = hashfunction addr len in
                let port = getelementat position e2 in
                (*Printf.printf "Appending rule (position %d) (%d,%d,Active)\n" position addr port;*)
                let rules = ruleswholenetwork_fornewtable addr port wholenetwork
                in (acctable (append acc rules) tl wholenetwork))
      in acctable Nil e1 e1

  (*
    Create a new sequence of rules with the same destination IP and the same output
    interface (one new rule for each source IP).
  *)
  in let createrules (dstip:int) (network:int list_t) (outputs:int list_t) : expr list_t =
       match outputs with
       | Nil -> Nil
       | Cons(out, os) -> ruleswholenetwork_fornewrules dstip out network

  (*
    Set the policies in each rule of the actual forwarding table: the number of
    allowed write operations is fixed to nwrite for each destination, the number of
    read operations is fixed to nread.
  *)
  in let rec actpol (nread:int) (nwrite:int) (table:expr list_t) : expr list_t =
       match table with
       | Nil -> Nil
       | Cons(Rule_e(s_ip, d_ip, p, f, cr, cw), rs) ->
         Cons(Rule_e(s_ip, d_ip, p, f, nread, nwrite), (actpol nread nwrite rs))
       | _ -> failwith("Type error")

  (*
    Find the first rule with destination IP address the new IP address to which we
    have to forward all the packets directed versus the old IP address. If such a
    rule is present insiede the forwarding table then return the corresponding
    output interface, else return -1.
  *)
  in let rec findfirstmatchforIP (newip:int) (table:expr list_t) : int =
       match table with
       | Nil -> -1
       | Cons(Rule_e(_, d_ip, p, _, _, _), t1) ->
         if (newip = d_ip)
         then p
         else findfirstmatchforIP newip t1
       | _ -> failwith("Type error")

  (*
    If findfirstmatchforIP returns -1 then the table remains unchanged, otherwise
    the output interface number associated to the new IP address is substituted as
    new output interface in all the rules for the old IP addresses.
  *)
  in let handlemob (oldip:int) (newip:int) (table:expr list_t) : expr list_t =
       let newport = findfirstmatchforIP newip table
       in if (newport = -1) then table
       else
       let rec aux oldip' tb newport' =
         match tb with
         | Nil -> Nil
         | Cons(Rule_e(s_ip, d_ip, p, f, cr, cw) as r1, t1) ->
           if (oldip' = d_ip)
           then Cons(Rule_e(s_ip, d_ip, newport', f, cr, cw), (aux oldip' t1 newport'))
           else Cons(r1, (aux oldip' t1 newport'))
         | _ -> failwith("Type error")
       in aux oldip table newport


  in match e with
  | Rule_e _ | Ports_e _ | IPaddresses_e _ | Filter_e _ as e' -> e'
  | Table_e e -> Table_e(checkfwdtable e)
  | Install (e1, e2) ->
    (match ((eval e1), (eval e2)) with
     | (IPaddresses_e i, Ports_e p) -> Table_e((createtable i p))
     | _ -> failwith("Type error"))
  | GetRules (destip, e1, e2) ->
    (match (destip, (eval e1), (eval e2)) with
     | (destip', IPaddresses_e i, Ports_e p) -> Table_e((createrules destip' i p))
     | _ -> failwith("Type error"))
  | Delete (e1, e2) ->
    (match ((eval e1), (eval e2)) with
     | (Rule_e _ as r, Table_e t) -> Table_e((del r t))
     | _ -> failwith("Type error"))
  | Suspend (e1, e2) ->
    (match ((eval e1), (eval e2)) with
     | (Rule_e _ as r, Table_e t) -> Table_e((susp r t))
     | _ -> failwith("Type error"))
  | Activate (e1, e2) ->
    (match ((eval e1), (eval e2)) with
    | (Rule_e _ as r, Table_e t) -> Table_e(act r t)
    | _ -> failwith("Type error"))
  | ActivatePolicies (r, w, e1) ->
    (match (eval e1) with
     | (Table_e t) -> Table_e(actpol r w t)
     | _ -> failwith("Type error"))
  | HandleMobility (oldip, newip, e1) ->
    (match (eval e1) with
     | (Table_e t) -> Table_e(handlemob oldip newip t)
     | _ -> failwith("Type error"))
