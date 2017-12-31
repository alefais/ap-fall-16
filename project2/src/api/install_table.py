# Author: Alessandra Fais
# Mat: 481017
# Computer Science and Networking
# Advanced Programming 2016/17
# Homework 2

from constants import rule_key as rk


def install(ip_addresses, interfaces):
    """
    Function that constructs a new forwarding table
    starting from a list of ip addresses (the network)
    and a list of output interfaces of the switch.
    :param ip_addresses: the network
    :param interfaces: the output interfaces
    :return: a new forwarding table
    """
    forwarding_table = {}
    count = 0
    for ip in ip_addresses:
        forwarding_table[ip] = {rk.DEST_PORT: interfaces[count % len(interfaces)], rk.STATE: True}
        count += 1
    return forwarding_table
