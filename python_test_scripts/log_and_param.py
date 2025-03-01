"""

Following Bitcraze tutorial for connecting to CrazyFlie and logging parameters.

Test script (untested).

"""

import logging
import time

# CrazyFlie libraries
import cflib.crtp # Scanning for CrazyFlie instances
from cflib.crazyflie import Crazyflie
from cflib.crazyflie.syncCrazyflie import SyncCrazyflie
from cflib.utils import uri_helper
from cflib.crazyflie.log import LogConfig
from cflib.crazyflie.syncLogger import SyncLogger

# Radio uri of CrazyFlie for connection purposes
uri = uri_helper.uri_from_env(default="radio://0/80/2M/E7E7E7E7E7") # TODO: checl what uri our Crazylie has

# Only show errors from Crazyflie logging
logging.basicConfig(level=logging.ERROR)


def log_stabilizer_callback(timestamp, data, log_conf):
    print('[%d][%s]: %s' % (timestamp, log_conf.name, data))


def log_cf_async(scf, log_conf):
    cf = scf.cf
    cf.log.add_config(log_conf) # add config to CrazyFlie logging framework

    log_conf.data_received_cb.add_callback(log_stabilizer_callback)
    
    # manually start/stop log config
    log_conf.start()
    time.sleep(5)
    log_conf.stop()


def param_stabilizer_estimator_callback(name, value):
    print("The CrazyFlie has parameter " + name + "set at value: " + value)

# Reading and setting parameters
def param_async(scf, group_str, name_str):
    cf = scf.cf
    name = group_str + "." + name_str

    cf.param.add_update_callback(group=group_str, name=name_str, cb=param_stabilizer_estimator_callback)
    time.sleep(1) # wait for CrazyFlie response instead of immediate connection loss

    cf.param.set_value(name, 2) # for setting parameters
    # check which parameters can be set in parameter TOC in CFclient
    time.sleep(1)
    cf.param.set_value(name, 1)


"""
Logging function

Parameters
----------
    scf - SyncCrazyFlie instance
    log_conf - Logging configuration
"""
def log_cf(scf, log_conf):
    with SyncLogger(scf, log_conf) as logger:
        for entry in logger:
            timestamp = entry[0]
            data = entry[1]
            log_conf_name = entry[2]

            print('[%d][%s]: %s' % (timestamp, log_conf_name, data))

            break


def connect_cf():
    print("Successfully connected to CrazyFlie.")


if __name__ == "__main__":
    # Initialise drivers
    cflib.crtp.init_drivers()

    # Define logging config for CrazyFlie stabiliser 
    log_stabilizer = LogConfig(name="Stabilizer", period_in_ms=10)
    log_stabilizer.add_variable("stabilizer.roll", "float")
    log_stabilizer.add_variable("stabilizer.pitch", "float")
    log_stabilizer.add_variable("stabilizer.yaw", "float")

    group = "stabilizer"
    name = "estimator"

    # Create synchronous Crazyflie instance
    with SyncCrazyflie(uri, cf=Crazyflie(rw_cache="./cache")) as scf:
        connect_cf()
        #log_cf_async(scf, log_stabilizer)
        param_async(scf, group, name)