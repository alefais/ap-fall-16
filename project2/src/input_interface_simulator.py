# Author: Alessandra Fais
# Mat: 481017
# Computer Science and Networking
# Advanced Programming 2016/17
# Homework 2

from constants import packet_key as pk


class InputInterfaceSimulator:
    """
    The input interface simulator class: it simulates
    the arrive of the packets into the input interfaces
    of the switch.
    """

    def __init__(self, input_interfaces, ip_addresses):
        """
        The constructor.
        :param input_interfaces: the list of the input interfaces of the switch
        :param ip_addresses: the network
        """
        self.interfaces = input_interfaces
        self.ip_addresses = ip_addresses

    def gen(self):
        """
        Given the list of the input interfaces of the switch
        and the list of the ip addresses (the network), generates
        a packet and yield a pair (if, p) with the
        meaning of "packet p arrived into the input interface if
        of the switch".
        :return: yield a pair containing the input interface and the packet
        """
        gen = True
        count1 = 0
        count2 = len(self.ip_addresses) - 1
        iteration = 0
        while gen:
            iteration += 1

            protocol = 'TCP'
            op_type = 'write'
            if count1 % 2 == 0:
                protocol = 'UDP'
                op_type = 'read'

            header = {pk.SRC_IP: self.ip_addresses[count1], pk.DEST_IP: self.ip_addresses[count2]}
            payload = {pk.PROTOCOL: protocol, pk.OP_TYPE: op_type}

            packet = {pk.HEADER: header, pk.PAYLOAD: payload}

            interface = self.interfaces[(iteration + 5) % len(self.interfaces)]

            if count1 == len(self.ip_addresses) - 1:
                count1 = 0
            else:
                count1 += 1
            if count2 == 0:
                count2 = len(self.ip_addresses) - 1
            else:
                count2 -= 1

            if iteration == 20:
                gen = False  # Number of packets to be generated

            yield (interface, packet)
