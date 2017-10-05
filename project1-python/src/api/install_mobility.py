# Author: Alessandra Fais
# Mat: 481017
# Computer Science and Networking
# Advanced Programming 2016/17
# Homework 2


def install_mobility(ip_addresses):
    """
    Function that constructs a table to manage the mobility:
    each entry associates an old ip address to a new ip address.
    When the switch elaborates a packet addressed to the old ip
    address, and there is an entry in the mobility table, this
    means that that packet has to be forwarded to the new ip address.
    :param ip_addresses: the network
    :return: a new table for the mobility
    """
    mobility_table = {}
    count = 2
    while count < len(ip_addresses):
        mobility_table[ip_addresses[count]] = ip_addresses[count - 2]
        count += 2
    return mobility_table
