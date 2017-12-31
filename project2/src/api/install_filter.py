# Author: Alessandra Fais
# Mat: 481017
# Computer Science and Networking
# Advanced Programming 2016/17
# Homework 2

from constants import packet_key as p
from ipaddress import ip_address


def install_filter(filter_list, fil):
    """
    Function that allows to install a filter.
    :param filter_list: the list of the filters
    :param fil: the filter to install
    :return: True if fil is a well formed filter (filter: packet -> bool)
            and can be installed correctly, False otherwise
    """
    test_header = {p.SRC_IP: ip_address('127.0.0.1'), p.DEST_IP: ip_address('127.0.0.1')}
    test_payload = {p.PROTOCOL: 'TCP', p.OP_TYPE: 'write'}
    test_packet = {p.HEADER: test_header, p.PAYLOAD: test_payload}

    try:
        result = fil(test_packet)
        if isinstance(result, bool):
            filter_list.append(fil)
            return True
        else:
            return False
    except:
        return False
