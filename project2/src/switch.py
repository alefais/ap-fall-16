# Author: Alessandra Fais
# Mat: 481017
# Computer Science and Networking
# Advanced Programming 2016/17
# Homework 2

from print_auxiliary import *
from constants import rule_key as rk, packet_key as pk, policy_key as polk, colors as col


class Switch:
    """
    The switch class.
    """

    def __init__(self, output_interfaces, packet_gen, controller, user_interaction):
        """
        The constructor.
        :param output_interfaces: the list of the output interfaces of the switch
        :param packet_gen: the input interface simulator
        :param controller: the controller
        :param user_interaction: flag used for the simulation
        """
        self.user_interaction = user_interaction
        self.interfaces = output_interfaces
        self.producer = packet_gen
        self.forwarding_table = {}
        self.policy_table = {}
        self.mobility_table = {}
        self.filter_list = []
        self.controller = controller
        self.output_list = []
        self.dropped_packets = []

    def user_simulation_control(self, n, x):
        req_in = True
        while req_in:
            com = input(col.GREEN + 'Insert command (--help to see the command list): ' + col.ENDC)
            if com == '--help':
                print(col.GREEN + 'COMMANDS:\n' +
                      'ftable\t->\tprint the forwarding table\n' +
                      'poltable ->\tprint the policy table\n' +
                      'mobtable ->\tprint the mobility table\n' +
                      'output\t->\tprint the output packet list\n' +
                      'dropped\t->\tprint the dropped packet list\n' +
                      'packet\t->\tprint the current packet\n' +
                      'go\t->\tproceed with the next iteration\n' +
                      'execute\t->\tcomplete the execution\n' + col.ENDC)
            elif com == 'ftable':
                print_forwarding_table(self.forwarding_table)
            elif com == 'poltable':
                print_policy_table(self.policy_table)
            elif com == 'mobtable':
                print_mobility_table(self.mobility_table)
            elif com == 'output':
                print_out_packet_list(self.output_list)
            elif com == 'dropped':
                print_dropped_packet_list(self.dropped_packets)
            elif com == 'packet':
                print_packet(n, x)
            elif com == 'go':
                req_in = False
            elif com == 'execute':
                req_in = False
                self.user_interaction = False

    def main_loop(self):
        """
        The main loop of the switch.
        :return: None
        """

        def check_rule(packet, rule, ip):
            """
            If the rule in the forwarding table is active forward the packet
            through the corresponding output interface, otherwise drop the packet.
            :param packet: the packet to be forwarded
            :param rule: the matched rule of the forwarding table
            :param ip: the effective destination ip (can be different from that in the packet due to mobility)
            :return: True if the rule is active and the packet can be forwarded,
                    False otherwise (the packet has to be dropped)
            """
            # print('* Rule state: ' + str(rule[rk.STATE]))

            if rule[rk.STATE]:
                self.output_list.append((rule[rk.DEST_PORT], packet))
                colored_print_switch(' packet forwarded into output interface ' + str(rule[rk.DEST_PORT]))
                return True
            else:
                self.dropped_packets.append(('DROPPED for suspension of the rule', packet))
                colored_print_switch(' dropped packet, rule for ip address ' + str(ip) + ' is suspended')
                return False

        def check_policies(packet, rule, final_ip):
            """
            If the security policies are active check if the operation contained in
            the current packet is allowed for the specific pair of source and destination
            ip addresses, otherwise drop the packet.
            :param packet: the packet to be forwarded
            :param rule: the matched rule in the forwarding table
            :param final_ip: the effective destination ip (can be different from that in the packet due to mobility)
            :return: None
            """
            if not self.policy_table:
                check_rule(packet, rule, final_ip)
            else:
                policy = self.policy_table[((packet[pk.HEADER])[pk.SRC_IP], (packet[pk.HEADER])[pk.DEST_IP])]
                if (packet[pk.PAYLOAD])[pk.OP_TYPE] == 'read':
                    if policy[polk.READ_COUNTER] > 0:
                        if check_rule(packet, rule, final_ip):
                            policy[polk.READ_COUNTER] -= 1
                            colored_print_switch(' policy table updated')
                            # print_state(None, None, "Updated policy table:", self.policy_table, 1)
                    else:
                        self.dropped_packets.append(('DROPPED for security policy', packet))
                        colored_print_switch(' dropped packet, max number of READ operations reached from ' +
                                             str((packet[pk.HEADER])[pk.SRC_IP]) + ' to ' +
                                             str((packet[pk.HEADER])[pk.DEST_IP]))
                elif (packet[pk.PAYLOAD])[pk.OP_TYPE] == 'write':
                    if policy[polk.WRITE_COUNTER] > 0:
                        if check_rule(packet, rule, final_ip):
                            policy[polk.WRITE_COUNTER] -= 1
                            colored_print_switch(' policy table updated')
                            # print_state(None, None, "Updated policy table:", self.policy_table, 1)
                    else:
                        self.dropped_packets.append(('DROPPED for security policy', packet))
                        colored_print_switch(' dropped packet, max number of WRITE operations reached from ' +
                                             str((packet[pk.HEADER])[pk.SRC_IP]) + ' to ' +
                                             str((packet[pk.HEADER])[pk.DEST_IP]))

        def check_filters(packet):
            """
            Checks if the packet has to be filtered or not
            according to the list of filters installed.
            :param packet: the current packet
            :return: False if the packet has to be filtered (dropped),
                    True otherwise
            """
            for filt in self.filter_list:
                if filt(packet):
                    return True
            return False

        def handle_mobility(ip_address):
            """
            Look in the mobility table for a rule for the destination ip address
            (if this rule is present then the packet must be forwarded through
            the output interface for the new destination ip address, otherwise
            it must be forwarded through the old output interface).
            :param ip_address: the old ip address
            :return: if a rule exists return the new ip address, otherwise
                    return the old ip address
            """
            if not self.mobility_table:
                return ip_address
            else:
                new_ip_address = self.mobility_table.get(ip_address)
                if not new_ip_address:
                    return ip_address
                else:
                    colored_print_switch(' mobility applied from ' + str(ip_address) + ' to ' + str(new_ip_address))
                    return new_ip_address

        def print_state(s, msg1, msg2, table, t_type):
            """
            Format print for the forwarding table
            :param s: type of the update received from the controller
            :param msg1: string
            :param msg2: string
            :param table: the data structure to be printed
            :param t_type: the type of the table (forwarding, policy, mobility)
            :return: None
            """
            if s:
                colored_print_switch(' from the controller \'' + s + '\'')
            if not msg1:
                print(msg2)
            else:
                print(msg1)
            if t_type == 0:
                print_forwarding_table(table)
            elif t_type == 1:
                print_policy_table(table)
            elif t_type == 2:
                print_mobility_table(table)

        def check_for_updates(it_count):
            """
            Manage the updates received from the controller.
            :param it_count: the current iteration of the switch
            :return: None
            """
            c_reply = self.controller.manage_requests(('Update', it_count, None))
            if c_reply[0] == 'RuleDeleted' or c_reply[0] == 'RuleSuspended' or c_reply[0] == 'RuleActivated':
                self.forwarding_table = c_reply[1]
                print_state(c_reply[0], None, "Updated forwarding table:", self.forwarding_table, 0)
            elif c_reply[0] == 'PoliciesActivated':
                self.policy_table = c_reply[1]
                print_state(c_reply[0], "Policy table", None, self.policy_table, 1)
            elif c_reply[0] == 'MobilityActivated':
                self.mobility_table = c_reply[1]
                print_state(c_reply[0], "Mobility table", None, self.mobility_table, 2)
            elif c_reply[0] == 'FilterActivated':
                self.filter_list = c_reply[1]
                colored_print_switch(' from the controller \'' + c_reply[0] + '\'')
            elif c_reply[0] == 'NoUpdates':
                colored_print_switch(' from the controller \'' + c_reply[0] + '\'')

        if not self.forwarding_table:
            reply = self.controller.manage_requests(('Install', self.interfaces, self.forwarding_table))
            if reply[0] == 'TableCreated':
                self.forwarding_table = reply[1]
            print_state(reply[0], "Forwarding table:", None, self.forwarding_table, 0)

        go = True
        count = 0
        while go:
            if count % 2 == 0:
                check_for_updates(count)

            try:
                input_interface, current_packet = next(self.producer)

                if not (check_filters(current_packet)):
                    if self.user_interaction:
                        self.user_simulation_control(count + 1, (input_interface, current_packet))
                    else:
                        print_packet(count + 1, (input_interface, current_packet))

                    packet_ip_destination = (current_packet[pk.HEADER])[pk.DEST_IP]
                    mobility_ip_destination = handle_mobility(packet_ip_destination)
                    match_rule = self.forwarding_table.get(mobility_ip_destination)
                    if not match_rule:
                        reply = self.controller.manage_requests(
                            ('GetRule', self.interfaces[0], mobility_ip_destination))
                        if reply[0] == 'RuleCreated':
                            self.forwarding_table[mobility_ip_destination] = reply[1]
                            match_rule = reply[1]
                        print_state(reply[0], None, "Updated forwarding table:", self.forwarding_table, 0)

                    check_policies(current_packet, match_rule, mobility_ip_destination)
                else:
                    colored_print_switch(' packet dropped according to a filter')
                    self.dropped_packets.append(('FILTERED', current_packet))

                count += 1

            except StopIteration:
                go = False
                print_out_packet_list(self.output_list)
                print_dropped_packet_list(self.dropped_packets)
