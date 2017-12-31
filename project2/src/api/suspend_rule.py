# Author: Alessandra Fais
# Mat: 481017
# Computer Science and Networking
# Advanced Programming 2016/17
# Homework 2

from constants import rule_key as rk


def suspend_rule(key_addr_dest, forwarding_table):
    """
    Function that suspends a rule of the forwarding table.
    :param key_addr_dest: the ip corresponding to the rule to be suspended
    :param forwarding_table: the current forwarding table
    :return: None
    """
    (forwarding_table[key_addr_dest])[rk.STATE] = False
