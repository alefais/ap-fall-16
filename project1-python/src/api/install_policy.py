# Author: Alessandra Fais
# Mat: 481017
# Computer Science and Networking
# Advanced Programming 2016/17
# Homework 2

from constants import policy_key as pk


def install_policy(ip_addresses):
    """
    Function that constructs a table to manage the security policies:
    associates to a pair (source ip address, destination ip address) two
    counters (the number of allowed read and write operations from the
    source to the destination).
    :param ip_addresses: the network
    :return: a new table of policies
    """
    policy_table = {}
    for ip1 in ip_addresses:
        for ip2 in ip_addresses:
            if not (ip1 == ip2):
                policy_table[(ip1, ip2)] = {pk.READ_COUNTER: 2, pk.WRITE_COUNTER: 1}
            else:
                policy_table[(ip1, ip2)] = {pk.READ_COUNTER: 1, pk.WRITE_COUNTER: 1}
    return policy_table
