# Author: Alessandra Fais
# Mat: 481017
# Computer Science and Networking
# Advanced Programming 2016/17
# Homework 2

from constants import rule_key as rk, packet_key as pk, policy_key as polk, colors as col


def print_forwarding_table(dic):
    print('+ ----------------------------------------- +')
    print('|              FORWARDING TABLE             |')
    print('+ ----------------------------------------- +')
    print('IP destination\tOUT interface\tstate')
    [print(str(x) + '\t      ' + str((dic[x])[rk.DEST_PORT]) + '\t\t' + str((dic[x])[rk.STATE]))
     for x in sorted(dic.keys())]
    print()


def print_out_packet_list(lis):
    print('+ ----------------------------------------- +')
    print('|            OUTPUT PACKET LIST             |')
    print('+ ----------------------------------------- +')
    print('IP source\tIP destination\t  Protocol\tOP type\t  OUT interface')
    [print(str(x[1][pk.HEADER][pk.SRC_IP]) + '\t' + str(x[1][pk.HEADER][pk.DEST_IP]) + '\t     ' +
           str(x[1][pk.PAYLOAD][pk.PROTOCOL]) + '\t ' + str(x[1][pk.PAYLOAD][pk.OP_TYPE]) + '\t\t' + str(x[0]))
     for x in lis]
    print()


def print_dropped_packet_list(lis):
    print('+ ----------------------------------------- +')
    print('|            DROPPED PACKET LIST            |')
    print('+ ----------------------------------------- +')
    print('IP source\tIP destination\t  Protocol\tOP type\t   Cause')
    [print(str(x[1][pk.HEADER][pk.SRC_IP]) + '\t' + str(x[1][pk.HEADER][pk.DEST_IP]) + '\t     ' +
           str(x[1][pk.PAYLOAD][pk.PROTOCOL]) + '\t ' + str(x[1][pk.PAYLOAD][pk.OP_TYPE]) + '\t   ' + str(x[0]))
     for x in lis]
    print()


def print_packet(n, x):
    colored_print_switch(' processing packet ' + str(n) + '...')
    print('IP source\tIP destination\t  Protocol\tOP type\t   IN interface')
    print(str(x[1][pk.HEADER][pk.SRC_IP]) + '\t' + str(x[1][pk.HEADER][pk.DEST_IP]) + '\t     ' +
          str(x[1][pk.PAYLOAD][pk.PROTOCOL]) + '\t ' + str(x[1][pk.PAYLOAD][pk.OP_TYPE]) + '\t\t' + str(x[0]))
    print()


def print_policy_table(dic):
    print('+ ----------------------------------------- +')
    print('|                POLICY TABLE               |')
    print('+ ----------------------------------------- +')
    print('IP source\tIP destination\tRead counter\tWrite counter')
    [print(str(x[0]) + '\t' + str(x[1]) + '\t     ' + str((dic[x])[polk.READ_COUNTER]) + '\t\t     ' + str(
        (dic[x])[polk.WRITE_COUNTER]))
     for x in sorted(dic.keys())]
    print()


def print_mobility_table(dic):
    print('+ ----------------------------------------- +')
    print('|              MOBILITY TABLE               |')
    print('+ ----------------------------------------- +')
    print('IP old\t\tIP new')
    [print(str(x) + '\t' + str(dic[x])) for x in sorted(dic.keys())]
    print()


def colored_print_switch(s):
    print(col.YELLOW + col.BOLD + 'SWITCH:' + col.ENDC + s)


def colored_print_controller(s):
    print(col.BLUE + col.BOLD + 'CONTROLLER:' + col.ENDC + s)
