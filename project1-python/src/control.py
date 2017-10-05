# Author: Alessandra Fais
# Mat: 481017
# Computer Science and Networking
# Advanced Programming 2016/17
# Homework 2

from api.install_table import install
from api.delete_rule import delete_rule
from api.suspend_rule import suspend_rule
from api.activate_rule import activate_rule
from api.install_policy import install_policy
from api.install_mobility import install_mobility
from api.install_filter import install_filter
from print_auxiliary import *
from constants import rule_key as rk, packet_key as pk
import copy


class Controller:
    """
    The controller class
    """

    def __init__(self, ip_addresses):
        """
        The constructor.
        :param ip_addresses: the network
        """
        self.ip_addresses = ip_addresses
        self.backup_forwarding_table = {}
        self.backup_policy_table = {}
        self.backup_mobility_table = {}
        self.backup_filters = []
        self.times = 0
        self.set_pol = False
        self.set_mob = False

    def manage_requests(self, req):
        """
        Manage the requests received from the switch.
        :param req: a tuple that encapsulates the type of the request and eventual
                    parameters needed by the controller
        :return: a tuple that encapsulates the type of the reply and the data
        """
        data_type = ''
        data = None

        if req[0] == 'Install':
            colored_print_controller(' received request for a new forwarding table')
            interfaces, fwd_table = req[1], req[2]
            self.backup_forwarding_table = install(self.ip_addresses, interfaces)
            data_type = 'TableCreated'
            data = copy.deepcopy(self.backup_forwarding_table)

        elif req[0] == 'GetRule':
            colored_print_controller(' received request for a new rule for ' + str(req[2]))
            interface, ip_address = req[1], req[2]
            new_rule = {rk.DEST_PORT: interface, rk.STATE: True}
            self.backup_forwarding_table[ip_address] = new_rule
            if ip_address not in self.ip_addresses:
                self.ip_addresses.insert(0, ip_address)
            data_type = 'RuleCreated'
            data = copy.deepcopy(new_rule)

        elif req[0] == 'Update':
            colored_print_controller(' received request for a new update')
            if self.times == 1:
                designated_ip = sorted(list(self.backup_forwarding_table.keys()))[0]
                delete_rule(designated_ip, self.backup_forwarding_table)
                data_type = 'RuleDeleted'
                data = copy.deepcopy(self.backup_forwarding_table)

            elif self.times == 2:
                designated_ip = sorted(list(self.backup_forwarding_table.keys()))[0]
                suspend_rule(designated_ip, self.backup_forwarding_table)
                data_type = 'RuleSuspended'
                data = copy.deepcopy(self.backup_forwarding_table)

            elif self.times == 5:
                designated_ip = sorted(list(self.backup_forwarding_table.keys()))[0]
                activate_rule(designated_ip, self.backup_forwarding_table)
                data_type = 'RuleActivated'
                data = copy.deepcopy(self.backup_forwarding_table)

            elif (self.times == 3) and (not self.set_pol):
                self.set_pol = True
                self.backup_policy_table = install_policy(self.ip_addresses)
                data_type = 'PoliciesActivated'
                data = copy.deepcopy(self.backup_policy_table)

            elif (self.times == 4) and (not self.set_mob):
                self.set_mob = True
                self.backup_mobility_table = install_mobility(self.ip_addresses)
                data_type = 'MobilityActivated'
                data = copy.deepcopy(self.backup_mobility_table)

            elif self.times == 6:
                def tcp_filter(packet):
                    return (packet[pk.PAYLOAD])[pk.PROTOCOL] == 'TCP'

                install_filter(self.backup_filters, tcp_filter)
                data_type = 'FilterActivated'
                data = copy.deepcopy(self.backup_filters)
            else:
                data_type = 'NoUpdates'

            self.times = (self.times + 1) % 7

        return data_type, data
