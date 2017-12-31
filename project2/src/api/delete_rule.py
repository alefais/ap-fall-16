# Author: Alessandra Fais
# Mat: 481017
# Computer Science and Networking
# Advanced Programming 2016/17
# Homework 2


def delete_rule(key_addr_dest, forwarding_table):
    """
    Function that removes a rule from the forwarding table.
    :param key_addr_dest: the ip corresponding to the rule to be removed
    :param forwarding_table: the current forwarding table
    :return: None
    """
    del forwarding_table[key_addr_dest]
