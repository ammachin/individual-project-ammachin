"""

Class for using CrazyFlie

"""

import math
import numpy as np
import time

from threading import Event

import cflib.crtp
from cflib.crazyflie import Crazyflie
from cflib.crazyflie.log import LogConfig
from cflib.crazyflie.syncCrazyflie import SyncCrazyflie
from cflib.utils import uri_helper

class MobilityClass:
    deck_attached_event = Event()
    position_estimate = [0.0, 0.0, 0.0]
    URI = uri_helper.uri_from_env(default="radio://0/80/2M/E7E7E7E7E7")

    """
    Intialising class.
    """
    def __init__(self):
        cflib.crtp.init_drivers()

    """
    Callback to see if Loco Positioning Deck is attached.
    """
    def param_deck_loco(self, _, value_str):
        value = int(value_str)
        if(__debug__):
            print(value)

        if(value):
            self.deck_attached_event.set()
            print("Deck is attached")
        else:
            print("Deck is not attached")

    """
    Callback for finding current CrazyFlie position.
    """
    def log_pos_callback(self, timestamp, data, log_conf):
        self.position_estimate[0] = data["kalman.stateX"]
        self.position_estimate[1] = data["kalman.stateY"]
        self.position_estimate[2] = data["kalman.stateZ"]

        #print(f"Position: ({self.position_estimate[0]}, {self.position_estimate[1]}, {self.position_estimate[2]})")

    """
    Initiate the logging of CrazyFlie position coordinates
    """
    def init_log_config(self, scf: SyncCrazyflie):
        log_conf = LogConfig(name="Position", period_in_ms=10)
        log_conf.add_variable("kalman.stateX", "float")
        log_conf.add_variable("kalman.stateY", "float")
        log_conf.add_variable("kalman.stateZ", "float")

        scf.cf.log.add_config(log_conf)
        log_conf.data_received_cb.add_callback(self.log_pos_callback)
        log_conf.start()

    """
    Check to see whether the drone flight path is within the drone test area.
    """
    def check_pos(self, position):
        # Check x
        min_x = 0.2
        max_x = 6.6

        if(position[0] < min_x):
            position[0] = min_x
        elif(position[0] > max_x):
            position[0] = max_x

        # Check y
        min_y = 0.2
        max_y = 3.6

        if(position[1] < min_y):
            position[1] = min_y
        elif(position[1] > max_y):
            position[1] = max_y

        # Check z
        min_z = 0.1
        max_z = 3.7

        if(position[2] < min_z):
            position[2] = min_z
        if(position[2] > max_z):
            position[2] = max_z

        return position


    """
    Take-off function
    """
    def take_off(self, scf: SyncCrazyflie, position):
        cf = scf.cf

        take_off_time = 1.0
        sleep_time = 0.1
        steps = int(take_off_time / sleep_time)

        if(position[2] <= 1.0):
            vel = position[2] / take_off_time
        else:
            vel = 1.0 / take_off_time

        print(f"Take off at {position[2]}")

        for i in range(steps):
            cf.commander.send_velocity_world_setpoint(0, 0, vel, 0)
            time.sleep(sleep_time)

        if(position[2] > 1.0):
            for i in range(steps):
                cf.commander.send_position_setpoint(self.position_estimate[0], self.position_estimate[1], position[2], 0.0)
                time.sleep(sleep_time)

    """
    Hover function.
    """
    def hover(self, scf: SyncCrazyflie, hover_time):
        cf = scf.cf

        print(f"Hovering for {hover_time} seconds")
        sleep_time = 0.1
        steps = int(hover_time / sleep_time)
        for i in range(steps):
            cf.commander.send_hover_setpoint(0.0, 0.0, 0.0, 0.5)
            time.sleep(sleep_time)

    """
    Landing function.
    """
    def land(self, scf: SyncCrazyflie):
        cf = scf.cf
        sleep_time = 0.1

        print("Initiating landing")

        change = self.position_estimate[2]
        while(self.position_estimate[2] > 0.2):
            time.sleep(sleep_time)
            cf.commander.send_hover_setpoint(0.0, 0.0, 0.0, 0.0)
            time.sleep(sleep_time)
            cf.commander.send_position_setpoint(self.position_estimate[0], self.position_estimate[1], change, 0.0)
            change -= 0.1

    """
    Go-to function
    """
    def go_to(self, scf: SyncCrazyflie, position):
        cf = scf.cf
        sleep_time = 0.1

        print(f"Intiating go-to sequence: Flying to ({position[0]}, {position[1]}, {position[2]})")
        for i in range(50):
            cf.commander.send_position_setpoint(position[0], position[1], position[2], 0.0)
            time.sleep(sleep_time)